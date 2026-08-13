package com.example.soundscapemapper

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.soundscapemapper.ui.theme.SoundscapeMapperTheme
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var sensorLuz: Sensor? = null

    private var nivelLuzState = mutableStateOf(0f)
    private var decibeliosState = mutableStateOf(0.0)

    private var latitudActual = mutableStateOf(19.4326)
    private var longitudActual = mutableStateOf(-99.1332)

    private var isRecording = false
    private var audioRecord: AudioRecord? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
            iniciarCapturaAudio()
        }
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            obtenerUbicacionActual()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorLuz = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        val permisos = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permisos.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        requestPermissionLauncher.launch(permisos.toTypedArray())
        obtenerUbicacionActual()

        val db = AppDatabase.getDatabase(applicationContext)

        setContent {
            SoundscapeMapperTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PantallaPrincipalConDrawer(
                        decibelios = decibeliosState.value,
                        nivelLuz = nivelLuzState.value,
                        latitudActual = latitudActual.value,
                        longitudActual = longitudActual.value,
                        db = db,
                        onToggleServicio = { activar ->
                            try {
                                val intent = Intent(this, AudioMonitorService::class.java)
                                if (activar) {
                                    ContextCompat.startForegroundService(this, intent)
                                } else {
                                    stopService(intent)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    )
                }
            }
        }
    }

    private fun obtenerUbicacionActual() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        latitudActual.value = location.latitude
                        longitudActual.value = location.longitude
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            nivelLuzState.value = event.values[0]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onResume() {
        super.onResume()
        sensorLuz?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    private fun iniciarCapturaAudio() {
        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC, sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize
            )
            audioRecord?.startRecording()
            isRecording = true

            lifecycleScope.launch(Dispatchers.IO) {
                val buffer = ShortArray(bufferSize)
                while (isRecording) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        var sum = 0.0
                        for (i in 0 until readSize) {
                            sum += buffer[i] * buffer[i]
                        }
                        val amplitude = sum / readSize
                        val db = if (amplitude > 0) 10 * log10(amplitude) else 0.0

                        withContext(Dispatchers.Main) {
                            decibeliosState.value = String.format(Locale.US, "%.1f", db).toDouble()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// --- COMPONENTE DE GRÁFICA EN TIEMPO REAL ---

@Composable
fun GraficaEnTiempoReal(
    puntos: List<Float>,
    minVal: Float,
    maxVal: Float,
    colorLinea: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (puntos.size < 2) return@Canvas

        val width = size.width
        val height = size.height
        val stepX = width / (puntos.size - 1)

        val path = Path()
        val rango = if (maxVal - minVal == 0f) 1f else maxVal - minVal

        puntos.forEachIndexed { index, valor ->
            val x = index * stepX
            val valorNormalizado = ((valor - minVal) / rango).coerceIn(0f, 1f)
            val y = height - (valorNormalizado * height)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = colorLinea,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

// --- INTERFAZ ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipalConDrawer(
    decibelios: Double,
    nivelLuz: Float,
    latitudActual: Double,
    longitudActual: Double,
    db: AppDatabase,
    onToggleServicio: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var medicionesList by remember { mutableStateOf(listOf<Medicion>()) }
    var monitoreoActivo by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableStateOf(0) }

    var mostrandoDialogo by remember { mutableStateOf(false) }
    var analizandoSensors by remember { mutableStateOf(false) }
    var resultadoDiagnostico by remember { mutableStateOf("") }
    var dbCapturado by remember { mutableStateOf(0.0) }
    var luzCapturada by remember { mutableStateOf(0f) }
    var nombreLugarIngresado by remember { mutableStateOf("") }

    // HISTORIALES DE DATOS PARA LAS GRÁFICAS (Máximo 20 puntos)
    val historialSonido = remember { mutableStateListOf<Float>() }
    val historialLuz = remember { mutableStateListOf<Float>() }

    // Actualizar historial cuando cambien las métricas
    LaunchedEffect(decibelios) {
        historialSonido.add(decibelios.toFloat())
        if (historialSonido.size > 20) historialSonido.removeAt(0)
    }

    LaunchedEffect(nivelLuz) {
        historialLuz.add(nivelLuz)
        if (historialLuz.size > 20) historialLuz.removeAt(0)
    }

    // Cargar datos de la BD
    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val lista = db.medicionDao().obtenerTodasLasMediciones()
                withContext(Dispatchers.Main) {
                    medicionesList = lista
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun iniciarEscaneo() {
        nombreLugarIngresado = ""
        analizandoSensors = true
        mostrandoDialogo = true

        coroutineScope.launch {
            delay(3000)
            dbCapturado = decibelios
            luzCapturada = nivelLuz

            resultadoDiagnostico = if (dbCapturado > 65.0 || luzCapturada > 2500) {
                "Estresante"
            } else {
                "Tranquilo"
            }

            analizandoSensors = false
        }
    }

    fun guardarEnBD() {
        if (nombreLugarIngresado.isNotBlank()) {
            val fechaActual = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            val nuevaMedicion = Medicion(
                nombreLugar = nombreLugarIngresado,
                categoria = resultadoDiagnostico,
                decibelios = dbCapturado,
                nivelLuz = luzCapturada,
                latitud = latitudActual,
                longitud = longitudActual,
                fechaHora = fechaActual
            )

            coroutineScope.launch(Dispatchers.IO) {
                try {
                    db.medicionDao().insertarMedicion(nuevaMedicion)
                    val listaActualizada = db.medicionDao().obtenerTodasLasMediciones()
                    withContext(Dispatchers.Main) {
                        medicionesList = listaActualizada
                        mostrandoDialogo = false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun alternarCategoria(item: Medicion) {
        val nuevaCategoria = if (item.categoria == "Tranquilo") "Estresante" else "Tranquilo"
        val medicionEditada = item.copy(categoria = nuevaCategoria)

        coroutineScope.launch(Dispatchers.IO) {
            try {
                db.medicionDao().insertarMedicion(medicionEditada)
                val listaActualizada = db.medicionDao().obtenerTodasLasMediciones()
                withContext(Dispatchers.Main) {
                    medicionesList = listaActualizada
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun eliminarMedicion(item: Medicion) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                db.medicionDao().eliminarMedicion(item)
                val listaActualizada = db.medicionDao().obtenerTodasLasMediciones()
                withContext(Dispatchers.Main) {
                    medicionesList = listaActualizada
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun abrirNavegacionGPS(lat: Double, lng: Double) {
        try {
            val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")

            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val listaFiltrada = when (selectedTab) {
        1 -> medicionesList.filter { it.categoria == "Tranquilo" }
        2 -> medicionesList.filter { it.categoria == "Estresante" }
        else -> medicionesList
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "📍 Mis Lugares Mapeados",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                Divider()
                NavigationDrawerItem(
                    label = { Text("🌐 Todos los lugares (${medicionesList.size})") },
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(8.dp)
                )
                NavigationDrawerItem(
                    label = { Text("🌿 Espacios Tranquilos (${medicionesList.count { it.categoria == "Tranquilo" }})") },
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(8.dp)
                )
                NavigationDrawerItem(
                    label = { Text("⚠️ Espacios Ruidosos (${medicionesList.count { it.categoria == "Estresante" }})") },
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Soundscape Mapper", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menú")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Switch Servicio
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Protección auditiva 24/7", fontWeight = FontWeight.Bold)
                            Text("Alertar por ruido excesivo", fontSize = 12.sp)
                        }
                        Switch(
                            checked = monitoreoActivo,
                            onCheckedChange = { activo ->
                                monitoreoActivo = activo
                                onToggleServicio(activo)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // TARJETA DE SENSORES CON GRÁFICAS EN TIEMPO REAL
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // COLUMNA DE SONIDO CON SU GRÁFICA
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🔊 Sonido en Vivo", style = MaterialTheme.typography.labelMedium)
                            Text("$decibelios dB", fontSize = 22.sp, fontWeight = FontWeight.Bold)

                            Spacer(modifier = Modifier.height(8.dp))

                            GraficaEnTiempoReal(
                                puntos = historialSonido,
                                minVal = 0f,
                                maxVal = 100f,
                                colorLinea = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(35.dp)
                            )
                        }

                        Divider(
                            modifier = Modifier
                                .height(60.dp)
                                .width(1.dp)
                        )

                        // COLUMNA DE LUZ CON SU GRÁFICA
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("💡 Luz en Vivo", style = MaterialTheme.typography.labelMedium)
                            Text("${nivelLuz.toInt()} Lux", fontSize = 22.sp, fontWeight = FontWeight.Bold)

                            Spacer(modifier = Modifier.height(8.dp))

                            GraficaEnTiempoReal(
                                puntos = historialLuz,
                                minVal = 0f,
                                maxVal = (historialLuz.maxOrNull() ?: 100f).coerceAtLeast(100f),
                                colorLinea = Color(0xFFE65100),
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(35.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { iniciarEscaneo() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🔍 Analizar Entorno Actual", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // PESTAÑAS (TabRow)
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Todos") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("🌿 Tranquilo") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("⚠️ Ruidoso") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // LISTA FILTRADA
                if (listaFiltrada.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay registros en esta categoría",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(listaFiltrada) { item ->
                            val esTranquilo = item.categoria == "Tranquilo"
                            val cardBgColor = if (esTranquilo) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            val colorBadge = if (esTranquilo) Color(0xFF2E7D32) else Color(0xFFC62828)

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (esTranquilo) "🌿" else "⚠️",
                                            fontSize = 24.sp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.nombreLugar,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                            Text(
                                                text = "${item.decibelios} dB  •  ${item.nivelLuz.toInt()} Lux",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.DarkGray
                                            )
                                        }
                                        Badge(
                                            containerColor = colorBadge,
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = item.categoria,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = { abrirNavegacionGPS(item.latitud, item.longitud) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Navigation,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Volver al lugar", fontSize = 12.sp)
                                        }

                                        Row {
                                            IconButton(onClick = { alternarCategoria(item) }) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Cambiar Categoría",
                                                    tint = Color.DarkGray
                                                )
                                            }

                                            IconButton(onClick = { eliminarMedicion(item) }) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Eliminar Registro",
                                                    tint = Color(0xFFC62828)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrandoDialogo) {
        AlertDialog(
            onDismissRequest = { if (!analizandoSensors) mostrandoDialogo = false },
            confirmButton = {
                if (!analizandoSensors) {
                    Button(
                        onClick = { guardarEnBD() },
                        enabled = nombreLugarIngresado.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (resultadoDiagnostico == "Tranquilo") Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    ) {
                        Text("Guardar", color = Color.White)
                    }
                }
            },
            dismissButton = {
                if (!analizandoSensors) {
                    OutlinedButton(onClick = { mostrandoDialogo = false }) {
                        Text("Descartar")
                    }
                }
            },
            title = {
                Text(
                    text = if (analizandoSensors) "Midiendo Entorno..." else "Resultado del Análisis",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (analizandoSensors) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Analizando sonido, iluminación y posición GPS...")
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("🔊 Sonido: $dbCapturado dB")
                        Text("💡 Iluminación: ${luzCapturada.toInt()} Lux")

                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (resultadoDiagnostico == "Tranquilo") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (resultadoDiagnostico == "Tranquilo") "🌿" else "⚠️",
                                    fontSize = 28.sp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Lugar $resultadoDiagnostico",
                                        fontWeight = FontWeight.Bold,
                                        color = if (resultadoDiagnostico == "Tranquilo") Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                    Text(
                                        text = if (resultadoDiagnostico == "Tranquilo")
                                            "Ambiente cómodo y adecuado para la concentración."
                                        else
                                            "Niveles elevados de ruido o iluminación extrema.",
                                        fontSize = 12.sp,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = nombreLugarIngresado,
                            onValueChange = { nombreLugarIngresado = it },
                            label = { Text("Asigna un nombre a este lugar") },
                            placeholder = { Text("Ej: Biblioteca, Cafetería...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}