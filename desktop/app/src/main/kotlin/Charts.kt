package echo.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val ChartColors = listOf(0xFFD0BCFF, 0xFF60A5FA, 0xFF4ADE80, 0xFFFBBF24, 0xFFFF6B6B, 0xFFF472B6, 0xFF9CA3AF).map { Color(it) }

/** Donut chart with a legend; slices are (name, value). */
@Composable
fun PieChart(slices: List<Pair<String, Int>>) {
    val total = slices.sumOf { it.second }.coerceAtLeast(1)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Canvas(Modifier.size(170.dp)) {
            val stroke = 34.dp.toPx()
            var start = -90f
            slices.forEachIndexed { i, (_, v) ->
                val sweep = 360f * v / total
                drawArc(
                    ChartColors[i % ChartColors.size], start, (sweep - 1.5f).coerceAtLeast(0.5f), false,
                    topLeft = Offset(stroke / 2, stroke / 2), size = Size(size.width - stroke, size.height - stroke), style = Stroke(stroke),
                )
                start += sweep
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            slices.forEachIndexed { i, (name, v) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(ChartColors[i % ChartColors.size]))
                    Text(
                        "  $name  ${100 * v / total}%", maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(220.dp),
                    )
                }
            }
        }
    }
}

/** Vertical bars. [labels] are shown under each bar (blank to skip). */
@Composable
fun BarChart(labels: List<String>, values: List<Int>, color: Color, height: Dp = 140.dp) {
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    Column {
        Row(Modifier.fillMaxWidth().height(height), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            values.forEach { v ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    if (v > 0 && values.size <= 8) Text("$v", style = MaterialTheme.typography.labelSmall)
                    Box(
                        Modifier.fillMaxWidth().height((height - 18.dp) * v / max).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(color)
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            labels.forEach { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
        }
    }
}

/** Horizontal ranking bars: (name, value). */
@Composable
fun RankBars(rows: List<Pair<String, Int>>, color: Color) {
    val max = (rows.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { (name, v) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, Modifier.width(200.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                Box(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Box(Modifier.fillMaxWidth(v.toFloat() / max).height(14.dp).clip(RoundedCornerShape(7.dp)).background(color))
                }
                Text("$v", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
