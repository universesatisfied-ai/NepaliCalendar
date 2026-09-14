package com.rajeshwor.nepalicalendar.model

data class CalendarMonth(
    val year: Int,
    val month: Int,
    val days: Int = 30
)

data class Event(
    val id: Long = 0L,
    val title: String,
    val notes: String = "",
    val date: String = "",
    val category: String = "Personal"
)
