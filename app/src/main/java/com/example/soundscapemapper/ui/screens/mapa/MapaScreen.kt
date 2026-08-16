package com.example.soundscapemapper.ui.screens.mapa

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.soundscapemapper.AppDatabase
import com.example.soundscapemapper.ui.viewmodel.MapaViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourcePolicy
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

// ====================== CONSTANTES DE LA PANTALLA =======================

private const val ZOOM_VISTA_GENERAL = 13
private const val ZOOM_PARADA = 16
private val CENTRO_PACHUCA_GEO = GeoPoint(CENTRO_PACHUCA.latitud, CENTRO_PACHUCA.longitud)
private const val DURACION_CAMARA_MS = 700L

// UA propio, sin normalizar, para que OSM no bloquee los tiles.
private const val USER_AGENT = "SoundscapeMapper/1.0 (app educativa de monitoreo de ruido)"

/** Referencia viva al MapView (evita el estado Compose en la vista nativa). */
private class MapaHolder {
    var mapa: MapView? = null
}

// ============================== PANTALLA ================================
// "Diario Espacial": diario de salud auditiva sobre OpenStreetMap con tres
// capas: DIARIO (estancias del día con pins OMS), REFUGIOS (zonas de refugio
// sonoro de Pachuca) y CALOR (mapa de calor personal). Patrón anti-crash:
// ciclo de vida vía MapaHolder en un DisposableEffect estable; onDetach solo
// en onRelease; cámara y overlays desde LaunchedEffect keyed.

