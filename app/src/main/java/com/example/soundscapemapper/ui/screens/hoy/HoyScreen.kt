package com.example.soundscapemapper.ui.screens.hoy

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.soundscapemapper.AppDatabase
import com.example.soundscapemapper.Configuracion
import com.example.soundscapemapper.NivelRuido
import com.example.soundscapemapper.SoundAnalyzer
import com.example.soundscapemapper.sensor.SensorStateHolder
import com.example.soundscapemapper.ui.components.VisorHistorias
import com.example.soundscapemapper.ui.historias.GeneradorHistorias
import com.example.soundscapemapper.ui.historias.Historia
import com.example.soundscapemapper.ui.viewmodel.HoyViewModel
import java.time.LocalTime
import java.util.Calendar
import java.util.Locale
import kotlin.math.sin
import kotlin.math.max
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val HORA_SEG = 3600
private const val SEGUNDOS_DIA = 86400

private val AMANECER_INICIO = (5.75f * HORA_SEG).toInt()
private val AMANECER_FIN = (6.25f * HORA_SEG).toInt()
private val ATARDECER_INICIO = (19.25f * HORA_SEG).toInt()
private val ATARDECER_FIN = (19.75f * HORA_SEG).toInt()
private val NOCHE_INICIO = (19.5f * HORA_SEG).toInt()

private val Menta = Color(0xFF00F5A0)
private val Turquesa = Color(0xFF00D9F6)
private val Calido = Color(0xFFFFB74D)
private val CoralRiesgo = Color(0xFFFF6B6B)

private val SombraSuave = Shadow(
    color = Color.Black.copy(alpha = 0.14f),
    offset = Offset(0f, 1.5f),
    blurRadius = 3f
)

// Fondo claro fusionado: blanco hueso -> crema dorado según avanza el día.
private val FONDO_PUNTOS = listOf(
    0 to Color(0xFF0B0E14),
    (6 * HORA_SEG) to Color(0xFFFAFAFA),
    (12 * HORA_SEG) to Color(0xFFFEF3C7),
    (19.5f * HORA_SEG).toInt() to Color(0xFFFFFBEB),
    (19.55f * HORA_SEG).toInt() to Color(0xFF0B0E14),
    (24 * HORA_SEG) to Color(0xFF0B0E14)
)

// Resplandor: amarillo matutino suave -> miel -> dorado -> ámbar -> plata nocturna.
private val RESPLANDOR_PUNTOS = listOf(
    0 to Color(0xFFE2E8F0),
    (6 * HORA_SEG) to Color(0xFFFFF3C4),
    (12 * HORA_SEG) to Color(0xFFFDE047),
    (17 * HORA_SEG) to Color(0xFFFBBF24),
    (19.5f * HORA_SEG).toInt() to Color(0xFFF59E0B),
    (19.55f * HORA_SEG).toInt() to Color(0xFFE2E8F0),
    (24 * HORA_SEG) to Color(0xFFE2E8F0)
)

private fun clamp01(valor: Float): Float = valor.coerceIn(0f, 1f)

private fun colorEntre(sod: Int, puntos: List<Pair<Int, Color>>): Color {
    val s = sod % SEGUNDOS_DIA
    for (i in 0 until puntos.size - 1) {
        val (t0, c0) = puntos[i]
        val (t1, c1) = puntos[i + 1]
        if (s in t0..t1) {
            val u = ((s - t0).toFloat() / (t1 - t0)).coerceIn(0f, 1f)
            return lerp(c0, c1, u)
        }
    }
    return puntos.last().second
}

private fun esNoche(sod: Int): Boolean = sod >= NOCHE_INICIO || sod < 6 * HORA_SEG

private fun saludoDe(sod: Int): String = when {
    esNoche(sod) -> "Buenas noches"
    sod < 12 * HORA_SEG -> "Buenos días"
    else -> "Buenas tardes"
}

private data class Estrella(
    val x: Float,
    val y: Float,
    val radio: Float,
    val base: Float,
    val fase: Float,
    val azul: Boolean
)

private data class PaletaUI(
    val texto: Color,
    val textoSuave: Color,
    val hero: Color,
    val verde: Color,
    val card: Color,
    val track: Color,
    val elevacion: Dp,
    val sombra: Boolean
)

