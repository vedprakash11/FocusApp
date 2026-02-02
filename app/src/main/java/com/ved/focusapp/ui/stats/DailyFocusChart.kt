package com.ved.focusapp.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Simple bar chart for daily focus minutes (last 7 days).
 * X-axis: date labels; Y-axis: minutes. Today's bar is highlighted.
 * No external chart lib; uses Canvas only.
 */
data class DayData(val label: String, val minutes: Int, val isToday: Boolean)

@Composable
fun DailyFocusChart(
    dayData: List<DayData>,
    modifier: Modifier = Modifier
) {
    if (dayData.isEmpty()) return

    val barColor = MaterialTheme.colorScheme.primary
    val todayColor = MaterialTheme.colorScheme.tertiary
    val maxMinutes = dayData.maxOf { it.minutes }.coerceAtLeast(1)
    val chartHeight = 140.dp

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .padding(horizontal = 4.dp)
        ) {
            val barCount = dayData.size
            val totalWidth = size.width
            val paddingH = 4.dp.toPx()
            val barGap = 4.dp.toPx()
            val barTotalWidth = totalWidth - 2 * paddingH
            val barWidth = (barTotalWidth - (barCount - 1) * barGap) / barCount
            val chartH = size.height

            dayData.forEachIndexed { index, day ->
                val left = paddingH + index * (barWidth + barGap)
                val barHeightRatio = day.minutes.toFloat() / maxMinutes
                val barH = (chartH * barHeightRatio).coerceAtLeast(2f)
                val top = chartH - barH
                val color = if (day.isToday) todayColor else barColor
                drawRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barH)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            dayData.forEach { day ->
                Text(
                    text = day.label,
                    style = MaterialTheme.typography.labelSmall.copy(textAlign = TextAlign.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }
        }
    }
}
