package com.example.soundscapemapper

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Programa el resumen nocturno de la dosis de ruido. Se ejecuta alrededor de
 * las 8:00 PM y se reprograma cada día a la misma hora (WorkManager lo repite
 * cada 24 horas a partir del primer disparo).
 */
object NotificacionesProgramador {

    private const val TRABAJO_NOCHE = "resumen_nocturno_diario"
    private const val HORA_RESUMEN = 20
    private const val MINUTO_RESUMEN = 0

    fun programar(context: Context) {
        val ahora = Calendar.getInstance()
        val siguiente = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, HORA_RESUMEN)
            set(Calendar.MINUTE, MINUTO_RESUMEN)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(ahora)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val delayMs = siguiente.timeInMillis - ahora.timeInMillis

        val request = PeriodicWorkRequestBuilder<ResumenNocturnoWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            TRABAJO_NOCHE,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
