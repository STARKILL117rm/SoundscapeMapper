package com.example.soundscapemapper.ui.screens.analisis

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BarChart
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.soundscapemapper.ui.components.VerdeSalud
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

private val EmojisDisponibles = listOf("📍", "☕", "📚", "🏞️", "🚗", "🎧", "🏠", "✈️")

// Paleta limpia de alto contraste
private val ColorFondoLimpio = Color(0xFFF8FAFC)
private val ColorTextoPrincipal = Color(0xFF0F172A)
private val ColorTextoSecundario = Color(0xFF64748B)
private val ColorBordeSuave = Color(0xFFE2E8F0)

private suspend fun obtenerUbicacionFresca(contexto: android.content.Context): Location? =
    suspendCancellableCoroutine { continuation ->
        try {
            if (ContextCompat.checkSelfPermission(
                    contexto, Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }
            val client = LocationServices.getFusedLocationProviderClient(contexto)
            client.lastLocation
                .addOnSuccessListener { loc -> continuation.resume(loc) }
                .addOnFailureListener { continuation.resume(null) }
                .addOnCanceledListener { continuation.resume(null) }
        } catch (e: Exception) {
            continuation.resume(null)
        }
    }

private fun colorParaDbs(db: Double): Color = when {
    db < 70.0  -> VerdeSalud
    db <= 80.0 -> AmbarSalud
    else       -> RojoSalud
}

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
    var contextoEmoji by remember { mutableStateOf("📍") }
    var guardando by remember { mutableStateOf(false) }

    // Historial desde BD
    var historial by remember { mutableStateOf<List<Medicion>>(emptyList()) }
    var promedioHistorico by remember { mutableStateOf<Double?>(null) }
    var maximoHistorico by remember { mutableStateOf<Double?>(null) }
    var totalMediciones by remember { mutableStateOf(0) }
    var lugarMasRuidoso by remember { mutableStateOf<Medicion?>(null) }

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

        // Cargar historial de la BD
        withContext(Dispatchers.IO) {
            historial = db.medicionDao().obtenerUltimasMediciones(10)
            promedioHistorico = db.medicionDao().obtenerPromedioDecibelios()
            maximoHistorico = db.medicionDao().obtenerMaximoDecibelios()
            totalMediciones = db.medicionDao().contarMediciones()
            lugarMasRuidoso = db.medicionDao().obtenerLugarMasRuidoso()
        }
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
                val ubicacion = obtenerUbicacionFresca(contexto)
                val nueva = Medicion(
                    nombreLugar = nombreLugar.trim().ifBlank { "Lugar sin nombre" },
                    categoria = resultado ?: "Tranquilo",
                    contextoEmoji = contextoEmoji,
                    decibelios = dbFinal,
                    nivelLuz = sensorState.nivelLuz.value,
                    latitud = ubicacion?.latitude ?: sensorState.latitud.value,
                    longitud = ubicacion?.longitude ?: sensorState.longitud.value,
                    fechaHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                )
                db.medicionDao().insertarMedicion(nueva)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        onTerminar()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ColorFondoLimpio
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onTerminar) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = ColorTextoPrincipal
                    )
                }
                Spacer(Modifier.width(4.dp))
                Column {
                    Text(
                        "Analizar entorno",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextoPrincipal
                    )
                    Text(
                        "Mide el ruido de un lugar en 5 segundos",
                        fontSize = 12.sp,
                        color = ColorTextoSecundario
                    )
                }
            }

            if (!hayMicrofono) {
                Spacer(Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🎙️", fontSize = 22.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Sin permiso de micrófono las lecturas serán 0 dB. Concede el permiso en Ajustes.",
                            fontSize = 12.sp,
                            color = Color(0xFF92400E),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ─── Medidor principal (Fondo blanco pulido) ───
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, ColorBordeSuave)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (muestreando) "Escuchando el ambiente..." else "Resultado del análisis",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = ColorTextoPrincipal
                    )
                    Spacer(Modifier.height(10.dp))
                    Box(contentAlignment = Alignment.Center) {
                        MedidorArco(valor = valorAnimado.toDouble(), color = colorNivel)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${valorAnimado.toInt()}",
                                fontSize = 46.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colorNivel
                            )
                            Text("decibelios", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = ColorTextoSecundario)
                        }
                    }
                    if (muestreando) {
                        Spacer(Modifier.height(12.dp))
                        EspectroMuestras(muestras)
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF00796B))
                            Spacer(Modifier.width(8.dp))
                            Text("Muestreo... $segundosRestantes s", fontSize = 12.sp, color = ColorTextoSecundario)
                        }
                    } else {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Promedio de 5 segundos",
                            fontSize = 12.sp,
                            color = ColorTextoSecundario
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ─── Métricas contextuales (Fondo blanco con acentos) ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TarjetaMetricaLimpia(
                    modifier = Modifier.weight(1f),
                    titulo = "Luz",
                    valor = "${sensorState.nivelLuz.value.toInt()}",
                    unidad = "lux",
                    colorAcento = AzulInfo
                )
                TarjetaMetricaLimpia(
                    modifier = Modifier.weight(1f),
                    titulo = "GPS",
                    valor = String.format(Locale.US, "%.3f, %.3f", sensorState.latitud.value, sensorState.longitud.value),
                    unidad = "",
                    colorAcento = AzulInfo
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TarjetaMetricaLimpia(
                    modifier = Modifier.weight(1f),
                    titulo = "Máx hoy",
                    valor = "${sensorState.nivelMaximo.value.toInt()}",
                    unidad = "dB",
                    colorAcento = RojoSalud
                )
                TarjetaMetricaLimpia(
                    modifier = Modifier.weight(1f),
                    titulo = "Estado",
                    valor = SoundAnalyzer.clasificarNivel(valorMostrado).etiqueta,
                    unidad = "",
                    colorAcento = colorNivel
                )
            }

            resultado?.let { cat ->
                val tranquilo = cat == "Tranquilo"
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (tranquilo) Color(0xFFECFDF5) else Color(0xFFFEF2F2)
                    ),
                    border = BorderStroke(1.dp, if (tranquilo) Color(0xFFA7F3D0) else Color(0xFFFECACA))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (tranquilo) "🌿" else "⚠️", fontSize = 32.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Lugar ${if (tranquilo) "Tranquilo" else "Estresante"}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (tranquilo) Color(0xFF047857) else Color(0xFFB91C1C)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (tranquilo) {
                                    "Ambiente cómodo y adecuado para la concentración."
                                } else if (sensorState.nivelLuz.value > 2500 && SoundAnalyzer.clasificarNivel(dbFinal) != NivelRuido.PELIGRO) {
                                    "Iluminación extrema. Busca un lugar con luz más suave."
                                } else {
                                    SoundAnalyzer.clasificarNivel(dbFinal).recomendacion
                                },
                                fontSize = 12.sp,
                                color = ColorTextoPrincipal
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = nombreLugar,
                    onValueChange = { nombreLugar = it },
                    label = { Text("Asigna un nombre a este lugar") },
                    placeholder = { Text("Ej: Biblioteca, Cafetería...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = ColorTextoPrincipal,
                        unfocusedTextColor = ColorTextoPrincipal
                    )
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    "¿Con qué lo asocias?",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextoSecundario
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(EmojisDisponibles) { emoji ->
                        val seleccionado = contextoEmoji == emoji
                        Surface(
                            onClick = { contextoEmoji = emoji },
                            shape = RoundedCornerShape(16.dp),
                            color = if (seleccionado) Color(0xFFDCFCE7) else Color.White,
                            border = if (seleccionado) {
                                BorderStroke(1.5.dp, Color(0xFF10B981))
                            } else {
                                BorderStroke(1.dp, ColorBordeSuave)
                            }
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onTerminar,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !guardando,
                        border = BorderStroke(1.dp, ColorBordeSuave)
                    ) {
                        Text("Descartar", color = ColorTextoPrincipal)
                    }
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            guardar()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        enabled = !guardando,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (tranquilo) Color(0xFF00796B) else RojoSalud
                        )
                    ) {
                        if (guardando) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Guardando...")
                        } else {
                            Text("Guardar lugar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ─── Historial con gráfica (Limpio y legible) ───
            if (historial.isNotEmpty()) {
                Spacer(Modifier.height(26.dp))
                SeccionHistorial(
                    historial = historial,
                    promedio = promedioHistorico,
                    maximo = maximoHistorico,
                    total = totalMediciones,
                    lugarMasRuidoso = lugarMasRuidoso
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────────
//  SECCIÓN HISTORIAL CON GRÁFICA DE BARRAS LIMPIA
// ─────────────────────────────────────────────────────

@Composable
private fun SeccionHistorial(
    historial: List<Medicion>,
    promedio: Double?,
    maximo: Double?,
    total: Int,
    lugarMasRuidoso: Medicion?
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.BarChart,
                contentDescription = null,
                tint = Color(0xFF059669),
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Historial de mediciones",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = ColorTextoPrincipal
            )
        }
        Spacer(Modifier.height(14.dp))

        // Tarjetas de resumen de alto contraste
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TarjetaResumen(
                modifier = Modifier.weight(1f),
                titulo = "Total",
                valor = "$total",
                subtitulo = "mediciones",
                colorFondo = Color(0xFFEFF6FF),
                colorBorde = Color(0xFFBFDBFE),
                colorTitulo = Color(0xFF2563EB),
                colorValor = Color(0xFF1E3A8A)
            )
            TarjetaResumen(
                modifier = Modifier.weight(1f),
                titulo = "Promedio",
                valor = "${promedio?.toInt() ?: "-"} dB",
                subtitulo = "histórico",
                colorFondo = Color(0xFFFFFBEB),
                colorBorde = Color(0xFFFDE68A),
                colorTitulo = Color(0xFFD97706),
                colorValor = Color(0xFF78350F)
            )
            TarjetaResumen(
                modifier = Modifier.weight(1f),
                titulo = "Máximo",
                valor = "${maximo?.toInt() ?: "-"} dB",
                subtitulo = "registrado",
                colorFondo = Color(0xFFFFF1F2),
                colorBorde = Color(0xFFFECDD3),
                colorTitulo = Color(0xFFE11D48),
                colorValor = Color(0xFF881337)
            )
        }

        Spacer(Modifier.height(14.dp))

        // Lugar más ruidoso
        lugarMasRuidoso?.let { lugar ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                border = BorderStroke(1.dp, Color(0xFFFECDD3))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(lugar.contextoEmoji, fontSize = 26.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "📍 Lugar más ruidoso",
                            fontSize = 11.sp,
                            color = Color(0xFFE11D48),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            lugar.nombreLugar,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = ColorTextoPrincipal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            lugar.fechaHora,
                            fontSize = 11.sp,
                            color = ColorTextoSecundario
                        )
                    }
                    Text(
                        "${lugar.decibelios.toInt()} dB",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFE11D48),
                        fontSize = 20.sp
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // Gráfica de barras — últimas 10 mediciones
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, ColorBordeSuave)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Últimas ${historial.size} mediciones",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ColorTextoPrincipal
                )
                Spacer(Modifier.height(12.dp))
                GraficaBarras(
                    mediciones = historial.reversed(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
                Spacer(Modifier.height(10.dp))
                // Leyenda de colores
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ItemLeyenda(color = VerdeSalud, texto = "< 70 dB")
                    ItemLeyenda(color = AmbarSalud, texto = "70-80 dB")
                    ItemLeyenda(color = RojoSalud, texto = "> 80 dB")
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Lista detallada
        historial.forEachIndexed { idx, m ->
            FilaHistorial(medicion = m, indice = idx + 1)
            if (idx < historial.lastIndex) {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun GraficaBarras(
    mediciones: List<Medicion>,
    modifier: Modifier = Modifier
) {
    if (mediciones.isEmpty()) return

    val maxDb = mediciones.maxOf { it.decibelios }.coerceAtLeast(85.0)
    val lineaRef = 80.0 // línea de referencia OMS

    // Animación de entrada
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(mediciones) {
        animProgress.animateTo(1f, animationSpec = tween(800))
    }
    val progress = animProgress.value

    val colorVerde = VerdeSalud
    val colorAmbar = AmbarSalud
    val colorRojo = RojoSalud
    val colorLinea = Color(0xFFEF4444).copy(alpha = 0.6f)
    val colorFondoCuadricula = Color(0xFFE2E8F0)

    Canvas(modifier = modifier) {
        val paddingBottom = 32.dp.toPx()
        val paddingTop = 12.dp.toPx()
        val alturaUtil = size.height - paddingBottom - paddingTop
        val anchoTotal = size.width
        val anchoBarras = anchoTotal / mediciones.size.toFloat()
        val gapFraction = 0.25f

        // Líneas de cuadrícula horizontales (0, 40, 80 dB)
        listOf(0.0, 40.0, 80.0).forEach { nivel ->
            val y = paddingTop + alturaUtil * (1 - nivel / maxDb).toFloat()
            drawLine(
                color = colorFondoCuadricula,
                start = Offset(0f, y),
                end = Offset(anchoTotal, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Línea de referencia OMS (80 dB)
        val yLinea = paddingTop + alturaUtil * (1 - lineaRef / maxDb).toFloat()
        drawLine(
            color = colorLinea,
            start = Offset(0f, yLinea),
            end = Offset(anchoTotal, yLinea),
            strokeWidth = 2.dp.toPx(),
        )

        // Barras animadas
        mediciones.forEachIndexed { idx, m ->
            val proporcion = ((m.decibelios / maxDb).toFloat() * progress).coerceIn(0f, 1f)
            val altBarra = alturaUtil * proporcion
            val x = idx * anchoBarras + anchoBarras * gapFraction / 2f
            val ancho = anchoBarras * (1f - gapFraction)
            val y = paddingTop + alturaUtil - altBarra

            val color = when {
                m.decibelios < 70 -> colorVerde
                m.decibelios <= 80 -> colorAmbar
                else -> colorRojo
            }

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = 0.95f), color.copy(alpha = 0.6f)),
                    startY = y,
                    endY = paddingTop + alturaUtil
                ),
                topLeft = Offset(x, y),
                size = Size(ancho, altBarra.coerceAtLeast(4.dp.toPx())),
                cornerRadius = CornerRadius(6.dp.toPx())
            )
        }
    }
}

@Composable
private fun ItemLeyenda(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(4.dp))
        Text(texto, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = ColorTextoSecundario)
    }
}

@Composable
private fun FilaHistorial(medicion: Medicion, indice: Int) {
    val color = colorParaDbs(medicion.decibelios)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, ColorBordeSuave)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(medicion.contextoEmoji, fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    medicion.nombreLugar,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = ColorTextoPrincipal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    medicion.fechaHora,
                    fontSize = 11.sp,
                    color = ColorTextoSecundario
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${medicion.decibelios.toInt()} dB",
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = 17.sp
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = color.copy(alpha = 0.12f)
                ) {
                    Text(
                        when {
                            medicion.decibelios < 70 -> "Seguro"
                            medicion.decibelios <= 80 -> "Precaución"
                            else -> "Riesgo"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaResumen(
    modifier: Modifier,
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
        border = BorderStroke(1.dp, colorBorde)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                titulo,
                fontSize = 11.sp,
                color = colorTitulo,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                valor,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colorValor,
                textAlign = TextAlign.Center
            )
            Text(
                subtitulo,
                fontSize = 10.sp,
                color = ColorTextoSecundario
            )
        }
    }
}

@Composable
private fun TarjetaMetricaLimpia(
    modifier: Modifier,
    titulo: String,
    valor: String,
    unidad: String,
    colorAcento: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, ColorBordeSuave)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                titulo,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTextoSecundario
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    valor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorAcento
                )
                if (unidad.isNotBlank()) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        unidad,
                        fontSize = 12.sp,
                        color = ColorTextoSecundario,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
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
