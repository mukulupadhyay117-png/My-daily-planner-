package com.example.util

import com.example.data.model.RepeatRule
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateTimeUtils {
    private val keyFormat: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val readableDateFormat: SimpleDateFormat
        get() = SimpleDateFormat("EEE, MMM d", Locale.US)

    private val fullDateFormat: SimpleDateFormat
        get() = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US)

    private val monthYearFormat: SimpleDateFormat
        get() = SimpleDateFormat("MMMM yyyy", Locale.US)

    private val shortDayNameFormat: SimpleDateFormat
        get() = SimpleDateFormat("EEE", Locale.US)

    fun todayKey(): String = keyFormat.format(Calendar.getInstance().time)

    fun calendarToKey(cal: Calendar): String = keyFormat.format(cal.time)

    fun millisToKey(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return calendarToKey(cal)
    }

    fun keyToCalendar(dateKey: String): Calendar {
        val cal = Calendar.getInstance()
        val parsed = runCatching { keyFormat.parse(dateKey) }.getOrNull()
        if (parsed != null) {
            cal.time = parsed
        }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    fun formatReadableDate(dateKey: String): String {
        val today = todayKey()
        if (dateKey == today) return "Today"
        if (dateKey == addDays(today, 1)) return "Tomorrow"
        if (dateKey == addDays(today, -1)) return "Yesterday"
        return readableDateFormat.format(keyToCalendar(dateKey).time)
    }

    fun formatFullDate(dateKey: String): String =
        fullDateFormat.format(keyToCalendar(dateKey).time)

    fun formatMonthYear(year: Int, monthZeroBased: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return monthYearFormat.format(cal.time)
    }

    fun formatMinutes(minutes: Int): String {
        if (minutes < 0) return "Anytime"
        val hour24 = (minutes / 60).coerceIn(0, 23)
        val minute = (minutes % 60).coerceIn(0, 59)
        val amPm = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return String.format(Locale.US, "%d:%02d %s", hour12, minute, amPm)
    }

    fun formatDurationMinutes(startMinutes: Int, endMinutes: Int): String {
        val diff = if (endMinutes >= startMinutes) endMinutes - startMinutes else (1440 - startMinutes) + endMinutes
        val hrs = diff / 60
        val mins = diff % 60
        return when {
            hrs > 0 && mins > 0 -> "${hrs}h ${mins}m"
            hrs > 0 -> "${hrs}h"
            else -> "${mins}m"
        }
    }

    fun currentTimeMinutes(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    fun liveClockFormatted(): String {
        val fmt = SimpleDateFormat("hh:mm:ss a", Locale.US)
        return fmt.format(Calendar.getInstance().time)
    }

    fun addDays(dateKey: String, deltaDays: Int): String {
        val cal = keyToCalendar(dateKey)
        cal.add(Calendar.DAY_OF_MONTH, deltaDays)
        return calendarToKey(cal)
    }

    fun nextOccurrenceDateKey(dateKey: String, repeatRule: RepeatRule): String {
        val cal = keyToCalendar(dateKey)
        when (repeatRule) {
            RepeatRule.DAILY -> cal.add(Calendar.DAY_OF_MONTH, 1)
            RepeatRule.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            RepeatRule.MONTHLY -> cal.add(Calendar.MONTH, 1)
            RepeatRule.NONE -> {}
        }
        return calendarToKey(cal)
    }

    /** Returns 0 for Monday .. 6 for Sunday */
    fun dayOfWeekIndex(dateKey: String): Int {
        val cal = keyToCalendar(dateKey)
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }

    fun isRoutineScheduledOnDate(daysOfWeekMask: Int, dateKey: String): Boolean {
        val idx = dayOfWeekIndex(dateKey)
        return (daysOfWeekMask and (1 shl idx)) != 0
    }

    fun weekDateKeys(anchorDateKey: String): List<String> {
        val cal = keyToCalendar(anchorDateKey)
        val idx = dayOfWeekIndex(anchorDateKey)
        cal.add(Calendar.DAY_OF_MONTH, -idx) // Move to Monday
        return (0 until 7).map { offset ->
            val dayCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, offset) }
            calendarToKey(dayCal)
        }
    }

    fun lastNDaysKeys(days: Int, endDateKey: String = todayKey()): List<String> {
        return (days - 1 downTo 0).map { offset ->
            addDays(endDateKey, -offset)
        }
    }

    fun shortDayName(dateKey: String): String =
        shortDayNameFormat.format(keyToCalendar(dateKey).time)

    fun dayNumber(dateKey: String): Int =
        keyToCalendar(dateKey).get(Calendar.DAY_OF_MONTH)

    /** Returns 42 cells (6 weeks x 7 days, Mon..Sun) for month grid; null cells are outside month or padded with adjacent dates */
    data class MonthDayCell(
        val dateKey: String,
        val dayOfMonth: Int,
        val isCurrentMonth: Boolean
    )

    fun buildMonthGrid(year: Int, monthZeroBased: Int): List<MonthDayCell> {
        val firstOfMonth = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val firstKey = calendarToKey(firstOfMonth)
        val leadingEmpty = dayOfWeekIndex(firstKey) // 0 for Mon .. 6 for Sun
        val gridStart = (firstOfMonth.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, -leadingEmpty)
        }
        return (0 until 42).map { index ->
            val cellCal = (gridStart.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, index) }
            MonthDayCell(
                dateKey = calendarToKey(cellCal),
                dayOfMonth = cellCal.get(Calendar.DAY_OF_MONTH),
                isCurrentMonth = cellCal.get(Calendar.MONTH) == monthZeroBased
            )
        }
    }

    fun toTriggerMillis(dateKey: String, timeMinutes: Int, leadMinutes: Int = 0): Long {
        val cal = keyToCalendar(dateKey)
        val minutes = if (timeMinutes >= 0) timeMinutes else 9 * 60
        cal.set(Calendar.HOUR_OF_DAY, minutes / 60)
        cal.set(Calendar.MINUTE, minutes % 60)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.MINUTE, -leadMinutes)
        return cal.timeInMillis
    }

    fun greetingForHour(hour24: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): String = when (hour24) {
        in 4..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }
}
