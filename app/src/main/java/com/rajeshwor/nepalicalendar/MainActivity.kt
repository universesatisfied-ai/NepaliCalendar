package com.rajeshwor.nepalicalendar

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rajeshwor.nepalicalendar.data.ApiClient
import com.rajeshwor.nepalicalendar.model.CalendarDay
import com.rajeshwor.nepalicalendar.model.CalendarMonth
import com.rajeshwor.nepalicalendar.model.Event
import com.rajeshwor.nepalicalendar.navigation.Dest
import com.rajeshwor.nepalicalendar.ui.theme.DeepGreen
import com.rajeshwor.nepalicalendar.ui.theme.Lime
import com.rajeshwor.nepalicalendar.ui.theme.NepaliCalendarTheme
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { NepaliCalendarRoot() }
    }
}

private val monthNames = listOf("Baisakh","Jestha","Ashadh","Shrawan","Bhadra","Ashwin","Kartik","Mangsir","Poush","Magh","Falgun","Chaitra")
private val weekNames = listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")

@Composable
private fun NepaliCalendarRoot() {
    val context = LocalContext.current
    var dark by rememberSaveable { mutableStateOf(context.getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean("dark", false)) }
    NepaliCalendarTheme(dark = dark) {
        App(dark) { dark = it; context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("dark", it).apply() }
    }
}

@Composable
private fun App(dark: Boolean, onDarkChanged: (Boolean) -> Unit) {
    var destination by rememberSaveable { mutableStateOf(Dest.HOME) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                Dest.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = { Icon(item.icon, item.label) },
                        label = { Text(item.label, maxLines = 1) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (destination) {
                Dest.HOME -> Home({ destination = Dest.CALENDAR }, { destination = Dest.CONVERT }, { destination = Dest.FESTIVALS })
                Dest.CALENDAR -> CalendarScreen()
                Dest.FESTIVALS -> Festivals()
                Dest.CONVERT -> Converter()
                Dest.MORE -> More(dark, onDarkChanged)
            }
        }
    }
}

@Composable private fun Header(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        subtitle?.let { Text(it, Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable private fun Home(onCalendar: () -> Unit, onConvert: () -> Unit, onFestivals: () -> Unit) {
    val context = LocalContext.current; val api = remember(context) { ApiClient(context) }; val scope = rememberCoroutineScope()
    var today by remember { mutableStateOf("Loading today's date…") }; var connected by remember { mutableStateOf(false) }; var loading by remember { mutableStateOf(true) }
    fun refresh() { scope.launch { loading = true; try { today = todaySummary(api.today()); connected = true } catch (_: Throwable) { connected = false; today = "Calendar service unavailable. Cached data will be used when available." }; loading = false } }
    LaunchedEffect(Unit) { refresh() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(24.dp)) {
                Text("Nepali Calendar", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                Text("by Rajeshwor Maharjan", color = Lime, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(22.dp)); Text("TODAY", color = Color.White.copy(.7f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp)); Text(today, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp)); AssistChip(onClick = { refresh() }, label = { Text(if (connected) "Online • Refresh" else "Offline • Tap to retry") }, leadingIcon = { Icon(if (connected) Icons.Outlined.Check else Icons.Outlined.Refresh, null) })
            }
        }
        item {
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction("Calendar", Icons.Outlined.CalendarMonth, onCalendar, Modifier.weight(1f))
                QuickAction("Convert", Icons.Outlined.SwapHoriz, onConvert, Modifier.weight(1f))
                QuickAction("Festivals", Icons.Outlined.Event, onFestivals, Modifier.weight(1f))
            }
        }
        item { InfoCard("Reliable calendar data", "The app uses online services with local caching. If a service is temporarily unavailable, the last successful data remains available.", Icons.Outlined.CloudDone) }
    }
}

@Composable private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier) {
    Card(onClick = onClick, modifier = modifier.height(100.dp), shape = RoundedCornerShape(20.dp)) { Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) { Icon(icon, null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(8.dp)); Text(label, fontWeight = FontWeight.SemiBold) } }
}

@Composable private fun InfoCard(title: String, body: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(22.dp)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.Top) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(14.dp)); Column { Text(title, fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp)); Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
}

