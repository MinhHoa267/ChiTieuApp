package com.example.chitieuapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.chitieuapp.ui.stats.CategorySlice

/** Biểu đồ tròn dạng donut, tự vẽ bằng Canvas. */
@Composable
fun DonutChart(slices: List<CategorySlice>, modifier: Modifier = Modifier) {
    val total = slices.sumOf { it.amount }.toFloat()
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Canvas(
        modifier = modifier
            .size(180.dp)
            .semantics { contentDescription = "Biểu đồ tròn phân bổ theo danh mục" }
    ) {
        val strokeWidth = size.minDimension * 0.24f
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

        if (total <= 0f) {
            drawArc(
                color = trackColor, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(strokeWidth),
            )
            return@Canvas
        }
        var start = -90f
        slices.forEach { slice ->
            val sweep = slice.amount / total * 360f
            drawArc(
                color = Color(slice.color),
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokeWidth),
            )
            start += sweep
        }
    }
}

/** Biểu đồ cột theo ngày trong tháng. */
@Composable
fun DailyBarChart(values: List<Long>, barColor: Color, modifier: Modifier = Modifier) {
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L).toFloat()
    val baseline = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .semantics { contentDescription = "Biểu đồ cột theo ngày trong tháng" }
    ) {
        if (values.isEmpty()) return@Canvas
        val slot = size.width / values.size
        val barWidth = slot * 0.6f
        values.forEachIndexed { index, value ->
            val barHeight = size.height * (value / max)
            if (value > 0) {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(index * slot + (slot - barWidth) / 2, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 4),
                )
            }
        }
        drawLine(
            color = baseline,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 2f,
        )
    }
}

/** Nhãn trục ngày: 1, giữa tháng, cuối tháng. */
@Composable
fun DayAxisLabels(daysInMonth: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
    ) {
        Text("1", style = MaterialTheme.typography.labelSmall)
        Text("${daysInMonth / 2}", style = MaterialTheme.typography.labelSmall)
        Text("$daysInMonth", style = MaterialTheme.typography.labelSmall)
    }
}
