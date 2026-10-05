package com.bilal.humanactivityrecognition.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bilal.humanactivityrecognition.ui.AppViewModelProvider
import com.bilal.humanactivityrecognition.ui.components.DailyActivityChart
import com.bilal.humanactivityrecognition.ui.components.PredictionCard
import com.bilal.humanactivityrecognition.ui.components.SensorStatusCard
import com.bilal.humanactivityrecognition.utils.DateTimeUtils

@Composable
fun HomeScreen(
    onStartRecognition: () -> Unit,
    onOpenLive: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val live by viewModel.live.collectAsStateWithLifecycle()
    val today by viewModel.today.collectAsStateWithLifecycle()
    val source by viewModel.inputSource.collectAsStateWithLifecycle()

    com.bilal.humanactivityrecognition.ui.screens.ScreenColumn(
        title = "Activity Recognition",
        subtitle = "CNN-LSTM on smartphone accelerometer + gyroscope",
    ) {
        PredictionCard(live.current, live.isRunning)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (live.isRunning) {
                Button(
                    onClick = viewModel::stop,
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Filled.Stop, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Stop recognition")
                }
            } else {
                Button(onClick = onStartRecognition, modifier = Modifier.weight(1f).height(52.dp)) {
                    Icon(Icons.Filled.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start recognition")
                }
            }
            OutlinedButton(onClick = onOpenLive, modifier = Modifier.height(52.dp)) { Text("Live view") }
        }

        live.error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }

        SensorStatusCard(live.sensorStatus, live.isRunning, if (live.isRunning) live.inputSource else source, live.bufferedSamples)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row {
                    Text("Today's summary", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(
                        "Total ${DateTimeUtils.formatDuration(today.totalMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DailyActivityChart(today)
            }
        }
    }
}