@Composable private fun CalendarScreen() {
    val context = LocalContext.current; val api = remember(context) { ApiClient(context) }
    var year by rememberSaveable { mutableIntStateOf(2083) }; var month by rememberSaveable { mutableIntStateOf(6) }; var selected by rememberSaveable { mutableIntStateOf(1) }
    var data by remember { mutableStateOf(CalendarMonth(year, month, 30)) }; var loading by remember { mutableStateOf(true) }; var error by remember { mutableStateOf("") }
    LaunchedEffect(year, month) {
        loading = true; error = ""
        try { val raw = api.month(year, month); val first = try { extractIsoDate(api.bsToAd(year, month, 1)) } catch (_: Throwable) { null }; data = parseCalendar(year, month, raw, first) }
        catch (t: Throwable) { error = "Unable to load this month. Check your connection or try again." }
        selected = selected.coerceIn(1, data.days); loading = false
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Header("Calendar", "Bikram Sambat") }
        item {
            Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton({ if (month == 1) { year--; month = 12 } else month-- }) { Icon(Icons.Outlined.ChevronLeft, "Previous month") }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { Text("${monthNames[month-1]} $year", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); if (loading) LinearProgressIndicator(Modifier.width(130.dp).padding(top = 6.dp)) }
                IconButton({ if (month == 12) { year++; month = 1 } else month++ }) { Icon(Icons.Outlined.ChevronRight, "Next month") }
            }
        }
        item { OutlinedButton({ year=2083; month=6; selected=1 }, Modifier.padding(horizontal=16.dp)) { Icon(Icons.Outlined.Today, null); Spacer(Modifier.width(8.dp)); Text("Today") } }
        if (error.isNotBlank()) item { ErrorCard(error) }
        item { CalendarGrid(data, selected) { selected = it } }
        item {
            val entry = data.entries.firstOrNull { it.day == selected }
            Card(Modifier.padding(horizontal=16.dp).fillMaxWidth(), shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(20.dp)) {
                Text("Selected date", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("$selected ${monthNames[month-1]} $year", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, Modifier.padding(top=5.dp))
                entry?.ad?.takeIf { it.isNotBlank() }?.let { Text("AD $it", color = MaterialTheme.colorScheme.onSurfaceVariant, Modifier.padding(top=4.dp)) }
                entry?.festival?.takeIf { it.isNotBlank() }?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, Modifier.padding(top=10.dp)) }
                entry?.tithi?.takeIf { it.isNotBlank() }?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, Modifier.padding(top=3.dp)) }
            } }
        }
    }
}

@Composable private fun CalendarGrid(data: CalendarMonth, selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) { weekNames.forEach { Text(it, Modifier.weight(1f), textAlign=TextAlign.Center, style=MaterialTheme.typography.labelMedium, fontWeight=FontWeight.Bold, color=MaterialTheme.colorScheme.onSurfaceVariant) } }
        val cells = List(data.startWeekday.coerceIn(0,6)) { CalendarDay(0) } + data.entries
        val rows = (cells.size + 6) / 7
        repeat(rows) { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { repeat(7) { col ->
            val index=row*7+col; val entry=cells.getOrNull(index)
            Box(Modifier.weight(1f).aspectRatio(1f).clickable(enabled=entry?.day ?: 0 > 0) { onSelect(entry!!.day) }, contentAlignment=Alignment.Center) {
                if (entry != null && entry.day > 0) Surface(Modifier.fillMaxSize().padding(2.dp), RoundedCornerShape(13.dp), color=when { selected==entry.day -> MaterialTheme.colorScheme.primary; entry.holiday || entry.weekday==6 -> MaterialTheme.colorScheme.errorContainer; else -> MaterialTheme.colorScheme.surfaceVariant }) {
                    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) { Text("${entry.day}", fontWeight=FontWeight.Bold, color=if(selected==entry.day) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface); if(entry.festival.isNotBlank()) Text("•", color=if(selected==entry.day) Lime else MaterialTheme.colorScheme.primary) }
                }
            }
        } } }
    }
}

