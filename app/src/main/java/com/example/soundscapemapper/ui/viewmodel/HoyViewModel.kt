package com.example.soundscapemapper.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.soundscapemapper.AppDatabase
import com.example.soundscapemapper.Medicion
import com.example.soundscapemapper.SoundAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HoyViewModel(private val db: AppDatabase) : ViewModel() {

    var puntosSemana by mutableStateOf(List(7) { 0f })
        private set
    var diasSemana by mutableStateOf(List(7) { "" })
        private set
    var recientes by mutableStateOf(listOf<Medicion>())
        private set

    fun cargar() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fechas = ultimasFechas(7)
                val registros = db.registroExposicionDao()
                    .obtenerEntreFechas(fechas.first().first, fechas.last().first)
                val porFecha = registros.associateBy { it.fecha }

                val puntos = fechas.map { (str, _) ->
                    val r = porFecha[str]
                    if (r != null) SoundAnalyzer.puntosDosis(r.minutosSobre65, r.minutosSobre80).toFloat() else 0f
                }
                val etiquetas = fechas.map { it.second }
                val ultimas = db.medicionDao().obtenerTodasLasMediciones().take(3)

                withContext(Dispatchers.Main) {
                    puntosSemana = puntos
                    diasSemana = etiquetas
                    recientes = ultimas
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun ultimasFechas(n: Int): List<Pair<String, String>> {
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formatoCorto = SimpleDateFormat("EEE", Locale.getDefault())
        val hoy = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        return (n - 1 downTo 0).map { atras ->
            val cal = hoy.clone() as Calendar
            cal.add(Calendar.DAY_OF_YEAR, -atras)
            Pair(formato.format(cal.time), formatoCorto.format(cal.time).take(3))
        }
    }

    companion object {
        fun factory(db: AppDatabase): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = HoyViewModel(db) as T
        }
    }
}
