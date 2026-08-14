package com.example.soundscapemapper.ui.screens.yo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soundscapemapper.Configuracion
import com.example.soundscapemapper.sensor.SensorStateHolder
import com.example.soundscapemapper.ui.components.RojoSalud
import com.example.soundscapemapper.ui.components.VerdeSalud

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Guía y ajustes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Configura el monitoreo y aprende a leer tu entorno",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        Text("Ajustes", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                FilaInterruptor(
                    icono = Icons.Filled.NotificationsActive,
                    titulo = "Monitoreo en segundo plano",
                    descripcion = "Escucha el ruido aunque la app esté en segundo plano.",
                    activo = sensorState.monitoreoActivo.value,
                    color = VerdeSalud,
                    onToggle = onToggleServicio
                )
                Spacer(Modifier.height(8.dp))
                FilaInterruptor(
                    icono = Icons.Filled.Pause,
                    titulo = "Pausar captura de audio",
                    descripcion = "Detiene temporalmente el micrófono (ej. por privacidad).",
                    activo = sensorState.capturaPausada.value,
                    color = RojoSalud,
                    onToggle = onAlternarCaptura
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Umbral de alerta", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "Se recomienda protección a partir de ${umbral.toInt()} dB",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("${umbral.toInt()} dB", fontWeight = FontWeight.Bold, color = VerdeSalud)
                }
                Slider(
                    value = umbral,
                    onValueChange = {
                        umbral = it
                        onCambiarUmbral(it)
                    },
                    valueRange = Configuracion.UMBRAL_ALERTA_MINIMO.toFloat()..Configuracion.UMBRAL_ALERTA_MAXIMO.toFloat(),
                    steps = 7,
                    colors = SliderDefaults.colors(
                        thumbColor = VerdeSalud,
                        activeTrackColor = VerdeSalud
                    )
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("¿Cómo se mide el ruido?", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "El sonido se mide en decibelios (dB). Por debajo de 65 dB el ambiente suele ser cómodo; " +
                        "a partir de 80 dB puede dañar tu audición si se sostiene en el tiempo. La app estima el nivel " +
                        "con el micrófono, mide la luz ambiental (lux) y usa el GPS para ubicar cada medición.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("Escala de niveles", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        FilaNivel("0–65 dB", "Tranquilo", "Conversación, biblioteca, parque.", Verde)
        FilaNivel("65–80 dB", "Precaución", "Tráfico, cafetería con gente.", Ambar)
        FilaNivel("80+ dB", "Peligro", "Perforadora, concierto, bocina.", Rojo)

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Dosis diaria de ruido", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "La app acumula puntos de dosis mientras escucha:\n" +
                        "• 1 minuto entre 65 y 80 dB = 1 punto\n" +
                        "• 1 minuto a 80 dB o más = 4 puntos\n" +
                        "La meta diaria recomendada es 480 puntos. Superarla en rojo avisa de exposición excesiva.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Recomendaciones", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "• En zonas de 80+ dB protégete con audífonos de seguridad.\n" +
                        "• Busca espacios tranquilos para estudiar o descansar.\n" +
                        "• Una luz muy intensa (más de 2500 lux) también puede resultar estresante.\n" +
                        "• Mide el mismo lugar a distintas horas para elegir el mejor momento.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
private fun FilaInterruptor(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    descripcion: String,
    activo: Boolean,
    color: Color,
    onToggle: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = if (activo) color else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(descripcion, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
}

@Composable
private fun FilaNivel(rango: String, nombre: String, descripcion: String, color: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = color.copy(alpha = 0.15f)) {
                Column(
                    modifier = Modifier.padding(10.dp),
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

private val Verde = Color(0xFF2E7D32)
private val Ambar = Color(0xFFF9A825)
private val Rojo = Color(0xFFC62828)
