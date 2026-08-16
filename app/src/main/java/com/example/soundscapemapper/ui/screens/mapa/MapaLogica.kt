package com.example.soundscapemapper.ui.screens.mapa

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.example.soundscapemapper.Medicion
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.floor
import kotlin.math.round

// Lógica pura del mapa, aislada de Android/Compose para poder probarse en JVM.

internal enum class NivelRuido { SEGURO, MODERADO, RIESGO }

/** Clasificación OMS por decibelios. */
internal fun nivelParaDb(db: Double): NivelRuido = when {
    db < 70.0 -> NivelRuido.SEGURO      // Verde: seguro (< 70 dB)
    db <= 80.0 -> NivelRuido.MODERADO   // Amarillo: precaución (70-80 dB)
    else -> NivelRuido.RIESGO           // Rojo: riesgo (> 80 dB)
}

/** Color oficial del pin/badge según el nivel OMS. */
internal fun colorNivel(nivel: NivelRuido): Color = when (nivel) {
    NivelRuido.SEGURO -> Color(0xFF10B981)
    NivelRuido.MODERADO -> Color(0xFFF59E0B)
    NivelRuido.RIESGO -> Color(0xFFEF4444)
}

/** Etiqueta corta del badge de estado. */
internal fun estadoBadge(nivel: NivelRuido): String = when (nivel) {
    NivelRuido.SEGURO -> "Seguro"
    NivelRuido.MODERADO -> "Precaución"
    NivelRuido.RIESGO -> "Riesgo"
}

/** Una estancia diaria del "Diario Espacial". Inmutable. */
internal data class Estancia(
    val nombre: String,
    val categoria: String,
    val horaInicio: String,
    val horaFin: String,
    val duracionMin: Int,
    val decibelios: Double,
    val latitud: Double,
    val longitud: Double
) {
    val nivel: NivelRuido get() = nivelParaDb(decibelios)
}

/** Estancias de demostración de Pachuca (fallback cuando no hay datos del día). */
internal val ESTANCIAS_DEMO_PACHUCA: List<Estancia> = listOf(
    Estancia("Centro / Reloj Monumental", "Calle concurrida", "08:30", "09:05", 35, 82.0, 20.1275, -98.7319),
    Estancia("Oficina Zona Plateada", "Oficina", "09:30", "13:30", 240, 48.0, 20.0960, -98.7700),
    Estancia("Cafetería Centro Histórico", "Cafetería", "14:00", "15:00", 60, 52.0, 20.1230, -98.7400),
    Estancia("Gimnasio", "Gimnasio", "18:30", "20:00", 90, 78.0, 20.1040, -98.7630)
)

/** Punto de referencia simple para el centro de la ciudad. */
internal data class Coordenada(val latitud: Double, val longitud: Double)

/** Centro aproximado de Pachuca usado como referencia del mapa. */
internal val CENTRO_PACHUCA = Coordenada(20.1035, -98.7600)

/** Zona tranquila recomendada para descanso auditivo. */
internal data class RefugioSonoro(
    val nombre: String,
    val categoria: String, // "Parque" | "Biblioteca"
    val latitud: Double,
    val longitud: Double
)

/** Lista curada (offline) de refugios sonoros de Pachuca. */
internal val REFUGIOS_PACHUCA: List<RefugioSonoro> = listOf(
    RefugioSonoro("Parque Cultural Hidalguense (Ben Gurión)", "Parque", 20.0942, -98.7731),
    RefugioSonoro("Biblioteca Central Ricardo Garibay", "Biblioteca", 20.0943, -98.7747),
    RefugioSonoro("Parque Ecológico Cubitos", "Parque", 20.0965, -98.7396)
)

/** Umbral (min) de exposición en zonas ruidosas para sugerir un descanso. */
internal const val LIMITE_REFUGIO_MIN = 120.0

/** ¿La capa de refugios debe activarse? Proxy: minutos ≥ 80 dB acumulados hoy. */
internal fun refugioActivo(minutosSobre80: Double): Boolean =
    minutosSobre80 >= LIMITE_REFUGIO_MIN

/** Dosis simulada para la demo: minutos en estancias con dB > 75. */
internal fun dosisSimulada(estancias: List<Estancia>): Double =
    estancias.filter { it.decibelios > 75.0 }.sumOf { it.duracionMin.toDouble() }

