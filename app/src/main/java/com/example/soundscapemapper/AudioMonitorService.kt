package com.example.soundscapemapper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class AudioMonitorService : Service() {

    private var isMonitoring = false
    private var audioRecord: AudioRecord? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var ultimaNotificacionTime = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        crearCanalNotificaciones()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = crearNotificacionServicio("Monitoreando salud auditiva en segundo plano...")
        startForeground(1, notification)

        if (!isMonitoring) {
            iniciarMonitoreoAudio()
        }

        return START_STICKY
    }

    private fun iniciarMonitoreoAudio() {
        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC, sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize
            )
            audioRecord?.startRecording()
            isMonitoring = true

            val contexto = applicationContext
            val umbralAlerta = Configuracion.umbralAlerta(contexto)

            serviceScope.launch {
                val buffer = ShortArray(bufferSize)
                while (isMonitoring) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        val db = SoundAnalyzer.calcularDbA(buffer, readSize)

                        // Umbral de alerta configurable por el usuario (por defecto 80 dB)
                        if (db >= umbralAlerta) {
                            val tiempoActual = System.currentTimeMillis()
                            // Evitar spam de notificaciones: máximo 1 alerta cada 2 minutos
                            if (tiempoActual - ultimaNotificacionTime > 120_000) {
                                lanzarAlertaRuidoExcesivo(db)
                                ultimaNotificacionTime = tiempoActual
                            }
                        }
                    }
                    delay(1000) // Evaluar cada segundo para ahorrar batería
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun lanzarAlertaRuidoExcesivo(db: Double) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val alerta = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⚠️ ¡Nivel de ruido peligroso!")
            .setContentText("Se detectaron ${db.toInt()} dB. Por tu salud auditiva, se recomienda buscar un espacio más tranquilo.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
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
        isMonitoring = false
        audioRecord?.stop()
        audioRecord?.release()
        serviceScope.cancel()
    }

    companion object {
        const val CHANNEL_ID = "canal_sonido_salud"
    }
}