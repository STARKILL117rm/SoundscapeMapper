package com.example.soundscapemapper.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.soundscapemapper.AppDatabase
import com.example.soundscapemapper.Medicion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapaViewModel(private val db: AppDatabase) : ViewModel() {

    var mediciones by mutableStateOf(listOf<Medicion>())
        private set

    fun cargar() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val lista = db.medicionDao().obtenerTodasLasMediciones()
                withContext(Dispatchers.Main) { mediciones = lista }
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
