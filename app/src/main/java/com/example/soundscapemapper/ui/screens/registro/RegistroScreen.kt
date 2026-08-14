package com.example.soundscapemapper.ui.screens.registro

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.soundscapemapper.ui.components.AzulInfo
import com.example.soundscapemapper.ui.components.RojoSalud
import com.example.soundscapemapper.ui.components.VerdeSalud
import com.example.soundscapemapper.ui.viewmodel.RegistroViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroScreen(db: AppDatabase, onNuevaMedicion: () -> Unit) {
    val vm: RegistroViewModel = viewModel(factory = RegistroViewModel.factory(db))
    LaunchedEffect(Unit) { vm.cargar() }

    var pendienteBorrar by remember { mutableStateOf<Medicion?>(null) }
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registro de mediciones") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            androidx.compose.material3.ExtendedFloatingActionButton(
                onClick = onNuevaMedicion,
                containerColor = VerdeSalud,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Nueva medición", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Contador("Total", "${vm.mediciones.size}", MaterialTheme.colorScheme.onSurfaceVariant)
                Contador("Tranquilos", "${vm.mediciones.count { it.categoria == "Tranquilo" }}", VerdeSalud)
                Contador("Estresantes", "${vm.mediciones.count { it.categoria == "Estresante" }}", RojoSalud)
            }

            OutlinedTextField(
                value = vm.busqueda,
                onValueChange = { vm.busqueda = it },
                placeholder = { Text("Buscar por nombre…") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChipFiltro("Todos", vm.filtro == 0) { vm.filtro = 0 }
                ChipFiltro("🌿 Tranquilos", vm.filtro == 1) { vm.filtro = 1 }
                ChipFiltro("⚠️ Estresantes", vm.filtro == 2) { vm.filtro = 2 }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

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
                        ItemMedicion(
                            m = m,
                            onAlternarCategoria = { vm.alternarCategoria(m) },
                            onEliminar = { pendienteBorrar = m }
                        )
                    }
                }
            }
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
private fun RowScope.Contador(titulo: String, valor: String, color: Color) {
    Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(titulo, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(valor, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun ChipFiltro(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        label = { Text(texto, fontSize = 12.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = VerdeSalud,
            selectedLabelColor = Color.White,
            selectedLeadingIconColor = Color.White
        )
    )
}

@Composable
private fun ItemMedicion(
    m: Medicion,
    onAlternarCategoria: () -> Unit,
    onEliminar: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onAlternarCategoria()
        },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (m.categoria == "Tranquilo") "🌿" else "⚠️",
                    fontSize = 22.sp
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(m.nombreLugar, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        if (m.categoria == "Tranquilo") "Tranquilo · toca para marcar estresante"
                        else "Estresante · toca para marcar tranquilo",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEliminar) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DatoChip("${m.decibelios} dB", if (m.decibelios >= 80) RojoSalud else AzulInfo)
                DatoChip("${m.nivelLuz.toInt()} lux", AzulInfo)
                DatoChip(
                    String.format(java.util.Locale.US, "%.3f, %.3f", m.latitud, m.longitud),
                    AzulInfo
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(m.fechaHora, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DatoChip(texto: String, color: Color) {
    androidx.compose.material3.Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            texto,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
