package com.example.soundscapemapper

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabla_exposicion")
data class RegistroExposicion(
    @PrimaryKey val fecha: String,      // Formato yyyy-MM-dd
    val minutosSobre65: Double,         // Minutos de exposición ≥ 65 dB (precaución)
    val minutosSobre80: Double,         // Minutos de exposición ≥ 80 dB (peligro)
    val nivelMaximoDb: Double           // Nivel máximo registrado del día
)
