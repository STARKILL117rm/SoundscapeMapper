package com.example.soundscapemapper

import android.content.Context

object Configuracion {

    private const val PREF_NOMBRE = "espacio_seguro_prefs"
    private const val KEY_UMBRAL_ALERTA = "umbral_alerta_db"
    private const val KEY_SERVICIO_ACTIVO = "servicio_activo"
    private const val KEY_CAPTURA_PAUSADA = "captura_pausada"

    const val UMBRAL_ALERTA_DEFECTO = 80.0
    const val UMBRAL_ALERTA_MINIMO = 60.0
    const val UMBRAL_ALERTA_MAXIMO = 100.0

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NOMBRE, Context.MODE_PRIVATE)

    fun umbralAlerta(context: Context): Double =
        prefs(context).getFloat(KEY_UMBRAL_ALERTA, UMBRAL_ALERTA_DEFECTO.toFloat()).toDouble()

    fun guardarUmbralAlerta(context: Context, valor: Double) {
        prefs(context).edit().putFloat(KEY_UMBRAL_ALERTA, valor.toFloat()).apply()
    }

    fun servicioActivo(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SERVICIO_ACTIVO, false)

    fun guardarServicioActivo(context: Context, activo: Boolean) {
        prefs(context).edit().putBoolean(KEY_SERVICIO_ACTIVO, activo).apply()
    }

    fun capturaPausada(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CAPTURA_PAUSADA, false)

    fun guardarCapturaPausada(context: Context, pausada: Boolean) {
        prefs(context).edit().putBoolean(KEY_CAPTURA_PAUSADA, pausada).apply()
    }
}
