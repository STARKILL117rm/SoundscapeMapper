package com.example.soundscapemapper

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabla_mediciones")
data class Medicion(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombreLugar: String,
    val categoria: String,     // Ej: Cafetería, Parque, Biblioteca
    val decibelios: Double,    // Lectura del micrófono
    val nivelLuz: Float,       // Lectura del sensor de luz
    val latitud: Double,       // Coordenada GPS
    val longitud: Double,      // Coordenada GPS
    val fechaHora: String
)