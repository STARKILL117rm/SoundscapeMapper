package com.example.soundscapemapper

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RegistroExposicionDao {

    @Query("SELECT * FROM tabla_exposicion WHERE fecha = :fecha")
    suspend fun obtenerPorFecha(fecha: String): RegistroExposicion?

    @Query("SELECT * FROM tabla_exposicion WHERE fecha BETWEEN :inicio AND :fin ORDER BY fecha ASC")
    suspend fun obtenerEntreFechas(inicio: String, fin: String): List<RegistroExposicion>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(registro: RegistroExposicion)

    @Query("SELECT * FROM tabla_exposicion ORDER BY fecha DESC")
    suspend fun obtenerTodos(): List<RegistroExposicion>
}
