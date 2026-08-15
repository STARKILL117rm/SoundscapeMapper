package com.example.soundscapemapper.ui.historias

/**
 * Modelo de las "historias" estilo FLO: informes personalizados y
 * datos de salud del ruido presentados en páginas a pantalla completa.
 */
data class PaginaHistoria(
    val emoji: String,
    val titulo: String,
    val cuerpo: String,
    val colorHex: Long,
    val datoDestacado: String? = null
)

data class Historia(
    val id: String,
    val nombre: String,
    val emoji: String,
    val colorHex: Long,
    val paginas: List<PaginaHistoria>
)
