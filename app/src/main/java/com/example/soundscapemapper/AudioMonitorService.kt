package com.example.soundscapemapper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano que mantiene la captura de audio activa aunque la app
 * esté en segundo plano. La lectura del micrófono la realiza [AudioEngine] (el
 * único dueño del AudioRecord); aquí solo se conserva la notificación de servicio
 * y se disparan alertas empáticas cuando el ruido supera el umbral del usuario.
 */
class AudioMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var alertaJob: Job? = null
    private var ultimaNotificacionTime = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        crearCanalServicio()
        crearCanalAlertas()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = crearNotificacionServicio("Monitoreando tu salud auditiva en segundo plano…")
        startForeground(1, notification)

        AudioEngine.adquirir(this)

        if (alertaJob?.isActive != true) {
            alertaJob = serviceScope.launch {
                val umbralAlerta = Configuracion.umbralAlerta(this@AudioMonitorService)
                while (true) {
                    val db = AudioEngine.decibelios.value
                    if (db >= umbralAlerta) {
                        val tiempoActual = System.currentTimeMillis()
                        // Evitar spam: máximo 1 alerta cada 2 minutos.
                        if (tiempoActual - ultimaNotificacionTime > 120_000) {
                            lanzarAlertaRuidoExcesivo(db, umbralAlerta)
                            vibrarAlarma()
                            ultimaNotificacionTime = tiempoActual
                        }
                    }
                    delay(1000)
                }
            }
        }

        return START_STICKY
    }

    private fun lanzarAlertaRuidoExcesivo(db: Double, umbral: Double) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent para abrir la app al tocar la notificación
        val abrirApp = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val emoji = when {
            db >= 90 -> "🔴"
            db >= 80 -> "🟠"
            else     -> "🟡"
        }

        val consejo = when {
            db >= 90 -> "Usa protección auditiva de inmediato o retírate del área."
            db >= 80 -> "Considera alejarte o reducir la exposición en los próximos minutos."
            else     -> "El nivel está cerca de tu umbral de alerta (${umbral.toInt()} dB)."
        }

        val alerta = NotificationCompat.Builder(this, CHANNEL_ALERTAS)
            .setContentTitle("$emoji Ruido Excesivo Detectado — ${db.toInt()} dB")
            .setContentText(consejo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(consejo))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(abrirApp)
            .build()

        notificationManager.notify(ID_NOTIF_ALERTA, alerta)
    }

    private fun vibrarAlarma() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 300, 100, 300), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 100, 300), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 300, 100, 300), -1)
                }
            }
        } catch (_: Exception) { /* Si falla la vibración, ignorar */ }
    }

    private fun crearNotificacionServicio(texto: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_SERVICIO)
            .setContentTitle("🎙️ Espacio Seguro Activo")
            .setContentText(texto)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /** Canal de baja importancia para la notificación persistente del servicio. */
    private fun crearCanalServicio() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_SERVICIO,
                "Monitoreo en segundo plano",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación persistente que indica que el monitoreo de ruido está activo."
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    /** Canal de alta importancia para alertas de ruido excesivo — hace sonido y vibra. */
    private fun crearCanalAlertas() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ALERTAS,
                "Alertas de Ruido Excesivo",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas inmediatas cuando el nivel de ruido supera tu umbral configurado."
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 100, 300)
                enableLights(true)
                lightColor = android.graphics.Color.RED
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AudioEngine.liberar(this)
        serviceScope.cancel()
    }

    companion object {
        const val CHANNEL_SERVICIO = "canal_servicio_monitoreo"
        const val CHANNEL_ALERTAS  = "canal_alertas_ruido"
        /** @deprecated Usa CHANNEL_SERVICIO o CHANNEL_ALERTAS según el caso. */
        const val CHANNEL_ID       = CHANNEL_SERVICIO
        private const val ID_NOTIF_ALERTA = 2
    }
}
