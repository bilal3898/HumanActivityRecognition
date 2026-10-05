package com.bilal.humanactivityrecognition.utils

import com.bilal.humanactivityrecognition.data.sensor.SensorSample

/** Streaming IIR filter in Direct Form II Transposed (same maths as scipy.signal.lfilter). */
class IirFilter(private val b: DoubleArray, private val a: DoubleArray) {
    init {
        require(b.size == a.size && a[0] == 1.0) { "Expected normalised coefficients of equal length" }
    }

    private val state = DoubleArray(b.size - 1)
    private var initialised = false

    fun filter(x: Double): Double {
        if (!initialised) {
            primeSteadyState(x)
            initialised = true
        }
        val y = b[0] * x + state[0]
        for (i in 0 until state.size - 1) {
            state[i] = b[i + 1] * x - a[i + 1] * y + state[i + 1]
        }
        val last = state.size - 1
        state[last] = b[last + 1] * x - a[last + 1] * y
        return y
    }

    /** Equivalent to scipy's lfilter_zi * x0: behave as if x0 had been the input forever. */
    private fun primeSteadyState(x0: Double) {
        var acc = 0.0
        for (i in state.indices.reversed()) {
            acc += (b[i + 1] - a[i + 1]) * x0
            state[i] = acc
        }
    }

    fun reset() {
        state.fill(0.0)
        initialised = false
    }
}

/**
 * Converts irregular Android sensor events into a fixed 50 Hz stream.
 * Accelerometer values are linearly interpolated onto the grid; the latest gyroscope reading is held.
 */
class FixedRateResampler(
    private val periodNs: Long = Constants.SAMPLING_PERIOD_NS,
    private val maxGapNs: Long = Constants.MAX_SENSOR_GAP_NS,
) {
    private var lastAccTime = -1L
    private val lastAcc = FloatArray(3)
    private val latestGyro = FloatArray(3)
    private var nextGridTime = -1L

    fun onGyroscope(values: FloatArray) {
        values.copyInto(latestGyro, endIndex = 3)
    }

    fun onAccelerometer(timestampNs: Long, values: FloatArray, emit: (SensorSample) -> Unit) {
        if (lastAccTime < 0 || timestampNs - lastAccTime > maxGapNs) {
            lastAccTime = timestampNs
            values.copyInto(lastAcc, endIndex = 3)
            emit(sample(timestampNs, lastAcc))
            nextGridTime = timestampNs + periodNs
            return
        }
        if (timestampNs <= lastAccTime) return

        val span = (timestampNs - lastAccTime).toFloat()
        val interpolated = FloatArray(3)
        while (nextGridTime <= timestampNs) {
            val t = (nextGridTime - lastAccTime) / span
            for (i in 0..2) interpolated[i] = lastAcc[i] + (values[i] - lastAcc[i]) * t
            emit(sample(nextGridTime, interpolated))
            nextGridTime += periodNs
        }
        lastAccTime = timestampNs
        values.copyInto(lastAcc, endIndex = 3)
    }

    private fun sample(t: Long, acc: FloatArray) = SensorSample(
        timestampNs = t,
        accX = acc[0], accY = acc[1], accZ = acc[2],
        gyroX = latestGyro[0], gyroY = latestGyro[1], gyroZ = latestGyro[2],
    )

    fun reset() {
        lastAccTime = -1L
        nextGridTime = -1L
        latestGyro.fill(0f)
    }
}

/** Measures the effective rate of a stream over a sliding one-second window. */
class RateMeter(private val windowNs: Long = 1_000_000_000L) {
    private val times = ArrayDeque<Long>()

    fun tick(timestampNs: Long): Float {
        times.addLast(timestampNs)
        while (times.isNotEmpty() && timestampNs - times.first() > windowNs) times.removeFirst()
        return if (times.size < 2) 0f else (times.size - 1) * 1e9f / (times.last() - times.first()).coerceAtLeast(1)
    }

    fun reset() = times.clear()
}
