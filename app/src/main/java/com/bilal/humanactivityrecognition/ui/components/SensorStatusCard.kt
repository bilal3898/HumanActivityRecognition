package com.bilal.humanactivityrecognition.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bilal.humanactivityrecognition.data.sensor.SensorStatus
import com.bilal.humanactivityrecognition.domain.model.InputSource
import com.bilal.humanactivityrecognition.ui.theme.Amber500
import com.bilal.humanactivityrecognition.ui.theme.Emerald500
import com.bilal.humanactivityrecognition.ui.theme.Red500
import com.bilal.humanactivityrecognition.utils.Constants
import java.util.Locale

@Composable
fun SensorStatusCard(
    status: SensorStatus,
    isRunning: Boolean,
    inputSource: InputSource,
    bufferedSamples: Int,
    modifier: Modifier = Modifier,
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Sensor status", style = MaterialTheme.typography.titleMedium)
            StatusRow("Accelerometer", if (status.accelerometerAvailable) "Available" else "Not found",
                if (status.accelerometerAvailable) Emerald500 else Red500)
            StatusRow("Gyroscope", if (status.gyroscopeAvailable) "Available" else "Not found (zeros used)",
                if (status.gyroscopeAvailable) Emerald500 else Amber500)
            StatusRow("Input source", inputSource.displayName, MaterialTheme.colorScheme.primary)
            val rateText = when {
                !isRunning -> "Idle"
                inputSource == InputSource.DEMO_REPLAY -> "Replaying UCI windows"
                else -> String.format(Locale.US, "%.1f Hz (target %d Hz)", status.samplingRateHz, Constants.SAMPLING_RATE_HZ)
            }
            StatusRow("Sampling", rateText, if (isRunning) Emerald500 else MaterialTheme.colorScheme.onSurfaceVariant)
            if (isRunning && bufferedSamples < Constants.WINDOW_SIZE) {
                ConfidenceBar(
                    value = bufferedSamples / Constants.WINDOW_SIZE.toFloat(),
                    color = MaterialTheme.colorScheme.primary,
                    label = "Filling first 2.56 s window ($bufferedSamples/${Constants.WINDOW_SIZE})",
                    height = 6.dp,
                )
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
