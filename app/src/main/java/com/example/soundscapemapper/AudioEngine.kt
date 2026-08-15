package com.example.soundscapemapper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.example.soundscapemapper.sensor.SensorStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Motor único de captura de audio de la app.
 *
 * Tanto [MainActivity] (primer plano) como [AudioMonitorService] (segundo plano)
 * adquieren y liberan la captura mediante un contador de referencias, de modo
 * que solo exista un único AudioRecord en toda la aplicación y el micrófono
 * nunca sea disputado por dos procesos de escucha a la vez.
 */
object AudioEngine {

    private const val VENTANA_PROMEDIO = 30
    private const val INTERVALO_GUARDADO_MS = 15_000L

    private var capturando = false
    private var refCount = 0
    private var audioRecord: AudioRecord? = null
    private var scope: CoroutineScope? = null

    private var minutosAcumSobre65 = 0.0
    private var minutosAcumSobre80 = 0.0
    private var maximoSesionDb = 0.0

    private val _decibelios = MutableStateFlow(0.0)
    val decibelios: StateFlow<Double> = _decibelios

    /** Solicita la captura; incrementa la referencia e inicia si no está activa. */
    fun adquirir(context: Context) {
        refCount++
        iniciar(context)
    }

    /** Libera la captura; si nadie la necesita, detiene el micrófono. */
    fun liberar(context: Context) {
        refCount = (refCount - 1).coerceAtLeast(0)
        if (refCount == 0) {
            detener(context)
        }
    }

    fun iniciar(context: Context) {
        if (capturando) return
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC, sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize
            )
            audioRecord?.startRecording()
            capturando = true

            val appContext = context.applicationContext
            val ventanaPromedio = ArrayDeque<Double>()
            var ultimoTickMs = System.currentTimeMillis()
            var ultimoGuardadoMs = System.currentTimeMillis()

            scope = CoroutineScope(Dispatchers.IO + Job())
            scope?.launch {
                val buffer = ShortArray(bufferSize)
                while (capturando) {
                    if (SensorStateHolder.capturaPausada.value) {
                        delay(500)
                        continue
                    }
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        val ahora = System.currentTimeMillis()
                        val deltaSeg = (ahora - ultimoTickMs) / 1000.0
                        ultimoTickMs = ahora

                        val nivelDb = SoundAnalyzer.calcularDbA(buffer, readSize)
                        ventanaPromedio.addLast(nivelDb)
                        if (ventanaPromedio.size > VENTANA_PROMEDIO) ventanaPromedio.removeFirst()
                        val promedio = ventanaPromedio.average()
                        val maximoVentana = ventanaPromedio.maxOrNull() ?: promedio
                        if (maximoVentana > maximoSesionDb) maximoSesionDb = maximoVentana

                        if (promedio >= SoundAnalyzer.UMBRAL_OMS_SEGURO) {
                            minutosAcumSobre65 += deltaSeg / 60.0
                        }
                        if (promedio >= SoundAnalyzer.UMBRAL_OMS_PELIGRO) {
                            minutosAcumSobre80 += deltaSeg / 60.0
                        }

                        _decibelios.value = SoundAnalyzer.redondear(promedio, 1)
                        withContext(Dispatchers.Main) {
                            SensorStateHolder.decibelios.value = _decibelios.value
                            SensorStateHolder.nivelMaximo.value = SoundAnalyzer.redondear(maximoSesionDb, 1)
                        }

                        if (ahora - ultimoGuardadoMs >= INTERVALO_GUARDADO_MS) {
                            ultimoGuardadoMs = ahora
                            persistirExposicion(appContext)
                        }
                    }
                    delay(150)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun detener(context: Context) {
        if (!capturando) return
        capturando = false
        persistirExposicion(context.applicationContext)
        scope?.cancel()
        scope = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioRecord = null
    }

    /** Carga la exposición acumulada del día desde la base de datos. */
    fun cargarExposicionDelDia(context: Context) {
        val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val db = AppDatabase.getDatabase(context.applicationContext)
        CoroutineScope(Dispatchers.IO + Job()).launch {
            try {
                val registro = db.registroExposicionDao().obtenerPorFecha(fecha)
                withContext(Dispatchers.Main) {
                    if (registro != null) {
                        minutosAcumSobre65 = registro.minutosSobre65
                        minutosAcumSobre80 = registro.minutosSobre80
                        maximoSesionDb = registro.nivelMaximoDb
                        SensorStateHolder.dosisSobre65.value = SoundAnalyzer.redondear(registro.minutosSobre65, 1)
                        SensorStateHolder.dosisSobre80.value = SoundAnalyzer.redondear(registro.minutosSobre80, 1)
                        SensorStateHolder.nivelMaximo.value = SoundAnalyzer.redondear(registro.nivelMaximoDb, 1)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** Persiste la exposición acumulada en la base de datos (upsert por fecha). */
    fun persistirExposicion(context: Context) {
        val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val minutos65 = SoundAnalyzer.redondear(minutosAcumSobre65, 1)
        val minutos80 = SoundAnalyzer.redondear(minutosAcumSobre80, 1)
        val db = AppDatabase.getDatabase(context.applicationContext)
        CoroutineScope(Dispatchers.IO + Job()).launch {
            try {
                val anterior = db.registroExposicionDao().obtenerPorFecha(fecha)
                val maximo = maxOf(anterior?.nivelMaximoDb ?: 0.0, maximoSesionDb)
                db.registroExposicionDao().insertar(
                    RegistroExposicion(
                        fecha = fecha,
                        minutosSobre65 = minutos65,
                        minutosSobre80 = minutos80,
                        nivelMaximoDb = maximo
                    )
                )
                withContext(Dispatchers.Main) {
                    SensorStateHolder.dosisSobre65.value = minutos65
                    SensorStateHolder.dosisSobre80.value = minutos80
                    SensorStateHolder.nivelMaximo.value = SoundAnalyzer.redondear(maximo, 1)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
