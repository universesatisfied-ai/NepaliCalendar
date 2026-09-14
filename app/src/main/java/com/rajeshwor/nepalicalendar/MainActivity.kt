package com.rajeshwor.nepalicalendar

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as columnItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rajeshwor.nepalicalendar.data.ApiClient
import com.rajeshwor.nepalicalendar.model.*
import com.rajeshwor.nepalicalendar.navigation.Dest
import com.rajeshwor.nepalicalendar.ui.components.*
import com.rajeshwor.nepalicalendar.ui.theme.NepaliCalendarTheme
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge()
        setContent { NepaliCalendarTheme { App() } }
    }
}

private val monthNames = listOf("Baisakh","Jestha","Ashadh","Shrawan","Bhadra","Ashwin","Kartik","Mangsir","Poush","Magh","Falgun","Chaitra")
private val weekNames = listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")

@Composable fun App() {
    var dest by remember { mutableStateOf(Dest.HOME) }
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing, bottomBar = {
        NavigationBar { Dest.entries.forEach { d -> NavigationBarItem(dest == d, { dest = d }, { Icon(d.icon, d.label) }, label = { Text(d.label) }) } }
    }) { p -> Box(Modifier.padding(p).fillMaxSize()) { when(dest) {
        Dest.HOME -> Home({dest=Dest.CALENDAR},{dest=Dest.CONVERT})
        Dest.CALENDAR -> CalendarScreen()
        Dest.FESTIVALS -> Festivals()
        Dest.CONVERT -> Converter()
        Dest.MORE -> More()
    } } }
}

@Composable fun Home(onCalendar:()->Unit,onConvert:()->Unit) {
    val api=remember{ApiClient()}; val scope=rememberCoroutineScope(); var today by remember{mutableStateOf("Loading…")}; var online by remember{mutableStateOf(false)}
    LaunchedEffect(Unit){ try{today=api.today();online=true}catch(_:Throwable){today="No connection — cached information will be used."} }
    LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(bottom=24.dp)){
        item{BrandHeader("Nepali Calendar","by Rajeshwor Maharjan")}
        item{Card(Modifier.padding(horizontal=20.dp).fillMaxWidth(),shape=RoundedCornerShape(28.dp)){Column(Modifier.padding(22.dp)){Text("Today",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold);Text(SimpleDateFormat("EEEE, d MMMM yyyy",Locale.US).format(Date()),style=MaterialTheme.typography.titleLarge);Spacer(Modifier.height(10.dp));Text(extractSummary(today),style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(12.dp));Pill(if(online)"Online" else "Offline / retry available",online)}}}
        item{Spacer(Modifier.height(16.dp))}
        item{Row(Modifier.padding(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){Button(onCalendar,Modifier.weight(1f)){Icon(Icons.Outlined.CalendarMonth,null);Spacer(Modifier.width(6.dp));Text("Calendar")};OutlinedButton(onConvert,Modifier.weight(1f)){Icon(Icons.Outlined.SwapHoriz,null);Spacer(Modifier.width(6.dp));Text("Convert")}}}
        item{Spacer(Modifier.height(12.dp));ActionCard("Refresh today data","↻"){scope.launch{try{today=api.today();online=true}catch(_:Throwable){online=false}}}}
        item{Spacer(Modifier.height(8.dp));ActionCard("Upcoming festivals","★"){}
        }
    }
}

