package com.example.soundscapemapper.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val VerdeSalud = Color(0xFF4DB6AC)
val AmbarSalud = Color(0xFFF0B27A)
val RojoSalud = Color(0xFFE07A5F)
val AzulInfo = Color(0xFF5B8DB8)

@Composable
fun RowScope.TarjetaMetrica(
    titulo: String,
    valor: String,
    unidad: String,
    color: Color
) {
    Card(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(titulo, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(valor, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
                if (unidad.isNotEmpty()) {
                    Spacer(Modifier.width(3.dp))
                    Text(unidad, fontSize = 11.sp, color = color.copy(alpha = 0.8f), modifier = Modifier.padding(bottom = 3.dp))
                }
            }
        }
    }
}

@Composable
fun TarjetaContenedor(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        content()
    }
}

@Composable
fun AnilloProgreso(
    progreso: Float,
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    strokeWidth: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val arcSize = Size(size.toPx() - stroke, size.toPx() - stroke)
            drawArc(
                color = color.copy(alpha = 0.18f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = Offset(stroke / 2, stroke / 2),
                size = arcSize
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progreso.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = Offset(stroke / 2, stroke / 2),
                size = arcSize
            )
        }
        content()
    }
}

@Composable
fun BarraSemana(
    puntos: List<Float>,
    etiquetas: List<String>,
    indiceHoy: Int? = null
) {
    val max = puntos.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        puntos.forEachIndexed { i, valor ->
            val fraccion = (valor / max).toFloat().coerceIn(0.05f, 1f)
            val esHoy = i == indiceHoy
            val color = when {
                valor >= 480 -> RojoSalud
                valor >= 240 -> AmbarSalud
                valor > 0 -> VerdeSalud
                else -> Color(0xFFD8E0D4)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(60.dp)
                        .background(Color(0xFFE7ECE4), RoundedCornerShape(6.dp))
                        .padding(1.dp)
                        .then(
                            if (esHoy) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                            else Modifier
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraccion)
                            .background(color, RoundedCornerShape(6.dp))
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    etiquetas.getOrElse(i) { "" },
                    fontSize = 9.sp,
                    fontWeight = if (esHoy) FontWeight.Bold else FontWeight.Normal,
                    color = if (esHoy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun InsigniaCategoria(tranquilo: Boolean) {
    val color = if (tranquilo) VerdeSalud else RojoSalud
    val texto = if (tranquilo) "Tranquilo" else "Estresante"
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(4.dp))
        Text(texto, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun MedidorGauge(valor: Double, color: Color) {
    val limite = 120.0
    val progreso = (valor / limite).toFloat().coerceIn(0f, 1f)
    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(28.dp)
    ) {
        val stroke = 12.dp.toPx()
        val y = size.height / 2
        drawLine(
            color = Color(0xFFE0E0E0),
            start = Offset(stroke / 2, y),
            end = Offset(size.width - stroke / 2, y),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(stroke / 2, y),
            end = Offset(stroke / 2 + (size.width - stroke) * progreso, y),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun MedidorArco(valor: Double, color: Color, modifier: Modifier = Modifier) {
    val progreso = (valor / 120.0).toFloat().coerceIn(0f, 1f)
    val stroke = 16.dp
    Box(modifier = modifier.size(190.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radio = size.width / 2 - stroke.toPx() / 2
            val centro = Offset(size.width / 2, size.height / 2)
            val topLeft = Offset(centro.x - radio, centro.y - radio)
            val sizeArco = Size(radio * 2, radio * 2)
            drawArc(
                color = Color(0xFFE7ECE4),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = stroke.toPx(), cap = StrokeCap.Round),
                topLeft = topLeft,
                size = sizeArco
            )
            drawArc(
                color = color,
                startAngle = 135f,
                sweepAngle = 270f * progreso,
                useCenter = false,
                style = Stroke(width = stroke.toPx(), cap = StrokeCap.Round),
                topLeft = topLeft,
                size = sizeArco
            )
        }
    }
}