// Grafito oscuro todo el día para máxima legibilidad sobre fondos claros.
private val PaletaClara = PaletaUI(
    texto = Color(0xFF1E293B),
    textoSuave = Color(0xFF64748B),
    hero = Color(0xFF1E293B),
    verde = Color(0xFF059669),
    card = Color(0xFFFFFFFF),
    track = Color(0x14000000),
    elevacion = 6.dp,
    sombra = true
)

private val PaletaOscura = PaletaUI(
    texto = Color(0xFFFFFFFF),
    textoSuave = Color(0xFF94A3B8),
    hero = Color(0xFFE6EDF3),
    verde = Color(0xFF00F5A0),
    card = Color(0xFF161B22),
    track = Color(0x14FFFFFF),
    elevacion = 0.dp,
    sombra = false
)

private fun colorDelNivel(nivel: NivelRuido): Color = when (nivel) {
    NivelRuido.SEGURO -> Menta
    NivelRuido.PRECAUCION -> Calido
    NivelRuido.PELIGRO -> CoralRiesgo
}

private fun iconoDelEstado(nivel: NivelRuido): ImageVector = when (nivel) {
    NivelRuido.SEGURO -> Icons.Outlined.CheckCircle
    NivelRuido.PRECAUCION -> Icons.Outlined.Info
    NivelRuido.PELIGRO -> Icons.Outlined.Warning
}

private fun tituloDelEstado(nivel: NivelRuido): String = when (nivel) {
    NivelRuido.SEGURO -> "ESCUDO ACTIVO"
    NivelRuido.PRECAUCION -> "AMBIENTE MODERADO"
    NivelRuido.PELIGRO -> "ATENCIÓN"
}

private fun fraseEmpatica(nivel: NivelRuido): String = when (nivel) {
    NivelRuido.SEGURO -> "Tu entorno está en calma"
    NivelRuido.PRECAUCION -> "Te estamos cuidando"
    NivelRuido.PELIGRO -> "Entorno ruidoso detectado"
}

private fun iconoDeHistoria(historia: Historia): ImageVector = when {
    historia.id.startsWith("estado_actual") -> Icons.Outlined.GraphicEq
    historia.id.startsWith("dosis_hoy") -> Icons.Outlined.Timer
    historia.id.startsWith("dia_pesado") -> Icons.Outlined.Warning
    historia.id.startsWith("semana") -> Icons.Outlined.DateRange
    historia.id.startsWith("lugares") -> Icons.Outlined.Place
    historia.id.startsWith("sabias") -> Icons.Outlined.Lightbulb
    historia.id.startsWith("educativa_escala") -> Icons.Outlined.BarChart
    historia.id.startsWith("educativa_dosis") -> Icons.Outlined.Hearing
    historia.id.startsWith("educativa_protegete") -> Icons.Outlined.Security
    else -> Icons.Outlined.FavoriteBorder
}

