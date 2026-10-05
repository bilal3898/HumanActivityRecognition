package com.bilal.humanactivityrecognition

import com.bilal.humanactivityrecognition.data.model.ActivityClass
import com.bilal.humanactivityrecognition.domain.usecase.CalculateConfidenceUseCase
import com.bilal.humanactivityrecognition.domain.usecase.ConfidenceLevel
import com.bilal.humanactivityrecognition.ml.ActivityClassifier
import com.bilal.humanactivityrecognition.ml.InferenceEngine
import com.bilal.humanactivityrecognition.ml.ModelOutputProcessor
import com.bilal.humanactivityrecognition.utils.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityClassifierTest {
    private class FakeEngine(var output: FloatArray) : InferenceEngine {
        var calls = 0
        override fun run(input: Array<Array<FloatArray>>): FloatArray { calls++; return output }
        override fun close() = Unit
    }

    private fun window() = arrayOf(Array(Constants.WINDOW_SIZE) { FloatArray(Constants.NUM_CHANNELS) })

    @Test
    fun classifierReturnsArgmaxActivity() {
        val engine = FakeEngine(floatArrayOf(0.05f, 0.05f, 0.1f, 0.7f, 0.05f, 0.05f))
        val prediction = ActivityClassifier(engine, clock = { 42L }).classify(window())
        assertEquals(ActivityClass.SITTING, prediction.activity)
        assertEquals(0.7f, prediction.confidence, 1e-6f)
        assertEquals(42L, prediction.timestampMs)
        assertEquals(1, engine.calls)
    }

    @Test(expected = IllegalArgumentException::class)
    fun classifierRejectsWrongShape() {
        ActivityClassifier(FakeEngine(FloatArray(6))).classify(arrayOf(Array(10) { FloatArray(9) }))
    }

    @Test
    fun labelsFileOrderIsRespected() {
        val labels = ModelOutputProcessor.parseLabels("WALKING\nWALKING_UPSTAIRS\nWALKING_DOWNSTAIRS\nSITTING\nSTANDING\nLAYING\n")
        assertEquals(ActivityClass.entries, labels)
        val p = ModelOutputProcessor(labels).process(floatArrayOf(0f, 0f, 0f, 0f, 0f, 1f), 0)
        assertEquals(ActivityClass.LAYING, p.activity)
    }

    @Test
    fun smoothingSuppressesSingleWindowFlicker() {
        val engine = FakeEngine(floatArrayOf(0f, 0f, 0f, 0.1f, 0.9f, 0f))
        val classifier = ActivityClassifier(engine)
        val smoothing = CalculateConfidenceUseCase(windowSize = 3)
        smoothing(classifier.classify(window()))
        smoothing(classifier.classify(window()))
        engine.output = floatArrayOf(0f, 0f, 0f, 0.8f, 0.2f, 0f)
        val smoothed = smoothing(classifier.classify(window()))
        assertEquals(ActivityClass.STANDING, smoothed.activity)
        assertTrue(smoothed.confidence < 0.9f)
    }

    @Test
    fun confidenceLevels() {
        assertEquals(ConfidenceLevel.HIGH, CalculateConfidenceUseCase.level(0.95f))
        assertEquals(ConfidenceLevel.MEDIUM, CalculateConfidenceUseCase.level(0.6f))
        assertEquals(ConfidenceLevel.LOW, CalculateConfidenceUseCase.level(0.3f))
    }
}
