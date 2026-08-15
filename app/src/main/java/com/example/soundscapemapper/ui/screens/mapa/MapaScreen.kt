package com.example.soundscapemapper.ui.screens.mapa

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.soundscapemapper.AppDatabase
import com.example.soundscapemapper.BuildConfig
import com.example.soundscapemapper.sensor.SensorStateHolder
import com.example.soundscapemapper.ui.components.AzulInfo
import com.example.soundscapemapper.ui.components.RojoSalud
import com.example.soundscapemapper.ui.components.VerdeSalud
import com.example.soundscapemapper.ui.viewmodel.MapaViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

@Composable
fun MapaScreen(
    db: AppDatabase,
    sensorState: SensorStateHolder,
    onIrRegistro: () -> Unit
) {
    if (BuildConfig.MAPS_API_KEY.isBlank()) {
        MapaSinClave(onIrRegistro = onIrRegistro)
        return
    }

    val vm: MapaViewModel = viewModel(factory = MapaViewModel.factory(db))
    LaunchedEffect(Unit) { vm.cargar() }

    val scope = rememberCoroutineScope()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(sensorState.latitud.value, sensorState.longitud.value),
            16f
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = true
            )
        ) {
            vm.mediciones.forEach { m ->
                Marker(
                    state = MarkerState(position = LatLng(m.latitud, m.longitud)),
                    title = m.nombreLugar,
                    snippet = "${m.decibelios} dB · ${m.categoria} · ${m.fechaHora}",
                    icon = BitmapDescriptorFactory.defaultMarker(
                        if (m.categoria == "Tranquilo") BitmapDescriptorFactory.HUE_GREEN
                        else BitmapDescriptorFactory.HUE_RED
                    ),
                    onInfoWindowClick = { onIrRegistro() }
                )
            }
        }

        Leyenda(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )

        IconButton(
            onClick = {
                scope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(sensorState.latitud.value, sensorState.longitud.value),
                            16f
                        )
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
        ) {
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = "Centrar en mi ubicación",
                tint = VerdeSalud
            )
        }

        TarjetaEstadoMapa(
            sensorState = sensorState,
            onIrRegistro = onIrRegistro,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

@Composable
private fun Leyenda(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            PuntoLeyenda(VerdeSalud, "Tranquilo")
            Spacer(Modifier.height(4.dp))
            PuntoLeyenda(RojoSalud, "Estresante")
        }
    }
}

@Composable
private fun PuntoLeyenda(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(texto, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TarjetaEstadoMapa(
    sensorState: SensorStateHolder,
    onIrRegistro: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📍", fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${sensorState.decibelios.value} dB ahora · ${sensorState.nivelLuz.value.toInt()} lux",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    String.format(
                        java.util.Locale.US,
                        "%.4f, %.4f",
                        sensorState.latitud.value,
                        sensorState.longitud.value
                    ),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                onClick = onIrRegistro,
                shape = RoundedCornerShape(10.dp),
                color = AzulInfo.copy(alpha = 0.12f)
            ) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Ver registro", tint = AzulInfo)
                    Spacer(Modifier.width(4.dp))
                    Text("Registro", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AzulInfo)
                }
            }
        }
    }
}

@Composable
private fun MapaSinClave(onIrRegistro: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(VerdeSalud.copy(alpha = 0.10f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Map, contentDescription = null, tint = VerdeSalud, modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(8.dp))
                Text("Mapa no disponible", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "Aún falta configurar la clave de Google Maps",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text("Cómo activarlo (2 minutos)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                PasoMapa(1, "Abre console.cloud.google.com y crea un proyecto (gratis).")
                PasoMapa(2, "Busca \"Maps SDK for Android\" y actívalo.")
                PasoMapa(3, "APIs y servicios → Credenciales → Clave de API.")
                PasoMapa(4, "Copia la clave (AIza…) y pégala en gradle.properties:")
                Text(
                    "MAPS_API_KEY=AIza…",
                    fontSize = 11.sp,
                    color = VerdeSalud,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(start = 12.dp, top = 6.dp)
                        .background(VerdeSalud.copy(alpha = 0.10f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
                PasoMapa(5, "Recompila y el mapa aparecerá aquí con tus lugares.")
            }
        }

        Spacer(Modifier.height(16.dp))

        Surface(
            onClick = onIrRegistro,
            shape = RoundedCornerShape(12.dp),
            color = VerdeSalud
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Ver registro de mediciones", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PasoMapa(numero: Int, texto: String) {
    Row(modifier = Modifier.padding(vertical = 6.dp)) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(VerdeSalud, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("$numero", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            texto,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
