package com.bilal.humanactivityrecognition.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bilal.humanactivityrecognition.domain.model.ActivitySegment
import com.bilal.humanactivityrecognition.utils.DateTimeUtils
import kotlin.math.roundToInt

/** A history row: activity, start–end time, duration and confidence. */
@Composable
fun ActivityCard(segment: ActivitySegment, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ActivityIcon(segment.activity)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(segment.activity.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${DateTimeUtils.formatTime(segment.startTimeMs)} – ${DateTimeUtils.formatTime(segment.endTimeMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(DateTimeUtils.formatDuration(segment.durationMs), style = MaterialTheme.typography.titleMedium)
                Text(
                    "${(segment.averageConfidence * 100).roundToInt()}% avg",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
