package com.rajeshwor.nepalicalendar.navigation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class Dest(val label:String,val icon:ImageVector){HOME("Home",Icons.Outlined.Home),CALENDAR("Calendar",Icons.Outlined.CalendarMonth),FESTIVALS("Festivals",Icons.Outlined.Event),CONVERT("Convert",Icons.Outlined.SwapHoriz),MORE("More",Icons.Outlined.MoreHoriz)}
