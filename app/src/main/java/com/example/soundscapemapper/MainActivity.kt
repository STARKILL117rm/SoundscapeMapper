package com.example.soundscapemapper

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.soundscapemapper.sensor.SensorStateHolder
import com.example.soundscapemapper.ui.navigation.EspacioSeguroApp
import com.example.soundscapemapper.ui.screens.splash.SplashScreen
import com.example.soundscapemapper.ui.theme.SoundscapeMapperTheme
import com.google.android.gms.location.LocationServices

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var sensorLuz: Sensor? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
            AudioEngine.iniciar(this)
        }
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            obtenerUbicacionActual()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        SensorStateHolder.capturaPausada.value = Configuracion.capturaPausada(this)
        SensorStateHolder.monitoreoActivo.value = Configuracion.servicioActivo(this)
        if (SensorStateHolder.monitoreoActivo.value) {
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

        AudioEngine.cargarExposicionDelDia(this)
        NotificacionesProgramador.programar(this)
        obtenerUbicacionActual()

        setContent {
            SoundscapeMapperTheme {
                var mostrarSplash by remember { mutableStateOf(true) }

                Crossfade(
                    targetState = mostrarSplash,
                    animationSpec = tween(500),
                    label = "splash_fade"
                ) { enSplash ->
                    if (enSplash) {
                        SplashScreen(
                            onFinished = { mostrarSplash = false }
                        )
                    } else {
                        EspacioSeguroApp(
                            db = AppDatabase.getDatabase(applicationContext),
                            sensorState = SensorStateHolder,
                            onToggleServicio = { activo -> toggleServicio(activo) },
                            onAlternarCaptura = { pausada -> alternarCaptura(pausada) },
                            onCambiarUmbral = { valor ->
                                Configuracion.guardarUmbralAlerta(this, valor.toDouble())
                            }
                        )
                    }
                }
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
        SensorStateHolder.monitoreoActivo.value = activo
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
        SensorStateHolder.capturaPausada.value = pausada
        Configuracion.guardarCapturaPausada(this, pausada)
    }

    private fun obtenerUbicacionActual() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        SensorStateHolder.latitud.value = location.latitude
                        SensorStateHolder.longitud.value = location.longitude
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            SensorStateHolder.nivelLuz.value = event.values[0]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onResume() {
        super.onResume()
        sensorLuz?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        AudioEngine.adquirir(this)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        AudioEngine.liberar(this)
    }
}