@Composable
fun MapaScreen(db: AppDatabase) {
    val vm: MapaViewModel = viewModel(factory = MapaViewModel.factory(db))
    val holder = remember { MapaHolder() }
    val scope = rememberCoroutineScope()

    var mapaListo by remember { mutableStateOf(false) }

    // Datos derivados (con fallback de demostración si no hay estancias hoy).
    val estanciasHoy = remember(vm.mediciones) {
        agruparEnEstancias(vm.mediciones.filter { esDeHoy(it) })
    }
    val esDemo = remember(vm.cargado, estanciasHoy) { vm.cargado && estanciasHoy.isEmpty() }
    val estancias = if (esDemo) ESTANCIAS_DEMO_PACHUCA else estanciasHoy
    val dosisHoy = remember(vm.registroHoy, esDemo) {
        if (esDemo) dosisSimulada(ESTANCIAS_DEMO_PACHUCA)
        else vm.registroHoy?.minutosSobre80 ?: 0.0
    }
    val refugios = remember { REFUGIOS_PACHUCA }

    // Estado de la UI.
    var modo by remember { mutableStateOf(ModoMapa.DIARIO) }
    var filtro by remember { mutableStateOf(PatronFiltro.TODO) }

    // Malla de calor (según el filtro de patrón semanal activo).
    val puntosCalor = remember(vm.mediciones, filtro, esDemo) {
        if (esDemo) {
            ESTANCIAS_DEMO_PACHUCA.map { CalorPunto(it.latitud, it.longitud, it.decibelios) }
        } else {
            vm.mediciones.filter { cumplePatron(it, filtro) }
                .map { CalorPunto(it.latitud, it.longitud, it.decibelios) }
        }
    }
    val celdasCalor = remember(puntosCalor) { construirMallaCalor(puntosCalor) }

    val zonaMayorExposicion = remember(vm.mediciones, filtro, esDemo) {
        if (esDemo) zonaConMayorExposicionEstancias(ESTANCIAS_DEMO_PACHUCA)
        else zonaConMayorExposicion(vm.mediciones, filtro)
    }

    // Refugio sugerido (desde la estancia más ruidosa).
    val refugioSugerido = remember(estancias) {
        estancias.maxByOrNull { it.decibelios }
            ?.let { refugioMasCercano(it.latitud, it.longitud) }
    }
    val mostrarBanner = refugioActivo(dosisHoy) && modo != ModoMapa.CALOR

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { estancias.size })
    val refugioPagerState = rememberPagerState(initialPage = 0, pageCount = { refugios.size })

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val mapa = holder.mapa
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapa?.onResume()
                    vm.cargar()
                }
                Lifecycle.Event.ON_PAUSE -> mapa?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) { vm.cargar() }

    // Sincronización cámara ⇄ capa activa: anima al pin de la página actual.
    LaunchedEffect(modo, pagerState.currentPage, refugioPagerState.currentPage, mapaListo) {
        if (!mapaListo) return@LaunchedEffect
        val mapa = holder.mapa ?: return@LaunchedEffect
        val objetivo: GeoPoint? = when (modo) {
            ModoMapa.DIARIO -> estancias.getOrNull(pagerState.currentPage)
                ?.let { GeoPoint(it.latitud, it.longitud) }

            ModoMapa.REFUGIOS -> refugios.getOrNull(refugioPagerState.currentPage)
                ?.let { GeoPoint(it.latitud, it.longitud) }

            ModoMapa.CALOR -> null
        }
        if (objetivo == null) return@LaunchedEffect
        mapa.controller.animateTo(
            objetivo,
            ZOOM_PARADA.toDouble(),
            DURACION_CAMARA_MS
        )
    }

    // Aplica la capa de overlays según el modo activo.
    LaunchedEffect(modo, estancias, refugios, celdasCalor, mapaListo) {
        val mapa = holder.mapa ?: return@LaunchedEffect
        if (!mapaListo) return@LaunchedEffect
        aplicarOverlays(
            mapa = mapa,
            modo = modo,
            estancias = estancias,
            refugios = refugios,
            celdasCalor = celdasCalor,
            scope = scope,
            pagerState = pagerState,
            refugioPagerState = refugioPagerState
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                crearMapa(context, holder, onPrimerLayout = { mapaListo = true })
            },
            modifier = Modifier.fillMaxSize(),
            onRelease = { it.onDetach() }
        )

        // Cabecera + chips de capa/filtro.
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EncabezadoDiario(
                esDemo = esDemo,
                total = estancias.size
            )
            Spacer(Modifier.height(8.dp))
            ChipsModo(modo = modo, onCambiarModo = { modo = it })
            if (modo == ModoMapa.CALOR) {
                Spacer(Modifier.height(8.dp))
                ChipsFiltro(filtro = filtro, onCambiarFiltro = { filtro = it })
            }
        }

        // Contenido inferior (banner + carrusel/card según capa).
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (mostrarBanner) {
                BannerRefugio(
                    dosisMin = dosisHoy,
                    refugio = refugioSugerido,
                    onClick = { modo = ModoMapa.REFUGIOS }
                )
                Spacer(Modifier.height(10.dp))
            }
            when (modo) {
                ModoMapa.DIARIO -> HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) { page ->
                    estancias.getOrNull(page)?.let {
                        TarjetaEstancia(it, page, estancias.size)
                    }
                }

                ModoMapa.REFUGIOS -> HorizontalPager(
                    state = refugioPagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) { page ->
                    refugios.getOrNull(page)?.let {
                        TarjetaRefugio(it, page, refugios.size)
                    }
                }

                ModoMapa.CALOR -> TarjetaResumenCalor(
                    puntos = puntosCalor.size,
                    zona = zonaMayorExposicion,
                    filtro = filtro,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

// =========================== MAPA OSM (osmdroid) ========================

private fun crearMapa(
    context: Context,
    holder: MapaHolder,
    onPrimerLayout: () -> Unit
): MapView {
    // UA propio, configurado antes de que el MapView pida tiles.
    Configuration.getInstance().load(
        context,
        android.preference.PreferenceManager.getDefaultSharedPreferences(context)
    )
    Configuration.getInstance().userAgentValue = USER_AGENT

    return MapView(context).apply {
        // UA sin normalizar (TileSourcePolicy sin FLAG_USER_AGENT_NORMALIZED).
        setTileSource(
            XYTileSource(
                "Mapnik", 0, 19, 256, ".png",
                arrayOf("https://tile.openstreetmap.org/"),
                "© OpenStreetMap contributors",
                TileSourcePolicy(2, TileSourcePolicy.FLAG_NO_BULK or TileSourcePolicy.FLAG_NO_PREVENTIVE)
            )
        )
        setMultiTouchControls(true)
        setBuiltInZoomControls(false)
        setTilesScaledToDpi(false)

        controller.setCenter(CENTRO_PACHUCA_GEO)
        controller.setZoom(ZOOM_VISTA_GENERAL.toDouble())

        holder.mapa = this

        addOnFirstLayoutListener { _, _, _, _, _ ->
            onPrimerLayout()
        }
    }
}

/** Reemplaza la capa de overlays según el modo activo. */
private fun aplicarOverlays(
    mapa: MapView,
    modo: ModoMapa,
    estancias: List<Estancia>,
    refugios: List<RefugioSonoro>,
    celdasCalor: List<CeldaCalor>,
    scope: CoroutineScope,
    pagerState: PagerState,
    refugioPagerState: PagerState
) {
    mapa.overlays.removeAll { it is Marker || it is MapaHeatOverlay }
    when (modo) {
        ModoMapa.DIARIO -> estancias.forEachIndexed { index, estancia ->
            mapa.overlays.add(
                crearMarkerEstancia(mapa, estancia, scope, pagerState, index)
            )
        }

        ModoMapa.REFUGIOS -> refugios.forEachIndexed { index, refugio ->
            mapa.overlays.add(
                crearMarkerRefugio(mapa, refugio, scope, refugioPagerState, index)
            )
        }

        ModoMapa.CALOR -> if (celdasCalor.isNotEmpty()) {
            mapa.overlays.add(MapaHeatOverlay(celdasCalor))
        }
    }
    mapa.invalidate()
}

private fun crearMarkerEstancia(
    mapa: MapView,
    estancia: Estancia,
    scope: CoroutineScope,
    pagerState: PagerState,
    index: Int
): Marker = Marker(mapa).apply {
    position = GeoPoint(estancia.latitud, estancia.longitud)
    setAnchor(0.5f, 1.0f)
    icon = BitmapDrawable(
        mapa.context.resources,
        pinBitmap(colorNivel(estancia.nivel).toArgb())
    )
    title = estancia.nombre
    snippet = "${estancia.decibelios.toInt()} dB · ${estadoBadge(estancia.nivel)}"
    setOnMarkerClickListener { _, _ ->
        scope.launch { pagerState.animateScrollToPage(index) }
        true
    }
}

private fun crearMarkerRefugio(
    mapa: MapView,
    refugio: RefugioSonoro,
    scope: CoroutineScope,
    refugioPagerState: PagerState,
    index: Int
): Marker = Marker(mapa).apply {
    position = GeoPoint(refugio.latitud, refugio.longitud)
    setAnchor(0.5f, 0.5f)
    icon = BitmapDrawable(mapa.context.resources, arbolBitmap())
    title = refugio.nombre
    snippet = "${refugio.categoria} · ${distanciaCentroTexto(refugio.latitud, refugio.longitud)}"
    setOnMarkerClickListener { _, _ ->
        scope.launch { refugioPagerState.animateScrollToPage(index) }
        true
    }
}

// ============================== UI AUXILIAR =============================

@Composable
private fun EncabezadoDiario(esDemo: Boolean, total: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = Color(0xEEF9FBF7),
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981))
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Diario Espacial",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "· ${if (esDemo) "Demo" else "Hoy"} · $total estancias",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6B7280)
            )
        }
    }
}

