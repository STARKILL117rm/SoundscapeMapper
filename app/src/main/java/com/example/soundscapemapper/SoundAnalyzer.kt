package com.example.soundscapemapper

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.log10

enum class NivelRuido(val etiqueta: String, val colorHex: Long, val recomendacion: String) {
    SEGURO(
        "Seguro",
        0xFF2E7D32,
        "Ambiente cómodo y adecuado para la concentración. Disfruta del espacio."
    ),
    PRECAUCION(
        "Precaución",
        0xFFF9A825,
        "Ruido moderado. La exposición prolongada puede causar fatiga y estrés."
    ),
    PELIGRO(
        "Peligro",
        0xFFC62828,
        "Nivel elevado. Aléjate del lugar o usa protección auditiva."
    )
}

object SoundAnalyzer {

    const val UMBRAL_OMS_SEGURO = 65.0
    const val UMBRAL_OMS_PELIGRO = 80.0

    // Meta diaria de dosis: 480 puntos equivalen a 8 horas a 65 dB (referencia OMS).
    const val META_DOSIS_PUNTOS = 480.0

    // Factor de calibración para aproximar dB SPL a partir de la señal del micrófono.
    // Convierte el nivel RMS relativo (dBFS) a una escala de presión sonora usable.
    private const val OFFSET_CALIBRACION_DB = 94.0
    private const val MAX_AMPLITUD = 32767.0
    private const val MIN_DB = 20.0
    private const val MAX_DB = 120.0

    /**
     * Calcula un nivel de presión sonora aproximado en dB a partir de una ventana
     * de muestras PCM de 16 bits. Usa RMS -> dBFS -> escala dB con calibración.
     */
    fun calcularDbA(buffer: ShortArray, readSize: Int): Double {
        if (readSize <= 0) return MIN_DB
        var sumaCuadrados = 0.0
        for (i in 0 until readSize) {
            val valor = buffer[i].toDouble()
            sumaCuadrados += valor * valor
        }
        val rms = Math.sqrt(sumaCuadrados / readSize)
        if (rms <= 0.0) return MIN_DB
        val db = OFFSET_CALIBRACION_DB + 20.0 * log10(rms / MAX_AMPLITUD)
        return db.coerceIn(MIN_DB, MAX_DB)
    }

    /** Clasifica el nivel según los umbrales de referencia de la OMS. */
    fun clasificarNivel(db: Double): NivelRuido = when {
        db < UMBRAL_OMS_SEGURO -> NivelRuido.SEGURO
        db < UMBRAL_OMS_PELIGRO -> NivelRuido.PRECAUCION
        else -> NivelRuido.PELIGRO
    }

    /** Redondea un valor a la cantidad de decimales indicada (HALF_UP). */
    fun redondear(valor: Double, decimales: Int): Double =
        BigDecimal(valor).setScale(decimales, RoundingMode.HALF_UP).toDouble()

    /**
     * Puntos de dosis de ruido acumulados.
     * 1 minuto entre 65-80 dB = 1 punto; 1 minuto >= 80 dB = 4 puntos.
     * (minutosSobre65 ya incluye los >= 80, por eso se ponderan aparte).
     */
    fun puntosDosis(minutosSobre65: Double, minutosSobre80: Double): Double =
        minutosSobre65 + 3.0 * minutosSobre80
}
