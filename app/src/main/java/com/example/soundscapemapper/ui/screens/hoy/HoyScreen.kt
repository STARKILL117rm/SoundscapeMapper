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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.platform.LocalDensity
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
import com.example.soundscapemapper.ui.components.FilaHistorias
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
private val NOCHE_FIN = (5.5f * HORA_SEG).toInt()

private val Menta = Color(0xFF00F5A0)
private val Turquesa = Color(0xFF00D9F6)
private val Calido = Color(0xFFFFB74D)
private val CoralRiesgo = Color(0xFFFF6B6B)

private val SombraSuave = Shadow(
    color = Color.Black.copy(alpha = 0.14f),
    offset = Offset(0f, 1.5f),
    blurRadius = 3f
)

// Negro profundo de la noche: se mantiene SIEMPRE FIJO de 19:30 a 05:30.
private val NegroProfundo = Color(0xFF0B0E14)

// Fondo diurno: alba (blanco hueso) -> crema dorado -> atardecer de regreso a la noche.
// El bloque nocturno no está aquí: se resuelve aparte como color constante.
private val FONDO_DIA = listOf(
    (5.5f * HORA_SEG).toInt() to NegroProfundo,
    (6 * HORA_SEG) to Color(0xFFFAFAFA),
    (12 * HORA_SEG) to Color(0xFFFEF3C7),
    (19.25f * HORA_SEG).toInt() to Color(0xFFFFFBEB),
    (19.5f * HORA_SEG).toInt() to NegroProfundo
)

// Resplandor: amarillo matutino -> miel -> dorado -> ámbar -> azul nocturno profundo.
// De noche el tono es CONSTANTE para no aclarar ni volver grisáceo el fondo negro.
private val RESPLANDOR_PUNTOS = listOf(
    0 to Color(0xFF2A3A5C),
    (5.5f * HORA_SEG).toInt() to Color(0xFF2A3A5C),
    (6 * HORA_SEG) to Color(0xFFFFF3C4),
    (12 * HORA_SEG) to Color(0xFFFDE047),
    (17 * HORA_SEG) to Color(0xFFFBBF24),
    (19.5f * HORA_SEG).toInt() to Color(0xFFF59E0B),
    (19.55f * HORA_SEG).toInt() to Color(0xFF2A3A5C),
    (24 * HORA_SEG) to Color(0xFF2A3A5C)
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

private fun esNoche(sod: Int): Boolean = sod >= NOCHE_INICIO || sod < NOCHE_FIN

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
        acelerometro?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    // Física de resorte elástico: respuesta ágil al mover y micro-rebote orgánico al detenerse.
    val resorteElastico = spring<Float>(
        stiffness = 180f,
        dampingRatio = 0.55f
    )

    val densidad = LocalDensity.current
    val maxAstroPx = with(densidad) { 25.dp.toPx() }     // Capa 1 (Astro): hasta 25.dp
    val maxParallaxPx = with(densidad) { 10.dp.toPx() }  // Capa 2 (Estrellas/Resplandor): mitad

    val inclinacionX = (tiltX.coerceIn(-9f, 9f) / 9f)
    val inclinacionY = (tiltY.coerceIn(-9f, 9f) / 9f)

    // Capa 1 - Astro (Sol/Luna): mayor amplitud + rotación física sutil (-8° a +8°).
    val parX by animateFloatAsState(
        targetValue = inclinacionX * maxAstroPx,
        animationSpec = resorteElastico,
        label = "parX"
    )
    val parY by animateFloatAsState(
        targetValue = inclinacionY * maxAstroPx,
        animationSpec = resorteElastico,
        label = "parY"
    )
    val rotacionAstro by animateFloatAsState(
        targetValue = inclinacionX * 8f,
        animationSpec = resorteElastico,
        label = "rotacionAstro"
    )

    // Capa 2 - Estrellas / Resplandor: parallax a media velocidad (máx 10.dp).
    val parallaxX by animateFloatAsState(
        targetValue = inclinacionX * maxParallaxPx,
        animationSpec = resorteElastico,
        label = "parallaxX"
    )
    val parallaxY by animateFloatAsState(
        targetValue = inclinacionY * maxParallaxPx,
        animationSpec = resorteElastico,
        label = "parallaxY"
    )

    // De noche el fondo es SIEMPRE negro profundo fijo (#0B0E14), sin interpolación progresiva.
    val fondo = remember(sodActual) {
        if (esNoche(sodActual)) NegroProfundo else colorEntre(sodActual, FONDO_DIA)
    }
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
            mediciones = vm.mediciones,
            pctTiempoSeguro = pctSeguro
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
            parallaxX = parallaxX,
            parallaxY = parallaxY,
            rotacion = rotacionAstro,
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
                    claro = !noche
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
    parallaxX: Float,
    parallaxY: Float,
    rotacion: Float,
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

        // Cross-fade suave en el amanecer (05:45) y el anochecer (19:15).
        val subida = clamp01((sod - AMANECER_INICIO) / (AMANECER_FIN - AMANECER_INICIO).toFloat())
        val bajada = 1f - clamp01((sod - ATARDECER_INICIO) / (ATARDECER_FIN - ATARDECER_INICIO).toFloat())
        val solAlpha = subida * bajada
        // Usar OR (max) entre las dos transiciones para asegurar que la luna aparezca por la noche.
        val lunaAlpha = clamp01(max(1f - subida, 1f - bajada))

        // Velo atmosférico superior con la luz del momento (solo visible de día).
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(glow.copy(alpha = 0.22f * solAlpha), Color.Transparent),
                startY = 0f,
                endY = h * 0.60f
            ),
            size = Size(w, h)
        )

        // ── CAPA 2 (fondo medio): Resplandor ambiental en la esquina superior derecha.
        // Parallax a media velocidad (máx 10.dp) para dar profundidad 3D.
        val glowR = w * 0.70f
        val glowPos = Offset(
            x = w - (margenEnd + sunInset) - w * 0.05f + parallaxX,
            y = (margenTop + sunInset) + w * 0.05f + parallaxY
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    glow.copy(alpha = 0.22f * (solAlpha + lunaAlpha * 0.5f)),
                    Color.Transparent
                ),
                center = glowPos,
                radius = glowR
            ),
            center = glowPos,
            radius = glowR
        )

        // ── CAPA 2: Campo de estrellas titilantes de noche, parallax a media velocidad.
        if (lunaAlpha > 0.01f) {
            translate(left = parallaxX * 0.5f, top = parallaxY * 0.5f) {
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

        // ── CAPA 1 (primer plano): Sol en la esquina superior derecha (Alignment.TopEnd).
        // Desplazamiento amplio (hasta 25.dp) + rotación física sutil (-8° a +8°).
        if (solAlpha > 0.01f) {
            val sr = w * 0.07f
            val bobAngle = (titilar * 6.2832f).toDouble()
            val bobX = (sin(bobAngle) * (sr * 0.30f)).toFloat()
            val bobY = (sin(bobAngle * 0.7) * (sr * 0.18f)).toFloat()
            val sx = w - (margenEnd + sunInset) - sr + parX + bobX
            val sy = (margenTop + sunInset) + sr + parY + bobY
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
            rotate(rotacion, pivot = pivote) {
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
        }

        // ── CAPA 1: Luna creciente en la misma esquina superior derecha durante la noche,
        // con la misma amplitud de desplazamiento y rotación física.
        if (lunaAlpha > 0.01f) {
            val mr = w * 0.05f
            val mx = w - margenEnd - mr + parX
            val my = margenTop + mr + parY
            rotate(rotacion, pivot = Offset(mx, my)) {
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
