package com.bilal.humanactivityrecognition.data.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import android.hardware.SensorManager as AndroidSensorManager

/** Accelerometer including gravity (m/s²), matching UCI-HAR "total_acc" before unit conversion. */
class AccelerometerSensor(private val sensorManager: AndroidSensorManager) {
    val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val isAvailable: Boolean get() = sensor != null

    fun readings(samplingPeriodUs: Int): Flow<SensorReading> = sensorFlow(sensorManager, sensor, samplingPeriodUs)
}

internal fun sensorFlow(manager: AndroidSensorManager, sensor: Sensor?, samplingPeriodUs: Int): Flow<SensorReading> = callbackFlow {
    if (sensor == null) {
        close()
        return@callbackFlow
    }
    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            trySend(SensorReading(event.timestamp, event.values.copyOf(3)))
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }
    manager.registerListener(listener, sensor, samplingPeriodUs)
    awaitClose { manager.unregisterListener(listener) }
}
