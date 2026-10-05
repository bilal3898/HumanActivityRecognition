package com.bilal.humanactivityrecognition.data.sensor

import android.content.Context
import com.bilal.humanactivityrecognition.utils.Constants
import com.bilal.humanactivityrecognition.utils.FixedRateResampler
import com.bilal.humanactivityrecognition.utils.RateMeter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.hardware.SensorManager as AndroidSensorManager

/** Combines accelerometer + gyroscope into a single fixed-rate 50 Hz stream. */
class SensorManager(context: Context) {
    private val androidSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as AndroidSensorManager
    private val accelerometer = AccelerometerSensor(androidSensorManager)
    private val gyroscope = GyroscopeSensor(androidSensorManager)

    private val _status = MutableStateFlow(
        SensorStatus(
            accelerometerAvailable = accelerometer.isAvailable,
            gyroscopeAvailable = gyroscope.isAvailable,
            accelerometerName = accelerometer.sensor?.name,
            gyroscopeName = gyroscope.sensor?.name,
        )
    )
    val status: StateFlow<SensorStatus> = _status.asStateFlow()

    fun samples(samplingPeriodUs: Int = Constants.SAMPLING_PERIOD_US): Flow<SensorSample> = callbackFlow {
        val resampler = FixedRateResampler()
        val rateMeter = RateMeter()
        _status.update { it.copy(isStreaming = true, samplingRateHz = 0f) }

        launch {
            gyroscope.readings(samplingPeriodUs).collect { resampler.onGyroscope(it.values) }
        }
        launch {
            accelerometer.readings(samplingPeriodUs).collect { reading ->
                resampler.onAccelerometer(reading.timestampNs, reading.values) { sample ->
                    trySend(sample)
                }
                val rate = rateMeter.tick(reading.timestampNs)
                _status.update { it.copy(samplingRateHz = rate) }
            }
        }
        awaitClose { _status.update { it.copy(isStreaming = false, samplingRateHz = 0f) } }
    }
}
