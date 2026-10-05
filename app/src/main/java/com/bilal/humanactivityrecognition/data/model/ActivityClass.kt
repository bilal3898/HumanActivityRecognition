package com.bilal.humanactivityrecognition.data.model

import androidx.annotation.DrawableRes
import com.bilal.humanactivityrecognition.R

/** The six UCI-HAR activities, in the model's output order. */
enum class ActivityClass(
    val label: String,
    val displayName: String,
    @DrawableRes val iconRes: Int,
    val colorHex: Long,
) {
    WALKING("WALKING", "Walking", R.drawable.ic_walking, 0xFF2563EB),
    WALKING_UPSTAIRS("WALKING_UPSTAIRS", "Walking Upstairs", R.drawable.ic_upstairs, 0xFF7C3AED),
    WALKING_DOWNSTAIRS("WALKING_DOWNSTAIRS", "Walking Downstairs", R.drawable.ic_downstairs, 0xFFDB2777),
    SITTING("SITTING", "Sitting", R.drawable.ic_sitting, 0xFFF59E0B),
    STANDING("STANDING", "Standing", R.drawable.ic_standing, 0xFF10B981),
    LAYING("LAYING", "Laying", R.drawable.ic_laying, 0xFF0EA5E9);

    companion object {
        fun fromIndex(index: Int): ActivityClass = entries[index]

        fun fromLabel(label: String): ActivityClass =
            entries.firstOrNull { it.label == label.trim().uppercase() } ?: error("Unknown activity label: $label")
    }
}
