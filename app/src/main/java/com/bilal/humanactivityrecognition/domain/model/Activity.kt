package com.bilal.humanactivityrecognition.domain.model

import com.bilal.humanactivityrecognition.data.model.ActivityClass
import java.time.LocalDate

/** A continuous period during which the same activity was recognised. */
data class ActivitySegment(
    val id: Long,
    val activity: ActivityClass,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val averageConfidence: Float,
    val predictionCount: Int,
) {
    val durationMs: Long get() = (endTimeMs - startTimeMs).coerceAtLeast(0)

    /** Portion of this segment that lies within [fromMs, toMs). */
    fun durationWithin(fromMs: Long, toMs: Long): Long =
        (minOf(endTimeMs, toMs) - maxOf(startTimeMs, fromMs)).coerceAtLeast(0)
}

data class DailyActivitySummary(
    val date: LocalDate,
    val durations: Map<ActivityClass, Long>,
) {
    val totalMs: Long get() = durations.values.sum()

    fun durationOf(activity: ActivityClass): Long = durations[activity] ?: 0L
}

enum class InputSource(val displayName: String, val description: String) {
    PHONE_SENSORS("Phone sensors", "Live accelerometer + gyroscope at 50 Hz"),
    DEMO_REPLAY("UCI demo replay", "Replays bundled UCI-HAR test windows (works on emulators)"),
}

enum class RecognitionFrequency(val displayName: String, val stepSamples: Int) {
    FAST("Fast (every 0.64 s)", 32),
    NORMAL("Normal (every 1.28 s)", 64),
    BATTERY_SAVER("Battery saver (every 2.56 s)", 128);

    val stepMs: Long get() = stepSamples * 1000L / 50
}

data class AppSettings(
    val frequency: RecognitionFrequency = RecognitionFrequency.NORMAL,
    val inputSource: InputSource = InputSource.PHONE_SENSORS,
    val smoothingWindow: Int = 3,
    val notificationsEnabled: Boolean = true,
)
