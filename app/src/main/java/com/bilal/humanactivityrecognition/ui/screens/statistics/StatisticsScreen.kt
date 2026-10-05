package com.bilal.humanactivityrecognition.ui.screens.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bilal.humanactivityrecognition.data.model.ActivityClass
import com.bilal.humanactivityrecognition.domain.model.DailyActivitySummary
import com.bilal.humanactivityrecognition.ui.AppViewModelProvider
import com.bilal.humanactivityrecognition.ui.components.DailyActivityChart
import com.bilal.humanactivityrecognition.ui.components.WeeklyActivityChart
import com.bilal.humanactivityrecognition.ui.screens.ScreenColumn
import com.bilal.humanactivityrecognition.utils.DateTimeUtils
import java.time.LocalDate

@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val week by viewModel.week.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val today = week.lastOrNull() ?: DailyActivitySummary(LocalDate.now(), emptyMap())
    val weekTotals = DailyActivitySummary(
        LocalDate.now(),
        ActivityClass.entries.associateWith { cls -> week.sumOf { it.durationOf(cls) } },
    )

    ScreenColumn(title = "Statistics", subtitle = "Time spent in each activity") {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("Today", "Last 7 days").forEachIndexed { i, label ->
                SegmentedButton(
                    selected = tab == i,
                    onClick = { tab = i },
                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                ) { Text(label) }
            }
        }

        val summary = if (tab == 0) today else weekTotals
        SummaryTiles(summary)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (tab == 0) "Daily breakdown" else "Weekly totals", style = MaterialTheme.typography.titleMedium)
                DailyActivityChart(summary)
            }
        }

        if (tab == 1 && week.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Daily activity (stacked)", style = MaterialTheme.typography.titleMedium)
                    WeeklyActivityChart(week)
                }
            }
        }
    }
}

@Composable
private fun SummaryTiles(summary: DailyActivitySummary) {
    val walking = summary.durationOf(ActivityClass.WALKING)
    val stairs = summary.durationOf(ActivityClass.WALKING_UPSTAIRS) + summary.durationOf(ActivityClass.WALKING_DOWNSTAIRS)
    val stationary = summary.durationOf(ActivityClass.SITTING) + summary.durationOf(ActivityClass.STANDING)
    val laying = summary.durationOf(ActivityClass.LAYING)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile("Walking", walking, Modifier.weight(1f))
            Tile("Stairs (up/down)", stairs, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile("Sitting + standing", stationary, Modifier.weight(1f))
            Tile("Laying", laying, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Tile(label: String, durationMs: Long, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(DateTimeUtils.formatDuration(durationMs), style = MaterialTheme.typography.titleLarge)
        }
    }
}
