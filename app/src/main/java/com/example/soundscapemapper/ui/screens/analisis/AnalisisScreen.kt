package com.example.soundscapemapper.ui.screens.analisis

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.soundscapemapper.AppDatabase
import com.example.soundscapemapper.Medicion
import com.example.soundscapemapper.NivelRuido
import com.example.soundscapemapper.SoundAnalyzer
import com.example.soundscapemapper.sensor.SensorStateHolder
import com.example.soundscapemapper.ui.components.AmbarSalud
import com.example.soundscapemapper.ui.components.AzulInfo
import com.example.soundscapemapper.ui.components.MedidorArco
import com.example.soundscapemapper.ui.components.RojoSalud
import com.example.soundscapemapper.ui.components.TarjetaMetrica
import com.example.soundscapemapper.ui.components.VerdeSalud
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalisisScreen(
    db: AppDatabase,
    sensorState: SensorStateHolder,
    onReanudarCaptura: () -> Unit,
    onTerminar: () -> Unit
) {
    val contexto = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var muestreando by remember { mutableStateOf(false) }
    var segundosRestantes by remember { mutableStateOf(5) }
    var muestras by remember { mutableStateOf(listOf<Double>()) }
    var resultado by remember { mutableStateOf<String?>(null) }
    var dbFinal by remember { mutableStateOf(0.0) }
    var nombreLugar by remember { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }

    val hayMicrofono = ContextCompat.checkSelfPermission(
        contexto, Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(Unit) {
        if (sensorState.capturaPausada.value) {
            onReanudarCaptura()
        }
        muestreando = true
        muestras = emptyList()
        for (s in 5 downTo 1) {
            segundosRestantes = s
            muestras = muestras + sensorState.decibelios.value
            delay(1000)
        }
        val promedio = muestras.average()
        val nivel = SoundAnalyzer.clasificarNivel(promedio)
        dbFinal = SoundAnalyzer.redondear(promedio, 1)
        resultado = if (nivel == NivelRuido.PELIGRO || sensorState.nivelLuz.value > 2500) {
            "Estresante"
        } else {
            "Tranquilo"
        }
        muestreando = false
    }

    val valorMostrado = if (resultado != null) dbFinal else sensorState.decibelios.value
    val valorAnimado by animateFloatAsState(
        targetValue = valorMostrado.toFloat(),
        animationSpec = tween(400),
        label = "db"
    )
    val colorNivel = Color(SoundAnalyzer.clasificarNivel(valorMostrado).colorHex)

    fun guardar() {
        guardando = true
        scope.launch(Dispatchers.IO) {
            try {
                val nueva = Medicion(
                    nombreLugar = nombreLugar.trim().ifBlank { "Lugar sin nombre" },
                    categoria = resultado ?: "Tranquilo",
                    decibelios = dbFinal,
                    nivelLuz = sensorState.nivelLuz.value,
                    latitud = sensorState.latitud.value,
                    longitud = sensorState.longitud.value,
                    fechaHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                )
                db.medicionDao().insertarMedicion(nueva)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        onTerminar()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onTerminar) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Spacer(Modifier.width(4.dp))
            Text("Analizar entorno", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        if (!hayMicrofono) {
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AmbarSalud.copy(alpha = 0.14f))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🎙️", fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Sin permiso de micrófono las lecturas serán 0 dB. Concede el permiso en Ajustes.",
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (muestreando) "Midiendo el nivel de sonido..." else "Resultado del análisis",
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Box(contentAlignment = Alignment.Center) {
                    MedidorArco(valor = valorAnimado.toDouble(), color = colorNivel)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${valorAnimado.toInt()}",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorNivel
                        )
                        Text("decibelios", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (muestreando) {
                    Spacer(Modifier.height(10.dp))
                    EspectroMuestras(muestras)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Muestreo... $segundosRestantes s", fontSize = 12.sp)
                    }
                } else {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Promedio de 5 segundos",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TarjetaMetrica("Luz", "${sensorState.nivelLuz.value.toInt()}", "lux", AzulInfo)
            TarjetaMetrica(
                "GPS",
                String.format(Locale.US, "%.3f, %.3f", sensorState.latitud.value, sensorState.longitud.value),
                "",
                AzulInfo
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TarjetaMetrica("Máx hoy", "${sensorState.nivelMaximo.value.toInt()}", "dB", RojoSalud)
            TarjetaMetrica(
                "Estado",
                SoundAnalyzer.clasificarNivel(valorMostrado).etiqueta,
                "",
                colorNivel
            )
        }

        resultado?.let { cat ->
            val tranquilo = cat == "Tranquilo"
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (tranquilo) VerdeSalud.copy(alpha = 0.12f) else RojoSalud.copy(alpha = 0.12f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (tranquilo) "🌿" else "⚠️", fontSize = 30.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Lugar ${if (tranquilo) "Tranquilo" else "Estresante"}",
                            fontWeight = FontWeight.Bold,
                            color = if (tranquilo) VerdeSalud else RojoSalud
                        )
                        Text(
                            text = if (tranquilo) {
                                "Ambiente cómodo y adecuado para la concentración."
                            } else if (sensorState.nivelLuz.value > 2500 && SoundAnalyzer.clasificarNivel(dbFinal) != NivelRuido.PELIGRO) {
                                "Iluminación extrema. Busca un lugar con luz más suave."
                            } else {
                                SoundAnalyzer.clasificarNivel(dbFinal).recomendacion
                            },
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = nombreLugar,
                onValueChange = { nombreLugar = it },
                label = { Text("Asigna un nombre a este lugar") },
                placeholder = { Text("Ej: Biblioteca, Cafetería...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onTerminar,
                    modifier = Modifier.weight(1f),
                    enabled = !guardando
                ) {
                    Text("Descartar")
                }
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        guardar()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !guardando,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (tranquilo) VerdeSalud else RojoSalud
                    )
                ) {
                    if (guardando) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Guardando...")
                    } else {
                        Text("Guardar lugar")
                    }
                }
            }
        }
    }
}

@Composable
private fun EspectroMuestras(muestras: List<Double>) {
    if (muestras.isEmpty()) return
    val visibles = muestras.takeLast(8)
    val max = visibles.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        visibles.forEach { valor ->
            val color = Color(SoundAnalyzer.clasificarNivel(valor).colorHex)
            val altura = (valor / max).toFloat().coerceIn(0.08f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(altura)
                    .background(color, RoundedCornerShape(4.dp))
            )
        }
    }
}

