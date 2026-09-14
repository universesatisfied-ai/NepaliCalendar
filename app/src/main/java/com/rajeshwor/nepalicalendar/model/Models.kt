package com.rajeshwor.nepalicalendar.model

data class CalendarDay(val day:Int, val ad:String?=null, val holiday:Boolean=false, val festival:String?=null)
data class Festival(val title:String, val bsDate:String, val adDate:String?=null, val holiday:Boolean=false)
data class PersonalEvent(val id:Long, val title:String, val date:String, val notes:String="", val category:String="Personal")
