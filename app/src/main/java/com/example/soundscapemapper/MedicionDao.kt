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
}