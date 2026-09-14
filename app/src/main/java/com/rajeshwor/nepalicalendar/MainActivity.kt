package com.rajeshwor.nepalicalendar

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.ClipData
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
    LaunchedEffect(year,month){loading=true;try{
            val raw=api.month(year,month)
            val firstAd=try{extractIsoDate(api.bsToAd(year,month,1))}catch(_:Throwable){null}
            data=parseCalendar(year,month,raw,firstAd)
        }catch(_:Throwable){data=CalendarMonth(year,month,30)};loading=false;selected=selected.coerceIn(1,data.days)}
    Column(Modifier.fillMaxSize()){BrandHeader("Calendar","Bikram Sambat")
        Row(Modifier.padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically){IconButton({if(month==1){year--;month=12}else month--}){Icon(Icons.Outlined.ChevronLeft,null)};Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){Text("${monthNames[month-1]} $year",style=MaterialTheme.typography.titleLarge);if(loading)LinearProgressIndicator(Modifier.fillMaxWidth(.5f))};IconButton({if(month==12){year++;month=1}else month++}){Icon(Icons.Outlined.ChevronRight,null)}}
        TextButton({year=2083;month=6;selected=1},Modifier.align(Alignment.CenterHorizontally)){Text("Today")}
        Row(Modifier.padding(horizontal=16.dp).fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){weekNames.forEach{Text(it,Modifier.weight(1f),style=MaterialTheme.typography.labelMedium)}}
        CalendarGrid(data,selected){selected=it}
        val entry=data.entries.firstOrNull{it.day==selected};Card(Modifier.padding(16.dp).fillMaxWidth(),shape=RoundedCornerShape(24.dp)){Column(Modifier.padding(18.dp)){Text("Selected date",fontWeight=FontWeight.Bold);Text("$selected ${monthNames[month-1]} $year",style=MaterialTheme.typography.headlineSmall);if(entry!=null){if(entry.ad.isNotBlank())Text("AD ${entry.ad}");if(entry.festival.isNotBlank())Text(entry.festival,color=MaterialTheme.colorScheme.primary);if(entry.tithi.isNotBlank())Text(entry.tithi,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}
}

@Composable fun CalendarGrid(data:CalendarMonth,selected:Int,onSelect:(Int)->Unit){val cells=List(data.startWeekday){null}+data.entries;LazyVerticalGrid(GridCells.Fixed(7),Modifier.padding(horizontal=12.dp).height(300.dp)){gridItems(cells){e->if(e==null)Box(Modifier.height(42.dp))else{val holiday=e.holiday||e.weekday==6;Box(Modifier.padding(2.dp).height(42.dp).fillMaxWidth().clickable{onSelect(e.day)},contentAlignment=Alignment.Center){Surface(shape=RoundedCornerShape(12.dp),color=when{selected==e.day->MaterialTheme.colorScheme.primary;holiday->MaterialTheme.colorScheme.errorContainer;else->MaterialTheme.colorScheme.surfaceVariant}){Column(Modifier.padding(horizontal=8.dp,vertical=5.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("${e.day}",fontWeight=FontWeight.Bold,color=if(selected==e.day)MaterialTheme.colorScheme.onPrimary else Color.Unspecified);if(e.festival.isNotBlank())Text("•",color=MaterialTheme.colorScheme.tertiary)}}}}}}}

@Composable fun Festivals(){
    val api=remember{ApiClient()}; val scope=rememberCoroutineScope()
    var year by remember{mutableIntStateOf(2083)}; var festivals by remember{mutableStateOf<List<FestivalItem>>(emptyList())}
    var loading by remember{mutableStateOf(false)}; var error by remember{mutableStateOf("")}; var query by remember{mutableStateOf("")}; var holidaysOnly by remember{mutableStateOf(false)}
    LaunchedEffect(year){ loading=true; error=""; try{
        val out=mutableListOf<FestivalItem>()
        for(m in 1..12){
            try{
                val raw=api.month(year,m); val firstAd=try{extractIsoDate(api.bsToAd(year,m,1))}catch(_:Throwable){null}
                val month=parseCalendar(year,m,raw,firstAd)
                month.entries.filter{it.festival.isNotBlank()}.forEach{d->out.add(FestivalItem(d.festival,"${d.day} ${monthNames[m-1]} $year",d.ad,d.holiday))}
            }catch(_:Throwable){}
        }
        festivals=out.distinctBy{"${it.bsDate}|${it.name}"}
        if(festivals.isEmpty()) error="No festival data available. Check your internet connection."
    }catch(_:Throwable){error="Could not load festivals. Check your internet connection."} finally{loading=false} }
    val filtered=festivals.filter{(!holidaysOnly||it.holiday||it.name.isNotBlank()) && (query.isBlank()||it.name.contains(query,true)||it.bsDate.contains(query,true)||it.adDate.contains(query,true))}
    Column(Modifier.fillMaxSize()){
        BrandHeader("Festivals & Holidays","Official calendar events from the API")
        Row(Modifier.padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically){IconButton({year--}){Icon(Icons.Outlined.ChevronLeft,null)};Text("$year",Modifier.weight(1f),style=MaterialTheme.typography.titleLarge,textAlign=androidx.compose.ui.text.style.TextAlign.Center);IconButton({year++}){Icon(Icons.Outlined.ChevronRight,null)}}
        OutlinedTextField(query,{query=it},Modifier.padding(horizontal=16.dp).fillMaxWidth(),label={Text("Search festivals")},singleLine=true,leadingIcon={Icon(Icons.Outlined.Search,null)})
        Row(Modifier.padding(horizontal=16.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically){FilterChip(selected=holidaysOnly,onClick={holidaysOnly=!holidaysOnly},label={Text("Public holidays only")});Spacer(Modifier.weight(1f));if(loading)CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp)}
        if(error.isNotBlank())Text(error,Modifier.padding(horizontal=16.dp,vertical=8.dp),color=MaterialTheme.colorScheme.error)
        LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            if(!loading&&filtered.isEmpty()&&error.isBlank())item{Text("No matching festivals found.",color=MaterialTheme.colorScheme.onSurfaceVariant)}
            columnItems(filtered){f->Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp)){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Icon(if(f.holiday)Icons.Outlined.EventAvailable else Icons.Outlined.Event,null);Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text(f.name,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);Text("BS ${f.bsDate}",color=MaterialTheme.colorScheme.primary);if(f.adDate.isNotBlank())Text("AD ${f.adDate}",color=MaterialTheme.colorScheme.onSurfaceVariant)};if(f.holiday)Text("Holiday",color=MaterialTheme.colorScheme.error,fontWeight=FontWeight.Bold)}}}}
        }
    }
}

