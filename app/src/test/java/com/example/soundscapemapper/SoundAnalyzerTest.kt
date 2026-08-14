package com.example.soundscapemapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class SoundAnalyzerTest {

    @Test
    fun silencioProduceNivelMinimo() {
        val buffer = ShortArray(4410)
        val db = SoundAnalyzer.calcularDbA(buffer, buffer.size)
        assertEquals(20.0, db, 0.001)
    }

    @Test
    fun senalFuerteProduceNivelAlto() {
        val buffer = ShortArray(4410)
        for (i in buffer.indices) {
            buffer[i] = (sin(2 * PI * 440 * i / 44100.0) * 20000).toInt().toShort()
        }
        val db = SoundAnalyzer.calcularDbA(buffer, buffer.size)
        assertTrue("Se esperaba nivel alto, pero fue $db dB", db > 80.0)
    }

    @Test
    fun senalMediaProduceNivelIntermedio() {
        val buffer = ShortArray(4410)
        for (i in buffer.indices) {
            buffer[i] = (sin(2 * PI * 440 * i / 44100.0) * 1500).toInt().toShort()
        }
        val db = SoundAnalyzer.calcularDbA(buffer, buffer.size)
        assertTrue("Nivel esperado entre 60 y 80, pero fue $db dB", db in 60.0..80.0)
    }

    @Test
    fun clasificacionRespetaUmbralesOMS() {
        assertEquals(NivelRuido.SEGURO, SoundAnalyzer.clasificarNivel(50.0))
        assertEquals(NivelRuido.PRECAUCION, SoundAnalyzer.clasificarNivel(70.0))
        assertEquals(NivelRuido.PELIGRO, SoundAnalyzer.clasificarNivel(85.0))
    }

    @Test
    fun redondeoHaciaArriba() {
        assertEquals(67.3, SoundAnalyzer.redondear(67.3456, 1), 0.0)
    }
}
