package com.bilal.humanactivityrecognition.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.getDefault())
    private val dayFormatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())
    private val shortDayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())

    fun zone(): ZoneId = ZoneId.systemDefault()

    fun formatTime(epochMs: Long): String = Instant.ofEpochMilli(epochMs).atZone(zone()).format(timeFormatter)

    fun formatDay(date: LocalDate): String = date.format(dayFormatter)

    fun formatShortDay(date: LocalDate): String = date.format(shortDayFormatter)

    fun toLocalDate(epochMs: Long): LocalDate = Instant.ofEpochMilli(epochMs).atZone(zone()).toLocalDate()

    fun startOfDay(date: LocalDate): Long = date.atStartOfDay(zone()).toInstant().toEpochMilli()

    fun endOfDay(date: LocalDate): Long = startOfDay(date.plusDays(1))

    /** 75_000 -> "1m 15s", 3_900_000 -> "1h 05m". */
    fun formatDuration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return when {
            h > 0 -> String.format(Locale.US, "%dh %02dm", h, m)
            m > 0 -> String.format(Locale.US, "%dm %02ds", m, s)
            else -> "${s}s"
        }
    }

    /** Stopwatch format, e.g. "01:02:03". */
    fun formatTimer(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
        return String.format(Locale.US, "%02d:%02d:%02d", totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60)
    }
}
