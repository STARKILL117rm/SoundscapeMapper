package com.example.soundscapemapper.ui.screens.yo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soundscapemapper.Configuracion
import com.example.soundscapemapper.sensor.SensorStateHolder

// Paleta empática de la escala educativa (tonos pastel, nada alarmistas).
private val Menta = Color(0xFF81C784)
private val Ambar = Color(0xFFF0B27A)
private val Coral = Color(0xFFE07A5F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoScreen(
    sensorState: SensorStateHolder,
    onToggleServicio: (Boolean) -> Unit,
    onAlternarCaptura: (Boolean) -> Unit,
    onCambiarUmbral: (Float) -> Unit
) {
    val contexto = LocalContext.current
    var umbral by remember {
        mutableStateOf(Configuracion.umbralAlerta(contexto).toFloat())
    }
    var pestana by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)) {
            Text("Guía y ajustes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Configura tu monitoreo y aprende a cuidar tu audición",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        PrimaryTabRow(
            selectedTabIndex = pestana,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = pestana == 0,
                onClick = { pestana = 0 },
                text = { Text("Configuración") }
            )
            Tab(
                selected = pestana == 1,
                onClick = { pestana = 1 },
                text = { Text("Aprende") }
            )
        }

        when (pestana) {
            0 -> SeccionConfiguracion(
                monitoreoActivo = sensorState.monitoreoActivo.value,
                capturaPausada = sensorState.capturaPausada.value,
                umbral = umbral,
                onToggleServicio = onToggleServicio,
                onAlternarCaptura = onAlternarCaptura,
                onCambiarUmbral = { nuevo ->
                    umbral = nuevo
                    onCambiarUmbral(nuevo)
                }
            )

            else -> SeccionAprende()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeccionConfiguracion(
    monitoreoActivo: Boolean,
    capturaPausada: Boolean,
    umbral: Float,
    onToggleServicio: (Boolean) -> Unit,
    onAlternarCaptura: (Boolean) -> Unit,
    onCambiarUmbral: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                ListItem(
                    leadingContent = {
                        Icon(
                            Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = if (monitoreoActivo) Menta else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    headlineContent = { Text("Monitoreo en segundo plano", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    supportingContent = {
                        Text("Escucha el ruido aunque la app esté en segundo plano.", fontSize = 12.sp)
                    },
                    trailingContent = {
                        SwitchInterruptor(
                            activo = monitoreoActivo,
                            color = Menta,
                            onToggle = onToggleServicio
                        )
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ListItem(
                    leadingContent = {
                        Icon(
                            Icons.Filled.Pause,
                            contentDescription = null,
                            tint = if (capturaPausada) Ambar else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    headlineContent = { Text("Pausar captura de audio", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    supportingContent = {
                        Text("Detiene temporalmente el micrófono (ej. por privacidad).", fontSize = 12.sp)
                    },
                    trailingContent = {
                        SwitchInterruptor(
                            activo = capturaPausada,
                            color = Ambar,
                            onToggle = onAlternarCaptura
                        )
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Umbral de alerta", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "Te avisaremos cuando el ruido supere este nivel",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    UmbralSlider(umbral = umbral, onCambiar = onCambiarUmbral)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("💡", fontSize = 20.sp)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Se recomienda proteger la audición a partir de los 65 dB. Usa este umbral como referencia de tu entorno.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SwitchInterruptor(activo: Boolean, color: Color, onToggle: (Boolean) -> Unit) {
    Switch(
        checked = activo,
        onCheckedChange = onToggle,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = color,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            uncheckedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

@Composable
private fun UmbralSlider(umbral: Float, onCambiar: (Float) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val ancho = maxWidth
        val minimo = Configuracion.UMBRAL_ALERTA_MINIMO.toFloat()
        val maximo = Configuracion.UMBRAL_ALERTA_MAXIMO.toFloat()
        val fraccion = ((umbral - minimo) / (maximo - minimo)).coerceIn(0f, 1f)
        val anchoPildora = 54.dp
        val posicionX = (ancho * fraccion - anchoPildora / 2).coerceIn(0.dp, ancho - anchoPildora)

        Column {
            Box(
                modifier = Modifier
                    .offset(x = posicionX)
                    .width(anchoPildora)
                    .height(30.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Menta.copy(alpha = 0.16f),
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = "${umbral.toInt()} dB",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00695C),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Slider(
                value = umbral,
                onValueChange = onCambiar,
                valueRange = minimo..maximo,
                steps = 7,
                colors = SliderDefaults.colors(
                    thumbColor = Menta,
                    activeTrackColor = Menta
                )
            )
        }
    }
}

@Composable
private fun SeccionAprende() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TarjetaInformativa(
            emoji = "📖",
            color = Menta,
            titulo = "¿Cómo se mide el ruido?",
            cuerpo = "El sonido se mide en decibelios (dB). Por debajo de 65 dB el ambiente suele ser cómodo; " +
                "a partir de 80 dB puede dañar tu audición si se sostiene en el tiempo. La app estima el nivel " +
                "con el micrófono, mide la luz ambiental (lux) y usa el GPS para ubicar cada medición."
        )

        Text("Escala de niveles", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        FilaNivel(rango = "0 – 65 dB", nombre = "Tranquilo", descripcion = "Conversación, biblioteca, parque.", color = Menta)
        FilaNivel(rango = "65 – 80 dB", nombre = "Precaución", descripcion = "Tráfico, cafetería con gente.", color = Ambar)
        FilaNivel(rango = "80+ dB", nombre = "Peligro", descripcion = "Perforadora, concierto, bocina.", color = Coral)

        Text("Dosis diaria de ruido", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        TarjetaLista(
            emoji = "📊",
            color = AzulInfo,
            titulo = "Tu meta diaria",
            elementos = listOf(
                "1 minuto entre 65 y 80 dB = 1 punto",
                "1 minuto a 80 dB o más = 4 puntos",
                "Meta diaria recomendada: 480 puntos",
                "Superarla en rojo avisa de exposición excesiva"
            )
        )

        Text("Recomendaciones", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        TarjetaLista(
            emoji = "✨",
            color = Menta,
            titulo = "Cuida tu audición",
            elementos = listOf(
                "En zonas de 80+ dB protégete con audífonos de seguridad",
                "Busca espacios tranquilos para estudiar o descansar",
                "Una luz muy intensa (más de 2500 lux) también puede estresarte",
                "Mide el mismo lugar a distintas horas para elegir el mejor momento"
            )
        )
    }
}

@Composable
private fun TarjetaInformativa(emoji: String, color: Color, titulo: String, cuerpo: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 19.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                Text(cuerpo, fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
    }
}

@Composable
private fun FilaNivel(rango: String, nombre: String, descripcion: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = color.copy(alpha = 0.18f)) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(rango, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color)
                Text(descripcion, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TarjetaLista(emoji: String, color: Color, titulo: String, elementos: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 19.sp)
                }
                Spacer(Modifier.width(12.dp))
                Text(titulo, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(12.dp))
            elementos.forEach { elemento ->
                Row(
                    modifier = Modifier.padding(vertical = 5.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(8.dp)
                            .background(color, CircleShape)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        elemento,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private val AzulInfo = Color(0xFF5B8DB8)
