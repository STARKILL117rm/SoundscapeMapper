package com.example.soundscapemapper.ui.navigation

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.soundscapemapper.AppDatabase
import com.example.soundscapemapper.sensor.SensorStateHolder
import com.example.soundscapemapper.ui.components.VerdeSalud
import com.example.soundscapemapper.ui.screens.analisis.AnalisisScreen
import com.example.soundscapemapper.ui.screens.hoy.HoyScreen
import com.example.soundscapemapper.ui.screens.mapa.MapaScreen
import com.example.soundscapemapper.ui.screens.registro.RegistroScreen
import com.example.soundscapemapper.ui.screens.yo.YoScreen

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

    Scaffold(
        bottomBar = {
            if (mostrarBarra) {
                NavigationBar {
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
                                selectedIconColor = VerdeSalud,
                                selectedTextColor = VerdeSalud,
                                indicatorColor = VerdeSalud.copy(alpha = 0.12f)
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
                    sensorState = sensorState,
                    onAnalizar = { navController.navigate(Rutas.ANALISIS) },
                    onAlternarCaptura = onAlternarCaptura,
                    onIrA = { navController.navigate(it) }
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
                MapaScreen(
                    db = db,
                    sensorState = sensorState,
                    onIrRegistro = { navController.navigate(Rutas.REGISTRO) }
                )
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
                    onToggleServicio = onToggleServicio,
                    onAlternarCaptura = onAlternarCaptura,
                    onCambiarUmbral = onCambiarUmbral
                )
            }
        }
    }
}
