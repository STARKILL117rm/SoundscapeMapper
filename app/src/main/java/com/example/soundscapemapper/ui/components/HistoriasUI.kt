package com.example.soundscapemapper.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.soundscapemapper.ui.historias.Historia
import com.example.soundscapemapper.ui.historias.PaginaHistoria
import com.example.soundscapemapper.ui.historias.TipoGrafica
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DURACION_PAGINA_MS = 5000

private val Menta = Color(0xFF10B981)
private val Cian = Color(0xFF06B6D4)
private val RojoAlerta = Color(0xFFF87171)
private val AzulNoche = Color(0xFF0F172A)

// Paletas estilo "Spotify Wrapped" para fondos sin foto de stock.
// Noche profunda -> acento vivo, para que nunca quede un fondo plano.
private val GRADIENTES: Map<Long, Pair<Color, Color>> = mapOf(
    0xFF4DB6AC to (AzulNoche to Color(0xFF047857)), // VERDE  -> Esmeralda
    0xFFF0B27A to (AzulNoche to Color(0xFF7C2D12)), // ÁMBAR  -> Cobre cálido
    0xFFE07A5F to (AzulNoche to Color(0xFF7F1D1D)), // ROJO   -> Vino
    0xFF5B8DB8 to (AzulNoche to Color(0xFF1E3A8A)), // AZUL   -> Añil
    0xFF8E7CC3 to (AzulNoche to Color(0xFF4C1D95)), // VIOLETA-> Morado
    0xFF00897B to (AzulNoche to Color(0xFF065F46))  // TEAL   -> Bosque
)

private fun gradientePara(colorHex: Long): Pair<Color, Color> =
    GRADIENTES[colorHex] ?: (AzulNoche to lerp(Color(colorHex), Color.Black, 0.45f))

private fun iconoDeHistoria(historia: Historia): ImageVector = when {
    historia.id.startsWith("estado_actual") -> Icons.Outlined.GraphicEq
    historia.id.startsWith("dosis_hoy") -> Icons.Outlined.Timer
    historia.id.startsWith("pico_mas_alto") -> Icons.Outlined.Warning
    historia.id.startsWith("calidad_ambiental") -> Icons.Outlined.Eco
    historia.id.startsWith("dia_pesado") -> Icons.Outlined.Warning
    historia.id.startsWith("semana") -> Icons.Outlined.DateRange
    historia.id.startsWith("lugares") -> Icons.Outlined.Place
    historia.id.startsWith("sabias") -> Icons.Outlined.Lightbulb
    historia.id.startsWith("educativa_escala") -> Icons.Outlined.BarChart
    historia.id.startsWith("educativa_dosis") -> Icons.Outlined.Hearing
    historia.id.startsWith("educativa_protegete") -> Icons.Outlined.Security
    else -> Icons.Outlined.FavoriteBorder
}

// ─────────────────────────── FILA EXTERIOR ────────────────────────────

@Composable
fun FilaHistorias(
    historias: List<Historia>,
    vistas: Set<String>,
    onAbrir: (Historia) -> Unit,
    texto: Color,
    textoSuave: Color,
    claro: Boolean
) {
    if (historias.isEmpty()) return
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(historias, key = { it.id }) { historia ->
            HistoriaBurbuja(
                historia = historia,
                vista = historia.id in vistas,
                onClick = { onAbrir(historia) },
                texto = texto,
                textoSuave = textoSuave,
                claro = claro
            )
        }
    }
}

@Composable
private fun HistoriaBurbuja(
    historia: Historia,
    vista: Boolean,
    onClick: () -> Unit,
    texto: Color,
    textoSuave: Color,
    claro: Boolean
) {
    // Anillo: neón gradiente si no vista; gris discreto si vista.
    val anillo = when {
        vista && claro -> Brush.linearGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)))
        vista -> Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
        else -> Brush.linearGradient(listOf(Menta, Cian))
    }
    // Interior: crema cálido en día, azul profundo en noche.
    val interior = if (claro) Color(0xFFFFFBEB) else Color(0xFF1E293B)
    // Borde interior interno sutil para dar definición.
    val bordeInterior = if (claro) Color(0xFFFDE68A).copy(alpha = 0.65f)
    else Color.White.copy(alpha = if (vista) 0.08f else 0.16f)

    val tintIcono = when {
        vista && claro -> Color(0xFF94A3B8)
        vista -> Color.White.copy(alpha = 0.40f)
        claro -> Color(0xFF0F172A)
        else -> Color.White
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.size(74.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(CircleShape)
                    .background(anillo)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.5.dp)
                        .clip(CircleShape)
                        .background(interior)
                        .border(1.dp, bordeInterior, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconoDeHistoria(historia),
                        contentDescription = historia.nombre,
                        tint = tintIcono,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            historia.badge?.let { badge ->
                val fondoBadge: Any = when {
                    vista && claro -> Color(0xFFE2E8F0)
                    vista -> Color(0xFF334155)
                    else -> Brush.linearGradient(listOf(Menta, Cian))
                }
                val colorBadge = when {
                    vista && claro -> Color(0xFF64748B)
                    vista -> Color.White.copy(alpha = 0.5f)
                    else -> Color(0xFF04241E)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 7.dp)
                        .clip(RoundedCornerShape(50))
                        .then(
                            if (fondoBadge is Brush) Modifier.background(fondoBadge)
                            else Modifier.background(fondoBadge as Color)
                        )
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp,
                        color = colorBadge
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            historia.nombre,
            fontSize = 11.sp,
            fontWeight = if (vista) FontWeight.Medium else FontWeight.Bold,
            color = if (vista) textoSuave else texto,
            maxLines = 1
        )
    }
}

