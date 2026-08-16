package com.example.soundscapemapper

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MedicionDao {

    // OnConflictStrategy.REPLACE permite actualizar el registro cuando editas su categoría (Tranquilo/Estresante)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarMedicion(medicion: Medicion)

    @Query("SELECT * FROM tabla_mediciones ORDER BY id DESC")
    suspend fun obtenerTodasLasMediciones(): List<Medicion>

    // Permite eliminar la ubicación de la base de datos de Room
    @Delete
    suspend fun eliminarMedicion(medicion: Medicion)

    // Mediciones del día actual (formato dd/MM/yyyy HH:mm)
    @Query("SELECT * FROM tabla_mediciones WHERE fechaHora LIKE :prefijoDia || '%' ORDER BY id DESC")
    suspend fun obtenerMedicionesDeHoy(prefijoDia: String): List<Medicion>

    // Promedio histórico de decibelios
    @Query("SELECT AVG(decibelios) FROM tabla_mediciones")
    suspend fun obtenerPromedioDecibelios(): Double?

    // Máximo histórico de decibelios
    @Query("SELECT MAX(decibelios) FROM tabla_mediciones")
    suspend fun obtenerMaximoDecibelios(): Double?

    // Total de mediciones registradas
    @Query("SELECT COUNT(*) FROM tabla_mediciones")
    suspend fun contarMediciones(): Int

    // El lugar más ruidoso registrado (mayor decibelios)
    @Query("SELECT * FROM tabla_mediciones ORDER BY decibelios DESC LIMIT 1")
    suspend fun obtenerLugarMasRuidoso(): Medicion?

    // El lugar más silencioso registrado (menor decibelios, mayor a 0)
    @Query("SELECT * FROM tabla_mediciones WHERE decibelios > 0 ORDER BY decibelios ASC LIMIT 1")
    suspend fun obtenerLugarMasSilencioso(): Medicion?

    // Últimas N mediciones para gráfica
    @Query("SELECT * FROM tabla_mediciones ORDER BY id DESC LIMIT :limite")
    suspend fun obtenerUltimasMediciones(limite: Int): List<Medicion>

    // Mediciones por fecha de los últimos 7 días (para gráfica semanal)
    @Query("SELECT * FROM tabla_mediciones WHERE fechaHora LIKE :dia || '%'")
    suspend fun obtenerMedicionesDelDia(dia: String): List<Medicion>
}