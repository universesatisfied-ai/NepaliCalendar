package com.rajeshwor.nepalicalendar.model

data class CalendarMonth(val year: Int, val month: Int, val days: Int, val startWeekday: Int = 0, val entries: List<CalendarDay> = emptyList())
data class CalendarDay(val day: Int, val ad: String = "", val weekday: Int = 0, val festival: String = "", val holiday: Boolean = false, val tithi: String = "")
data class Event(val id: Long = 0L, val title: String, val notes: String = "", val date: String, val time: String = "", val category: String = "Personal", val color: Long = 0xFF18251DL, val reminder: Boolean = false)

data class FestivalItem(
    val name: String,
    val bsDate: String,
    val adDate: String = "",
    val holiday: Boolean = false
)