// ─────────────────────────── VISOR INTERNO ────────────────────────────

private fun tiempoRelativo(id: String): String {
    val fechaId = id.substringAfterLast("_", "")
    if (fechaId.isEmpty()) return "Hoy"
    return try {
        val formato = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dia = formato.parse(fechaId) ?: return "Hoy"
        val mins = ((System.currentTimeMillis() - dia.time) / 60000L).toInt()
        when {
            mins < 1 -> "Ahora"
            mins < 60 -> "Hace $mins min"
            mins < 24 * 60 -> "Hace ${mins / 60} h"
            else -> "Hace ${mins / (24 * 60)} d"
        }
    } catch (e: Exception) {
        "Hoy"
    }
}

@Composable
fun VisorHistorias(
    historias: List<Historia>,
    indiceInicial: Int,
    onCerrar: () -> Unit,
    onVista: (String) -> Unit
) {
    val paginas = historias.flatMapIndexed { i, h ->
        h.paginas.mapIndexed { j, p -> Triple(i, j, p) }
    }
    if (paginas.isEmpty()) {
        LaunchedEffect(Unit) { onCerrar() }
        return
    }

    val paginaInicial = historias
        .take(indiceInicial.coerceIn(0, historias.size - 1))
        .sumOf { it.paginas.size }
        .coerceIn(0, paginas.size - 1)

    val scope = rememberCoroutineScope()

    // Slide lateral 100% nativo de Android vía PagerState.
    val pagerState = rememberPagerState(
        initialPage = paginaInicial,
        pageCount = { paginas.size }
    )

    val progress = remember { Animatable(0f) }
    val offsetCierre = remember { Animatable(0f) }
    var pausado by remember { mutableStateOf(false) }
    var uiVisible by remember { mutableStateOf(true) }
    var cerrar by remember { mutableStateOf(false) }
    var paginaProcesada by remember { mutableIntStateOf(pagerState.currentPage) }

    val altoPantallaPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenHeightDp.dp.toPx()
    }

    val alfaUi by animateFloatAsState(
        targetValue = if (uiVisible) 1f else 0f,
        animationSpec = tween(150),
        label = "ui"
    )

    // Temporizador del segmento activo: 0f -> 1f en 5 s.
    // Se reinicia SOLO al cambiar de página (por swipe, tap o auto-avance).
    LaunchedEffect(pagerState.currentPage, pausado) {
        if (pagerState.currentPage != paginaProcesada) {
            progress.snapTo(0f)
            paginaProcesada = pagerState.currentPage
            val (i, _, _) = paginas[pagerState.currentPage]
            historias.take(i + 1).forEach { onVista(it.id) }
        }
        if (pausado) {
            progress.stop()
            return@LaunchedEffect
        }
        // Reanuda desde el milisegundo exacto en que se congeló.
        val restante = ((1f - progress.value) * DURACION_PAGINA_MS).toInt().coerceAtLeast(1)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = restante, easing = LinearEasing)
        )
        if (pagerState.currentPage < paginas.size - 1) {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
        } else {
            onCerrar()
        }
    }

    LaunchedEffect(cerrar) {
        if (cerrar) onCerrar()
    }

    val alphaCierre = (1f - (offsetCierre.value / (altoPantallaPx * 0.6f)))
        .coerceIn(0.25f, 1f)

    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AzulNoche)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = offsetCierre.value
                        alpha = alphaCierre
                    }
                    .pointerInput(paginas.size) {
                        detectTapGestures(
                            onTap = { offset ->
                                val ancho = size.width.toFloat()
                                val actual = pagerState.currentPage
                                when {
                                    offset.x < ancho * 0.30f && actual > 0 ->
                                        scope.launch { pagerState.animateScrollToPage(actual - 1) }
                                    offset.x < ancho * 0.30f -> Unit
                                    actual < paginas.size - 1 ->
                                        scope.launch { pagerState.animateScrollToPage(actual + 1) }
                                    else -> onCerrar()
                                }
                            },
                            onPress = {
                                // Pausa solo en pulsación larga (>250 ms): evita parpadeo en toques rápidos.
                                val job = scope.launch {
                                    delay(250)
                                    uiVisible = false
                                    pausado = true
                                }
                                try {
                                    tryAwaitRelease()
                                } finally {
                                    job.cancel()
                                    pausado = false
                                    uiVisible = true
                                }
                            }
                        )
                    }
                    .pointerInput(paginas.size) {
                        var totalY = 0f
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                if (dragAmount > 0) {
                                    change.consume()
                                    totalY = (totalY + dragAmount).coerceAtLeast(0f)
                                    scope.launch { offsetCierre.snapTo(totalY) }
                                }
                            },
                            onDragEnd = {
                                if (totalY > altoPantallaPx * 0.18f) {
                                    cerrar = true
                                } else {
                                    scope.launch {
                                        offsetCierre.animateTo(0f, tween(240, easing = FastOutSlowInEasing))
                                    }
                                }
                            }
                        )
                    }
            ) {
                HorizontalPager(
                    state = pagerState,
                    pageSpacing = 0.dp,
                    beyondViewportPageCount = 1,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val (i, _, pagina) = paginas[page]
                    ContenidoPagina(
                        pagina = pagina,
                        nombreHistoria = historias[i].nombre,
                        imagenUrlFallback = historias[i].imagenUrl,
                        alfaPie = alfaUi
                    )
                }
            }

            // Cabecera fija: barra segmentada UNIFICADA + contexto (no se mueve con el slide).
            val (i, j, _) = paginas[pagerState.currentPage]
            val historia = historias[i]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .pointerInput(Unit) {}
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.55f),
                            1f to Color.Transparent
                        )
                    )
                    .padding(top = 8.dp, bottom = 6.dp)
            ) {
                // Barra de progreso UNIFICADA: N segmentos LinearProgressIndicator.
                // Pasados = 100% blanco · actual = anima 0f -> 1f · futuros = pista translúcida.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    historia.paginas.forEachIndexed { k, _ ->
                        val fraccion = when {
                            k < j -> 1f
                            k == j -> progress.value
                            else -> 0f
                        }
                        LinearProgressIndicator(
                            progress = { fraccion },
                            modifier = Modifier
                                .weight(1f)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f),
                            strokeCap = StrokeCap.Butt
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))

                // Contexto: título + paso relativo + hora (se atenúa al mantener presionado).
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.graphicsLayer { alpha = alfaUi }
                    ) {
                        Text(
                            historia.nombre,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${j + 1} de ${historia.paginas.size} · ${tiempoRelativo(historia.id)}",
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 11.sp
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onCerrar) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Cerrar historias",
                            tint = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContenidoPagina(
    pagina: PaginaHistoria,
    nombreHistoria: String,
    imagenUrlFallback: String?,
    alfaPie: Float = 1f
) {
    val color = Color(pagina.colorHex)
    val imagenUrl = pagina.imagenUrl ?: imagenUrlFallback
    val contexto = LocalContext.current

    // Decodifica las fotos al tamaño de pantalla (no 4K): apertura instantánea + caché en memoria.
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val tamanoPantalla = remember {
        with(density) {
            config.screenWidthDp.dp.toPx().toInt() to
                config.screenHeightDp.dp.toPx().toInt()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Capa base: fondo dinámico vivo (nunca un fondo plano).
        FondoDinamico(pagina = pagina)

        // Foto de contexto a pantalla completa si la historia la incluye.
        if (!imagenUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(contexto)
                    .data(imagenUrl)
                    .crossfade(false)
                    .size(width = tamanoPantalla.first, height = tamanoPantalla.second)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Overlay de gradiente oscuro (negro 60% -> transparente) para contraste.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.60f),
                        0.50f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.45f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Tarjeta de datos central (Glassmorphism).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    pagina.chip?.let { chip ->
                        ChipSalud(chip, alerta = pagina.alerta)
                        Spacer(Modifier.height(14.dp))
                    }

                    Text(
                        pagina.titulo,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))

                    pagina.metrica?.let { metrica ->
                        Text(
                            metrica,
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(14.dp))
                    }

                    when (pagina.tipoGrafica) {
                        TipoGrafica.ONDA -> OndaAudio(
                            tendencia = pagina.tendencia,
                            color = if (pagina.alerta) RojoAlerta else color,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                        )
                        TipoGrafica.NIVEL -> IndicadorNivel(
                            nivel = pagina.tendencia,
                            color = color,
                            modifier = Modifier.size(84.dp)
                        )
                        TipoGrafica.NINGUNA -> Unit
                    }

                    if (pagina.tipoGrafica != TipoGrafica.NINGUNA) {
                        Spacer(Modifier.height(16.dp))
                    }

                    Text(
                        pagina.cuerpo,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = Color.White.copy(alpha = 0.88f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                nombreHistoria,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.graphicsLayer { alpha = alfaPie }
            )
        }
    }
}

// ─────────── FONDO DINÁMICO "SPOTIFY WRAPPED" (sin foto de stock) ──────

@Composable
private fun FondoDinamico(pagina: PaginaHistoria) {
    val (tope, base) = gradientePara(pagina.colorHex)
    val acento = Color(pagina.colorHex)
    val transicion = rememberInfiniteTransition(label = "fondo")
    val t by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "t"
    )
    val t2 by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(11000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "t2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to tope, 1f to base))
    ) {
        // Círculos de luz difuminados flotando.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset { IntOffset((60 + (t * 40f).toInt()), 40) }
                .size(340.dp)
                .blur(70.dp)
                .background(acento.copy(alpha = 0.30f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset { IntOffset((-40 - (t2 * 50f).toInt()), -30) }
                .size(300.dp)
                .blur(80.dp)
                .background(acento.copy(alpha = 0.22f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset { IntOffset((t2 * 80f).toInt() - 40, 20) }
                .size(160.dp)
                .blur(50.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
        )

        // Ondas de frecuencia sutiles que se desplazan.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val fase = t * 2f * PI.toFloat()
            val trazo = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            repeat(4) { linea ->
                val path = Path()
                val alturaBase = size.height * (0.62f + linea * 0.09f)
                val amplitud = size.height * (0.018f + 0.006f * linea)
                val despFase = fase + linea * 1.1f
                val pasos = 40
                for (i in 0..pasos) {
                    val x = size.width * i / pasos
                    val u = i.toFloat() / pasos
                    val y = alturaBase + sin(
                        (u * 2f * PI.toFloat() * 2.5f + despFase).toDouble()
                    ).toFloat() * amplitud
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.05f + 0.03f * linea),
                    style = trazo
                )
            }
        }
    }
}

@Composable
private fun ChipSalud(texto: String, alerta: Boolean) {
    val acento = if (alerta) RojoAlerta else Menta
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(acento.copy(alpha = 0.22f))
            .padding(horizontal = 13.dp, vertical = 5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(acento)
        )
        Text(
            texto,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = if (alerta) Color(0xFFFFB4AB) else Color(0xFFA7F3D0)
        )
    }
}

@Composable
private fun OndaAudio(
    tendencia: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val transicion = rememberInfiniteTransition(label = "onda")
    val pulso by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulso"
    )
    val alturas = remember {
        val rnd = java.util.Random(7)
        List(26) { 0.16f + rnd.nextFloat() * 0.30f }
    }
    val factor = tendencia.coerceIn(0.15f, 1f)

    Canvas(modifier = modifier) {
        val anchoBarra = size.width / (alturas.size * 1.55f)
        val paso = anchoBarra * 1.55f
        alturas.forEachIndexed { i, base ->
            val ondulado = 0.72f + 0.28f *
                ((sin((i * 0.9f + pulso * 6.2832f).toDouble()).toFloat() + 1f) / 2f)
            val altura = size.height * base.coerceIn(0.12f, 1f) * factor * ondulado
            drawRoundRect(
                color = color.copy(alpha = 0.95f),
                topLeft = Offset(i * paso + anchoBarra * 0.25f, (size.height - altura) / 2f),
                size = Size(anchoBarra, altura),
                cornerRadius = CornerRadius(anchoBarra / 2f)
            )
        }
    }
}

@Composable
private fun IndicadorNivel(
    nivel: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animado by animateFloatAsState(
        targetValue = nivel.coerceIn(0f, 1f),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "nivel"
    )
    Canvas(modifier = modifier) {
        val stroke = 9.dp.toPx()
        val inset = stroke / 2 + 5.dp.toPx()
        val dimension = Size(size.width - inset * 2, size.height - inset * 2)
        drawArc(
            color = Color.White.copy(alpha = 0.18f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = dimension,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            brush = Brush.sweepGradient(listOf(color, color.copy(alpha = 0.45f))),
            startAngle = -90f,
            sweepAngle = 360f * animado,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = dimension,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}
