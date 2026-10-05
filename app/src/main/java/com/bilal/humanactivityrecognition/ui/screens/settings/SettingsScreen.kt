package com.bilal.humanactivityrecognition.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bilal.humanactivityrecognition.BuildConfig
import com.bilal.humanactivityrecognition.domain.model.InputSource
import com.bilal.humanactivityrecognition.domain.model.RecognitionFrequency
import com.bilal.humanactivityrecognition.ui.AppViewModelProvider
import com.bilal.humanactivityrecognition.ui.screens.ScreenColumn

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val sensors by viewModel.sensorStatus.collectAsStateWithLifecycle()

    ScreenColumn(title = "Settings") {
        Section("Recognition frequency") {
            RecognitionFrequency.entries.forEach { option ->
                RadioRow(option.displayName, null, settings.frequency == option) { viewModel.setFrequency(option) }
            }
        }

        Section("Sensor settings") {
            InputSource.entries.forEach { option ->
                RadioRow(option.displayName, option.description, settings.inputSource == option) { viewModel.setInputSource(option) }
            }
            Text("Prediction smoothing (windows averaged)", style = MaterialTheme.typography.bodyMedium)
            val options = listOf(1, 3, 5)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { i, n ->
                    SegmentedButton(
                        selected = settings.smoothingWindow == n,
                        onClick = { viewModel.setSmoothing(n) },
                        shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    ) { Text(if (n == 1) "Off" else "$n") }
                }
            }
            InfoRow("Accelerometer", sensors.accelerometerName ?: "Not available")
            InfoRow("Gyroscope", sensors.gyroscopeName ?: "Not available")
            InfoRow("Target sampling rate", "50 Hz")
        }

        Section("Notifications") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Show current activity", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Updates the ongoing notification while recognition runs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = settings.notificationsEnabled, onCheckedChange = viewModel::setNotifications)
            }
        }

        Section("Model information") {
            val info = viewModel.modelInfo
            InfoRow("Architecture", "Conv1D ×3 → LSTM(64) → Dense")
            InfoRow("Input", "128 × 9 (2.56 s @ 50 Hz)")
            info["parameters"]?.let { InfoRow("Parameters", it) }
            info["size_kb"]?.let { InfoRow("TFLite size", "$it KB") }
            info["test_accuracy"]?.toDoubleOrNull()?.let { InfoRow("UCI-HAR test accuracy", "%.2f%%".format(it * 100)) }
            info["macro_f1"]?.toDoubleOrNull()?.let { InfoRow("Macro F1", "%.3f".format(it)) }
            info["version"]?.let { InfoRow("Model version", it) }
        }

        Section("About project") {
            Text(
                "Human Activity Recognition using a hybrid CNN-LSTM trained on the UCI-HAR dataset " +
                    "(30 volunteers, 6 activities). The CNN learns local motion patterns from accelerometer and " +
                    "gyroscope signals while the LSTM captures their temporal order. Inference runs fully on-device " +
                    "with TensorFlow Lite.",
                style = MaterialTheme.typography.bodyMedium,
            )
            InfoRow("App version", BuildConfig.VERSION_NAME)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun RadioRow(title: String, subtitle: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = selected, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