@Composable fun Converter(){
    val api=remember{ApiClient()}
    val scope=rememberCoroutineScope()
    val context=androidx.compose.ui.platform.LocalContext.current
    var bs by remember{mutableStateOf("2083-06-01")}
    var ad by remember{mutableStateOf("2026-09-14")}
    var bsToAd by remember{mutableStateOf(true)}
    var result by remember{mutableStateOf("")}
    var error by remember{mutableStateOf("")}
    var busy by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize()){
        BrandHeader("Date Converter","Bikram Sambat ↔ Gregorian")
        Card(Modifier.padding(16.dp).fillMaxWidth(),shape=RoundedCornerShape(26.dp)){
            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                Text(if(bsToAd)"BS → AD" else "AD → BS",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                Text(if(bsToAd)"Convert a Bikram Sambat date to Gregorian." else "Convert a Gregorian date to Bikram Sambat.",color=MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value=if(bsToAd)bs else ad,
                    onValueChange={if(bsToAd)bs=it else ad=it;error="";result=""},
                    modifier=Modifier.fillMaxWidth(),
                    label={Text(if(bsToAd)"BS date" else "AD date")},
                    supportingText={Text("Format: YYYY-MM-DD")},
                    singleLine=true
                )
                if(error.isNotBlank()) Text(error,color=MaterialTheme.colorScheme.error)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically){
                    Button(onClick={
                        scope.launch{
                            busy=true;error="";result=""
                            try{
                                val parts=(if(bsToAd)bs else ad).trim().split("-")
                                if(parts.size!=3) throw IllegalArgumentException()
                                val nums=parts.map{it.toInt()}
                                if(nums.any{it<0}) throw IllegalArgumentException()
                                val raw=if(bsToAd)api.bsToAd(nums[0],nums[1],nums[2])else api.adToBs(nums[0],nums[1],nums[2])
                                result=parseConversionResult(raw,bsToAd)
                                if(result.isBlank()) throw IllegalStateException()
                            }catch(_:NumberFormatException){error="Enter a valid date in YYYY-MM-DD format."}
                            catch(_:IllegalArgumentException){error="Enter a valid date in YYYY-MM-DD format."}
                            catch(_:Throwable){error="Could not convert this date. Check the date and your internet connection."}
                            busy=false
                        }
                    },enabled=!busy){
                        if(busy)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp) else Text("Convert")
                    }
                    OutlinedButton(onClick={bsToAd=!bsToAd;result="";error=""}){Icon(Icons.Outlined.SwapHoriz,null);Spacer(Modifier.width(4.dp));Text("Swap")}
                    TextButton(onClick={bs="";ad="";result="";error=""}){Text("Clear")}
                }
                if(result.isNotBlank()){
                    Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)){
                        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                            Text("Result",fontWeight=FontWeight.Bold)
                            Text(result,style=MaterialTheme.typography.headlineSmall)
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                OutlinedButton(onClick={
                                    val clip=ClipData.newPlainText("Nepali Calendar date",result)
                                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(clip)
                                }){Icon(Icons.Outlined.ContentCopy,null);Spacer(Modifier.width(4.dp));Text("Copy")}
                                OutlinedButton(onClick={
                                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,result)},"Share date"))
                                }){Icon(Icons.Outlined.Share,null);Spacer(Modifier.width(4.dp));Text("Share")}
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable fun More(){var show by remember{mutableStateOf(false)};var dark by remember{mutableStateOf(false)};var events by remember{mutableStateOf(loadEvents())};Column(Modifier.fillMaxSize()){BrandHeader("More","Settings, events & information");LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{ActionCard("Personal events","+"){show=true}};item{Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp)){Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.DarkMode,null);Text("Dark theme",Modifier.weight(1f).padding(start=14.dp));Switch(dark,{dark=it})}}};item{Text("Saved events",style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(top=10.dp))};columnItems(events){e->Card(Modifier.fillMaxWidth()){Row(Modifier.padding(16.dp)){Column(Modifier.weight(1f)){Text(e.title,fontWeight=FontWeight.Bold);Text("${e.date} ${e.time}",color=MaterialTheme.colorScheme.primary);if(e.notes.isNotBlank())Text(e.notes,maxLines=2,overflow=TextOverflow.Ellipsis)}};IconButton({events=events.filterNot{it.id==e.id};saveEvents(events)}){Icon(Icons.Outlined.Delete,null)}}};item{Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp)){Text("Nepali Calendar",style=MaterialTheme.typography.titleLarge);Text("Developed by Rajeshwor Maharjan");Text("Jetpack Compose + Material 3",color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}};if(show){EventDialog({e->events=events+e;saveEvents(events);show=false},{show=false})}}}

