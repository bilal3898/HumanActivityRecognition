package com.bilal.humanactivityrecognition.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A history segment: consecutive predictions of the same activity merged together. */
@Entity(tableName = "predictions", indices = [Index("startTime"), Index("endTime")])
data class PredictionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activity: String,
    val startTime: Long,
    val endTime: Long,
    val confidenceSum: Float,
    val predictionCount: Int,
    val source: String,
) {
    val averageConfidence: Float get() = if (predictionCount == 0) 0f else confidenceSum / predictionCount
}
