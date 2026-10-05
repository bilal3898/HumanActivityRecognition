package com.bilal.humanactivityrecognition.data.repository

import com.bilal.humanactivityrecognition.data.model.ActivityClass
import com.bilal.humanactivityrecognition.data.model.ActivityPrediction
import com.bilal.humanactivityrecognition.data.sensor.SensorStatus
import com.bilal.humanactivityrecognition.database.PredictionDao
import com.bilal.humanactivityrecognition.database.PredictionEntity
import com.bilal.humanactivityrecognition.domain.model.ActivitySegment
import com.bilal.humanactivityrecognition.domain.model.DailyActivitySummary
import com.bilal.humanactivityrecognition.domain.model.InputSource
import com.bilal.humanactivityrecognition.utils.Constants
import com.bilal.humanactivityrecognition.utils.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate

data class LiveRecognitionState(
    val isRunning: Boolean = false,
    val startedAtMs: Long? = null,
    val inputSource: InputSource = InputSource.PHONE_SENSORS,
    val current: ActivityPrediction? = null,
    val recent: List<ActivityPrediction> = emptyList(),
    val sensorStatus: SensorStatus = SensorStatus(),
    val bufferedSamples: Int = 0,
    val totalPredictions: Int = 0,
    val error: String? = null,
)

class ActivityRepository(private val dao: PredictionDao) {
    private val _live = MutableStateFlow(LiveRecognitionState())
    val live: StateFlow<LiveRecognitionState> = _live.asStateFlow()

    // ---- Live state (written by ActivityRecognitionService) ----

    fun onRecognitionStarted(source: InputSource, nowMs: Long = System.currentTimeMillis()) = _live.update {
        LiveRecognitionState(isRunning = true, startedAtMs = nowMs, inputSource = source, sensorStatus = it.sensorStatus)
    }

    fun onRecognitionStopped() = _live.update { it.copy(isRunning = false, startedAtMs = null, bufferedSamples = 0) }

    fun onSensorStatus(status: SensorStatus) = _live.update { it.copy(sensorStatus = status) }

    fun onBufferProgress(samples: Int) = _live.update { if (it.bufferedSamples == samples) it else it.copy(bufferedSamples = samples) }

    fun onError(message: String?) = _live.update { it.copy(error = message) }

    suspend fun onPrediction(prediction: ActivityPrediction, stepMs: Long, source: InputSource) {
        _live.update {
            it.copy(
                current = prediction,
                recent = (listOf(prediction) + it.recent).take(Constants.RECENT_PREDICTIONS_LIMIT),
                totalPredictions = it.totalPredictions + 1,
                error = null,
            )
        }
        recordSegment(prediction, stepMs, source)
    }

    private suspend fun recordSegment(prediction: ActivityPrediction, stepMs: Long, source: InputSource) {
        val last = dao.latest()
        val ts = prediction.timestampMs
        if (last != null && last.activity == prediction.activity.label && ts - last.endTime <= Constants.SEGMENT_MERGE_GAP_MS) {
            dao.update(
                last.copy(
                    endTime = ts,
                    confidenceSum = last.confidenceSum + prediction.confidence,
                    predictionCount = last.predictionCount + 1,
                )
            )
        } else {
            val start = if (last != null && ts - last.endTime <= Constants.SEGMENT_MERGE_GAP_MS) last.endTime else ts - stepMs
            dao.insert(
                PredictionEntity(
                    activity = prediction.activity.label,
                    startTime = start,
                    endTime = ts,
                    confidenceSum = prediction.confidence,
                    predictionCount = 1,
                    source = source.name,
                )
            )
        }
    }

    // ---- History & statistics ----

    fun recentSegments(limit: Int = 500): Flow<List<ActivitySegment>> =
        dao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

    fun segmentsBetween(fromMs: Long, toMs: Long): Flow<List<ActivitySegment>> =
        dao.observeBetween(fromMs, toMs).map { list -> list.map { it.toDomain() } }

    fun dailySummaries(endDate: LocalDate, days: Int): Flow<List<DailyActivitySummary>> {
        val startDate = endDate.minusDays((days - 1).toLong())
        return segmentsBetween(DateTimeUtils.startOfDay(startDate), DateTimeUtils.endOfDay(endDate)).map { segments ->
            (0 until days).map { offset ->
                val date = startDate.plusDays(offset.toLong())
                summarize(date, segments)
            }
        }
    }

    suspend fun clearHistory() = dao.clear()

    companion object {
        fun summarize(date: LocalDate, segments: List<ActivitySegment>): DailyActivitySummary {
            val from = DateTimeUtils.startOfDay(date)
            val to = DateTimeUtils.endOfDay(date)
            val durations = ActivityClass.entries.associateWith { cls ->
                segments.filter { it.activity == cls }.sumOf { it.durationWithin(from, to) }
            }
            return DailyActivitySummary(date, durations)
        }
    }
}

fun PredictionEntity.toDomain() = ActivitySegment(
    id = id,
    activity = ActivityClass.fromLabel(activity),
    startTimeMs = startTime,
    endTimeMs = endTime,
    averageConfidence = averageConfidence,
    predictionCount = predictionCount,
)