@Composable private fun Festivals() {
    val context=LocalContext.current; val api=remember(context){ApiClient(context)}; val scope=rememberCoroutineScope(); var year by rememberSaveable{mutableIntStateOf(2083)}; var month by rememberSaveable{mutableIntStateOf(6)}; var entries by remember{mutableStateOf<List<CalendarDay>>(emptyList())}; var loading by remember{mutableStateOf(true)}; var error by remember{mutableStateOf("")}; var search by rememberSaveable{mutableStateOf("")}; var holidaysOnly by rememberSaveable{mutableStateOf(false)}
    fun load(){scope.launch{loading=true; error=""; try{entries=parseCalendar(year,month,api.month(year,month),null).entries.filter{it.festival.isNotBlank()||it.holiday}}catch(_ : Throwable){entries=emptyList();error="Unable to load festivals. Check your connection and try again."};loading=false}}
    LaunchedEffect(year,month){load()}
    val filtered=entries.filter{(!holidaysOnly||it.holiday)&&(search.isBlank()||it.festival.contains(search,true)||it.day.toString()==search.trim())}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=28.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Header("Festivals & Holidays","Search public holidays and festivals")};item{Row(Modifier.padding(horizontal=16.dp).fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){IconButton({if(month==1){year--;month=12}else month--}){Icon(Icons.Outlined.ChevronLeft,"Previous month")};Text("${monthNames[month-1]} $year",Modifier.weight(1f),textAlign=TextAlign.Center,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);IconButton({if(month==12){year++;month=1}else month++}){Icon(Icons.Outlined.ChevronRight,"Next month")}}};item{OutlinedTextField(search,{search=it},Modifier.padding(horizontal=16.dp).fillMaxWidth(),label={Text("Search")},leadingIcon={Icon(Icons.Outlined.Search,null)},singleLine=true)};item{FilterChip(holidaysOnly,{holidaysOnly=!holidaysOnly},Modifier.padding(horizontal=16.dp),label={Text("Holidays only")})};if(error.isNotBlank())item{ErrorCard(error)};if(loading)item{Box(Modifier.fillMaxWidth().padding(30.dp),Alignment.Center){CircularProgressIndicator()}} else if(filtered.isEmpty())item{InfoCard("Nothing found","There are no matching festivals in this month.",Icons.Outlined.Event)} else items(filtered){e->Card(Modifier.padding(horizontal=16.dp).fillMaxWidth(),RoundedCornerShape(20.dp)){Row(Modifier.padding(17.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.Event,null,tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text(e.festival.ifBlank{"Public holiday"},fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis);Text("${e.day} ${monthNames[month-1]} $year${e.ad.takeIf{it.isNotBlank()}?.let{" • AD $it"}?:""}",color=MaterialTheme.colorScheme.onSurfaceVariant)};if(e.holiday)AssistChip({},{Text("Holiday")})}}}}
}

@Composable private fun Converter() {
    val context=LocalContext.current; val api=remember(context){ApiClient(context)}; val scope=rememberCoroutineScope(); var bsMode by rememberSaveable{mutableStateOf(true)}; var year by rememberSaveable{mutableIntStateOf(2083)}; var month by rememberSaveable{mutableIntStateOf(6)}; var day by rememberSaveable{mutableIntStateOf(1)}; var result by rememberSaveable{mutableStateOf("")}; var busy by remember{mutableStateOf(false)}; var error by rememberSaveable{mutableStateOf("")}
    val yearRange=if(bsMode) 2000..2090 else 1943..2034; val maxDay=32
    fun convert(){scope.launch{busy=true;error="";result="";try{val raw=if(bsMode)api.bsToAd(year,month,day)else api.adToBs(year,month,day);result=conversionSummary(raw,bsMode)}catch(_:Throwable){error="Conversion failed. Check the selected date and connection."};busy=false}}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=28.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){item{Header("Date Converter","Choose a date — no typing required")};item{Row(Modifier.padding(horizontal=16.dp).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(bsMode,{if(!busy){bsMode=!bsMode;year=if(bsMode)2083 else 2026;month=6;day=1;result="";error=""}},label={Text("BS → AD")});FilterChip(!bsMode,{if(!busy){bsMode=!bsMode;year=if(bsMode)2083 else 2026;month=6;day=1;result="";error=""}},label={Text("AD → BS")})}};item{Card(Modifier.padding(horizontal=16.dp).fillMaxWidth(),RoundedCornerShape(26.dp)){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text(if(bsMode)"Bikram Sambat date" else "Gregorian date",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);DropdownField("Year",year,yearRange.toList(),{year=it},Modifier.fillMaxWidth());Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){DropdownField("Month",month,(1..12).toList(),{month=it;day=day.coerceAtMost(32)},Modifier.weight(1f));DropdownField("Day",day,(1..maxDay).toList(),{day=it},Modifier.weight(1f))};if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error);Button(convert,Modifier.fillMaxWidth(),enabled=!busy){if(busy)CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp)else{Icon(Icons.Outlined.Sync,null);Spacer(Modifier.width(8.dp));Text("Convert date")}};TextButton({result="";error="";year=if(bsMode)2083 else 2026;month=6;day=1},Modifier.align(Alignment.End)){Text("Reset")}}};if(result.isNotBlank())item{Card(Modifier.padding(horizontal=16.dp).fillMaxWidth(),RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)){Column(Modifier.padding(20.dp)){Text("Converted date",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold);Text(result,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,Modifier.padding(top=6.dp));Text("You can copy this result from the Android share menu in a later release.",color=MaterialTheme.colorScheme.onSurfaceVariant,Modifier.padding(top=5.dp))}}}}
}

