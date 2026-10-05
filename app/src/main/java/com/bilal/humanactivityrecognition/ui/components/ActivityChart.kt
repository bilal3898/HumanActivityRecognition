package com.bilal.humanactivityrecognition.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bilal.humanactivityrecognition.data.model.ActivityClass
import com.bilal.humanactivityrecognition.domain.model.DailyActivitySummary
import com.bilal.humanactivityrecognition.ui.theme.color
import com.bilal.humanactivityrecognition.utils.DateTimeUtils

/** Horizontal bars: time spent per activity for one day. */
@Composable
fun DailyActivityChart(summary: DailyActivitySummary, modifier: Modifier = Modifier) {
    val max = summary.durations.values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ActivityClass.entries.forEach { activity ->
            val duration = summary.durationOf(activity)
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActivityIcon(activity, size = 28.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row {
                        Text(activity.displayName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(DateTimeUtils.formatDuration(duration), style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.height(4.dp))
                    ConfidenceBar(duration / max.toFloat(), activity.color, height = 8.dp)
                }
            }
        }
    }
}

/** Stacked vertical bars: one per day, segmented by activity. */
@Composable
fun WeeklyActivityChart(days: List<DailyActivitySummary>, modifier: Modifier = Modifier) {
    val maxTotal = days.maxOfOrNull { it.totalMs }?.coerceAtLeast(1L) ?: 1L
    val track = MaterialTheme.colorScheme.surfaceVariant
    Column(modifier) {
        Row(Modifier.fillMaxWidth().height(160.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            days.forEach { day ->
                Canvas(Modifier.width(26.dp).height(160.dp)) {
                    drawRoundRect(track, cornerRadius = CornerRadius(8f, 8f))
                    var y = size.height
                    ActivityClass.entries.forEach { activity ->
                        val h = size.height * day.durationOf(activity) / maxTotal
                        if (h > 0f) {
                            y -= h
                            drawRect(activity.color, topLeft = Offset(0f, y), size = Size(size.width, h))
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            days.forEach { day ->
                Text(
                    DateTimeUtils.formatShortDay(day.date),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(36.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        ChartLegend()
    }
}

@Composable
fun ChartLegend(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ActivityClass.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { activity ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(activity.color, CircleShape))
                        Spacer(Modifier.width(4.dp))
                        Text(activity.displayName, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