@Composable
fun HoyScreen(
    db: AppDatabase,
    sensorState: SensorStateHolder
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

    val ahora = Calendar.getInstance()
    val minutosDelDia = (ahora.get(Calendar.HOUR_OF_DAY) * 60 + ahora.get(Calendar.MINUTE))
        .coerceAtLeast(1)
    val pctSeguro = ((1 - sensorState.dosisSobre65.value / minutosDelDia) * 100)
        .toInt()
        .coerceIn(0, 100)

    var sodActual by remember { mutableIntStateOf(LocalTime.now().toSecondOfDay()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            sodActual = LocalTime.now().toSecondOfDay()
            delay(1000L)
        }
    }

    val contexto = LocalContext.current
    val sensorManager = remember { contexto.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val acelerometro = remember { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    DisposableEffect(acelerometro) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event ?: return
                tiltX = event.values[0]
                tiltY = event.values[1]
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        acelerometro?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    // Desplazamiento objetivo por inclinación y física de retorno con spring.
    val targetOffsetX = tiltX.coerceIn(-9f, 9f) * 10f
    val targetOffsetY = tiltY.coerceIn(-9f, 9f) * 10f
    val parX by animateFloatAsState(
        targetValue = targetOffsetX,
        animationSpec = spring(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "parX"
    )
    val parY by animateFloatAsState(
        targetValue = targetOffsetY,
        animationSpec = spring(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "parY"
    )

    val fondo = remember(sodActual) { colorEntre(sodActual, FONDO_PUNTOS) }
    val noche = esNoche(sodActual)
    val pal = if (noche) PaletaOscura else PaletaClara
    val saludo = saludoDe(sodActual)

    val fondoAnimado by animateColorAsState(
        targetValue = fondo,
        animationSpec = tween(1400, easing = FastOutSlowInEasing),
        label = "fondo"
    )
    val textoAnimado by animateColorAsState(
        targetValue = pal.texto,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "texto"
    )
    val textoSuaveAnimado by animateColorAsState(
        targetValue = pal.textoSuave,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "textoSuave"
    )
    val heroAnimado by animateColorAsState(
        targetValue = pal.hero,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "hero"
    )
    val verdeAnimado by animateColorAsState(
        targetValue = pal.verde,
        animationSpec = tween(1000),
        label = "verde"
    )
    val cardAnimada by animateColorAsState(
        targetValue = pal.card,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "card"
    )
    val trackAnimado by animateColorAsState(
        targetValue = pal.track,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "track"
    )
    val elevacionAnimada by animateDpAsState(
        targetValue = pal.elevacion,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "elevacion"
    )
    val sombra = pal.sombra

    var historiasVistas by remember { mutableStateOf(Configuracion.historiasVistas(contexto)) }
    var historiaAbierta by remember { mutableStateOf<Historia?>(null) }

    val historias = remember(
        decibelios,
        sensorState.nivelMaximo.value,
        sensorState.dosisSobre65.value,
        sensorState.dosisSobre80.value,
        puntosHoy,
        vm.registrosSemana,
        vm.mediciones
    ) {
        GeneradorHistorias.generar(
            decibeliosAhora = decibelios,
            nivelMaximoHoy = sensorState.nivelMaximo.value,
            minutosSobre65Hoy = sensorState.dosisSobre65.value,
            minutosSobre80Hoy = sensorState.dosisSobre80.value,
            puntosHoy = puntosHoy,
            registrosSemana = vm.registrosSemana,
            mediciones = vm.mediciones
        )
    }

    val estiloInsight = MaterialTheme.typography.bodySmall.copy(
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        shadow = if (sombra) SombraSuave else null
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(fondoAnimado)
    ) {
        CieloAmbiental(
            sod = sodActual,
            parX = parX,
            parY = parY,
            fondo = fondoAnimado
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            AnimatedContent(
                targetState = saludo,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(450)) +
                        slideInVertically(animationSpec = tween(450)) { it / 5 })
                        .togetherWith(
                            fadeOut(animationSpec = tween(300)) +
                                slideOutVertically(animationSpec = tween(300)) { -it / 5 }
                        )
                },
                label = "saludo"
            ) { s ->
                Text(
                    text = s,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        shadow = if (sombra) SombraSuave else null
                    ),
                    fontWeight = FontWeight.Bold,
                    color = textoAnimado
                )
            }

            Spacer(Modifier.height(20.dp))

            Column {
                Text(
                    text = "Insights de tu día",
                    style = estiloInsight,
                    color = textoSuaveAnimado
                )
                Spacer(Modifier.height(10.dp))
                FilaHistorias(
                    historias = historias,
                    vistas = historiasVistas,
                    onAbrir = { historiaAbierta = it },
                    texto = textoAnimado,
                    textoSuave = textoSuaveAnimado,
                    claro = !noche,
                    verde = verdeAnimado
                )
            }

            Spacer(Modifier.height(24.dp))

            AnilloProteccion(
                decibelios = decibelios,
                nivel = nivel,
                verde = verdeAnimado,
                texto = heroAnimado,
                textoSuave = textoSuaveAnimado,
                sombra = sombra,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(24.dp))

            TarjetaBienestar(
                puntosHoy = puntosHoy,
                progreso = progresoDosis,
                pctSeguro = pctSeguro,
                nivelMaximo = sensorState.nivelMaximo.value,
                minutosSobre65 = sensorState.dosisSobre65.value,
                nivel = nivel,
                superficie = cardAnimada,
                track = trackAnimado,
                texto = textoAnimado,
                textoSuave = textoSuaveAnimado,
                verde = verdeAnimado,
                elevacion = elevacionAnimada
            )

            Spacer(Modifier.height(24.dp))
        }
    }

    historiaAbierta?.let { abierta ->
        VisorHistorias(
            historias = historias,
            indiceInicial = historias.indexOf(abierta).coerceAtLeast(0),
            onCerrar = { historiaAbierta = null },
            onVista = { id ->
                Configuracion.marcarHistoriaVista(contexto, id)
                historiasVistas = Configuracion.historiasVistas(contexto)
            }
        )
    }
}

