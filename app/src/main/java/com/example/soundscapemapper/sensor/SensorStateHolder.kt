package com.example.soundscapemapper.sensor

import androidx.compose.runtime.mutableStateOf

/**
 * Estado compartido de los sensores y la configuración en vivo.
 * El AudioEngine (micrófono) y el MainActivity (luz y GPS) actualizan
 * estos valores; las pantallas solo los leen.
 */
object SensorStateHolder {
    val decibelios = mutableStateOf(0.0)
    val nivelMaximo = mutableStateOf(0.0)
    val nivelLuz = mutableStateOf(0f)
    val latitud = mutableStateOf(19.4326)
    val longitud = mutableStateOf(-99.1332)
    val dosisSobre65 = mutableStateOf(0.0)
    val dosisSobre80 = mutableStateOf(0.0)
    val capturaPausada = mutableStateOf(false)
    val monitoreoActivo = mutableStateOf(false)
}
