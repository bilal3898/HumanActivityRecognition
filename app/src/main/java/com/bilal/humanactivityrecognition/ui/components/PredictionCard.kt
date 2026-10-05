package com.bilal.humanactivityrecognition.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bilal.humanactivityrecognition.data.model.ActivityPrediction
import com.bilal.humanactivityrecognition.domain.usecase.CalculateConfidenceUseCase
import com.bilal.humanactivityrecognition.ui.theme.color
import kotlin.math.roundToInt

/** Large card showing the current activity and its confidence. */
@Composable
fun PredictionCard(prediction: ActivityPrediction?, isRunning: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = prediction?.activity?.color?.copy(alpha = 0.10f) ?: MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Current activity", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (prediction == null) {
                Text(
                    if (isRunning) "Analysing movement…" else "Press Start to begin recognition",
                    style = MaterialTheme.typography.titleLarge,
                )
            } else {
                AnimatedContent(prediction.activity, label = "activity") { activity ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ActivityIcon(activity, size = 64.dp)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(activity.displayName, style = MaterialTheme.typography.headlineMedium)
                            val level = CalculateConfidenceUseCase.level(prediction.confidence)
                            Text(
                                "${(prediction.confidence * 100).roundToInt()}% · ${level.displayName} confidence",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                ConfidenceBar(prediction.confidence, prediction.activity.color)
                prediction.groundTruth?.let { truth ->
                    val correct = truth == prediction.activity
                    Text(
                        "Ground truth: ${truth.displayName} ${if (correct) "✓" else "✗"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (correct) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