@Composable
private fun ChipsModo(modo: ModoMapa, onCambiarModo: (ModoMapa) -> Unit, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        ModoMapa.entries.forEach { candidato ->
            FilterChip(
                selected = modo == candidato,
                onClick = { onCambiarModo(candidato) },
                label = { Text(etiquetaModo(candidato)) }
            )
        }
    }
}

@Composable
private fun ChipsFiltro(filtro: PatronFiltro, onCambiarFiltro: (PatronFiltro) -> Unit, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        PatronFiltro.entries.forEach { candidato ->
            FilterChip(
                selected = filtro == candidato,
                onClick = { onCambiarFiltro(candidato) },
                label = { Text(etiquetaPatron(candidato)) }
            )
        }
    }
}

private fun etiquetaModo(modo: ModoMapa): String = when (modo) {
    ModoMapa.DIARIO -> "Diario"
    ModoMapa.REFUGIOS -> "Refugios"
    ModoMapa.CALOR -> "Calor"
}

@Composable
private fun TarjetaEstancia(
    estancia: Estancia,
    indice: Int,
    total: Int
) {
    val color = colorNivel(estancia.nivel)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFFF9FBF7),
        shadowElevation = 14.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${indice + 1} · $total",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF9CA3AF)
                )
                Spacer(Modifier.weight(1f))
                BadgeEstado(nivel = estancia.nivel)
            }

            Spacer(Modifier.height(10.dp))

            Text(
                estancia.nombre,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )

            Spacer(Modifier.height(4.dp))

            Text(
                estancia.categoria,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF9CA3AF)
            )

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    "${estancia.horaInicio} - ${estancia.horaFin} hrs  ·  ${estancia.duracionMin} min",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6B7280)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${estancia.decibelios.toInt()}",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = color
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "dB",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun TarjetaRefugio(refugio: RefugioSonoro, indice: Int, total: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFFF9FBF7),
        shadowElevation = 14.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0D9488))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${indice + 1} · $total",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF9CA3AF)
                )
                Spacer(Modifier.weight(1f))
                BadgeRefugio()
            }

            Spacer(Modifier.height(10.dp))

            Text(
                refugio.nombre,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )

            Spacer(Modifier.height(4.dp))

            Text(
                refugio.categoria,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6B7280)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                distanciaCentroTexto(refugio.latitud, refugio.longitud),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6B7280)
            )
        }
    }
}

