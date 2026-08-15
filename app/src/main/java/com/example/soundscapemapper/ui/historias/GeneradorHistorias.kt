package com.example.soundscapemapper.ui.historias

import com.example.soundscapemapper.Medicion
import com.example.soundscapemapper.RegistroExposicion
import com.example.soundscapemapper.SoundAnalyzer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Genera la lista de historias del día mezclando informes personalizados
 * (calculados con los datos reales del usuario) y datos educativos de salud.
 */
object GeneradorHistorias {

    private const val VERDE = 0xFF4DB6AC
    private const val AMBAR = 0xFFF0B27A
    private const val ROJO = 0xFFE07A5F
    private const val AZUL = 0xFF5B8DB8
    private const val VIOLETA = 0xFF8E7CC3
    private const val TEAL = 0xFF00897B

    private val datosSabias = listOf(
        "Solo 5 minutos a 100 dB (lo que mide un concierto) pueden dañar tu audición de forma permanente.",
        "8 horas a 85 dB es el límite máximo diario que la OMS recomienda sin protección auditiva.",
        "Un grito puede alcanzar ~120 dB; a ese nivel el daño auditivo es casi inmediato.",
        "El ruido nocturno por encima de 55 dB fragmenta el sueño, aunque no te despiertes.",
        "La exposición crónica al ruido eleva el estrés y la presión arterial, incluso sin que lo notes.",
        "El oído no tiene párpados: a diferencia de los ojos, no puede cerrarse para bloquear el ruido.",
        "Dos motos acelerando juntas superan los 100 dB; con un cruce puedes acercarte al límite diario.",
        "La OMS vincula el ruido ambiental excesivo con problemas de salud cardiovascular.",
        "Un buen audífono de seguridad reduce 20-30 dB lo que percibes, como alejarte mucho del foco.",
        "El tráfico de una avenida (80 dB) sostenido 8 horas duplica la dosis diaria permitida."
    )

    fun generar(
        decibeliosAhora: Double,
        nivelMaximoHoy: Double,
        minutosSobre65Hoy: Double,
        minutosSobre80Hoy: Double,
        puntosHoy: Double,
        registrosSemana: List<RegistroExposicion>,
        mediciones: List<Medicion>,
        diaDelAnio: Int = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    ): List<Historia> {
        val lista = mutableListOf<Historia>()
        val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)

        val nivel = SoundAnalyzer.clasificarNivel(decibeliosAhora)
        lista += Historia(
            id = "estado_actual_$fechaHoy",
            nombre = "Tu entorno",
            emoji = "🎧",
            colorHex = nivel.colorHex,
            paginas = listOf(
                PaginaHistoria(
                    emoji = "🔊",
                    titulo = "Tu entorno ahora mismo",
                    cuerpo = "El micrófono registra ${decibeliosAhora.toInt()} dB, un ambiente en nivel ${nivel.etiqueta.lowercase()}.",
                    colorHex = nivel.colorHex,
                    datoDestacado = "${decibeliosAhora.toInt()} dB"
                ),
                PaginaHistoria(
                    emoji = "💡",
                    titulo = "¿Qué significa?",
                    cuerpo = nivel.recomendacion,
                    colorHex = nivel.colorHex
                )
            )
        )

        val progreso = (puntosHoy / SoundAnalyzer.META_DOSIS_PUNTOS).coerceIn(0.0, 1.0)
        val colorDosis = when {
            progreso >= 1.0 -> ROJO
            progreso >= 0.5 -> AMBAR
            else -> VERDE
        }
        lista += Historia(
            id = "dosis_hoy_$fechaHoy",
            nombre = "Dosis hoy",
            emoji = "⏱️",
            colorHex = colorDosis,
            paginas = listOf(
                PaginaHistoria(
                    emoji = "🎯",
                    titulo = "Tu dosis de ruido de hoy",
                    cuerpo = if (puntosHoy > 0) {
                        "Llevas ${puntosHoy.toInt()} puntos acumulados, el ${(progreso * 100).toInt()}% de la meta diaria recomendada."
                    } else {
                        "Todavía no acumulas puntos hoy. Sigue escuchando para conocer tu dosis."
                    },
                    colorHex = colorDosis,
                    datoDestacado = "${puntosHoy.toInt()} / ${SoundAnalyzer.META_DOSIS_PUNTOS.toInt()} pts"
                ),
                PaginaHistoria(
                    emoji = "📈",
                    titulo = "Minutos y máximo",
                    cuerpo = "≥65 dB: ${minutosSobre65Hoy.toInt()} min · ≥80 dB: ${minutosSobre80Hoy.toInt()} min.\nTu nivel más alto del día fue ${nivelMaximoHoy.toInt()} dB.",
                    colorHex = colorDosis
                )
            )
        )