@Composable private fun <T> DropdownField(label:String,value:T,values:List<T>,onValue:(T)->Unit,modifier:Modifier=Modifier){var expanded by remember{mutableStateOf(false)};Box(modifier){OutlinedButton({expanded=true},Modifier.fillMaxWidth()){Text(value.toString(),maxLines=1,overflow=TextOverflow.Ellipsis);Spacer(Modifier.weight(1f));Icon(Icons.Outlined.ArrowDropDown,null)};DropdownMenu(expanded,{expanded=false},Modifier.heightIn(max=320.dp)){values.forEach{v->DropdownMenuItem(text={Text(v.toString())},onClick={onValue(v);expanded=false})}}}}

@Composable private fun More(dark:Boolean,onDarkChanged:(Boolean)->Unit){val context=LocalContext.current;var events by remember{mutableStateOf(loadEvents(context))};var editing by remember{mutableStateOf<Event?>(null)};var dialog by remember{mutableStateOf(false)};var settings by remember{mutableStateOf(false)};LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=28.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Header("More","Personal events and app settings")};item{Button({editing=null;dialog=true},Modifier.padding(horizontal=16.dp).fillMaxWidth()){Icon(Icons.Outlined.Add,null);Spacer(Modifier.width(8.dp));Text("Add personal event")}};item{Card(Modifier.padding(horizontal=16.dp).fillMaxWidth(),RoundedCornerShape(22.dp)){Column(Modifier.padding(18.dp)){Text("Personal events",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Spacer(Modifier.height(8.dp));if(events.isEmpty())Text("No events yet. Add a date, time, category, notes and optional reminder.",color=MaterialTheme.colorScheme.onSurfaceVariant);events.sortedBy{it.date}.forEach{e->ListItem(headlineContent={Text(e.title,fontWeight=FontWeight.SemiBold)},supportingContent={Text("${e.date}${if(e.time.isNotBlank())" • ${e.time}" else ""} • ${e.category}")},trailingContent={Row{IconButton({editing=e;dialog=true}){Icon(Icons.Outlined.Edit,"Edit")};IconButton({events=events.filterNot{it.id==e.id};saveEvents(context,events)}){Icon(Icons.Outlined.DeleteOutline,"Delete")}}})}}}};item{OutlinedButton({settings=true},Modifier.padding(horizontal=16.dp).fillMaxWidth()){Icon(Icons.Outlined.Settings,null);Spacer(Modifier.width(8.dp));Text("Settings")}};item{InfoCard("Nepali Calendar","Designed for clear, fast access to Bikram Sambat dates. Developer: Rajeshwor Maharjan.",Icons.Outlined.Info)}};if(settings)SettingsDialog(dark,onDarkChanged){settings=false};if(dialog)EventDialog(editing,{saved->events=if(events.any{it.id==saved.id})events.map{if(it.id==saved.id)saved else it}else events+saved;saveEvents(context,events);dialog=false},{dialog=false})}