/** Distancia en metros entre dos coordenadas (fórmula haversine). */
internal fun distanciaMetros(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val radioTierra = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2)
    return radioTierra * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

/** Refugio más cercano a una coordenada. */
internal fun refugioMasCercano(lat: Double, lon: Double): RefugioSonoro? =
    REFUGIOS_PACHUCA.minByOrNull { distanciaMetros(lat, lon, it.latitud, it.longitud) }

/** Distancia legible (m/km) desde el centro de Pachuca. */
internal fun distanciaCentroTexto(lat: Double, lon: Double): String {
    val metros = distanciaMetros(CENTRO_PACHUCA.latitud, CENTRO_PACHUCA.longitud, lat, lon)
    return if (metros < 1000) "≈ ${metros.toInt()} m del centro" else "≈ ${round(metros / 1000 * 10) / 10} km del centro"
}

internal enum class PatronFiltro { TODO, LABORALES, FIN_DE_SEMANA }

internal fun etiquetaPatron(patron: PatronFiltro): String = when (patron) {
    PatronFiltro.TODO -> "Todo"
    PatronFiltro.LABORALES -> "Laborales"
    PatronFiltro.FIN_DE_SEMANA -> "Fin de semana"
}

private val FORMATO_FECHAHORA = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
private val FORMATO_HORA = SimpleDateFormat("HH:mm", Locale.getDefault())

/** Parsea el String "dd/MM/yyyy HH:mm" que guardan las mediciones. */
internal fun fechaDeMedicion(medicion: Medicion): Calendar? {
    val fecha = try {
        FORMATO_FECHAHORA.parse(medicion.fechaHora)
    } catch (e: Exception) {
        null
    } ?: return null
    return Calendar.getInstance().apply { time = fecha }
}

internal fun esFinDeSemana(cal: Calendar): Boolean {
    val dia = cal.get(Calendar.DAY_OF_WEEK)
    return dia == Calendar.SATURDAY || dia == Calendar.SUNDAY
}

internal fun esDiaLaboral(cal: Calendar): Boolean = !esFinDeSemana(cal)

internal fun esDeHoy(medicion: Medicion, hoy: Calendar = Calendar.getInstance()): Boolean {
    val cal = fechaDeMedicion(medicion) ?: return false
    return cal.get(Calendar.YEAR) == hoy.get(Calendar.YEAR) &&
        cal.get(Calendar.MONTH) == hoy.get(Calendar.MONTH) &&
        cal.get(Calendar.DAY_OF_MONTH) == hoy.get(Calendar.DAY_OF_MONTH)
}

internal fun cumplePatron(medicion: Medicion, patron: PatronFiltro): Boolean {
    val cal = fechaDeMedicion(medicion) ?: return true
    return when (patron) {
        PatronFiltro.TODO -> true
        PatronFiltro.LABORALES -> esDiaLaboral(cal)
        PatronFiltro.FIN_DE_SEMANA -> esFinDeSemana(cal)
    }
}

/** Si no hay muestras del mismo lugar en este lapso, se separa la estancia. */
private const val MAX_GAP_ESTANCIA_MIN = 45L

/**
 * Agrupa mediciones en estancias (mismo lugar, mismo día y hueco ≤
 * [MAX_GAP_ESTANCIA_MIN]) en orden cronológico.
 */
