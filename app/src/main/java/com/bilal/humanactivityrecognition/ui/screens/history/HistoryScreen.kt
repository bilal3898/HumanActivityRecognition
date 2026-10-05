package com.bilal.humanactivityrecognition.ui.screens.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bilal.humanactivityrecognition.data.model.ActivityClass
import com.bilal.humanactivityrecognition.ui.AppViewModelProvider
import com.bilal.humanactivityrecognition.ui.components.ActivityCard
import com.bilal.humanactivityrecognition.ui.screens.ScreenColumn
import com.bilal.humanactivityrecognition.utils.DateTimeUtils

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val grouped by viewModel.groupedSegments.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }

    ScreenColumn(title = "History", subtitle = "Detected activities with start/end time and duration") {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filter == null, onClick = { viewModel.setFilter(null) }, label = { Text("All") })
            ActivityClass.entries.forEach { activity ->
                FilterChip(
                    selected = filter == activity,
                    onClick = { viewModel.setFilter(activity) },
                    label = { Text(activity.displayName) },
                )
            }
        }

        if (grouped.isEmpty()) {
            Text("No activities recorded yet. Start recognition from the Home tab.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            grouped.forEach { (date, segments) ->
                Text(
                    "${DateTimeUtils.formatDay(date)} · ${DateTimeUtils.formatDuration(segments.sumOf { it.durationMs })}",
                    style = MaterialTheme.typography.titleMedium,
                )
                segments.forEach { ActivityCard(it) }
            }
            TextButton(onClick = { confirmClear = true }) { Text("Clear history", color = MaterialTheme.colorScheme.error) }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history?") },
            text = { Text("All recorded activity segments will be deleted.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearHistory(); confirmClear = false }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}
