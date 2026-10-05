package com.bilal.humanactivityrecognition.domain.usecase

import com.bilal.humanactivityrecognition.data.model.ActivityPrediction
import com.bilal.humanactivityrecognition.ml.ActivityClassifier

class PredictActivityUseCase(
    private val classifier: ActivityClassifier,
    private val confidence: CalculateConfidenceUseCase = CalculateConfidenceUseCase(),
) {
    fun setSmoothingWindow(size: Int) {
        confidence.windowSize = size
    }

    operator fun invoke(window: SensorWindow): ActivityPrediction {
        val raw = classifier.classify(window.data).copy(groundTruth = window.groundTruth)
        return confidence(raw)
    }

    fun reset() = confidence.reset()
}
