package com.example.soundscapemapper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
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
        crearCanalNotificaciones()
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
                            lanzarAlertaRuidoExcesivo(db)
                            ultimaNotificacionTime = tiempoActual
                        }
                    }
                    delay(1000)
                }
            }
        }

        return START_STICKY
    }

    private fun lanzarAlertaRuidoExcesivo(db: Double) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val alerta = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Cuidado: entorno ruidoso")
            .setContentText(
                "Se detectaron ${db.toInt()} dB. Si vas a quedarte más de 15 minutos, " +
                    "considera usar protección auditiva."
            )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2, alerta)
    }

    private fun crearNotificacionServicio(texto: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Espacio Seguro Activo")
            .setContentText(texto)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Monitoreo de Ruido Ambiental",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AudioEngine.liberar(this)
        serviceScope.cancel()
    }

    companion object {
        const val CHANNEL_ID = "canal_sonido_salud"
    }
}
