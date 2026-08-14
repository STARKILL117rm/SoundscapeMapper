package com.example.soundscapemapper.ui.screens.hoy

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.soundscapemapper.AppDatabase
import com.example.soundscapemapper.NivelRuido
import com.example.soundscapemapper.SoundAnalyzer
import com.example.soundscapemapper.sensor.SensorStateHolder
import com.example.soundscapemapper.ui.components.AmbarSalud
import com.example.soundscapemapper.ui.components.AnilloProgreso
import com.example.soundscapemapper.ui.components.AzulInfo
import com.example.soundscapemapper.ui.components.BarraSemana
import com.example.soundscapemapper.ui.components.InsigniaCategoria
import com.example.soundscapemapper.ui.components.RojoSalud
import com.example.soundscapemapper.ui.components.TarjetaMetrica
import com.example.soundscapemapper.ui.components.VerdeSalud
import com.example.soundscapemapper.ui.navigation.Rutas
import com.example.soundscapemapper.ui.viewmodel.HoyViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun HoyScreen(
    db: AppDatabase,
    sensorState: SensorStateHolder,
    onAnalizar: () -> Unit,
    onAlternarCaptura: (Boolean) -> Unit,
    onIrA: (String) -> Unit
) {
    val vm: HoyViewModel = viewModel(factory = HoyViewModel.factory(db))
    LaunchedEffect(Unit) { vm.cargar() }

    val decibelios = sensorState.decibelios.value
    val nivel = SoundAnalyzer.clasificarNivel(decibelios)
    val puntosHoy = SoundAnalyzer.puntosDosis(
        sensorState.dosisSobre65.value,
        sensorState.dosisSobre80.value
    )
    val progresoDosis = (puntosHoy / SoundAnalyzer.META_DOSIS_PUNTOS).toFloat()
    val colorDosis = when {
        progresoDosis >= 1f -> RojoSalud
        progresoDosis >= 0.5f -> AmbarSalud
        else -> VerdeSalud
    }
    val animado by animateFloatAsState(
        targetValue = progresoDosis.coerceIn(0f, 1f),
        animationSpec = tween(900),
        label = "dosis"
    )

    val locale = LocalConfiguration.current.locales[0]
    val fechaHoy = SimpleDateFormat("EEEE, d 'de' MMMM", locale).format(Date())
        .replaceFirstChar { it.titlecase(locale) }
    val saludo = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Buenos días"
        in 12..17 -> "Buenas tardes"
        else -> "Buenas noches"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$saludo 👋",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = fechaHoy,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                EstadoCaptura(
                    pausada = sensorState.capturaPausada.value,
                    onToggle = { onAlternarCaptura(!sensorState.capturaPausada.value) }
                )
            }
        }

        item {
            HeroDosis(
                decibelios = decibelios,
                puntosHoy = puntosHoy,
                progresoAnimado = animado,
                colorDosis = colorDosis,
                nivel = nivel,
                pausada = sensorState.capturaPausada.value,
                onAnalizar = onAnalizar
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AccionRapida("Analizar", Icons.Filled.GraphicEq, VerdeSalud, onAnalizar)
                AccionRapida("Mapa", Icons.Filled.Map, AzulInfo, { onIrA(Rutas.MAPA) })
                AccionRapida("Registro", Icons.AutoMirrored.Filled.ArrowForward, AmbarSalud, { onIrA(Rutas.REGISTRO) })
                AccionRapida("Guía", Icons.Filled.Person, MaterialTheme.colorScheme.primary, { onIrA(Rutas.YO) })
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TarjetaMetrica("Máx hoy", "${sensorState.nivelMaximo.value.toInt()}", "dB", RojoSalud)
                TarjetaMetrica("≥65 dB", "${sensorState.dosisSobre65.value.toInt()}", "min", AmbarSalud)
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TarjetaMetrica("≥80 dB", "${sensorState.dosisSobre80.value.toInt()}", "min", RojoSalud)
                TarjetaMetrica("Luz", "${sensorState.nivelLuz.value.toInt()}", "lux", AzulInfo)
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Exposición esta semana", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(
                            "Meta 480 pts",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "Puntos de dosis de ruido por día (1 min 65-80 dB = 1 pt · ≥80 dB = 4 pts)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    BarraSemana(
                        puntos = vm.puntosSemana,
                        etiquetas = vm.diasSemana,
                        indiceHoy = vm.diasSemana.size - 1
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Últimos lugares", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        if (vm.recientes.isNotEmpty()) {
                            TextButton(onClick = { onIrA(Rutas.REGISTRO) }) {
                                Text("Ver todos", fontSize = 12.sp)
                            }
                        }
                    }
                    if (vm.recientes.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🗺️", fontSize = 34.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Aún no has medido ningún lugar.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                onClick = onAnalizar,
                                shape = RoundedCornerShape(10.dp),
                                color = VerdeSalud
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Analizar un lugar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    } else {
                        vm.recientes.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (item.categoria == "Tranquilo") "🌿" else "⚠️", fontSize = 20.sp)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.nombreLugar, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        "${item.decibelios} dB · ${item.fechaHora}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                InsigniaCategoria(tranquilo = item.categoria == "Tranquilo")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EstadoCaptura(pausada: Boolean, onToggle: () -> Unit) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(14.dp),
        color = if (pausada) AmbarSalud.copy(alpha = 0.14f) else VerdeSalud.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (pausada) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (pausada) "Reanudar escucha" else "Pausar escucha",
                tint = if (pausada) AmbarSalud else VerdeSalud,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                if (pausada) "En pausa" else "Escuchando",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (pausada) AmbarSalud else VerdeSalud
            )
        }
    }
}

@Composable
private fun HeroDosis(
    decibelios: Double,
    puntosHoy: Double,
    progresoAnimado: Float,
    colorDosis: Color,
    nivel: NivelRuido,
    pausada: Boolean,
    onAnalizar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(nivel.colorHex).copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnilloProgreso(
                progreso = progresoAnimado,
                color = colorDosis,
                size = 120.dp,
                strokeWidth = 12.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$decibelios",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(nivel.colorHex)
                    )
                    Text("dB ahora", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Dosis de ruido de hoy", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    "${puntosHoy.toInt()} / ${SoundAnalyzer.META_DOSIS_PUNTOS.toInt()} pts · ${(progresoAnimado.coerceIn(0f, 1f) * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = colorDosis,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(nivel.colorHex), CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Estado: ${nivel.etiqueta}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(nivel.colorHex)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    nivel.recomendacion,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = onAnalizar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(
                            if (pausada) AmbarSalud.copy(alpha = 0.3f) else VerdeSalud,
                            RoundedCornerShape(12.dp)
                        ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        if (pausada) "Reanuda la escucha para medir" else "Analizar este lugar",
                        color = if (pausada) AmbarSalud else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.AccionRapida(
    etiqueta: String,
    icono: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icono,
                contentDescription = etiqueta,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(etiqueta, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = color)
        }
    }
}
