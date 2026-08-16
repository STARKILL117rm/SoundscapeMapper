package com.example.soundscapemapper.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.soundscapemapper.AppDatabase
import com.example.soundscapemapper.Medicion
import com.example.soundscapemapper.RegistroExposicion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Expone las mediciones y el registro de exposición de hoy para el mapa. */
class MapaViewModel(private val db: AppDatabase) : ViewModel() {

    var mediciones by mutableStateOf(listOf<Medicion>())
        private set

    var registroHoy by mutableStateOf<RegistroExposicion?>(null)
        private set

    var cargado by mutableStateOf(false)
        private set

    fun cargar() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val lista = db.medicionDao().obtenerTodasLasMediciones()
                val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val registro = db.registroExposicionDao().obtenerPorFecha(hoy)
                withContext(Dispatchers.Main) {
                    mediciones = lista
                    registroHoy = registro
                    cargado = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        fun factory(db: AppDatabase): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = MapaViewModel(db) as T
        }
    }
}
