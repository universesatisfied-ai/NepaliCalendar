package com.rajeshwor.nepalicalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rajeshwor.nepalicalendar.data.ApiClient
import com.rajeshwor.nepalicalendar.navigation.Dest
import com.rajeshwor.nepalicalendar.ui.components.*
import com.rajeshwor.nepalicalendar.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);enableEdgeToEdge();setContent{NepaliCalendarTheme{App()}}}}

@Composable fun App(){var dest by remember{mutableStateOf(Dest.HOME)}; val width=androidx.compose.material3.adaptive.currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass
 Scaffold(contentWindowInsets=WindowInsets.safeDrawing,bottomBar={NavigationBar{Dest.entries.forEach{d->NavigationBarItem(selected=dest==d,onClick={dest=d},icon={Icon(d.icon,d.label)},label={Text(d.label)})}}}){p->Box(Modifier.padding(p).fillMaxSize()){when(dest){Dest.HOME->Home({dest=Dest.CALENDAR},{dest=Dest.CONVERT});Dest.CALENDAR->CalendarScreen();Dest.FESTIVALS->Festivals();Dest.CONVERT->Converter();Dest.MORE->More()}}}}

@Composable fun Home(cal:()->Unit,conv:()->Unit){val scope=rememberCoroutineScope();var status by remember{mutableStateOf("Online data available when refreshed")};var today by remember{mutableStateOf("Loading today…")};val api=remember{ApiClient()};LaunchedEffect(Unit){try{today=api.today();status="Online"}catch(_:Throwable){today="Tap refresh to try again";status="Offline / cached fallback"}}
 LazyColumn(Modifier.fillMaxSize()){item{BrandHeader("Nepali Calendar","by Rajeshwor Maharjan")};item{Card(Modifier.padding(horizontal=20.dp).fillMaxWidth(),shape=androidx.compose.foundation.shape.RoundedCornerShape(28.dp)){Column(Modifier.padding(22.dp)){Text("Today",color=MaterialTheme.colorScheme.primary);Text(SimpleDateFormat("EEEE, d MMMM yyyy",Locale.US).format(Date()),style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(8.dp));Text(today.take(500),style=MaterialTheme.typography.bodyMedium);Spacer(Modifier.height(12.dp));Pill(status,status=="Online")}}};item{Spacer(Modifier.height(18.dp))};item{Row(Modifier.padding(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){Button(onClick=cal,modifier=Modifier.weight(1f)){Icon(Icons.Outlined.CalendarMonth,null);Spacer(Modifier.width(6.dp));Text("Calendar")};OutlinedButton(onClick=conv,modifier=Modifier.weight(1f)){Icon(Icons.Outlined.SwapHoriz,null);Spacer(Modifier.width(6.dp));Text("Convert")}}};item{Spacer(Modifier.height(14.dp))};item{ActionCard("Refresh calendar data","↻"){scope.launch{try{today=api.today();status="Online"}catch(_:Throwable){status="Offline / cached fallback"}}}};item{Spacer(Modifier.height(30.dp))}}
}

@Composable fun CalendarScreen(){var year by remember{mutableIntStateOf(2083)};var month by remember{mutableIntStateOf(1)};var selected by remember{mutableIntStateOf(1)};val api=remember{ApiClient()};var raw by remember{mutableStateOf("")};LaunchedEffect(year,month){try{raw=api.month(year,month)}catch(_:Throwable){raw=""}};LazyColumn{item{BrandHeader("Calendar","Bikram Sambat monthly calendar")};item{Row(Modifier.padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically){IconButton({if(month==1){year--;month=12}else month--}){Icon(Icons.Outlined.ChevronLeft,null)};Text("$year • Month $month",Modifier.weight(1f),style=MaterialTheme.typography.titleLarge);IconButton({if(month==12){year++;month=1}else month++}){Icon(Icons.Outlined.ChevronRight,null)}}};item{TextButton({year=2083;month=1;selected=1},Modifier.padding(horizontal=20.dp)){Text("Today")}};item{CalendarGrid(selected){selected=it}};item{Card(Modifier.padding(20.dp).fillMaxWidth()){Column(Modifier.padding(20.dp)){Text("Selected date",style=MaterialTheme.typography.titleMedium);Text("BS $year/$month/$selected",style=MaterialTheme.typography.headlineSmall);if(raw.isNotBlank()){Spacer(Modifier.height(8.dp));Text("Live calendar data loaded",color=MaterialTheme.colorScheme.primary)}}}}}
}
@Composable fun CalendarGrid(selected:Int,onSelect:(Int)->Unit){val days=(1..32).toList();Column(Modifier.padding(20.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat").forEach{Text(it,Modifier.width(44.dp),style=MaterialTheme.typography.labelSmall)}};Spacer(Modifier.height(8.dp));LazyVerticalGrid(columns=GridCells.Fixed(7),modifier=Modifier.height(330.dp),userScrollEnabled=false){items(days){d->Box(Modifier.padding(2.dp).height(44.dp),contentAlignment=Alignment.Center){if(d<=30){FilterChip(selected=selected==d,onClick={onSelect(d)},label={Text("$d")})}}}}}}

@Composable fun Festivals(){val list=listOf("Dashain","Tihar","Buddha Jayanti","Constitution Day","Holi","Nepali New Year","Teej","Indra Jatra");Column(Modifier.fillMaxSize()){BrandHeader("Festivals & Holidays","Upcoming Nepali festivals and public holidays");LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(list){f->Card(Modifier.fillMaxWidth(),shape=androidx.compose.foundation.shape.RoundedCornerShape(20.dp)){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(42.dp),contentAlignment=Alignment.Center){Text("★")};Column{Text(f,style=MaterialTheme.typography.titleMedium);Text("BS date • AD date",color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}}}}

@Composable fun Converter(){var bs by remember{mutableStateOf("")};var ad by remember{mutableStateOf("")};var direction by remember{mutableStateOf(true)};Column(Modifier.fillMaxSize()){BrandHeader("Date Converter","Convert Bikram Sambat ↔ Gregorian");Card(Modifier.padding(20.dp).fillMaxWidth()){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){Text(if(direction)"BS → AD" else "AD → BS",style=MaterialTheme.typography.titleLarge);OutlinedTextField(if(direction)bs else ad,{if(direction)bs=it else ad=it},Modifier.fillMaxWidth(),label={Text(if(direction)"BS date (YYYY-MM-DD)" else "AD date (YYYY-MM-DD)")});Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Button({direction=!direction}){Icon(Icons.Outlined.SwapHoriz,null);Text(" Swap")};OutlinedButton({bs="";ad=""}){Text("Clear")}};Text("Conversion uses the Nepali date API when connected.",style=MaterialTheme.typography.bodySmall)}}}}

@Composable fun More(){LazyColumn{item{BrandHeader("More","Settings and information")};item{ActionCard("Settings","⚙"){}};item{ActionCard("Personal events","＋"){}};item{ActionCard("Notifications & reminders","🔔"){}};item{ActionCard("About Nepali Calendar","ⓘ"){}};item{Card(Modifier.padding(20.dp).fillMaxWidth()){Column(Modifier.padding(20.dp)){Text("Nepali Calendar",style=MaterialTheme.typography.titleLarge);Text("Developed by Rajeshwor Maharjan");Text("Compose + Material 3 • Responsive Android UI",color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}}
