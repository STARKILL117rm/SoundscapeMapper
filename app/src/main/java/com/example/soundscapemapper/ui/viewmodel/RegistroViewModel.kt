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

class RegistroViewModel(private val db: AppDatabase) : ViewModel() {

    var mediciones by mutableStateOf(listOf<Medicion>())
        private set
    var filtro by mutableStateOf(0)
    var busqueda by mutableStateOf("")

    val listaFiltrada: List<Medicion>
        get() = mediciones.filter { m ->
            val porCategoria = when (filtro) {
                1 -> m.categoria == "Tranquilo"
                2 -> m.categoria == "Estresante"
                3 -> m.nivelLuz >= 300f
                else -> true
            }
            val porNombre = busqueda.isBlank() || m.nombreLugar.contains(busqueda, ignoreCase = true)
            porCategoria && porNombre
        }

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

    fun alternarCategoria(item: Medicion) {
        val nueva = item.copy(categoria = if (item.categoria == "Tranquilo") "Estresante" else "Tranquilo")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.medicionDao().insertarMedicion(nueva)
                val lista = db.medicionDao().obtenerTodasLasMediciones()
                withContext(Dispatchers.Main) { mediciones = lista }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun eliminar(item: Medicion) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.medicionDao().eliminarMedicion(item)
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
            override fun <T : ViewModel> create(modelClass: Class<T>): T = RegistroViewModel(db) as T
        }
    }
}
