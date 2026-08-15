package com.example.soundscapemapper.ui.screens.registro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.soundscapemapper.AppDatabase
import com.example.soundscapemapper.Medicion
import com.example.soundscapemapper.SoundAnalyzer
import com.example.soundscapemapper.ui.components.AzulInfo
import com.example.soundscapemapper.ui.components.InsigniaCategoria
import com.example.soundscapemapper.ui.components.RojoSalud
import com.example.soundscapemapper.ui.components.VerdeSalud
import com.example.soundscapemapper.ui.viewmodel.RegistroViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroScreen(db: AppDatabase, onNuevaMedicion: () -> Unit) {
    val vm: RegistroViewModel = viewModel(factory = RegistroViewModel.factory(db))
    LaunchedEffect(Unit) { vm.cargar() }

    var seleccionada by remember { mutableStateOf<Medicion?>(null) }
    var pendienteBorrar by remember { mutableStateOf<Medicion?>(null) }
    val haptic = LocalHapticFeedback.current
    val hojaSheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registro") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNuevaMedicion,
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                    defaultElevation = 3.dp
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Nueva medición", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = vm.busqueda,
                onValueChange = { vm.busqueda = it },
                placeholder = { Text("Buscar por nombre…") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp)
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChipFiltro("Todos", vm.filtro == 0) { vm.filtro = 0 }
                ChipFiltro("Tranquilos", vm.filtro == 1) { vm.filtro = 1 }
                ChipFiltro("Estresantes", vm.filtro == 2) { vm.filtro = 2 }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 10.dp))

            val lista = vm.listaFiltrada
            if (vm.mediciones.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🗺️", fontSize = 42.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Aún no hay mediciones.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        "Toca 'Nueva medición' para analizar tu primer lugar.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else if (lista.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🔍", fontSize = 42.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Sin resultados con este filtro.",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Prueba otra búsqueda o cambia de categoría.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(lista, key = { it.id }) { m ->
                        ItemLugar(
                            m = m,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                seleccionada = m
                            }
                        )
                    }
                }
            }
        }
    }

    seleccionada?.let { m ->
        ModalBottomSheet(
            onDismissRequest = { seleccionada = null },
            sheetState = hojaSheet
        ) {
            DetalleMedicion(
                m = m,
                onCerrar = { seleccionada = null },
                onCambiarEstado = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.alternarCategoria(m)
                    seleccionada = null
                },
                onEliminar = {
                    pendienteBorrar = m
                    seleccionada = null
                }
            )
        }
    }

    pendienteBorrar?.let { m ->
        AlertDialog(
            onDismissRequest = { pendienteBorrar = null },
            title = { Text("¿Eliminar este lugar?") },
            text = { Text("\"${m.nombreLugar}\" (${m.decibelios} dB) se borrará del registro.") },
            confirmButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.eliminar(m)
                    pendienteBorrar = null
                }) {
                    Text("Eliminar", color = RojoSalud, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendienteBorrar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ChipFiltro(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        label = { Text(texto, fontSize = 12.sp) },
        shape = RoundedCornerShape(14.dp),
        border = if (seleccionado) {
            androidx.compose.foundation.BorderStroke(1.dp, VerdeSalud.copy(alpha = 0.4f))
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = VerdeSalud.copy(alpha = 0.14f),
            selectedLabelColor = Color(0xFF00695C),
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
private fun ItemLugar(m: Medicion, onClick: () -> Unit) {
    val colorCategoria = if (m.categoria == "Tranquilo") VerdeSalud else RojoSalud
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(colorCategoria.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(m.contextoEmoji, fontSize = 22.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = m.nombreLugar.ifBlank { "Lugar sin nombre" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = m.fechaHora,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            InsigniaCategoria(tranquilo = m.categoria == "Tranquilo")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetalleMedicion(
    m: Medicion,
    onCerrar: () -> Unit,
    onCambiarEstado: () -> Unit,
    onEliminar: () -> Unit
) {
    val nivel = SoundAnalyzer.clasificarNivel(m.decibelios)
    val colorNivel = Color(nivel.colorHex)
    val esTranquilo = m.categoria == "Tranquilo"
    val colorCategoria = if (esTranquilo) VerdeSalud else RojoSalud

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 36.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(colorCategoria.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(m.contextoEmoji, fontSize = 26.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = m.nombreLugar.ifBlank { "Lugar sin nombre" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                InsigniaCategoria(tranquilo = esTranquilo)
            }
        }

        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorNivel.copy(alpha = 0.10f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Nivel de ruido",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format(Locale.US, "%.1f", m.decibelios),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorNivel
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "dB",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorNivel.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }
                }
                Text(
                    text = nivel.etiqueta,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorNivel
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        FilaDetalle("Luz ambiental", "${m.nivelLuz.toInt()} lux", AzulInfo)
        FilaDetalle("Coordenadas GPS", String.format(Locale.US, "%.4f, %.4f", m.latitud, m.longitud), AzulInfo)
        FilaDetalle("Fecha y hora", m.fechaHora, MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onCambiarEstado,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (esTranquilo) "Marcar estresante" else "Marcar tranquilo",
                    color = if (esTranquilo) RojoSalud else VerdeSalud,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = onEliminar,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RojoSalud.copy(alpha = 0.12f),
                    contentColor = RojoSalud
                )
            ) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Eliminar", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = onCerrar,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Cerrar", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FilaDetalle(etiqueta: String, valor: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = etiqueta,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = valor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}
