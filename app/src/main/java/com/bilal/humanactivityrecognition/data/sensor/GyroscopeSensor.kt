package com.bilal.humanactivityrecognition.data.sensor

import android.hardware.Sensor
import kotlinx.coroutines.flow.Flow
import android.hardware.SensorManager as AndroidSensorManager

/** Gyroscope angular velocity (rad/s), matching UCI-HAR "body_gyro". */
class GyroscopeSensor(private val sensorManager: AndroidSensorManager) {
    val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    val isAvailable: Boolean get() = sensor != null

    fun readings(samplingPeriodUs: Int): Flow<SensorReading> = sensorFlow(sensorManager, sensor, samplingPeriodUs)
}
