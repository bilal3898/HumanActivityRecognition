package com.bilal.humanactivityrecognition.ml

import com.bilal.humanactivityrecognition.data.model.ActivityPrediction
import com.bilal.humanactivityrecognition.utils.Constants

/** Runs the CNN-LSTM on a single preprocessed window. */
class ActivityClassifier(
    private val engine: InferenceEngine,
    private val outputProcessor: ModelOutputProcessor = ModelOutputProcessor(),
    private val clock: () -> Long = System::currentTimeMillis,
) : AutoCloseable {

    fun classify(window: Array<Array<FloatArray>>): ActivityPrediction {
        require(window.size == 1 && window[0].size == Constants.WINDOW_SIZE && window[0][0].size == Constants.NUM_CHANNELS) {
            "Expected input shape [1][${Constants.WINDOW_SIZE}][${Constants.NUM_CHANNELS}]"
        }
        val start = System.nanoTime()
        val probabilities = engine.run(window)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        return outputProcessor.process(probabilities, clock(), elapsedMs)
    }

    override fun close() = engine.close()
}