@Composable private fun SettingsDialog(dark:Boolean,onDarkChanged:(Boolean)->Unit,onDismiss:()->Unit){AlertDialog(onDismissRequest=onDismiss,title={Text("Settings")},text={Column{SettingRow("Dark theme",dark){onDarkChanged(!dark)}}},confirmButton={TextButton(onDismiss){Text("Done")}})}
@Composable private fun SettingRow(label:String,checked:Boolean,onClick:()->Unit){Row(Modifier.fillMaxWidth().clickable{onClick()},verticalAlignment=Alignment.CenterVertically){Text(label,Modifier.weight(1f));Switch(checked,onClick)}}

@Composable private fun EventDialog(initial:Event?,onSave:(Event)->Unit,onDismiss:()->Unit){var title by remember(initial){mutableStateOf(initial?.title ?: "")};var year by remember(initial){mutableIntStateOf(initial?.date?.split("-")?.getOrNull(0)?.toIntOrNull()?:2083)};var month by remember(initial){mutableIntStateOf(initial?.date?.split("-")?.getOrNull(1)?.toIntOrNull()?:6)};var day by remember(initial){mutableIntStateOf(initial?.date?.split("-")?.getOrNull(2)?.toIntOrNull()?:1)};var time by remember(initial){mutableStateOf(initial?.time?:"")};var category by remember(initial){mutableStateOf(initial?.category?:"Personal")};var notes by remember(initial){mutableStateOf(initial?.notes?:"")};var reminder by remember(initial){mutableStateOf(initial?.reminder?:false)};var error by remember{mutableStateOf("")};val context=LocalContext.current;AlertDialog(onDismissRequest=onDismiss,title={Text(if(initial==null)"Add personal event" else "Edit personal event")},text={Column(Modifier.fillMaxWidth().heightIn(max=520.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){OutlinedTextField(title,{title=it},Modifier.fillMaxWidth(),label={Text("Event title")},singleLine=true);Text("Date",fontWeight=FontWeight.SemiBold);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){DropdownField("Year",year,(2000..2090).toList(),{year=it},Modifier.weight(1.2f));DropdownField("Month",month,(1..12).toList(),{month=it},Modifier.weight(1f));DropdownField("Day",day,(1..32).toList(),{day=it},Modifier.weight(1f))};OutlinedButton({val c=Calendar.getInstance();TimePickerDialog(context,{_,h,m->time="%02d:%02d".format(h,m)},c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),true).show()},Modifier.fillMaxWidth()){Icon(Icons.Outlined.Schedule,null);Spacer(Modifier.width(8.dp));Text(if(time.isBlank())"Add time" else "Time: $time")};OutlinedTextField(category,{category=it},Modifier.fillMaxWidth(),label={Text("Category")},singleLine=true);OutlinedTextField(notes,{notes=it},Modifier.fillMaxWidth(),label={Text("Notes")},minLines=2,maxLines=4);SettingRow("Reminder",reminder){reminder=!reminder};if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)}},confirmButton={Button({if(title.isBlank())error="Enter an event title." else onSave(Event(initial?.id?:System.currentTimeMillis(),title.trim(),notes.trim(),"%04d-%02d-%02d".format(year,month,day),time,category.ifBlank{"Personal"},initial?.color?:DeepGreen.value.toLong(),reminder))}){Text("Save")}},dismissButton={TextButton(onDismiss){Text("Cancel")}})}

@Composable private fun ErrorCard(message:String){Card(Modifier.padding(horizontal=16.dp).fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.errorContainer),shape=RoundedCornerShape(18.dp)){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Outlined.ErrorOutline,null);Spacer(Modifier.width(10.dp));Text(message,color=MaterialTheme.colorScheme.onErrorContainer)}}}

