package com.example.soundscapemapper.ui.historias

/**
 * Modelo de las "historias" estilo FLO: informes personalizados y
 * datos de salud del ruido presentados en páginas a pantalla completa.
 */

/** Tipo de gráfica animada que se dibuja dentro de la tarjeta de cristal. */
enum class TipoGrafica {
    /** Sin gráfica. */
    NINGUNA,

    /** Onda de audio estilizada con barras (Waveform). */
    ONDA,

    /** Indicador circular de nivel / porcentaje. */
    NIVEL
}

data class PaginaHistoria(
    val emoji: String,
    val titulo: String,
    val cuerpo: String,
    val colorHex: Long,
    val datoDestacado: String? = null,
    /** Métrica principal en grande (ej. "52 dB", "24%"). */
    val metrica: String? = null,
    /** Etiqueta oficial de salud en chip (ej. "Nivel Seguro · OMS"). */
    val chip: String? = null,
    /** URL de la fotografía contextual de fondo (Unsplash). */
    val imagenUrl: String? = null,
    /** Gráfica animada a dibujar en la tarjeta. */
    val tipoGrafica: TipoGrafica = TipoGrafica.NINGUNA,
    /** Intensidad de la onda o porcentaje del indicador (0..1). */
    val tendencia: Float = 0.5f,
    /** Si true, el chip y la gráfica usan acento rojo de alerta. */
    val alerta: Boolean = false
)

data class Historia(
    val id: String,
    val nombre: String,
    val emoji: String,
    val colorHex: Long,
    val paginas: List<PaginaHistoria>,
    /** Micro-badge flotante de la burbuja (ej. "PICO", "HOY", "OMS"). */
    val badge: String? = null,
    /** URL de la fotografía contextual de la burbuja/fondo. */
    val imagenUrl: String? = null
)
