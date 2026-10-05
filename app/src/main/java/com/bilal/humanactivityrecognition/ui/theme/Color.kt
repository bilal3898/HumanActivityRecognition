package com.bilal.humanactivityrecognition.ui.theme

import androidx.compose.ui.graphics.Color
import com.bilal.humanactivityrecognition.data.model.ActivityClass

val Blue600 = Color(0xFF2563EB)
val Blue800 = Color(0xFF1E40AF)
val Indigo900 = Color(0xFF1E3A8A)
val Slate50 = Color(0xFFF8FAFC)
val Slate100 = Color(0xFFF1F5F9)
val Slate500 = Color(0xFF64748B)
val Slate800 = Color(0xFF1E293B)
val Slate900 = Color(0xFF0F172A)
val Emerald500 = Color(0xFF10B981)
val Amber500 = Color(0xFFF59E0B)
val Red500 = Color(0xFFEF4444)

val ActivityClass.color: Color get() = Color(colorHex)