@Composable
private fun CieloAmbiental(
    sod: Int,
    parX: Float,
    parY: Float,
    fondo: Color
) {
    val estrellas = remember {
        val rnd = java.util.Random(42)
        List(26) { i ->
            Estrella(
                x = rnd.nextFloat(),
                y = rnd.nextFloat() * 0.55f,
                radio = 1f + rnd.nextFloat() * 1.6f,
                base = 0.45f + rnd.nextFloat() * 0.5f,
                fase = rnd.nextFloat() * 6.2832f,
                azul = i % 3 == 0
            )
        }
    }

    val cielo = rememberInfiniteTransition(label = "cielo")
    val titilar by cielo.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "titilar"
    )
    val rotarSol by cielo.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotarSol"
    )

    val glow = remember(sod) { colorEntre(sod, RESPLANDOR_PUNTOS) }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val margenTop = 26.dp.toPx()
        val margenEnd = 32.dp.toPx()
        val sunInset = 14.dp.toPx() // separa el astro del borde para evitar que los rayos se corten

        // Cross-fade suave en el amanecer (06:00) y el anochecer (19:30).
        val subida = clamp01((sod - AMANECER_INICIO) / (AMANECER_FIN - AMANECER_INICIO).toFloat())
        val bajada = 1f - clamp01((sod - ATARDECER_INICIO) / (ATARDECER_FIN - ATARDECER_INICIO).toFloat())
        val solAlpha = subida * bajada
        // Usar OR (max) entre las dos transiciones para asegurar que la luna aparezca por la noche.
        val lunaAlpha = clamp01(max(1f - subida, 1f - bajada))

        // Velo atmosférico superior con la luz del momento.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(glow.copy(alpha = 0.22f * solAlpha), Color.Transparent),
                startY = 0f,
                endY = h * 0.60f
            ),
            size = Size(w, h)
        )

        // Resplandor ambiental en la esquina superior derecha, junto al astro.
        val glowR = w * 0.70f
        // Hacer movimiento más visible y divertido: combinar inclinación con un pequeño "bobbing".
        val sr = w * 0.07f
        val bobAngle = (titilar * 6.2832f).toDouble()
        val bobX = (sin(bobAngle) * (sr * 0.45f)).toFloat()
        val bobY = (sin(bobAngle * 0.7) * (sr * 0.28f)).toFloat()
        val finalParX = parX * 1.15f + bobX
        val finalParY = parY * 1.15f + bobY

        val glowPos = Offset(
            x = w - (margenEnd + sunInset) - w * 0.05f + finalParX,
            y = (margenTop + sunInset) + w * 0.05f + finalParY
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    glow.copy(alpha = 0.30f * (solAlpha + lunaAlpha * 0.7f)),
                    Color.Transparent
                ),
                center = glowPos,
                radius = glowR
            ),
            center = glowPos,
            radius = glowR
        )

        // Campo de estrellas titilantes de noche.
        if (lunaAlpha > 0.01f) {
            translate(left = finalParX * 0.25f, top = finalParY * 0.20f) {
                estrellas.forEach { e ->
                    val alfa = (
                        e.base * (0.35 + 0.65 * ((sin(e.fase + titilar * 6.2832f) + 1.0) / 2.0))
                        ).toFloat()
                    drawCircle(
                        color = (if (e.azul) Color(0xFF93C5FD) else Color(0xFFE2E8F0))
                            .copy(alpha = alfa * lunaAlpha),
                        radius = e.radio.dp.toPx(),
                        center = Offset(e.x * w, e.y * h)
                    )
                }
            }
        }

        // Sol: esquina superior derecha (Alignment.TopEnd, top 12dp / end 16dp)
        // con desplazamiento por sensor y retorno físico con spring.
        if (solAlpha > 0.01f) {
            val sx = w - (margenEnd + sunInset) - sr + finalParX
            val sy = (margenTop + sunInset) + sr + finalParY
            val pivote = Offset(sx, sy)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glow.copy(alpha = 0.55f * solAlpha),
                        Color.Transparent
                    ),
                    center = pivote,
                    radius = sr * 2.8f
                ),
                center = pivote,
                radius = sr * 2.8f
            )
            rotate(rotarSol, pivot = pivote) {
                repeat(12) { i ->
                    rotate(i * 30f, pivot = pivote) {
                        // acortar ligeramente las líneas y usar el pivote seguro para evitar recortes
                        drawLine(
                            color = Color(0xFFFFC266).copy(alpha = solAlpha),
                            start = Offset(sx, sy - sr * 1.05f),
                            end = Offset(sx, sy - sr * 1.55f),
                            strokeWidth = 4.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
            drawCircle(glow.copy(alpha = solAlpha), sr, pivote)
        }

        // Luna creciente: misma esquina superior derecha durante la noche.
        if (lunaAlpha > 0.01f) {
            val mr = w * 0.05f
            val mx = w - margenEnd - mr + parX
            val my = margenTop + mr + parY
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE2E8F0).copy(alpha = 0.40f * lunaAlpha),
                        Color.Transparent
                    ),
                    center = Offset(mx, my),
                    radius = mr * 3f
                ),
                center = Offset(mx, my),
                radius = mr * 3f
            )
            drawCircle(Color(0xFFE2E8F0).copy(alpha = lunaAlpha), mr, Offset(mx, my))
            drawCircle(
                color = fondo.copy(alpha = lunaAlpha),
                radius = mr * 0.90f,
                center = Offset(mx + mr * 0.45f, my - mr * 0.16f)
            )
        }
    }
}

