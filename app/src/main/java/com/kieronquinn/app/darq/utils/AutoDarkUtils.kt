package com.kieronquinn.app.darq.utils

import android.content.Context
import android.text.format.DateFormat
import java.util.Calendar

object AutoDarkUtils {

    /**
     * Evaluates whether the given [now] time falls within a custom dark schedule window defined by
     * [startTimeMins] and [endTimeMins] (minutes from midnight).
     * Handles both overnight wrap-around (e.g., 20:00 to 07:00) and same-day (e.g., 09:00 to 17:00) schedules.
     */
    fun isCustomScheduleDark(
        startTimeMins: Int,
        endTimeMins: Int,
        now: Calendar = Calendar.getInstance()
    ): Boolean {
        if (startTimeMins == endTimeMins) return false

        val currentMins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        return if (startTimeMins > endTimeMins) {
            // Overnight schedule e.g., 20:00 (1200) -> 07:00 (420)
            currentMins >= startTimeMins || currentMins < endTimeMins
        } else {
            // Same-day schedule e.g., 09:00 (540) -> 17:00 (1020)
            currentMins >= startTimeMins && currentMins < endTimeMins
        }
    }

    /**
     * Calculates the delay in milliseconds from [now] to the next occurrence of [targetMinsFromMidnight].
     * If the target time today has already passed, it calculates the delay to that time tomorrow.
     */
    fun calculateNextDelayMillis(
        targetMinsFromMidnight: Int,
        now: Calendar = Calendar.getInstance()
    ): Long {
        val targetCalendar = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, targetMinsFromMidnight / 60)
            set(Calendar.MINUTE, targetMinsFromMidnight % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (targetCalendar.timeInMillis <= now.timeInMillis) {
            targetCalendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return targetCalendar.timeInMillis - now.timeInMillis
    }

    /**
     * Formats minutes from midnight into a user-localized time string (e.g., "8:00 PM" or "20:00")
     * using the device's system 12h/24h time format setting.
     */
    fun formatMinutesFromMidnight(context: Context, minutesFromMidnight: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minutesFromMidnight / 60)
            set(Calendar.MINUTE, minutesFromMidnight % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return DateFormat.getTimeFormat(context).format(calendar.time)
    }

}
