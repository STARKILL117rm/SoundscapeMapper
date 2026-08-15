package com.example.soundscapemapper

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Medicion::class, RegistroExposicion::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun medicionDao(): MedicionDao

    abstract fun registroExposicionDao(): RegistroExposicionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** v3 → v4: agrega el icono de contexto a las mediciones conservando los datos. */
        private val MIGRACION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE tabla_mediciones ADD COLUMN contextoEmoji TEXT NOT NULL DEFAULT '📍'"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "soundscape_database"
                )
                    .addMigrations(MIGRACION_3_4)
                    .fallbackToDestructiveMigration(true) // Último recurso si algo falla
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}