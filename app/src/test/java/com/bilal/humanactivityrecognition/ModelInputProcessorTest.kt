package com.bilal.humanactivityrecognition

import com.bilal.humanactivityrecognition.data.sensor.SensorSample
import com.bilal.humanactivityrecognition.ml.ModelInputProcessor
import com.bilal.humanactivityrecognition.utils.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ModelInputProcessorTest {
    private fun sample(i: Int, accZ: Float = Constants.STANDARD_GRAVITY) =
        SensorSample(i * Constants.SAMPLING_PERIOD_NS, 0f, 0f, accZ, 0.1f, 0.2f, 0.3f)

    @Test
    fun emitsFirstWindowWhenBufferIsFullThenEveryStep() {
        val processor = ModelInputProcessor(stepSize = 64)
        val emittedAt = mutableListOf<Int>()
        for (i in 1..400) if (processor.add(sample(i)) != null) emittedAt += i
        assertEquals(listOf(128, 192, 256, 320, 384), emittedAt)
    }

    @Test
    fun windowHasModelShapeAndUciChannelOrder() {
        val processor = ModelInputProcessor(stepSize = 128)
        var window: Array<Array<FloatArray>>? = null
        for (i in 1..128) window = processor.add(sample(i))
        assertNotNull(window)
        window!!
        assertEquals(1, window.size)
        assertEquals(Constants.WINDOW_SIZE, window[0].size)
        assertEquals(Constants.NUM_CHANNELS, window[0][0].size)
        val last = window[0].last()
        // Phone at rest, z up: total_acc_z = 1 g, body_acc ≈ 0, gyro passed through.
        assertEquals(0f, last[2], 1e-3f)
        assertEquals(0.1f, last[3], 0f)
        assertEquals(0.3f, last[5], 0f)
        assertEquals(1f, last[8], 1e-5f)
    }

    @Test
    fun bodyAccelerationCapturesSuddenMovement() {
        val processor = ModelInputProcessor()
        for (i in 1..100) processor.add(sample(i))
        val channels = processor.toChannels(sample(101, accZ = 2 * Constants.STANDARD_GRAVITY))
        assertEquals(2f, channels[8], 1e-5f)
        assertEquals(1f, channels[2], 0.01f)
    }

    @Test
    fun resetClearsBuffer() {
        val processor = ModelInputProcessor(stepSize = 32)
        for (i in 1..127) processor.add(sample(i))
        processor.reset()
        assertEquals(0, processor.bufferedSamples)
        assertNull(processor.add(sample(1)))
    }
}