@Composable fun CalendarScreen(){
    val api=remember{ApiClient()}; var year by remember{mutableIntStateOf(2083)};var month by remember{mutableIntStateOf(6)};var selected by remember{mutableIntStateOf(1)};var data by remember{mutableStateOf(CalendarMonth(year,month,30))};var loading by remember{mutableStateOf(false)}
    LaunchedEffect(year,month){loading=true;try{data=parseCalendar(year,month,api.month(year,month))}catch(_:Throwable){data=CalendarMonth(year,month,30)};loading=false;selected=selected.coerceIn(1,data.days)}
    Column(Modifier.fillMaxSize()){BrandHeader("Calendar","Bikram Sambat")
        Row(Modifier.padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically){IconButton({if(month==1){year--;month=12}else month--}){Icon(Icons.Outlined.ChevronLeft,null)};Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){Text("${monthNames[month-1]} $year",style=MaterialTheme.typography.titleLarge);if(loading)LinearProgressIndicator(Modifier.fillMaxWidth(.5f))};IconButton({if(month==12){year++;month=1}else month++}){Icon(Icons.Outlined.ChevronRight,null)}}
        TextButton({year=2083;month=6;selected=1},Modifier.align(Alignment.CenterHorizontally)){Text("Today")}
        Row(Modifier.padding(horizontal=16.dp).fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){weekNames.forEach{Text(it,Modifier.weight(1f),style=MaterialTheme.typography.labelMedium)}}
        CalendarGrid(data,selected){selected=it}
        val entry=data.entries.firstOrNull{it.day==selected};Card(Modifier.padding(16.dp).fillMaxWidth(),shape=RoundedCornerShape(24.dp)){Column(Modifier.padding(18.dp)){Text("Selected date",fontWeight=FontWeight.Bold);Text("$selected ${monthNames[month-1]} $year",style=MaterialTheme.typography.headlineSmall);if(entry!=null){if(entry.ad.isNotBlank())Text("AD ${entry.ad}");if(entry.festival.isNotBlank())Text(entry.festival,color=MaterialTheme.colorScheme.primary);if(entry.tithi.isNotBlank())Text(entry.tithi,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}
}

@Composable fun CalendarGrid(data:CalendarMonth,selected:Int,onSelect:(Int)->Unit){val cells=List(data.startWeekday){null}+data.entries;LazyVerticalGrid(GridCells.Fixed(7),Modifier.padding(horizontal=12.dp).height(300.dp)){gridItems(cells){e->if(e==null)Box(Modifier.height(42.dp))else{val holiday=e.holiday||e.weekday==7;Box(Modifier.padding(2.dp).height(42.dp).fillMaxWidth().clickable{onSelect(e.day)},contentAlignment=Alignment.Center){Surface(shape=RoundedCornerShape(12.dp),color=when{selected==e.day->MaterialTheme.colorScheme.primary;holiday->MaterialTheme.colorScheme.errorContainer;else->MaterialTheme.colorScheme.surfaceVariant}){Column(Modifier.padding(horizontal=8.dp,vertical=5.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("${e.day}",fontWeight=FontWeight.Bold,color=if(selected==e.day)MaterialTheme.colorScheme.onPrimary else Color.Unspecified);if(e.festival.isNotBlank())Text("•",color=MaterialTheme.colorScheme.tertiary)}}}}}}}

@Composable fun Festivals(){val festivals=listOf("Nepali New Year" to "1 Baisakh","Buddha Jayanti" to "Baisakh","Teej" to "Bhadra","Constitution Day" to "3 Ashwin","Dashain" to "Ashwin–Kartik","Tihar" to "Kartik","Chhath Parva" to "Kartik","Holi" to "Falgun");Column(Modifier.fillMaxSize()){BrandHeader("Festivals & Holidays","Plan important Nepali dates");LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){columnItems(festivals){(n,d)->Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp)){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Event,null);Spacer(Modifier.width(14.dp));Column{Text(n,style=MaterialTheme.typography.titleMedium);Text(d,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}}}}

@Composable fun Converter(){val api=remember{ApiClient()};val scope=rememberCoroutineScope();var bs by remember{mutableStateOf("")};var ad by remember{mutableStateOf("")};var bsToAd by remember{mutableStateOf(true)};var result by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)};Column(Modifier.fillMaxSize()){BrandHeader("Date Converter","Bikram Sambat ↔ Gregorian");Card(Modifier.padding(16.dp).fillMaxWidth(),shape=RoundedCornerShape(26.dp)){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text(if(bsToAd)"BS → AD" else "AD → BS",style=MaterialTheme.typography.titleLarge);OutlinedTextField(if(bsToAd)bs else ad,{if(bsToAd)bs=it else ad=it},Modifier.fillMaxWidth(),label={Text(if(bsToAd)"BS date: YYYY-MM-DD" else "AD date: YYYY-MM-DD")},singleLine=true);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({scope.launch{busy=true;result=try{val p=(if(bsToAd)bs else ad).trim().split("-").map{it.toInt()};val r=if(bsToAd)api.bsToAd(p[0],p[1],p[2])else api.adToBs(p[0],p[1],p[2]);extractSummary(r)}catch(e:Throwable){"Invalid date or network error"};busy=false}}){if(busy)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)else Text("Convert")};OutlinedButton({bsToAd=!bsToAd;result=""}){Icon(Icons.Outlined.SwapHoriz,null);Text("Swap")};TextButton({bs="";ad="";result=""}){Text("Clear")}};if(result.isNotBlank())Card(Modifier.fillMaxWidth()){Text(result,Modifier.padding(16.dp),style=MaterialTheme.typography.titleMedium)}}}}

