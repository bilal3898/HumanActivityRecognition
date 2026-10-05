package com.bilal.humanactivityrecognition.ui.screens.recognition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bilal.humanactivityrecognition.data.model.ActivityClass
import com.bilal.humanactivityrecognition.ui.AppViewModelProvider
import com.bilal.humanactivityrecognition.ui.components.ActivityIcon
import com.bilal.humanactivityrecognition.ui.components.ConfidenceBar
import com.bilal.humanactivityrecognition.ui.components.PredictionCard
import com.bilal.humanactivityrecognition.ui.components.SensorStatusCard
import com.bilal.humanactivityrecognition.ui.screens.ScreenColumn
import com.bilal.humanactivityrecognition.ui.theme.color
import com.bilal.humanactivityrecognition.utils.DateTimeUtils
import kotlin.math.roundToInt

@Composable
fun RecognitionScreen(
    onStartRecognition: () -> Unit,
    viewModel: RecognitionViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val live by viewModel.live.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    val elapsed = live.startedAtMs?.let { now - it } ?: 0L

    ScreenColumn(title = "Live Recognition") {
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Recognition timer", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(DateTimeUtils.formatTimer(elapsed), style = MaterialTheme.typography.headlineMedium)
                    Text("${live.totalPredictions} predictions", style = MaterialTheme.typography.labelMedium)
                }
                if (live.isRunning) {
                    Button(onClick = viewModel::stop, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text("Stop")
                    }
                } else {
                    Button(onClick = onStartRecognition) { Text("Start") }
                }
            }
        }

        PredictionCard(live.current, live.isRunning)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Class probabilities", style = MaterialTheme.typography.titleMedium)
                ActivityClass.entries.forEachIndexed { i, activity ->
                    ConfidenceBar(
                        value = live.current?.probabilities?.getOrNull(i) ?: 0f,
                        color = activity.color,
                        label = activity.displayName,
                        height = 8.dp,
                    )
                }
                live.current?.let {
                    Text(
                        "Inference time: ${it.inferenceTimeMs} ms",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        SensorStatusCard(live.sensorStatus, live.isRunning, live.inputSource, live.bufferedSamples)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Prediction history", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                if (live.recent.isEmpty()) {
                    Text("No predictions yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                live.recent.forEachIndexed { index, p ->
                    if (index > 0) HorizontalDivider()
                    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        ActivityIcon(p.activity, size = 30.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(p.activity.displayName, Modifier.weight(1f))
                        Text("${(p.confidence * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            DateTimeUtils.formatTime(p.timestampMs),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