        if (puntosHoy >= SoundAnalyzer.META_DOSIS_PUNTOS * 0.8) {
            lista += Historia(
                id = "dia_pesado_$fechaHoy",
                nombre = "Día pesado",
                emoji = "🌊",
                colorHex = ROJO,
                paginas = listOf(
                    PaginaHistoria(
                        emoji = "🌊",
                        titulo = "Tu oído ha trabajado mucho hoy",
                        cuerpo = "Llevas ${puntosHoy.toInt()} puntos (el ${(progreso * 100).toInt()}% de tu meta). Hoy tu entorno estuvo más ruidoso de lo recomendado.",
                        colorHex = ROJO,
                        datoDestacado = "${puntosHoy.toInt()} pts"
                    ),
                    PaginaHistoria(
                        emoji = "🧘",
                        titulo = "Regálate un descanso",
                        cuerpo = "Considera 30 minutos de silencio o música suave sin audífonos antes de dormir. Tu oído te lo agradecerá.",
                        colorHex = ROJO
                    )
                )
            )
        }

        val puntosSemana = registrosSemana.sumOf {
            SoundAnalyzer.puntosDosis(it.minutosSobre65, it.minutosSobre80)
        }
        val peorDia = registrosSemana.maxByOrNull {
            SoundAnalyzer.puntosDosis(it.minutosSobre65, it.minutosSobre80)
        }
        lista += Historia(
            id = "semana_$fechaHoy",
            nombre = "Tu semana",
            emoji = "📊",
            colorHex = AZUL,
            paginas = listOf(
                PaginaHistoria(
                    emoji = "🗓️",
                    titulo = "Tu semana en números",
                    cuerpo = if (registrosSemana.isEmpty()) {
                        "Aún no hay datos de la semana. Escucha el entorno para construir tu resumen."
                    } else {
                        "En los últimos 7 días acumulaste ${puntosSemana.toInt()} puntos de dosis de ruido."
                    },
                    colorHex = AZUL,
                    datoDestacado = if (registrosSemana.isNotEmpty()) "${puntosSemana.toInt()} pts / 7 días" else null
                ),
                PaginaHistoria(
                    emoji = "🔥",
                    titulo = "Tu día más ruidoso",
                    cuerpo = if (peorDia != null && puntosSemana > 0) {
                        "El ${nombreDia(peorDia.fecha)} fue el más intenso (${SoundAnalyzer.puntosDosis(peorDia.minutosSobre65, peorDia.minutosSobre80).toInt()} pts, máximo ${peorDia.nivelMaximoDb.toInt()} dB)."
                    } else {
                        "Todavía no tenemos un día de máxima exposición. Mide a distintas horas y lugares."
                    },
                    colorHex = AZUL
                )
            )
        )

        val masRuidoso = mediciones.maxByOrNull { it.decibelios }
        val masTranquilo = mediciones.minByOrNull { it.decibelios }
        lista += Historia(
            id = "lugares_$fechaHoy",
            nombre = "Tus lugares",
            emoji = "📍",
            colorHex = VERDE,
            paginas = listOf(
                PaginaHistoria(
                    emoji = "😮‍💨",
                    titulo = "Tu lugar más ruidoso",
                    cuerpo = if (masRuidoso != null) {
                        "'${masRuidoso.nombreLugar}' registró ${masRuidoso.decibelios.toInt()} dB."
                    } else {
                        "Mide tu primer lugar para descubrir cuál es el más ruidoso."
                    },
                    colorHex = VERDE,
                    datoDestacado = masRuidoso?.let { "${it.decibelios.toInt()} dB" }
                ),
                PaginaHistoria(
                    emoji = "🌿",
                    titulo = "Tu refugio tranquilo",
                    cuerpo = if (masTranquilo != null) {
                        "'${masTranquilo.nombreLugar}' es tu sitio más silencioso, con ${masTranquilo.decibelios.toInt()} dB."
                    } else {
                        "Busca un lugar silencioso, mídelo y guárdalo como tu refugio."
                    },
                    colorHex = VERDE
                ),
                PaginaHistoria(
                    emoji = "🏷️",
                    titulo = "¿Cuántos has medido?",
                    cuerpo = if (mediciones.isNotEmpty()) {
                        "Tienes ${mediciones.size} lugares guardados. Cuantos más midas, mejores serán tus informes."
                    } else {
                        "Toca 'Analizar este lugar' para guardar tu primera medición."
                    },
                    colorHex = VERDE
                )
            )
        )

