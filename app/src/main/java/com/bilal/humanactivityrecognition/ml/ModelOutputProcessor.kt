package com.bilal.humanactivityrecognition.ml

import com.bilal.humanactivityrecognition.data.model.ActivityClass
import com.bilal.humanactivityrecognition.data.model.ActivityPrediction

class ModelOutputProcessor(private val labels: List<ActivityClass> = ActivityClass.entries) {

    fun process(probabilities: FloatArray, timestampMs: Long, inferenceTimeMs: Long = 0): ActivityPrediction {
        require(probabilities.size == labels.size) { "Expected ${labels.size} probabilities, got ${probabilities.size}" }
        val best = argmax(probabilities)
        val ordered = ActivityClass.entries.map { cls -> probabilities[labels.indexOf(cls)] }
        return ActivityPrediction(
            activity = labels[best],
            confidence = probabilities[best],
            probabilities = ordered,
            timestampMs = timestampMs,
            inferenceTimeMs = inferenceTimeMs,
        )
    }

    companion object {
        fun argmax(values: FloatArray): Int {
            var best = 0
            for (i in 1 until values.size) if (values[i] > values[best]) best = i
            return best
        }

        fun parseLabels(text: String): List<ActivityClass> =
            text.lines().filter { it.isNotBlank() }.map { ActivityClass.fromLabel(it) }
    }
}