@Composable fun More(){var show by remember{mutableStateOf(false)};var dark by remember{mutableStateOf(false)};var events by remember{mutableStateOf(loadEvents())};Column(Modifier.fillMaxSize()){BrandHeader("More","Settings, events & information");LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{ActionCard("Personal events","+"){show=true}};item{Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp)){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.DarkMode,null);Text("Dark theme",Modifier.weight(1f).padding(start=14.dp));Switch(dark,{dark=it})}}};item{Text("Saved events",style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(top=10.dp))};columnItems(events){e->Card(Modifier.fillMaxWidth()){Row(Modifier.padding(16.dp)){Column(Modifier.weight(1f)){Text(e.title,fontWeight=FontWeight.Bold);Text("${e.date} ${e.time}",color=MaterialTheme.colorScheme.primary);if(e.notes.isNotBlank())Text(e.notes,maxLines=2,overflow=TextOverflow.Ellipsis)}};IconButton({events=events.filterNot{it.id==e.id};saveEvents(events)}){Icon(Icons.Outlined.Delete,null)}}};item{Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp)){Text("Nepali Calendar",style=MaterialTheme.typography.titleLarge);Text("Developed by Rajeshwor Maharjan");Text("Jetpack Compose + Material 3",color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}};if(show){EventDialog({e->events=events+e;saveEvents(events);show=false},{show=false})}}}

@Composable fun EventDialog(onSave:(Event)->Unit,onDismiss:()->Unit){var title by remember{mutableStateOf("")};var date by remember{mutableStateOf("2083-06-01")};var notes by remember{mutableStateOf("")};AlertDialog(onDismissRequest=onDismiss,title={Text("Add event")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(title,{title=it},label={Text("Title")},singleLine=true);OutlinedTextField(date,{date=it},label={Text("BS date")},singleLine=true);OutlinedTextField(notes,{notes=it},label={Text("Notes")})}},confirmButton={Button({if(title.isNotBlank())onSave(Event(System.currentTimeMillis(),title,notes,date))}){Text("Save")}},dismissButton={TextButton(onDismiss){Text("Cancel")}})}

private fun parseCalendar(year:Int,month:Int,raw:String):CalendarMonth{val arr=mutableListOf<CalendarDay>();fun walk(v:Any?){when(v){is JSONObject->{var day=v.optInt("day",v.optInt("date",0));if(day in 1..31){val ad=v.optString("ad",v.optString("gregorian",v.optString("englishDate","")));val festival=v.optString("festival",v.optString("event",v.optString("name","")));val wd=v.optInt("weekday",0);val hol=v.optBoolean("holiday",false);val t=v.optString("tithi","");arr.add(CalendarDay(day,ad,wd,festival,hol,t))};val it=v.keys();while(it.hasNext())walk(v.get(it.next()))};is JSONArray->for(i in 0 until v.length())walk(v.get(i))}}
walk(try{JSONObject(raw)}catch(_:Throwable){try{JSONArray(raw)}catch(_:Throwable){null}});val unique=arr.groupBy{it.day}.values.map{it.first()}.sortedBy{it.day};val days=unique.maxOfOrNull{it.day}?:30;val start=unique.firstOrNull()?.weekday?.let{if(it in 1..7)it-1 else 0}?:0;return CalendarMonth(year,month,days,start,unique.ifEmpty{(1..days).map{CalendarDay(it)}})}
private fun extractSummary(raw:String):String{if(raw.isBlank())return "Unavailable";return try{val o=JSONObject(raw);listOf("formatted","bs","date","nepali","today").firstNotNullOfOrNull{key->o.optString(key).takeIf{it.isNotBlank()}}?:o.toString().replace("\\n"," ").take(220)}catch(_:Throwable){raw.replace("\\n"," ").take(220)}}
private fun prefs(ctx:Context)=ctx.getSharedPreferences("events",Context.MODE_PRIVATE)
private fun loadEvents():List<Event>{return emptyList()}
private fun saveEvents(e:List<Event>){ }
