package com.bilal.humanactivityrecognition.domain.usecase

import com.bilal.humanactivityrecognition.data.model.ActivityClass
import com.bilal.humanactivityrecognition.data.model.ActivityPrediction

enum class ConfidenceLevel(val displayName: String) { HIGH("High"), MEDIUM("Medium"), LOW("Low") }

/**
 * Temporal smoothing: averages class probabilities over the last [windowSize] predictions,
 * which suppresses single-window flickers (e.g. sitting ↔ standing).
 */
class CalculateConfidenceUseCase(windowSize: Int = 3) {
    var windowSize: Int = windowSize
        set(value) {
            field = value.coerceAtLeast(1)
            while (history.size > field) history.removeFirst()
        }

    private val history = ArrayDeque<List<Float>>()

    operator fun invoke(raw: ActivityPrediction): ActivityPrediction {
        history.addLast(raw.probabilities)
        while (history.size > windowSize) history.removeFirst()
        val averaged = List(raw.probabilities.size) { i -> history.sumOf { it[i].toDouble() }.toFloat() / history.size }
        val best = averaged.indices.maxBy { averaged[it] }
        return raw.copy(activity = ActivityClass.fromIndex(best), confidence = averaged[best], probabilities = averaged)
    }

    fun reset() = history.clear()

    companion object {
        fun level(confidence: Float): ConfidenceLevel = when {
            confidence >= 0.8f -> ConfidenceLevel.HIGH
            confidence >= 0.5f -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }
    }
}