@Composable
private fun FilaHistorias(
    historias: List<Historia>,
    vistas: Set<String>,
    onAbrir: (Historia) -> Unit,
    texto: Color,
    textoSuave: Color,
    claro: Boolean,
    verde: Color
) {
    if (historias.isEmpty()) return
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(historias, key = { it.id }) { historia ->
            BurbujaHistoria(
                historia = historia,
                vista = historia.id in vistas,
                onClick = { onAbrir(historia) },
                texto = texto,
                textoSuave = textoSuave,
                claro = claro,
                verde = verde
            )
        }
    }
}

@Composable
private fun BurbujaHistoria(
    historia: Historia,
    vista: Boolean,
    onClick: () -> Unit,
    texto: Color,
    textoSuave: Color,
    claro: Boolean,
    verde: Color
) {
    val color = Color(historia.colorHex)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .then(
                    if (vista) {
                        Modifier.background(if (claro) Color(0xFFD1D5DB) else Color(0xFF3A3D42))
                    } else {
                        Modifier.background(
                            Brush.sweepGradient(
                                listOf(Menta, Turquesa, Menta)
                            )
                        )
                    }
                )
                .padding(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(
                        if (claro) Color(0xFFFFF9E6) else Color(0xFF1E222B)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(
                            1.dp,
                            if (vista) {
                                if (claro) Color(0xFFD1D5DB)
                                else Color.White.copy(alpha = 0.06f)
                            } else {
                                color.copy(alpha = 0.35f)
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        iconoDeHistoria(historia),
                        contentDescription = historia.nombre,
                        tint = when {
                            !vista && claro -> verde
                            vista && claro -> Color(0xFF1E293B)
                            vista -> textoSuave
                            else -> color
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            historia.nombre,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                shadow = if (claro) SombraSuave else null
            ),
            color = if (vista) textoSuave else texto,
            maxLines = 1
        )
    }
}

@Composable
private fun AnilloProteccion(
    decibelios: Double,
    nivel: NivelRuido,
    verde: Color,
    texto: Color,
    textoSuave: Color,
    sombra: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (nivel == NivelRuido.SEGURO) verde else colorDelNivel(nivel)
    val pista = if (sombra) {
        Color.Black.copy(alpha = 0.08f)
    } else {
        Color.White.copy(alpha = 0.12f)
    }
    val animado by animateFloatAsState(
        targetValue = (decibelios / 120.0).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "db"
    )

    val respiracion = rememberInfiniteTransition(label = "respiracion")
    val escala by respiracion.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "escala"
    )

    Box(
        modifier = modifier
            .size(300.dp)
            .scale(escala),
        contentAlignment = Alignment.Center
    ) {
        CanvasComposable(progreso = animado, verde = verde, pista = pista)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.16f)
            ) {
                Icon(
                    iconoDelEstado(nivel),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier
                        .padding(7.dp)
                        .size(20.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = tituloDelEstado(nivel),
                fontSize = 10.sp,
                letterSpacing = 2.5.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format(Locale.US, "%.1f", decibelios),
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Bold,
                    color = texto
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "dB",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textoSuave,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = fraseEmpatica(nivel),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    shadow = if (sombra) SombraSuave else null
                ),
                color = textoSuave,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CanvasComposable(
    progreso: Float,
    verde: Color,
    pista: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = 20.dp.toPx()
        val margen = stroke / 2 + 8.dp.toPx()
        val lado = size.width - margen * 2
        val topLeft = Offset(margen, margen)
        val dimension = Size(lado, lado)
        val centro = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(verde.copy(alpha = 0.14f), Color.Transparent),
                center = centro,
                radius = size.width / 2f
            ),
            radius = size.width / 2f
        )
        drawArc(
            color = pista,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
            topLeft = topLeft,
            size = dimension
        )
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    verde.copy(alpha = 0.35f),
                    Turquesa,
                    verde.copy(alpha = 0.35f)
                ),
                center = centro
            ),
            startAngle = -90f,
            sweepAngle = 360f * progreso.coerceIn(0f, 1f),
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
            topLeft = topLeft,
            size = dimension
        )
    }
}

