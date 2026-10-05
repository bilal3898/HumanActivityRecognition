package com.bilal.humanactivityrecognition.data.sensor

/** One resampled 50 Hz reading: accelerometer in m/s² (incl. gravity), gyroscope in rad/s. */
data class SensorSample(
    val timestampNs: Long,
    val accX: Float,
    val accY: Float,
    val accZ: Float,
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float,
)

/** Raw event from a single hardware sensor. */
data class SensorReading(
    val timestampNs: Long,
    val values: FloatArray,
) {
    override fun equals(other: Any?): Boolean =
        other is SensorReading && timestampNs == other.timestampNs && values.contentEquals(other.values)

    override fun hashCode(): Int = 31 * timestampNs.hashCode() + values.contentHashCode()
}

data class SensorStatus(
    val accelerometerAvailable: Boolean = false,
    val gyroscopeAvailable: Boolean = false,
    val accelerometerName: String? = null,
    val gyroscopeName: String? = null,
    val samplingRateHz: Float = 0f,
    val isStreaming: Boolean = false,
) {
    val isReady: Boolean get() = accelerometerAvailable
}
