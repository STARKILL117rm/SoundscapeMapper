package com.example.soundscapemapper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Genera el resumen nocturno de la dosis de ruido del día con un tono empático.
 * Se programa para ejecutarse cerca de las 8:00 PM.
 */
class ResumenNocturnoWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val db = AppDatabase.getDatabase(applicationContext)
        val registro = try {
            db.registroExposicionDao().obtenerPorFecha(fecha)
        } catch (e: Exception) {
            null
        }
        if (registro == null) return Result.success()

        val puntos = SoundAnalyzer.puntosDosis(registro.minutosSobre65, registro.minutosSobre80)
        val progreso = (puntos / SoundAnalyzer.META_DOSIS_PUNTOS).coerceIn(0.0, 1.0)

        val mensaje = when {
            progreso >= 1.0 ->
                "Tu dosis de ruido llegó al ${(progreso * 100).toInt()}% hoy. ¡Hora de desconectar y descansar!"
            progreso >= 0.5 ->
                "Llevas el ${(progreso * 100).toInt()}% de tu dosis de ruido. Un rato de silencio vendría bien."
            else ->
                "Tu oído descansó bien hoy. Sigue eligiendo espacios tranquilos."
        }

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CANAL_RESUMEN,
                    "Resumen de tu día",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }

        val notificacion = NotificationCompat.Builder(applicationContext, CANAL_RESUMEN)
            .setContentTitle("Resumen de tu día")
            .setContentText(mensaje)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        manager.notify(3, notificacion)

        return Result.success()
    }

    companion object {
        private const val CANAL_RESUMEN = "canal_resumen_nocturno"
    }
}
