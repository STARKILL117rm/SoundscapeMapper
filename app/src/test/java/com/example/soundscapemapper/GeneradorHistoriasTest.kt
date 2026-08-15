package com.example.soundscapemapper

import com.example.soundscapemapper.ui.historias.GeneradorHistorias
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneradorHistoriasTest {

    private val mediciones = listOf(
        Medicion(
            id = 1,
            nombreLugar = "Cafetería",
            categoria = "Estresante",
            decibelios = 88.4,
            nivelLuz = 300f,
            latitud = 20.048,
            longitud = -98.817,
            fechaHora = "13/08/2026 09:00"
        ),
        Medicion(
            id = 2,
            nombreLugar = "Biblioteca",
            categoria = "Tranquilo",
            decibelios = 45.2,
            nivelLuz = 120f,
            latitud = 20.05,
            longitud = -98.82,
            fechaHora = "13/08/2026 10:00"
        )
    )

    private val registrosSemana = listOf(
        RegistroExposicion(fecha = "2026-08-11", minutosSobre65 = 30.0, minutosSobre80 = 5.0, nivelMaximoDb = 88.0),
        RegistroExposicion(fecha = "2026-08-12", minutosSobre65 = 60.0, minutosSobre80 = 10.0, nivelMaximoDb = 92.0),
        RegistroExposicion(fecha = "2026-08-13", minutosSobre65 = 120.0, minutosSobre80 = 0.0, nivelMaximoDb = 79.0)
    )

    @Test
    fun generaInformesPersonalizadosConDatos() {
        val historias = GeneradorHistorias.generar(
            decibeliosAhora = 70.0,
            nivelMaximoHoy = 79.0,
            minutosSobre65Hoy = 120.0,
            minutosSobre80Hoy = 0.0,
            puntosHoy = 60.0,
            registrosSemana = registrosSemana,
            mediciones = mediciones,
            diaDelAnio = 100
        )

        val ids = historias.map { it.id }
        assertTrue(ids.any { it.startsWith("estado_actual_") })
        assertTrue(ids.any { it.startsWith("dosis_hoy_") })
        assertTrue(ids.any { it.startsWith("semana_") })
        assertTrue(ids.any { it.startsWith("lugares_") })
        assertTrue(ids.any { it.startsWith("sabias_") })
        assertTrue(ids.any { it.startsWith("educativa_") })

        val dosis = historias.first { it.id.startsWith("dosis_hoy_") }
        assertEquals("60 / 480 pts", dosis.paginas.first().datoDestacado)

        val semana = historias.first { it.id.startsWith("semana_") }
        assertEquals("255 pts / 7 días", semana.paginas.first().datoDestacado)

        val lugares = historias.first { it.id.startsWith("lugares_") }
        assertTrue(lugares.paginas.any { it.datoDestacado == "88 dB" })
        assertTrue(lugares.paginas.any { it.cuerpo.contains("Biblioteca") })
    }

    @Test
    fun sinDatosSigueGenerandoHistoriasAmigables() {
        val historias = GeneradorHistorias.generar(
            decibeliosAhora = 30.0,
            nivelMaximoHoy = 0.0,
            minutosSobre65Hoy = 0.0,
            minutosSobre80Hoy = 0.0,
            puntosHoy = 0.0,
            registrosSemana = emptyList(),
            mediciones = emptyList(),
            diaDelAnio = 5
        )

        assertFalse(historias.isEmpty())
        assertTrue(historias.any { it.id.startsWith("estado_actual_") })
        assertTrue(historias.any { it.id.startsWith("dosis_hoy_") })
        assertTrue(historias.any { it.id.startsWith("sabias_") })

        val lugares = historias.first { it.id.startsWith("lugares_") }
        assertTrue(lugares.paginas.first().cuerpo.contains("primer lugar"))
    }

    @Test
    fun lasSabiasRotanSegunElDia() {
        val conDia1 = GeneradorHistorias.generar(
            decibeliosAhora = 30.0, nivelMaximoHoy = 0.0,
            minutosSobre65Hoy = 0.0, minutosSobre80Hoy = 0.0,
            puntosHoy = 0.0, registrosSemana = emptyList(),
            mediciones = emptyList(), diaDelAnio = 1
        )
        val conDia2 = GeneradorHistorias.generar(
            decibeliosAhora = 30.0, nivelMaximoHoy = 0.0,
            minutosSobre65Hoy = 0.0, minutosSobre80Hoy = 0.0,
            puntosHoy = 0.0, registrosSemana = emptyList(),
            mediciones = emptyList(), diaDelAnio = 2
        )

        val sabias1 = conDia1.first { it.id.startsWith("sabias_") }
        val sabias2 = conDia2.first { it.id.startsWith("sabias_") }
        assertFalse(sabias1.id == sabias2.id)
        assertFalse(sabias1.paginas.first().cuerpo == sabias2.paginas.first().cuerpo)
    }

    @Test
    fun laEducativaRotaCadaSemana() {
        fun educativa(dia: Int) = GeneradorHistorias.generar(
            decibeliosAhora = 30.0, nivelMaximoHoy = 0.0,
            minutosSobre65Hoy = 0.0, minutosSobre80Hoy = 0.0,
            puntosHoy = 0.0, registrosSemana = emptyList(),
            mediciones = emptyList(), diaDelAnio = dia
        ).first { it.id.startsWith("educativa_") }.id

        assertEquals("educativa_escala", educativa(1))
        assertEquals("educativa_dosis", educativa(8))
        assertFalse(educativa(1) == educativa(8))
    }

    @Test
    fun dosisAltaGeneraHistoriaDeDiaPesado() {
        val historias = GeneradorHistorias.generar(
            decibeliosAhora = 82.0,
            nivelMaximoHoy = 95.0,
            minutosSobre65Hoy = 400.0,
            minutosSobre80Hoy = 100.0,
            puntosHoy = 400.0,
            registrosSemana = registrosSemana,
            mediciones = mediciones,
            diaDelAnio = 1
        )
        assertTrue("Se esperaba la historia 'día pesado'", historias.any { it.id.startsWith("dia_pesado_") })
    }

    @Test
    fun dosisBajaNoGeneraHistoriaDeDiaPesado() {
        val historias = GeneradorHistorias.generar(
            decibeliosAhora = 30.0,
            nivelMaximoHoy = 0.0,
            minutosSobre65Hoy = 0.0,
            minutosSobre80Hoy = 0.0,
            puntosHoy = 60.0,
            registrosSemana = emptyList(),
            mediciones = emptyList(),
            diaDelAnio = 1
        )
        assertFalse(historias.any { it.id.startsWith("dia_pesado_") })
    }
}
