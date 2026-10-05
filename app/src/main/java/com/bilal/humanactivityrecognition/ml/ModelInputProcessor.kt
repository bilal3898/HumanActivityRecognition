package com.bilal.humanactivityrecognition.ml

import com.bilal.humanactivityrecognition.data.sensor.SensorSample
import com.bilal.humanactivityrecognition.utils.Constants
import com.bilal.humanactivityrecognition.utils.IirFilter

/**
 * Turns the 50 Hz sensor stream into model windows exactly like UCI-HAR:
 * channels = [body_acc xyz, body_gyro xyz, total_acc xyz], acceleration in g.
 * Gravity is removed with the same 0.3 Hz Butterworth low-pass used to build the dataset.
 * Standardisation happens inside the TFLite model.
 */
class ModelInputProcessor(
    private val windowSize: Int = Constants.WINDOW_SIZE,
    stepSize: Int = 64,
) {
    var stepSize: Int = stepSize
        set(value) {
            require(value in 1..windowSize)
            field = value
        }

    private val gravityFilters = Array(3) { IirFilter(Constants.GRAVITY_FILTER_B, Constants.GRAVITY_FILTER_A) }
    private val buffer = ArrayDeque<FloatArray>(windowSize + 1)
    private var samplesSinceLastWindow = 0

    val bufferedSamples: Int get() = buffer.size

    /** Returns a [1][windowSize][9] window when one is ready, otherwise null. */
    fun add(sample: SensorSample): Array<Array<FloatArray>>? {
        buffer.addLast(toChannels(sample))
        if (buffer.size > windowSize) buffer.removeFirst()
        samplesSinceLastWindow++
        if (buffer.size == windowSize && samplesSinceLastWindow >= stepSize) {
            samplesSinceLastWindow = 0
            return arrayOf(Array(windowSize) { buffer[it].copyOf() })
        }
        return null
    }

    fun toChannels(sample: SensorSample): FloatArray {
        val total = floatArrayOf(
            sample.accX / Constants.STANDARD_GRAVITY,
            sample.accY / Constants.STANDARD_GRAVITY,
            sample.accZ / Constants.STANDARD_GRAVITY,
        )
        val body = FloatArray(3) { i -> (total[i] - gravityFilters[i].filter(total[i].toDouble())).toFloat() }
        return floatArrayOf(
            body[0], body[1], body[2],
            sample.gyroX, sample.gyroY, sample.gyroZ,
            total[0], total[1], total[2],
        )
    }

    fun reset() {
        buffer.clear()
        samplesSinceLastWindow = 0
        gravityFilters.forEach { it.reset() }
    }
}