        lista += historiaSabias(diaDelAnio)

        val educativas = listOf(escala(), dosisDiaria(), protegete())
        lista += educativas[(diaDelAnio / 7) % educativas.size]

        return lista
    }

    private fun historiaSabias(diaDelAnio: Int): Historia {
        val hechos = listOf(
            datosSabias[diaDelAnio % datosSabias.size],
            datosSabias[(diaDelAnio + 1) % datosSabias.size]
        )
        return Historia(
            id = "sabias_${diaDelAnio % datosSabias.size}",
            nombre = "Sabías",
            emoji = "✨",
            colorHex = VIOLETA,
            paginas = hechos.mapIndexed { i, hecho ->
                PaginaHistoria(
                    emoji = "🤯",
                    titulo = if (i == 0) "¿Sabías que...?" else "Y además...",
                    cuerpo = hecho,
                    colorHex = VIOLETA
                )
            }
        )
    }

    private fun escala(): Historia = Historia(
        id = "educativa_escala",
        nombre = "Escala",
        emoji = "📶",
        colorHex = VERDE,
        paginas = listOf(
            PaginaHistoria(
                emoji = "🌿",
                titulo = "0–65 dB · Tranquilo",
                cuerpo = "Conversación, biblioteca, parque. Tu oído trabaja sin esfuerzo.",
                colorHex = VERDE
            ),
            PaginaHistoria(
                emoji = "🚗",
                titulo = "65–80 dB · Precaución",
                cuerpo = "Tráfico, cafetería con gente. Sostenido, puede causar fatiga y estrés.",
                colorHex = AMBAR
            ),
            PaginaHistoria(
                emoji = "⚠️",
                titulo = "80+ dB · Peligro",
                cuerpo = "Perforadora, concierto, bocina. Puede dañar tu audición con el tiempo.",
                colorHex = ROJO
            )
        )
    )

    private fun dosisDiaria(): Historia = Historia(
        id = "educativa_dosis",
        nombre = "Dosis diaria",
        emoji = "🧮",
        colorHex = AMBAR,
        paginas = listOf(
            PaginaHistoria(
                emoji = "🧮",
                titulo = "Cómo suman los puntos",
                cuerpo = "1 minuto entre 65 y 80 dB = 1 punto.\n1 minuto a 80 dB o más = 4 puntos.",
                colorHex = AMBAR,
                datoDestacado = "Meta: 480 pts"
            ),
            PaginaHistoria(
                emoji = "🎧",
                titulo = "Por qué 480",
                cuerpo = "La meta de 480 puntos equivale a 8 horas a 65 dB, la referencia diaria de exposición que recomienda la OMS.",
                colorHex = AMBAR
            )
        )
    )

    private fun protegete(): Historia = Historia(
        id = "educativa_protegete",
        nombre = "Protégete",
        emoji = "🛡️",
        colorHex = ROJO,
        paginas = listOf(
            PaginaHistoria(
                emoji = "🛡️",
                titulo = "Cuida tus oídos",
                cuerpo = "En zonas de 80+ dB usa audífonos de seguridad; el daño se acumula aunque no duela.",
                colorHex = ROJO
            ),
            PaginaHistoria(
                emoji = "🏞️",
                titulo = "Busca refugio",
                cuerpo = "Encuentra espacios tranquilos para estudiar o descansar; tu concentración y tu ánimo lo agradecen.",
                colorHex = ROJO
            ),
            PaginaHistoria(
                emoji = "💡",
                titulo = "Luz y horarios",
                cuerpo = "Una luz muy intensa (más de 2500 lux) también estresa. Mide el mismo lugar a distintas horas para elegir tu mejor momento.",
                colorHex = ROJO
            )
        )
    )

    private fun nombreDia(fecha: String): String = try {
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        SimpleDateFormat("EEEE", Locale.getDefault())
            .format(formato.parse(fecha)!!)
            .replaceFirstChar { it.uppercase() }
    } catch (e: Exception) {
        fecha
    }
}
