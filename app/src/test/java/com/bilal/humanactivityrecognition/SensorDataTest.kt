package com.bilal.humanactivityrecognition

import com.bilal.humanactivityrecognition.data.sensor.SensorSample
import com.bilal.humanactivityrecognition.utils.Constants
import com.bilal.humanactivityrecognition.utils.FixedRateResampler
import com.bilal.humanactivityrecognition.utils.IirFilter
import com.bilal.humanactivityrecognition.utils.RateMeter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sin

class SensorDataTest {

    @Test
    fun resamplerProducesFixed50HzGridFromIrregularEvents() {
        val resampler = FixedRateResampler()
        val out = mutableListOf<SensorSample>()
        resampler.onGyroscope(floatArrayOf(0.1f, 0.2f, 0.3f))
        // ~100 Hz events with jitter, acc_x ramps linearly with time (1 unit per second).
        var t = 0L
        repeat(300) { i ->
            t += 10_000_000L + (if (i % 2 == 0) 2_000_000L else -2_000_000L)
            resampler.onAccelerometer(t, floatArrayOf(t / 1e9f, 0f, 9.81f)) { out += it }
        }
        val dts = out.zipWithNext { a, b -> b.timestampNs - a.timestampNs }.distinct()
        assertEquals(listOf(Constants.SAMPLING_PERIOD_NS), dts)
        // Linear interpolation should reproduce the ramp exactly on the grid.
        out.forEach { assertEquals(it.timestampNs / 1e9f, it.accX, 1e-4f) }
        assertEquals(0.2f, out.last().gyroY, 0f)
        val expected = (t - out.first().timestampNs) / Constants.SAMPLING_PERIOD_NS + 1
        assertEquals(expected, out.size.toLong())
    }

    @Test
    fun resamplerRestartsAfterLongGap() {
        val resampler = FixedRateResampler()
        val out = mutableListOf<SensorSample>()
        resampler.onAccelerometer(0L, floatArrayOf(0f, 0f, 0f)) { out += it }
        resampler.onAccelerometer(5_000_000_000L, floatArrayOf(1f, 1f, 1f)) { out += it }
        assertEquals(2, out.size)
        assertEquals(5_000_000_000L, out[1].timestampNs)
    }

    @Test
    fun gravityFilterHasUnitDcGainAndStartsInSteadyState() {
        val filter = IirFilter(Constants.GRAVITY_FILTER_B, Constants.GRAVITY_FILTER_A)
        repeat(10) { assertEquals(1.0, filter.filter(1.0), 1e-6) }
    }

    @Test
    fun gravityFilterRemovesWalkingFrequencies() {
        val filter = IirFilter(Constants.GRAVITY_FILTER_B, Constants.GRAVITY_FILTER_A)
        var maxAfterSettling = 0.0
        for (n in 0 until 2000) {
            val x = 1.0 + 0.5 * sin(2 * Math.PI * 2.0 * n / 50.0) // gravity + 2 Hz step oscillation
            val y = filter.filter(x)
            if (n > 1000) maxAfterSettling = maxOf(maxAfterSettling, abs(y - 1.0))
        }
        assertTrue("2 Hz ripple should be strongly attenuated, was $maxAfterSettling", maxAfterSettling < 0.01)
    }

    @Test
    fun rateMeterEstimatesFrequency() {
        val meter = RateMeter()
        var rate = 0f
        for (i in 0..200) rate = meter.tick(i * 20_000_000L)
        assertEquals(50f, rate, 0.5f)
    }
}