@Composable fun EventDialog(onSave:(Event)->Unit,onDismiss:()->Unit){var title by remember{mutableStateOf("")};var date by remember{mutableStateOf("2083-06-01")};var notes by remember{mutableStateOf("")};AlertDialog(onDismissRequest=onDismiss,title={Text("Add event")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(title,{title=it},label={Text("Title")},singleLine=true);OutlinedTextField(date,{date=it},label={Text("BS date")},singleLine=true);OutlinedTextField(notes,{notes=it},label={Text("Notes")})}},confirmButton={Button({if(title.isNotBlank())onSave(Event(System.currentTimeMillis(),title,notes,date))}){Text("Save")}},dismissButton={TextButton(onDismiss){Text("Cancel")}})}

private fun parseConversionResult(raw:String,bsToAd:Boolean):String{
    val o=try{JSONObject(raw)}catch(_:Throwable){return extractIsoDate(raw) ?: raw.trim().takeIf{it.isNotBlank()} ?: ""}
    fun find(v:Any?,vararg keys:String):String{
        when(v){
            is JSONObject->{
                for(k in keys){val value=v.optString(k);if(value.isNotBlank())return value}
                val it=v.keys();while(it.hasNext()){val r=find(v.get(it.next()),*keys);if(r.isNotBlank())return r}
            }
            is JSONArray->for(i in 0 until v.length()){val r=find(v.get(i),*keys);if(r.isNotBlank())return r}
        }
        return ""
    }
    return if(bsToAd){
        find(o,"formatted").ifBlank{find(o,"date").ifBlank{extractIsoDate(raw).orEmpty()}}
    }else{
        find(o,"formatted").ifBlank{find(o,"date").ifBlank{
            val y=find(o,"year");val m=find(o,"month");val d=find(o,"day");if(y.isNotBlank()&&m.isNotBlank()&&d.isNotBlank())"$y-${m.padStart(2,'0')}-${d.padStart(2,'0')}" else ""
        }}
    }
}

