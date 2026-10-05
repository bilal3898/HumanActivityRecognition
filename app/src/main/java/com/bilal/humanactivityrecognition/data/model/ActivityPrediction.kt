package com.bilal.humanactivityrecognition.data.model

data class ActivityPrediction(
    val activity: ActivityClass,
    val confidence: Float,
    /** Probability per [ActivityClass], in enum order. */
    val probabilities: List<Float>,
    val timestampMs: Long,
    val inferenceTimeMs: Long = 0,
    /** Ground-truth label when replaying UCI demo windows. */
    val groundTruth: ActivityClass? = null,
)
