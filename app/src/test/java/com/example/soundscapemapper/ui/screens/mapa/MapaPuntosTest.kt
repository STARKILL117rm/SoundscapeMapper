package com.example.soundscapemapper.ui.screens.mapa

import androidx.compose.ui.graphics.Color
import com.example.soundscapemapper.Medicion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapaPuntosTest {

    private fun medicion(
        nombre: String,
        db: Double,
        lat: Double,
        lon: Double,
        fechaHora: String,
        categoria: String = "Oficina"
    ) = Medicion(
        nombreLugar = nombre,
        categoria = categoria,
        decibelios = db,
        nivelLuz = 500f,
        latitud = lat,
        longitud = lon,
        fechaHora = fechaHora
    )

    @Test
    fun nivelParaDb_clasificaUmbralesOMS() {
        assertEquals(NivelRuido.SEGURO, nivelParaDb(55.0))
        assertEquals(NivelRuido.SEGURO, nivelParaDb(69.9))
        assertEquals(NivelRuido.MODERADO, nivelParaDb(70.0))
        assertEquals(NivelRuido.MODERADO, nivelParaDb(80.0))
        assertEquals(NivelRuido.RIESGO, nivelParaDb(80.1))
        assertEquals(NivelRuido.RIESGO, nivelParaDb(95.0))
    }

    @Test
    fun colorNivel_usaColoresHexDeLaOMS() {
        assertEquals(Color(0xFF10B981), colorNivel(NivelRuido.SEGURO))
        assertEquals(Color(0xFFF59E0B), colorNivel(NivelRuido.MODERADO))
        assertEquals(Color(0xFFEF4444), colorNivel(NivelRuido.RIESGO))
    }

    @Test
    fun estadoBadge_porNivel() {
        assertEquals("Seguro", estadoBadge(NivelRuido.SEGURO))
        assertEquals("Precaución", estadoBadge(NivelRuido.MODERADO))
        assertEquals("Riesgo", estadoBadge(NivelRuido.RIESGO))
    }

    @Test
    fun estanciasDemoPachuca_contienenLosCuatroEscenarios() {
        assertEquals(4, ESTANCIAS_DEMO_PACHUCA.size)

        val centro = ESTANCIAS_DEMO_PACHUCA[0]
        assertEquals("Centro / Reloj Monumental", centro.nombre)
        assertEquals("08:30", centro.horaInicio)
        assertEquals(35, centro.duracionMin)
        assertEquals(NivelRuido.RIESGO, centro.nivel)

        val oficina = ESTANCIAS_DEMO_PACHUCA[1]
        assertEquals("Oficina Zona Plateada", oficina.nombre)
        assertEquals(48.0, oficina.decibelios, 0.01)
        assertEquals(NivelRuido.SEGURO, oficina.nivel)

        val cafeteria = ESTANCIAS_DEMO_PACHUCA[2]
        assertEquals("Cafetería Centro Histórico", cafeteria.nombre)
        assertEquals(60, cafeteria.duracionMin)
        assertEquals(NivelRuido.SEGURO, cafeteria.nivel)

        val gimnasio = ESTANCIAS_DEMO_PACHUCA[3]
        assertEquals("Gimnasio", gimnasio.nombre)
        assertEquals("18:30", gimnasio.horaInicio)
        assertEquals(NivelRuido.MODERADO, gimnasio.nivel)

        assertTrue(ESTANCIAS_DEMO_PACHUCA.all { it.duracionMin > 0 })
        assertTrue(ESTANCIAS_DEMO_PACHUCA.all { it.latitud in 20.0..20.2 && it.longitud in -98.8..-98.7 })
    }

    @Test
    fun agruparEnEstancias_fusionaMismoLugar_ySeparaPorLugar() {
        val lista = listOf(
            medicion("Oficina", 50.0, 20.096, -98.770, "14/08/2026 09:00"),
            medicion("Oficina", 52.0, 20.096, -98.770, "14/08/2026 09:30"),
            medicion("Oficina", 48.0, 20.096, -98.770, "14/08/2026 10:00"),
            medicion("Cafetería", 45.0, 20.123, -98.740, "14/08/2026 10:30")
        )
        val estancias = agruparEnEstancias(lista)

        assertEquals(2, estancias.size)

        val oficina = estancias[0]
        assertEquals("Oficina", oficina.nombre)
        assertEquals("09:00", oficina.horaInicio)
        assertEquals("10:00", oficina.horaFin)
        assertEquals(60, oficina.duracionMin)
        assertEquals(50.0, oficina.decibelios, 0.1)
        assertEquals(20.096, oficina.latitud, 0.0001)

        assertEquals("Cafetería", estancias[1].nombre)
    }

    @Test
    fun agruparEnEstancias_cortaPorHuecoMayorACuarentaYCincoMinutos() {
        val lista = listOf(
            medicion("Casa", 40.0, 20.1, -98.75, "14/08/2026 08:00"),
            medicion("Casa", 40.0, 20.1, -98.75, "14/08/2026 10:00")
        )
        val estancias = agruparEnEstancias(lista)
        assertEquals(2, estancias.size)
    }

    @Test
    fun agruparEnEstancias_ignoraFechasNoParseables() {
        val lista = listOf(
            medicion("Casa", 40.0, 20.1, -98.75, "no es fecha"),
            medicion("Oficina", 50.0, 20.096, -98.770, "14/08/2026 09:00")
        )
        val estancias = agruparEnEstancias(lista)
        assertEquals(1, estancias.size)
        assertEquals("Oficina", estancias[0].nombre)
    }

    @Test
    fun patronLaboralYFinDeSemana() {
        val lunes = medicion("Oficina", 60.0, 20.1, -98.75, "10/08/2026 09:00")
        val sabado = medicion("Parque", 55.0, 20.1, -98.75, "15/08/2026 09:00")

        assertTrue(cumplePatron(lunes, PatronFiltro.LABORALES))
        assertEquals(false, cumplePatron(sabado, PatronFiltro.LABORALES))
        assertEquals(false, cumplePatron(lunes, PatronFiltro.FIN_DE_SEMANA))
        assertTrue(cumplePatron(sabado, PatronFiltro.FIN_DE_SEMANA))
        assertTrue(cumplePatron(lunes, PatronFiltro.TODO))
    }

    @Test
    fun refugioActivo_soloAlSuperarElUmbral() {
        assertEquals(false, refugioActivo(0.0))
        assertEquals(false, refugioActivo(119.9))
        assertTrue(refugioActivo(120.0))
        assertTrue(refugioActivo(240.0))
    }

    @Test
    fun dosisSimulada_sumaMinutosDeEstanciasRuidosas() {
        assertEquals(125.0, dosisSimulada(ESTANCIAS_DEMO_PACHUCA), 0.01)
        assertEquals(0.0, dosisSimulada(emptyList()), 0.001)
    }

    @Test
    fun distanciaMetros_haversineConocida() {
        val metros = distanciaMetros(20.1275, -98.7319, 20.1275, -98.7319)
        assertEquals(0.0, metros, 0.01)

        val norte = distanciaMetros(20.0, -98.0, 20.0, -98.0)
        assertEquals(0.0, norte, 0.01)

        val centro = distanciaMetros(CENTRO_PACHUCA.latitud, CENTRO_PACHUCA.longitud, 20.1275, -98.7319)
        assertTrue(centro > 2000)
    }

    @Test
    fun refugioMasCercano_desdeElCentro() {
        val refugio = refugioMasCercano(CENTRO_PACHUCA.latitud, CENTRO_PACHUCA.longitud)
        assertTrue(refugio != null)
        assertEquals("Parque Cultural Hidalguense (Ben Gurión)", refugio?.nombre)
    }

    @Test
    fun refugiosPachuca_contienenTresZonasCuradas() {
        assertEquals(3, REFUGIOS_PACHUCA.size)
        assertTrue(REFUGIOS_PACHUCA.all { it.latitud in 20.0..20.2 && it.longitud in -98.8..-98.7 })
        assertTrue(REFUGIOS_PACHUCA.map { it.nombre }.distinct().size == 3)
    }

    @Test
    fun construirMallaCalor_promediaPorCelda() {
        val puntos = listOf(
            CalorPunto(20.1005, -98.7605, 70.0),
            CalorPunto(20.1007, -98.7607, 90.0),
            CalorPunto(20.1030, -98.7630, 50.0)
        )
        val malla = construirMallaCalor(puntos)

        assertEquals(2, malla.size)

        val densa = malla.first { it.conteo == 2 }
        assertEquals(80.0, densa.decibelios, 0.01)

        val suelta = malla.first { it.conteo == 1 }
        assertEquals(50.0, suelta.decibelios, 0.01)

        assertTrue(construirMallaCalor(emptyList()).isEmpty())
    }

    @Test
    fun colorCalorCeldaArgb_vaDeVerdeARojo() {
        val verde = colorCalorCeldaArgb(50.0)
        val medio = colorCalorCeldaArgb(75.0)
        val rojo = colorCalorCeldaArgb(90.0)

        assertEquals(0xFF10B981.toInt(), verde)
        assertEquals(0xFFEF4444.toInt(), rojo)

        fun canal(argb: Int, desp: Int) = (argb shr desp) and 0xFF

        val rVerde = canal(verde, 16)
        val rRojo = canal(rojo, 16)
        assertTrue(rRojo > rVerde)

        val gVerde = canal(verde, 8)
        val gRojo = canal(rojo, 8)
        assertTrue(gVerde > gRojo)

        val rMedio = canal(medio, 16)
        val gMedio = canal(medio, 8)
        assertTrue(rMedio > 200)
        assertTrue(gMedio in gRojo..gVerde)
    }

    @Test
    fun zonaConMayorExposicion_nombreDelMasRuidoso() {
        val lista = listOf(
            medicion("Cafetería", 55.0, 20.123, -98.740, "14/08/2026 09:00"),
            medicion("Centro", 88.0, 20.127, -98.731, "14/08/2026 09:05")
        )
        assertEquals("Centro", zonaConMayorExposicion(lista, PatronFiltro.TODO))
        assertNull(zonaConMayorExposicion(lista, PatronFiltro.FIN_DE_SEMANA))
        assertEquals("Centro / Reloj Monumental", zonaConMayorExposicionEstancias(ESTANCIAS_DEMO_PACHUCA))
    }
}
