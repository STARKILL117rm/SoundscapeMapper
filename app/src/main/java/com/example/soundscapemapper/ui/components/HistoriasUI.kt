package com.example.soundscapemapper.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.soundscapemapper.ui.historias.Historia
import com.example.soundscapemapper.ui.historias.PaginaHistoria
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DURACION_PAGINA_MS = 4500

@Composable
fun FilaHistorias(
    historias: List<Historia>,
    vistas: Set<String>,
    onAbrir: (Historia) -> Unit
) {
    if (historias.isEmpty()) return
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(historias, key = { it.id }) { historia ->
            HistoriaBurbuja(
                historia = historia,
                vista = historia.id in vistas,
                onClick = { onAbrir(historia) }
            )
        }
    }
}

@Composable
private fun HistoriaBurbuja(
    historia: Historia,
    vista: Boolean,
    onClick: () -> Unit
) {
    val color = Color(historia.colorHex)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .clip(CircleShape)
                .then(
                    if (vista) Modifier.background(Color(0xFFD9DED6))
                    else Modifier.background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.45f))))
                )
                .padding(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, color.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(historia.emoji, fontSize = 26.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            historia.nombre,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (vista) MaterialTheme.colorScheme.onSurfaceVariant else color,
            maxLines = 1
        )
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
    val pagerState = rememberPagerState(initialPage = paginaInicial) { paginas.size }
    val progreso = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var pausado by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        val historiaIndex = paginas[pagerState.currentPage].first
        historias.take(historiaIndex + 1).forEach { onVista(it.id) }
    }

    LaunchedEffect(pagerState.currentPage) {
        progreso.snapTo(0f)
        val pasoMs = 100L
        var transcurrido = 0L
        while (transcurrido < DURACION_PAGINA_MS) {
            if (!pausado) {
                delay(pasoMs)
                transcurrido += pasoMs
                progreso.snapTo((transcurrido.toFloat() / DURACION_PAGINA_MS).coerceIn(0f, 1f))
            } else {
                delay(pasoMs)
            }
        }
        if (pagerState.currentPage < paginas.size - 1) {
            pagerState.animateScrollToPage(pagerState.currentPage + 1)
        } else {
            onCerrar()
        }
    }

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
                .background(Color(0xFF12150F))
                .statusBarsPadding()
                .navigationBarsPadding()
                .pointerInput(pagerState.currentPage, paginas.size) {
                    detectTapGestures(
                        onPress = {
                            // Mantener presionado pausa el avance automático.
                            pausado = true
                            try {
                                tryAwaitRelease()
                            } finally {
                                pausado = false
                            }
                        },
                        onLongPress = { /* la pausa ya está activa durante la presión */ },
                        onTap = { offset ->
                            val ancho = size.width.toFloat()
                            when {
                                offset.x < ancho / 3f -> {
                                    if (pagerState.currentPage > 0) {
                                        scope.launch { pagerState.scrollToPage(pagerState.currentPage - 1) }
                                    }
                                }

                                else -> {
                                    if (pagerState.currentPage < paginas.size - 1) {
                                        scope.launch { pagerState.scrollToPage(pagerState.currentPage + 1) }
                                    } else {
                                        onCerrar()
                                    }
                                }
                            }
                        }
                    )
                }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val (i, j, pagina) = paginas[page]
                ContenidoPagina(
                    pagina = pagina,
                    nombreHistoria = historias[i].nombre,
                    numero = j + 1,
                    total = historias[i].paginas.size
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                val (i, j, _) = paginas[pagerState.currentPage]
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    historias[i].paginas.forEachIndexed { k, _ ->
                        val fraccion = when {
                            k < j -> 1f
                            k == j -> progreso.value
                            else -> 0f
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.28f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .fillMaxWidth(fraccion)
                                    .background(Color.White, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        historias[paginas[pagerState.currentPage].first].nombre,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
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
    numero: Int,
    total: Int
) {
    val color = Color(pagina.colorHex)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(pagina.emoji, fontSize = 56.sp)
            Spacer(Modifier.height(24.dp))
            pagina.datoDestacado?.let { dato ->
                Text(
                    dato,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = color,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                pagina.titulo,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                pagina.cuerpo,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Color.White.copy(alpha = 0.72f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            Text(
                "$numero / $total · $nombreHistoria",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.45f)
            )
        }
    }
}
