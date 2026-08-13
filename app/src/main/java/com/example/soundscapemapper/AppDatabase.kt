package com.example.soundscapemapper

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Medicion::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun medicionDao(): MedicionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "soundscape_database"
                )
                    .fallbackToDestructiveMigration() // Evita cierres si la estructura cambia
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}