@Composable
private fun TarjetaBienestar(
    puntosHoy: Double,
    progreso: Float,
    pctSeguro: Int,
    nivelMaximo: Double,
    minutosSobre65: Double,
    nivel: NivelRuido,
    superficie: Color,
    track: Color,
    texto: Color,
    textoSuave: Color,
    verde: Color,
    elevacion: Dp
) {
    val fraccion = progreso.coerceIn(0f, 1f)
    val barraAnimada by animateFloatAsState(
        targetValue = fraccion,
        animationSpec = tween(900),
        label = "dosis"
    )
    val colorDestacado = when {
        pctSeguro >= 85 -> verde
        pctSeguro >= 50 -> texto
        else -> Calido
    }
    val colorDosis = when {
        fraccion >= 1f -> CoralRiesgo
        fraccion >= 0.5f -> Calido
        else -> verde
    }

    val picoDeHoy = if (nivelMaximo > 0) {
        String.format(Locale.US, "%.1f dB", nivelMaximo)
    } else {
        "—"
    }

    val (etiquetaCalidad, colorCalidad) = when (nivel) {
        NivelRuido.SEGURO -> Pair("Tranquilo", verde)
        NivelRuido.PRECAUCION -> Pair("Moderado", Calido)
        NivelRuido.PELIGRO -> Pair("Ruidoso", CoralRiesgo)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = superficie
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevacion)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RESUMEN OMS · HOY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = textoSuave
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = colorDosis.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "${puntosHoy.toInt()} / ${SoundAnalyzer.META_DOSIS_PUNTOS.toInt()} pts",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorDosis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Has pasado el $pctSeguro% de tu día en zonas seguras",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = colorDestacado
            )
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                MiniMetrico(
                    icono = Icons.Outlined.Timer,
                    etiqueta = "Minutos en alerta",
                    valor = "${minutosSobre65.toInt()} min",
                    color = colorDosis,
                    texto = texto,
                    textoSuave = textoSuave,
                    modifier = Modifier.weight(1f)
                )
                MiniMetrico(
                    icono = Icons.Outlined.BarChart,
                    etiqueta = "Pico más alto",
                    valor = picoDeHoy,
                    color = colorDosis,
                    texto = texto,
                    textoSuave = textoSuave,
                    modifier = Modifier.weight(1f)
                )
                MiniMetrico(
                    icono = Icons.Outlined.Eco,
                    etiqueta = "Calidad ambiental",
                    valor = etiquetaCalidad,
                    color = colorCalidad,
                    texto = texto,
                    textoSuave = textoSuave,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dosis de ruido consumida",
                    fontSize = 10.sp,
                    color = textoSuave
                )
                Text(
                    text = "${(fraccion * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorDosis
                )
            }
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(track)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(barraAnimada.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(verde, Turquesa)
                            ),
                            RoundedCornerShape(6.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun MiniMetrico(
    icono: ImageVector,
    etiqueta: String,
    valor: String,
    color: Color,
    texto: Color,
    textoSuave: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.14f)
        ) {
            Icon(
                icono,
                contentDescription = null,
                tint = color,
                modifier = Modifier
                    .padding(7.dp)
                    .size(18.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = etiqueta,
            fontSize = 10.sp,
            color = textoSuave,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = valor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = texto
        )
    }
}