private fun parseCalendar(year:Int,month:Int,raw:String,firstAd:String?):CalendarMonth{val arr=mutableListOf<CalendarDay>();fun text(o:JSONObject,vararg keys:String)=keys.firstNotNullOfOrNull{key->o.optString(key).takeIf{it.isNotBlank()}}?:"";fun walk(v:Any?){when(v){is JSONObject->{val day=parseDay(text(v,"n"),v.optInt("day",0));if(day in 1..32){val wd=v.optInt("d",v.optInt("weekday",v.optInt("weekdayIndex",0)));val weekday=if(wd in 1..7)wd-1 else wd.coerceIn(0,6);val ad=text(v,"ad","date","gregorian","englishDate").ifBlank{if(firstAd!=null)addDays(firstAd,day-1)else""};arr+=CalendarDay(day,ad,weekday,text(v,"f","festival","event","name","title"),v.optBoolean("h",v.optBoolean("holiday",false)),text(v,"t","tithi"))};val it=v.keys();while(it.hasNext())walk(v.get(it.next()))};is JSONArray->for(i in 0 until v.length())walk(v.get(i))}};walk(try{JSONObject(raw)}catch(_:Throwable){try{JSONArray(raw)}catch(_:Throwable){null}});val unique=arr.groupBy{it.day}.values.map{it.first()}.sortedBy{it.day};val days=unique.maxOfOrNull{it.day}?:30;val start=unique.firstOrNull()?.weekday?:0;val fallback=(1..days).map{d->CalendarDay(d,if(firstAd!=null)addDays(firstAd,d-1)else"",(start+d-1)%7)};return CalendarMonth(year,month,days,start,unique.ifEmpty{fallback})}
private fun parseDay(value:String,fallback:Int)=value.map{when(it){'०'->'0';'१'->'1';'२'->'2';'३'->'3';'४'->'4';'५'->'5';'६'->'6';'७'->'7';'८'->'8';'९'->'9';else->it}}.joinToString("").toIntOrNull()?:fallback
private fun extractIsoDate(raw:String):String?=Regex("\\b(\\d{4}-\\d{2}-\\d{2})\\b").find(raw)?.groupValues?.get(1)
private fun addDays(iso:String,days:Int):String{val f=SimpleDateFormat("yyyy-MM-dd",Locale.US).apply{isLenient=false;timeZone=TimeZone.getTimeZone("UTC")};val d=f.parse(iso)?:return iso;return Calendar.getInstance(TimeZone.getTimeZone("UTC"),Locale.US).apply{time=d;add(Calendar.DAY_OF_MONTH,days)}.let{f.format(it.time)}}
private fun todaySummary(raw:String):String=try{val root=JSONObject(raw);val data=root.optJSONObject("data")?:root;val bs=data.optJSONObject("bs");val ad=data.optJSONObject("ad");if(bs!=null&&ad!=null)"${bs.optString("monthName","BS")} ${bs.optInt("day")}, ${bs.optInt("year")}  •  ${ad.optString("date","")}" else root.optString("formatted").ifBlank{raw.take(180)}}catch(_:Throwable){raw.take(180)}
private fun conversionSummary(raw:String,bsMode:Boolean):String=try{val root=JSONObject(raw);val data=root.optJSONObject("data")?:root;val ad=data.optJSONObject("ad");val bs=data.optJSONObject("bs");if(bsMode&&ad!=null)"${ad.optString("date","")} • ${ad.optString("weekday","")}" else if(bs!=null)"${bs.optString("monthName","")} ${bs.optInt("day")}, ${bs.optInt("year")} • ${bs.optString("weekday","")}" else extractIsoDate(raw)?:raw.take(180)}catch(_:Throwable){extractIsoDate(raw)?:raw.take(180)}
private fun prefs(c:Context)=c.getSharedPreferences("events",Context.MODE_PRIVATE)
private fun loadEvents(c:Context):List<Event>=try{val a=JSONArray(prefs(c).getString("items","[]")?:"[]");(0 until a.length()).mapNotNull{i->a.optJSONObject(i)?.let{o->Event(o.optLong("id"),o.optString("title"),o.optString("notes"),o.optString("date"),o.optString("time"),o.optString("category","Personal"),o.optLong("color",DeepGreen.value.toLong()),o.optBoolean("reminder"))}}}catch(_:Throwable){emptyList()}
private fun saveEvents(c:Context,events:List<Event>){val a=JSONArray();events.forEach{e->a.put(JSONObject().apply{put("id",e.id);put("title",e.title);put("notes",e.notes);put("date",e.date);put("time",e.time);put("category",e.category);put("color",e.color);put("reminder",e.reminder)})};prefs(c).edit().putString("items",a.toString()).apply()}