private fun parseCalendar(year:Int,month:Int,raw:String,firstAd:String?):CalendarMonth{
    val arr=mutableListOf<CalendarDay>()
    val root:Any?=try{JSONObject(raw)}catch(_:Throwable){try{JSONArray(raw)}catch(_:Throwable){null}}
    fun text(o:JSONObject,vararg keys:String):String=keys.firstNotNullOfOrNull{key->
        o.optString(key).takeIf{it.isNotBlank()}
    } ?: ""
    fun walk(v:Any?){
        when(v){
            is JSONObject->{
                val day=parseDay(text(v,"n"),v.optInt("day",0))
                if(day in 1..32){
                    val weekday=v.optInt("d",v.optInt("weekday",0)).let{if(it in 1..7)it-1 else if(it in 0..6)it else 0}
                    val festival=text(v,"f","festival","event","name")
                    val holiday=v.optBoolean("h",v.optBoolean("holiday",false))
                    val tithi=text(v,"t","tithi")
                    val ad=if(firstAd!=null) addDays(firstAd,day-1) else text(v,"ad","gregorian","englishDate")
                    arr.add(CalendarDay(day,ad,weekday,festival,holiday,tithi))
                }
                val it=v.keys();while(it.hasNext())walk(v.get(it.next()))
            }
            is JSONArray->for(i in 0 until v.length())walk(v.get(i))
        }
    }
    walk(root)
    val unique=arr.groupBy{it.day}.values.map{it.first()}.sortedBy{it.day}
    val days=unique.maxOfOrNull{it.day} ?: 30
    val start=unique.firstOrNull()?.weekday ?: 0
    return CalendarMonth(year,month,days,start,unique.ifEmpty{(1..days).map{d->CalendarDay(d,if(firstAd!=null)addDays(firstAd,d-1) else "",(start+d-1)%7)}})
}

private fun parseDay(value:String,fallback:Int):Int{
    if(value.isBlank()) return fallback
    val normalized=value.map{c->
        when(c){
            '०'->'0';'१'->'1';'२'->'2';'३'->'3';'४'->'4';
            '५'->'5';'६'->'6';'७'->'7';'८'->'8';'९'->'9';else->c
        }
    }.joinToString("")
    return normalized.toIntOrNull() ?: fallback
}

private fun extractIsoDate(raw:String):String?{
    val regex=Regex("\\b(\\d{4}-\\d{2}-\\d{2})\\b")
    return regex.find(raw)?.groupValues?.get(1)
}

private fun addDays(iso:String,days:Int):String{
    val f=SimpleDateFormat("yyyy-MM-dd",Locale.US)
    f.isLenient=false
    val d=f.parse(iso) ?: return iso
    val cal=Calendar.getInstance(TimeZone.getTimeZone("UTC"),Locale.US).apply{time=d}
    cal.add(Calendar.DAY_OF_MONTH,days)
    return f.format(cal.time)
}

private fun extractSummary(raw:String):String{if(raw.isBlank())return "Unavailable";return try{val o=JSONObject(raw);listOf("formatted","bs","date","nepali","today").firstNotNullOfOrNull{key->o.optString(key).takeIf{it.isNotBlank()}}?:o.toString().replace("\\n"," ").take(220)}catch(_:Throwable){raw.replace("\\n"," ").take(220)}}
private fun prefs(ctx:Context)=ctx.getSharedPreferences("events",Context.MODE_PRIVATE)
private fun loadEvents():List<Event>{return emptyList()}
private fun saveEvents(e:List<Event>){ }
