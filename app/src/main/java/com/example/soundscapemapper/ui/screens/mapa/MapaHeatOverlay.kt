package com.example.soundscapemapper.ui.screens.mapa

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Overlay
import kotlin.math.max
import kotlin.math.min

/**
 * Overlay de calor: dibuja la cuadrícula de celdas sobre el mapa. El alpha
 * crece con el número de muestras; el color depende de los dB promedio.
 */
internal class MapaHeatOverlay(
    private val celdas: List<CeldaCalor>
) : Overlay() {

    private val pintura = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val proyeccion = mapView.projection
        val noroeste = Point()
        val sureste = Point()
        for (celda in celdas) {
            proyeccion.toPixels(GeoPoint(celda.latitud, celda.longitud), noroeste)
            proyeccion.toPixels(
                GeoPoint(celda.latitud + CELDA_GRADOS, celda.longitud + CELDA_GRADOS),
                sureste
            )
            pintura.color = colorCalorCeldaArgb(celda.decibelios)
            pintura.alpha = (70 + celda.conteo * 35).coerceAtMost(235)
            canvas.drawRect(
                Rect(
                    min(noroeste.x, sureste.x),
                    min(noroeste.y, sureste.y),
                    max(noroeste.x, sureste.x),
                    max(noroeste.y, sureste.y)
                ),
                pintura
            )
        }
    }
}
