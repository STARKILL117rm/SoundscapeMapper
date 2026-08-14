package com.example.soundscapemapper

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.soundscapemapper.sensor.SensorStateHolder
import com.example.soundscapemapper.ui.navigation.EspacioSeguroApp
import com.example.soundscapemapper.ui.theme.SoundscapeMapperTheme
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity(), SensorEventListener {

    private val sensorState = SensorStateHolder()

    private lateinit var sensorManager: SensorManager
    private var sensorLuz: Sensor? = null

    private var isRecording = false
    private var audioRecord: AudioRecord? = null

    // Acumuladores de exposición diaria (dosis de ruido)
    private var minutosAcumSobre65 = 0.0
    private var minutosAcumSobre80 = 0.0
    private var maximoSesionDb = 0.0

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
            iniciarCapturaAudio()
        }
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            obtenerUbicacionActual()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        sensorState.capturaPausada.value = Configuracion.capturaPausada(this)
        sensorState.monitoreoActivo.value = Configuracion.servicioActivo(this)
        if (sensorState.monitoreoActivo.value) {
            iniciarServicio()
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorLuz = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        val permisos = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permisos.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionLauncher.launch(permisos.toTypedArray())
        obtenerUbicacionActual()

        val db = AppDatabase.getDatabase(applicationContext)
        cargarExposicionDelDia(db)

        setContent {
            SoundscapeMapperTheme {
                EspacioSeguroApp(
                    db = db,
                    sensorState = sensorState,
                    onToggleServicio = { activo -> toggleServicio(activo) },
                    onAlternarCaptura = { pausada -> alternarCaptura(pausada) },
                    onCambiarUmbral = { valor ->
                        sensorState.let {
                            Configuracion.guardarUmbralAlerta(this, valor.toDouble())
                        }
                    }
                )
            }
        }
    }

    private fun iniciarServicio() {
        try {
            val intent = Intent(this, AudioMonitorService::class.java)
            ContextCompat.startForegroundService(this, intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toggleServicio(activo: Boolean) {
        sensorState.monitoreoActivo.value = activo
        Configuracion.guardarServicioActivo(this, activo)
        try {
            val intent = Intent(this, AudioMonitorService::class.java)
            if (activo) {
                ContextCompat.startForegroundService(this, intent)
            } else {
                stopService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun alternarCaptura(pausada: Boolean) {
        sensorState.capturaPausada.value = pausada
        Configuracion.guardarCapturaPausada(this, pausada)
    }

    private fun obtenerUbicacionActual() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        sensorState.latitud.value = location.latitude
                        sensorState.longitud.value = location.longitude
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            sensorState.nivelLuz.value = event.values[0]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onResume() {
        super.onResume()
        sensorLuz?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    private fun iniciarCapturaAudio() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
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
            isRecording = true

            val dbLocal = AppDatabase.getDatabase(applicationContext)
            val ventanaPromedio = ArrayDeque<Double>()
            var ultimoTickMs = System.currentTimeMillis()
            var ultimoGuardadoMs = System.currentTimeMillis()

            lifecycleScope.launch(Dispatchers.IO) {
                val buffer = ShortArray(bufferSize)
                while (isRecording) {
                    if (sensorState.capturaPausada.value) {
                        delay(500)
                        continue
                    }
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        val ahora = System.currentTimeMillis()
                        val deltaSeg = (ahora - ultimoTickMs) / 1000.0
                        ultimoTickMs = ahora

                        val db = SoundAnalyzer.calcularDbA(buffer, readSize)
                        ventanaPromedio.addLast(db)
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

                        withContext(Dispatchers.Main) {
                            sensorState.decibelios.value = SoundAnalyzer.redondear(promedio, 1)
                            sensorState.nivelMaximo.value = SoundAnalyzer.redondear(maximoSesionDb, 1)
                        }

                        if (ahora - ultimoGuardadoMs >= 15_000) {
                            ultimoGuardadoMs = ahora
                            persistirExposicion(dbLocal)
                        }
                    }
                    delay(150)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cargarExposicionDelDia(db: AppDatabase) {
        val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val registro = db.registroExposicionDao().obtenerPorFecha(fecha)
                withContext(Dispatchers.Main) {
                    if (registro != null) {
                        minutosAcumSobre65 = registro.minutosSobre65
                        minutosAcumSobre80 = registro.minutosSobre80
                        maximoSesionDb = registro.nivelMaximoDb
                        sensorState.dosisSobre65.value = SoundAnalyzer.redondear(registro.minutosSobre65, 1)
                        sensorState.dosisSobre80.value = SoundAnalyzer.redondear(registro.minutosSobre80, 1)
                        sensorState.nivelMaximo.value = SoundAnalyzer.redondear(registro.nivelMaximoDb, 1)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun persistirExposicion(db: AppDatabase) {
        val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val minutos65 = SoundAnalyzer.redondear(minutosAcumSobre65, 1)
        val minutos80 = SoundAnalyzer.redondear(minutosAcumSobre80, 1)
        lifecycleScope.launch(Dispatchers.IO) {
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
                    sensorState.dosisSobre65.value = minutos65
                    sensorState.dosisSobre80.value = minutos80
                    sensorState.nivelMaximo.value = SoundAnalyzer.redondear(maximo, 1)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRecording = false
        if (minutosAcumSobre65 > 0.0 || minutosAcumSobre80 > 0.0) {
            persistirExposicion(AppDatabase.getDatabase(applicationContext))
        }
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        // Ventana de promedio móvil (~5 s con lecturas cada ~150 ms)
        private const val VENTANA_PROMEDIO = 30
    }
}