@Composable
private fun TarjetaResumenCalor(
    puntos: Int,
    zona: String?,
    filtro: PatronFiltro,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFFF9FBF7),
        shadowElevation = 14.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Mapa de Calor Personal",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                "$puntos muestras · Patrón: ${etiquetaPatron(filtro)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6B7280)
            )

            if (zona != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Zona con mayor exposición: $zona",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                PuntoLeyenda(Color(0xFF10B981), "<60 dB")
                Spacer(Modifier.width(14.dp))
                PuntoLeyenda(Color(0xFFF59E0B), "70 dB")
                Spacer(Modifier.width(14.dp))
                PuntoLeyenda(Color(0xFFEF4444), ">80 dB")
            }
        }
    }
}

@Composable
private fun PuntoLeyenda(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            texto,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF6B7280)
        )
    }
}

@Composable
private fun BadgeEstado(nivel: NivelRuido) {
    val color = colorNivel(nivel)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            estadoBadge(nivel),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun BadgeRefugio() {
    val color = Color(0xFF0D9488)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            "Refugio sonoro",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun BannerRefugio(
    dosisMin: Double,
    refugio: RefugioSonoro?,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0D9488),
        shadowElevation = 10.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${dosisMin.toInt()} min en zonas ruidosas",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "Ir a refugios ›",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCCFBF1)
                )
            }
            if (refugio != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Descanso sugerido: ${refugio.nombre} (${refugio.categoria})",
                    fontSize = 12.sp,
                    color = Color(0xFFF0FDFA)
                )
            }
        }
    }
}

// ======================== DIBUJO DE ICONOS (pin y árbol) ================
// Pin de mapa con el color hex OMS exacto y árbol verde para los refugios.

private fun pinBitmap(colorArgb: Int): Bitmap {
    val size = 96
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val cx = size / 2f
    val relleno = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorArgb }
    val trazo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 7f
    }

    // Punta triangular inferior (el ancla del pin se fija en la base).
    val punta = Path().apply {
        moveTo(cx - 17f, 41f)
        lineTo(cx + 17f, 41f)
        lineTo(cx, 71f)
        close()
    }
    canvas.drawPath(punta, relleno)
    canvas.drawPath(punta, trazo)

    // Cabeza circular.
    canvas.drawCircle(cx, 38f, 26f, relleno)
    canvas.drawCircle(cx, 38f, 26f, trazo)

    // Orificio interior blanco (estilo de pin de mapa).
    relleno.color = android.graphics.Color.WHITE
    canvas.drawCircle(cx, 38f, 9f, relleno)

    return bmp
}

/** Icono de árbol (verde) para marcar las zonas de refugio sonoro. */
private fun arbolBitmap(): Bitmap {
    val size = 96
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val cx = size / 2f
    val copa = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF10B981.toInt() }
    val tronco = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF92400E.toInt() }
    val anillo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    // Tronco.
    canvas.drawRect(cx - 7f, 58f, cx + 7f, 76f, tronco)

    // Copa (tres círculos superpuestos).
    canvas.drawCircle(cx, 42f, 18f, copa)
    canvas.drawCircle(cx - 16f, 50f, 13f, copa)
    canvas.drawCircle(cx + 16f, 50f, 13f, copa)

    // Anillo blanco para distinguirse sobre el mapa.
    canvas.drawCircle(cx, 42f, 18f, anillo)
    canvas.drawCircle(cx - 16f, 50f, 13f, anillo)
    canvas.drawCircle(cx + 16f, 50f, 13f, anillo)

    return bmp
}
