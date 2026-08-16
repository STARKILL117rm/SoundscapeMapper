package com.example.soundscapemapper.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.soundscapemapper.AppDatabase
import com.example.soundscapemapper.sensor.SensorStateHolder
import com.example.soundscapemapper.ui.screens.analisis.AnalisisScreen
import com.example.soundscapemapper.ui.screens.hoy.HoyScreen
import com.example.soundscapemapper.ui.screens.mapa.MapaScreen
import com.example.soundscapemapper.ui.screens.registro.RegistroScreen
import com.example.soundscapemapper.ui.screens.yo.YoScreen
import java.time.LocalTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

object Rutas {
    const val HOY = "hoy"
    const val ANALISIS = "analisis"
    const val MAPA = "mapa"
    const val REGISTRO = "registro"
    const val YO = "yo"
}

data class Destino(val ruta: String, val etiqueta: String, val icono: ImageVector)

private val destinos = listOf(
    Destino(Rutas.HOY, "Hoy", Icons.Filled.Home),
    Destino(Rutas.MAPA, "Mapa", Icons.Filled.Map),
    Destino(Rutas.REGISTRO, "Registro", Icons.AutoMirrored.Filled.List),
    Destino(Rutas.YO, "Guía", Icons.Filled.Person)
)

private val FondoOscuro = Color(0xFF0D1117)
private val Menta = Color(0xFF00F5A0)
private val Turquesa = Color(0xFF00D9F6)
private val GrisInactivo = Color(0xFF8B949E)

private data class TemaBarra(
    val fondo: Color,
    val indicador: Color,
    val seleccionado: Color,
    val inactivo: Color
)

private fun temaBarraPara(hora: Int): TemaBarra = when (hora) {
    in 6..19 -> TemaBarra(
        // Día y tarde usan esquema claro para mejor legibilidad; la noche usa el esquema oscuro.
        fondo = Color(0xFFFAFAFA),
        indicador = Color(0xFFDCFCE7),
        seleccionado = Color(0xFF059669),
        inactivo = Color(0xFF9CA3AF)
    )
    else -> TemaBarra(
        fondo = Color(0xFF0B0E14),
        indicador = Color(0xFF1F2937),
        seleccionado = Menta,
        inactivo = GrisInactivo
    )
}

@Composable
fun EspacioSeguroApp(
    db: AppDatabase,
    sensorState: SensorStateHolder,
    onToggleServicio: (Boolean) -> Unit,
    onAlternarCaptura: (Boolean) -> Unit,
    onCambiarUmbral: (Float) -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = backStackEntry?.destination?.route

    val mostrarBarra = rutaActual in destinos.map { it.ruta }

    var horaBarra by remember { mutableIntStateOf(LocalTime.now().hour) }
    LaunchedEffect(Unit) {
        while (isActive) {
            horaBarra = LocalTime.now().hour
            delay(1000L)
        }
    }
    val temaBarra = remember(horaBarra) { temaBarraPara(horaBarra) }
    val fondoBarra by animateColorAsState(temaBarra.fondo, tween(1000), label = "fondoBarra")
    val indicadorBarra by animateColorAsState(temaBarra.indicador, tween(1000), label = "indicadorBarra")
    val seleccionadoBarra by animateColorAsState(temaBarra.seleccionado, tween(1000), label = "seleccionadoBarra")
    val inactivoBarra by animateColorAsState(temaBarra.inactivo, tween(1000), label = "inactivoBarra")

    Scaffold(
        containerColor = FondoOscuro,
        bottomBar = {
            if (mostrarBarra) {
                NavigationBar(
                    containerColor = fondoBarra,
                    tonalElevation = 0.dp
                ) {
                    destinos.forEach { destino ->
                        val seleccionado = rutaActual == destino.ruta
                        NavigationBarItem(
                            selected = seleccionado,
                            onClick = {
                                navController.navigate(destino.ruta) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destino.icono, contentDescription = destino.etiqueta) },
                            label = { Text(destino.etiqueta, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = seleccionadoBarra,
                                selectedTextColor = seleccionadoBarra,
                                indicatorColor = indicadorBarra,
                                unselectedIconColor = inactivoBarra,
                                unselectedTextColor = inactivoBarra
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Rutas.HOY,
            modifier = Modifier.padding(padding),
            enterTransition = {
                androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220)) +
                    androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(220)) { it / 8 }
            },
            exitTransition = {
                androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160))
            },
            popEnterTransition = {
                androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220)) +
                    androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(220)) { -it / 8 }
            },
            popExitTransition = {
                androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160)) +
                    androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(160)) { it / 8 }
            }
        ) {
            composable(Rutas.HOY) {
                HoyScreen(
                    db = db,
                    sensorState = sensorState
                )
            }
            composable(Rutas.ANALISIS) {
                AnalisisScreen(
                    db = db,
                    sensorState = sensorState,
                    onReanudarCaptura = { onAlternarCaptura(false) },
                    onTerminar = { navController.popBackStack() }
                )
            }
            composable(Rutas.MAPA) {
                MapaScreen(db = db)
            }
            composable(Rutas.REGISTRO) {
                RegistroScreen(
                    db = db,
                    onNuevaMedicion = { navController.navigate(Rutas.ANALISIS) }
                )
            }
            composable(Rutas.YO) {
                YoScreen(
                    sensorState = sensorState,
                    db = db,
                    onToggleServicio = onToggleServicio,
                    onAlternarCaptura = onAlternarCaptura,
                    onCambiarUmbral = onCambiarUmbral
                )
            }
        }
    }
}