internal fun agruparEnEstancias(mediciones: List<Medicion>): List<Estancia> {
    data class Muestra(val medicion: Medicion, val cal: Calendar)

    val ordenadas = mediciones
        .mapNotNull { m -> fechaDeMedicion(m)?.let { Muestra(m, it) } }
        .sortedWith(compareBy<Muestra> { it.cal.timeInMillis }.thenBy { it.medicion.id })

    val grupos = mutableListOf<MutableList<Muestra>>()
    for (muestra in ordenadas) {
        val ultimo = grupos.lastOrNull()?.lastOrNull()
        val mismoLugar = ultimo != null && ultimo.medicion.nombreLugar == muestra.medicion.nombreLugar
        val mismoDia = ultimo != null && esMismoDia(ultimo.cal, muestra.cal)
        val hueco = ultimo?.let { muestra.cal.timeInMillis - it.cal.timeInMillis } ?: 0L
        val abrirNuevo = ultimo == null || !mismoLugar || !mismoDia ||
            hueco > MAX_GAP_ESTANCIA_MIN * 60_000L
        if (abrirNuevo) grupos.add(mutableListOf(muestra)) else grupos.last().add(muestra)
    }

    return grupos.map { grupo ->
        val primer = grupo.first().medicion
        val inicioMs = grupo.minOf { it.cal.timeInMillis }
        val finMs = grupo.maxOf { it.cal.timeInMillis }
        Estancia(
            nombre = primer.nombreLugar,
            categoria = primer.categoria,
            horaInicio = FORMATO_HORA.format(Date(inicioMs)),
            horaFin = FORMATO_HORA.format(Date(finMs)),
            duracionMin = ((finMs - inicioMs) / 60_000L).toInt().coerceAtLeast(1),
            decibelios = redondear1(grupo.map { it.medicion.decibelios }.average()),
            latitud = primer.latitud,
            longitud = primer.longitud
        )
    }
}

private fun esMismoDia(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.MONTH) == b.get(Calendar.MONTH) &&
        a.get(Calendar.DAY_OF_MONTH) == b.get(Calendar.DAY_OF_MONTH)

internal fun redondear1(valor: Double): Double = round(valor * 10) / 10

/** Tamaño de celda de la cuadrícula en grados (~220 m). */
internal const val CELDA_GRADOS = 0.002

internal data class CalorPunto(val latitud: Double, val longitud: Double, val decibelios: Double)

internal data class CeldaCalor(
    val latitud: Double,
    val longitud: Double,
    val decibelios: Double,
    val conteo: Int
)

/** Construye la malla de calor agrupando puntos por celda y promediando dB. */
internal fun construirMallaCalor(puntos: List<CalorPunto>): List<CeldaCalor> {
    if (puntos.isEmpty()) return emptyList()
    val acumulado = HashMap<Pair<Int, Int>, MutableList<Double>>()
    for (punto in puntos) {
        val ix = floor(punto.latitud / CELDA_GRADOS).toInt()
        val iy = floor(punto.longitud / CELDA_GRADOS).toInt()
        acumulado.getOrPut(ix to iy) { mutableListOf() }.add(punto.decibelios)
    }
    return acumulado.map { (clave, dbs) ->
        val (ix, iy) = clave
        CeldaCalor(
            latitud = (ix + 0.5) * CELDA_GRADOS,
            longitud = (iy + 0.5) * CELDA_GRADOS,
            decibelios = redondear1(dbs.average()),
            conteo = dbs.size
        )
    }
}

/** Color del mapa de calor (verde → amarillo → rojo) según dB promedio. */
internal fun colorParaCalor(db: Double): Color {
    val seguro = Color(0xFF10B981)
    val precaucion = Color(0xFFF59E0B)
    val riesgo = Color(0xFFEF4444)
    return when {
        db < 60.0 -> seguro
        db < 70.0 -> lerp(seguro, precaucion, ((db - 60.0) / 10.0).toFloat().coerceIn(0f, 1f))
        db < 80.0 -> lerp(precaucion, riesgo, ((db - 70.0) / 10.0).toFloat().coerceIn(0f, 1f))
        else -> riesgo
    }
}

/** ARGB entero del color de calor (para el Canvas de osmdroid). */
internal fun colorCalorCeldaArgb(decibelios: Double): Int {
    val c = colorParaCalor(decibelios)
    return (0xFF shl 24) or
        ((c.red * 255).toInt() shl 16) or
        ((c.green * 255).toInt() shl 8) or
        (c.blue * 255).toInt()
}

/** Nombre del lugar con mayor exposición en las mediciones filtradas. */
internal fun zonaConMayorExposicion(mediciones: List<Medicion>, patron: PatronFiltro): String? =
    mediciones.filter { cumplePatron(it, patron) }.maxByOrNull { it.decibelios }?.nombreLugar

internal fun zonaConMayorExposicionEstancias(estancias: List<Estancia>): String? =
    estancias.maxByOrNull { it.decibelios }?.nombre

internal enum class ModoMapa { DIARIO, REFUGIOS, CALOR }
