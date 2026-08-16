package com.example.soundscapemapper.ui.screens.yo

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.soundscapemapper.AppDatabase
import com.example.soundscapemapper.Configuracion
import com.example.soundscapemapper.Medicion
import com.example.soundscapemapper.sensor.SensorStateHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


// Paleta empática de la escala educativa (tonos pastel, nada alarmistas).
private val Menta = Color(0xFF81C784)
private val Ambar = Color(0xFFF0B27A)
private val Coral = Color(0xFFE07A5F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoScreen(
    sensorState: SensorStateHolder,
    db: AppDatabase,
    onToggleServicio: (Boolean) -> Unit,
    onAlternarCaptura: (Boolean) -> Unit,
    onCambiarUmbral: (Float) -> Unit
) {
    val contexto = LocalContext.current
    var umbral by remember {
        mutableStateOf(Configuracion.umbralAlerta(contexto).toFloat())
    }
    var pestana by remember { mutableIntStateOf(0) }

    // Estadísticas reales desde la BD
    var totalMediciones by remember { mutableStateOf(0) }
    var promedioDb by remember { mutableStateOf<Double?>(null) }
    var maximoDb by remember { mutableStateOf<Double?>(null) }
    var lugarMasRuidoso by remember { mutableStateOf<Medicion?>(null) }
    var lugarMasSilencioso by remember { mutableStateOf<Medicion?>(null) }
    var ultimasMediciones by remember { mutableStateOf<List<Medicion>>(emptyList()) }

    LaunchedEffect(pestana) {
        if (pestana == 2) { // Solo carga al entrar en "Mis Stats"
            withContext(Dispatchers.IO) {
                totalMediciones = db.medicionDao().contarMediciones()
                promedioDb = db.medicionDao().obtenerPromedioDecibelios()
                maximoDb = db.medicionDao().obtenerMaximoDecibelios()
                lugarMasRuidoso = db.medicionDao().obtenerLugarMasRuidoso()
                lugarMasSilencioso = db.medicionDao().obtenerLugarMasSilencioso()
                ultimasMediciones = db.medicionDao().obtenerUltimasMediciones(10)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Cabecera con alto contraste
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 14.dp)) {
                    Text(
                        "Guía y Ajustes",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Configura tu monitoreo y aprende a cuidar tu audición y confort visual",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            PrimaryTabRow(
                selectedTabIndex = pestana,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                listOf("Configuración", "Aprende OMS", "Mis Stats").forEachIndexed { idx, label ->
                    Tab(
                        selected = pestana == idx,
                        onClick = { pestana = idx },
                        text = {
                            Text(
                                label,
                                fontWeight = if (pestana == idx) FontWeight.Bold else FontWeight.Normal,
                                color = if (pestana == idx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
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
                1 -> SeccionAprende()
                else -> SeccionStats(
                    total = totalMediciones,
                    promedio = promedioDb,
                    maximo = maximoDb,
                    lugarMasRuidoso = lugarMasRuidoso,
                    lugarMasSilencioso = lugarMasSilencioso,
                    ultimasMediciones = ultimasMediciones
                )
            }
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
    var tipoAlertaSeleccionado by remember { mutableStateOf("Vibración") }

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
                    headlineContent = {
                        Text(
                            "Monitoreo en segundo plano",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    supportingContent = {
                        Text(
                            "Escucha el ruido aunque la app esté en segundo plano.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                    headlineContent = {
                        Text(
                            "Pausar captura de audio",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    supportingContent = {
                        Text(
                            "Detiene temporalmente el micrófono (ej. por privacidad).",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                            Text(
                                "Umbral de alerta auditiva",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Te avisaremos cuando el entorno supere este nivel de riesgo",
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

        // Selector de Tipo de Alerta (Vibración / Notificación / Aviso sonoro)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Modo de Alerta Preferido",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Selecciona el tipo de aviso al superar tu umbral de ruido",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val opciones = listOf("🔕 Vibración", "🔔 Notificación", "🔊 Sonido")
                    opciones.forEach { opcion ->
                        val seleccionada = tipoAlertaSeleccionado == opcion
                        FilterChip(
                            selected = seleccionada,
                            onClick = { tipoAlertaSeleccionado = opcion },
                            label = { Text(opcion, fontSize = 12.sp, fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Tarjeta del Sensor de Luz y Confort Visual (Alineada al PDF)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Ambar.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("💡", fontSize = 20.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Sensor de Confort Visual (Luz)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Evalúa la iluminación del lugar junto al ruido (Ideal para estudiar: 300 - 750 lux).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Tarjeta de Recomendación OMS dinámicamente según el Umbral
        val (colorOMS, textoOMS) = when {
            umbral < 65f -> Pair(Menta, "Umbral Nivel Seguro (<65 dB): Ideal para concentración y descanso prolongado.")
            umbral <= 80f -> Pair(Ambar, "Umbral de Precaución (65 - 80 dB): Nivel moderado. Recomendado limitar exposición prolongada.")
            else -> Pair(Coral, "Umbral de Riesgo (>80 dB): OMS advierte que la exposición continua puede causar fatiga mental o daño irreversible.")
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colorOMS.copy(alpha = 0.14f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("⚠️", fontSize = 20.sp)
                Spacer(Modifier.width(12.dp))
                Text(
                    textoOMS,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
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
        val anchoPildora = 90.dp
        val posicionX = (ancho * fraccion - anchoPildora / 2).coerceIn(0.dp, ancho - anchoPildora)

        val (colorUmbral, etiquetaEstado) = when {
            umbral < 65f -> Pair(Menta, "Seguro")
            umbral <= 80f -> Pair(Ambar, "Moderado")
            else -> Pair(Coral, "Riesgo")
        }

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
                    color = colorUmbral.copy(alpha = 0.25f),
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = "${umbral.toInt()} dB · $etiquetaEstado",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorUmbral,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Slider(
                value = umbral,
                onValueChange = onCambiar,
                valueRange = minimo..maximo,
                steps = 7,
                colors = SliderDefaults.colors(
                    thumbColor = colorUmbral,
                    activeTrackColor = colorUmbral
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // CALCULADORA INTERACTIVA DE EXPOSICIÓN OMS
        CalculadoraExposicionOMS()

        TarjetaInformativa(
            emoji = "📖",
            color = Menta,
            titulo = "Monitoreo Multisensor (OMS)",
            cuerpo = "De acuerdo con la Organización Mundial de la Salud (OMS), el ruido ambiental constante deteriora la concentración, causa fatiga mental e incrementa el estrés. Esta app combina decibelios (dB), luz (lux) y GPS para geolocalizar espacios de confort."
        )

        Text(
            "Escala de niveles de ruido",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        FilaNivel(rango = "0 – 65 dB", nombre = "Tranquilo (Saludable)", descripcion = "Biblioteca, parque, zona de estudio.", color = Menta)
        FilaNivel(rango = "65 – 80 dB", nombre = "Precaución (Tráfico/Café)", descripcion = "Exposición moderada. Genera distracción.", color = Ambar)
        FilaNivel(rango = "80+ dB", nombre = "Peligro (Daño Mixto)", descripcion = "Zonas industriales, bocinas, conciertos.", color = Coral)

        Text(
            "Confort Visual e Iluminación (Lux)",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        TarjetaLista(
            emoji = "💡",
            color = Ambar,
            titulo = "Recomendaciones de Luz",
            elementos = listOf(
                "100 - 250 lux: Adecuado para descanso y relajación",
                "300 - 750 lux: Ideal para lectura, trabajo remoto y estudio",
                "Más de 2500 lux: Luz directa que puede producir deslumbramiento y fatiga ocular",
                "Busca equilibrio entre sonido bajo (<65 dB) y buena luz (>300 lux)"
            )
        )

        Text(
            "Dosis Diaria y Prevención",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        TarjetaLista(
            emoji = "📊",
            color = AzulInfo,
            titulo = "Hábitos Saludables Auditivos",
            elementos = listOf(
                "Utiliza tapones o audífonos con cancelación en lugares > 80 dB",
                "Aplica la regla 60/60 con tus audífonos (máx 60% de volumen por 60 min)",
                "Mide los mismos espacios en distintos horarios para planear tu viaje",
                "Revisa tu historial de exposición diario para mantener tu bienestar"
            )
        )
    }
}

// Calculadora interactiva de tiempo de exposición segura según la OMS
@Composable
private fun CalculadoraExposicionOMS() {
    var dbCalculador by remember { mutableStateOf(80f) }

    val (tiempoResumen, colorResumen) = when {
        dbCalculador < 75f -> Pair("Tiempo Ilimitado (Ambiente seguro y saludable)", Menta)
        dbCalculador <= 80f -> Pair("Hasta 8 horas continuas", Menta)
        dbCalculador <= 85f -> Pair("Hasta 2 horas continuas", Ambar)
        dbCalculador <= 90f -> Pair("Hasta 45 minutos continuos", Ambar)
        dbCalculador <= 98f -> Pair("Hasta 15 minutos continuos", Coral)
        else -> Pair("Menos de 2 minutos (Riesgo inminente)", Coral)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(colorResumen.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⏱️", fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Calculadora de Exposición OMS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Desliza para calcular el tiempo máximo seguro",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${dbCalculador.toInt()} dB",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResumen
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colorResumen.copy(alpha = 0.15f)
                ) {
                    Text(
                        tiempoResumen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResumen,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Slider(
                value = dbCalculador,
                onValueChange = { dbCalculador = it },
                valueRange = 60f..105f,
                steps = 8,
                colors = SliderDefaults.colors(
                    thumbColor = colorResumen,
                    activeTrackColor = colorResumen
                )
            )
        }
    }
}

@Composable
private fun TarjetaInformativa(emoji: String, color: Color, titulo: String, cuerpo: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.20f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 19.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    titulo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    cuerpo,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FilaNivel(rango: String, nombre: String, descripcion: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = color.copy(alpha = 0.20f)) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
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
                        .background(color.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 19.sp)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    titulo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private val AzulInfo = Color(0xFF5B8DB8)

// ─────────────────────────────────────────────────────
//  SECCIÓN MIS STATS — datos reales de la BD
// ─────────────────────────────────────────────────────

@Composable
private fun SeccionStats(
    total: Int,
    promedio: Double?,
    maximo: Double?,
    lugarMasRuidoso: Medicion?,
    lugarMasSilencioso: Medicion?,
    ultimasMediciones: List<Medicion>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Tarjetas resumen con colores nítidos y alto contraste
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TarjetaStatItem(
                modifier = Modifier.weight(1f),
                emoji = "📊",
                titulo = "Total",
                valor = "$total",
                subtitulo = "mediciones",
                colorFondo = Color(0xFFEFF6FF),
                colorBorde = Color(0xFFBFDBFE),
                colorTitulo = Color(0xFF2563EB),
                colorValor = Color(0xFF1E3A8A)
            )
            TarjetaStatItem(
                modifier = Modifier.weight(1f),
                emoji = "📈",
                titulo = "Promedio",
                valor = "${promedio?.toInt() ?: "-"} dB",
                subtitulo = "histórico",
                colorFondo = Color(0xFFFFFBEB),
                colorBorde = Color(0xFFFDE68A),
                colorTitulo = Color(0xFFD97706),
                colorValor = Color(0xFF78350F)
            )
            TarjetaStatItem(
                modifier = Modifier.weight(1f),
                emoji = "🔴",
                titulo = "Máximo",
                valor = "${maximo?.toInt() ?: "-"} dB",
                subtitulo = "registrado",
                colorFondo = Color(0xFFFFF1F2),
                colorBorde = Color(0xFFFECDD3),
                colorTitulo = Color(0xFFE11D48),
                colorValor = Color(0xFF881337)
            )
        }

        if (total == 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎙️", fontSize = 36.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Sin mediciones aún",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Haz tu primera medición desde la pestaña Registro para ver tus estadísticas aquí.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            return
        }

        // Lugar más ruidoso
        lugarMasRuidoso?.let { lugar ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECDD3))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(lugar.contextoEmoji, fontSize = 26.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("📍 Lugar más ruidoso", fontSize = 11.sp, color = Color(0xFFE11D48), fontWeight = FontWeight.Bold)
                        Text(lugar.nombreLugar, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(lugar.fechaHora, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${lugar.decibelios.toInt()} dB", fontWeight = FontWeight.ExtraBold, color = Color(0xFFE11D48), fontSize = 19.sp)
                }
            }
        }

        // Lugar más silencioso
        lugarMasSilencioso?.let { lugar ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(lugar.contextoEmoji, fontSize = 26.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🌿 Lugar más silencioso", fontSize = 11.sp, color = Color(0xFF047857), fontWeight = FontWeight.Bold)
                        Text(lugar.nombreLugar, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(lugar.fechaHora, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${lugar.decibelios.toInt()} dB", fontWeight = FontWeight.ExtraBold, color = Color(0xFF047857), fontSize = 19.sp)
                }
            }
        }

        // Mini gráfica de las últimas 10 mediciones
        if (ultimasMediciones.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Últimas ${ultimasMediciones.size} mediciones",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(10.dp))
                    MiniGraficaBarras(
                        mediciones = ultimasMediciones.reversed(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ItemLeyendaStat(color = Menta, texto = "< 70 dB")
                        ItemLeyendaStat(color = Ambar, texto = "70-80 dB")
                        ItemLeyendaStat(color = Coral, texto = "> 80 dB")
                    }
                }
            }
        }

        // Consejo basado en estadísticas
        val consejo = when {
            promedio == null -> null
            promedio > 80 -> Triple("⚠️", Coral, "Tu promedio supera los 80 dB. Considera visitar lugares más silenciosos para proteger tu audición.")
            promedio > 70 -> Triple("🟡", Ambar, "Tu promedio está en zona de precaución. Busca más espacios tranquilos en tu rutina.")
            else -> Triple("🌿", Menta, "¡Excelente! Tu promedio está en zona segura. Sigues cuidando bien tu salud auditiva.")
        }
        consejo?.let { (emoji, color, texto) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(emoji, fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(texto, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MiniGraficaBarras(mediciones: List<Medicion>, modifier: Modifier = Modifier) {
    if (mediciones.isEmpty()) return
    val maxDb = mediciones.maxOf { it.decibelios }.coerceAtLeast(85.0)
    val colorVerde = Menta
    val colorAmbar = Ambar
    val colorRojo = Coral
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(mediciones) { animProgress.animateTo(1f, animationSpec = tween(700)) }
    val progress = animProgress.value
    Canvas(modifier = modifier) {
        val paddingBottom = 24.dp.toPx()
        val paddingTop = 8.dp.toPx()
        val alturaUtil = size.height - paddingBottom - paddingTop
        val anchoTotal = size.width
        val anchoBarras = anchoTotal / mediciones.size.toFloat()
        val gap = 0.25f
        mediciones.forEachIndexed { idx, m ->
            val proporcion = ((m.decibelios / maxDb).toFloat() * progress).coerceIn(0f, 1f)
            val altBarra = alturaUtil * proporcion
            val x = idx * anchoBarras + anchoBarras * gap / 2f
            val ancho = anchoBarras * (1f - gap)
            val y = paddingTop + alturaUtil - altBarra
            val color = when {
                m.decibelios < 70 -> colorVerde
                m.decibelios <= 80 -> colorAmbar
                else -> colorRojo
            }
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = 0.95f), color.copy(alpha = 0.5f)),
                    startY = y, endY = paddingTop + alturaUtil
                ),
                topLeft = Offset(x, y),
                size = Size(ancho, altBarra.coerceAtLeast(4.dp.toPx())),
                cornerRadius = CornerRadius(5.dp.toPx())
            )
        }
    }
}

@Composable
private fun TarjetaStatItem(
    modifier: Modifier,
    emoji: String,
    titulo: String,
    valor: String,
    subtitulo: String,
    colorFondo: Color,
    colorBorde: Color,
    colorTitulo: Color,
    colorValor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorFondo),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorBorde)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 18.sp)
            Spacer(Modifier.height(2.dp))
            Text(titulo, fontSize = 10.sp, color = colorTitulo, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(
                valor,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colorValor,
                textAlign = TextAlign.Center
            )
            Text(
                subtitulo,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ItemLeyendaStat(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(9.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(texto, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

