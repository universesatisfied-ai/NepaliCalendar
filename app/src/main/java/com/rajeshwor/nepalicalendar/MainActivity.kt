package com.rajeshwor.nepalicalendar

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as columnItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rajeshwor.nepalicalendar.data.ApiClient
import com.rajeshwor.nepalicalendar.model.CalendarDay
import com.rajeshwor.nepalicalendar.model.CalendarMonth
import com.rajeshwor.nepalicalendar.model.Event
import com.rajeshwor.nepalicalendar.navigation.Dest
import com.rajeshwor.nepalicalendar.ui.components.ActionCard
import com.rajeshwor.nepalicalendar.ui.components.BrandHeader
import com.rajeshwor.nepalicalendar.ui.components.Pill
import com.rajeshwor.nepalicalendar.ui.theme.DeepGreen
import com.rajeshwor.nepalicalendar.ui.theme.Lime
import com.rajeshwor.nepalicalendar.ui.theme.NepaliCalendarTheme
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { NepaliCalendarRoot() }
    }
}

private val monthNames = listOf("Baisakh", "Jestha", "Ashadh", "Shrawan", "Bhadra", "Ashwin", "Kartik", "Mangsir", "Poush", "Magh", "Falgun", "Chaitra")
private val weekNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

@Composable
private fun NepaliCalendarRoot() {
    val context = LocalContext.current
    var dark by rememberSaveable { mutableStateOf(context.getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean("dark", false)) }
    NepaliCalendarTheme(dark = dark) {
        App(dark = dark, onDarkChanged = {
            dark = it
            context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("dark", it).apply()
        })
    }
}

@Composable
private fun App(dark: Boolean, onDarkChanged: (Boolean) -> Unit) {
    var destination by rememberSaveable { mutableStateOf(Dest.HOME) }
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                Dest.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, maxLines = 1) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (destination) {
                Dest.HOME -> Home(onCalendar = { destination = Dest.CALENDAR }, onConvert = { destination = Dest.CONVERT }, onFestivals = { destination = Dest.FESTIVALS })
                Dest.CALENDAR -> CalendarScreen()
                Dest.FESTIVALS -> Festivals()
                Dest.CONVERT -> Converter()
                Dest.MORE -> More(dark, onDarkChanged)
            }
        }
    }
}

@Composable
private fun ScreenTitle(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Home(onCalendar: () -> Unit, onConvert: () -> Unit, onFestivals: () -> Unit) {
    val api = remember { ApiClient(LocalContext.current) }
    val scope = rememberCoroutineScope()
    var todayRaw by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var online by remember { mutableStateOf(false) }

    fun refresh() {
        scope.launch {
            loading = true
            try { todayRaw = api.today(); online = true } catch (_: Throwable) { online = false }
            loading = false
        }
    }
    LaunchedEffect(Unit) { refresh() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { BrandHeader("Nepali Calendar", "by Rajeshwor Maharjan") }
        item {
            Card(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(Modifier.padding(22.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("TODAY", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .72f), fontWeight = FontWeight.Bold)
                        Pill(if (online) "Online" else "Offline", online)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(SimpleDateFormat("EEEE, d MMMM yyyy", Locale.US).format(Date()), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.height(8.dp))
                    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Lime)
                    else Text(extractSummary(todayRaw), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeAction("Calendar", Icons.Outlined.CalendarMonth, onCalendar, Modifier.weight(1f))
                HomeAction("Convert", Icons.Outlined.SwapHoriz, onConvert, Modifier.weight(1f))
                HomeAction("Festivals", Icons.Outlined.Event, onFestivals, Modifier.weight(1f))
            }
        }
        item { ActionCard("Refresh today data", Icons.Outlined.Refresh, ::refresh) }
        item {
            Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Data status", fontWeight = FontWeight.SemiBold)
                        Text(if (online) "Connected to the calendar service." else "No connection. Try refresh when online.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.height(94.dp), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(7.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun CalendarScreen() {
    val api = remember { ApiClient(LocalContext.current) }
    var year by rememberSaveable { mutableIntStateOf(2083) }
    var month by rememberSaveable { mutableIntStateOf(6) }
    var selected by rememberSaveable { mutableIntStateOf(1) }
    var data by remember { mutableStateOf(CalendarMonth(year, month, 30)) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(year, month) {
        loading = true; error = false
        try {
            val raw = api.month(year, month)
            val firstAd = try { extractIsoDate(api.bsToAd(year, month, 1)) } catch (_: Throwable) { null }
            data = parseCalendar(year, month, raw, firstAd)
        } catch (_: Throwable) {
            error = true
            data = CalendarMonth(year, month, 30)
        }
        selected = selected.coerceIn(1, data.days)
        loading = false
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenTitle("Calendar", "Bikram Sambat") }
        item {
            Row(Modifier.padding(horizontal = 12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton({ if (month == 1) { year--; month = 12 } else month-- }) { Icon(Icons.Outlined.ChevronLeft, "Previous month") }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${monthNames[month - 1]} $year", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(.55f))
                }
                IconButton({ if (month == 12) { year++; month = 1 } else month++ }) { Icon(Icons.Outlined.ChevronRight, "Next month") }
            }
        }
        item { TextButton({ year = 2083; month = 6; selected = 1 }) { Text("Today") } }
        if (error) item { StatusCard("Could not load this month. Showing a safe fallback calendar.") }
        item {
            Row(Modifier.padding(horizontal = 14.dp).fillMaxWidth()) {
                weekNames.forEach { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        item { CalendarGrid(data, selected) { selected = it } }
        item {
            val entry = data.entries.firstOrNull { it.day == selected }
            Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Selected date", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text("$selected ${monthNames[month - 1]} $year", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    if (entry != null) {
                        if (entry.ad.isNotBlank()) Text("AD ${entry.ad}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (entry.festival.isNotBlank()) Text(entry.festival, Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        if (entry.tithi.isNotBlank()) Text(entry.tithi, Modifier.padding(top = 3.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarGrid(data: CalendarMonth, selected: Int, onSelect: (Int) -> Unit) {
    val cells = List(data.startWeekday.coerceIn(0, 6)) { CalendarDay(0) } + data.entries
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.padding(horizontal = 10.dp).fillMaxWidth().heightIn(min = 300.dp, max = 390.dp),
        userScrollEnabled = false,
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        gridItems(cells) { entry ->
            if (entry.day == 0) Box(Modifier.height(48.dp))
            else {
                val holiday = entry.holiday || entry.weekday == 6
                val selectedDay = selected == entry.day
                Box(Modifier.padding(2.dp).height(48.dp).fillMaxWidth().clickable { onSelect(entry.day) }, contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(13.dp),
                        color = when { selectedDay -> MaterialTheme.colorScheme.primary; holiday -> MaterialTheme.colorScheme.errorContainer; else -> MaterialTheme.colorScheme.surfaceVariant }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text("${entry.day}", fontWeight = FontWeight.Bold, color = if (selectedDay) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                            if (entry.festival.isNotBlank()) Text("•", color = if (selectedDay) Lime else MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Festivals() {
    val api = remember { ApiClient(LocalContext.current) }
    val scope = rememberCoroutineScope()
    var year by rememberSaveable { mutableIntStateOf(2083) }
    var month by rememberSaveable { mutableIntStateOf(6) }
    var items by remember { mutableStateOf<List<CalendarDay>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var search by rememberSaveable { mutableStateOf("") }
    var holidaysOnly by rememberSaveable { mutableStateOf(false) }

    fun load() {
        scope.launch {
            loading = true
            try {
                val raw = api.month(year, month)
                items = parseCalendar(year, month, raw, null).entries.filter { it.festival.isNotBlank() || it.holiday }
            } catch (_: Throwable) { items = emptyList() }
            loading = false
        }
    }
    LaunchedEffect(year, month) { load() }
    val filtered = items.filter { (!holidaysOnly || it.holiday) && (search.isBlank() || it.festival.contains(search, true) || it.day.toString() == search.trim()) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenTitle("Festivals & Holidays", "Important dates from the selected BS month") }
        item {
            Row(Modifier.padding(horizontal = 12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton({ if (month == 1) { year--; month = 12 } else month-- }) { Icon(Icons.Outlined.ChevronLeft, "Previous month") }
                Text("${monthNames[month - 1]} $year", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                IconButton({ if (month == 12) { year++; month = 1 } else month++ }) { Icon(Icons.Outlined.ChevronRight, "Next month") }
            }
        }
        item { OutlinedTextField(search, { search = it }, Modifier.padding(horizontal = 16.dp).fillMaxWidth(), singleLine = true, label = { Text("Search festivals") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }) }
        item { FilterChip(holidaysOnly, { holidaysOnly = !holidaysOnly }, label = { Text("Holidays only") }, leadingIcon = if (holidaysOnly) ({ Icon(Icons.Outlined.Check, null) }) else null, modifier = Modifier.padding(horizontal = 16.dp)) }
        if (loading) item { Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        else if (filtered.isEmpty()) item { StatusCard("No matching festivals were found for this month.") }
        else columnItems(filtered) { entry ->
            Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(44.dp), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Event, null, tint = MaterialTheme.colorScheme.primary) } }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(entry.festival.ifBlank { "Public holiday" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${entry.day} ${monthNames[month - 1]} $year${if (entry.ad.isNotBlank()) " • AD ${entry.ad}" else ""}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (entry.holiday) AssistChip(onClick = {}, label = { Text("Holiday") })
                }
            }
        }
    }
}

@Composable
private fun Converter() {
    val api = remember { ApiClient(LocalContext.current) }
    val scope = rememberCoroutineScope()
    var input by rememberSaveable { mutableStateOf("") }
    var bsToAd by rememberSaveable { mutableStateOf(true) }
    var result by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf("") }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenTitle("Date Converter", "Bikram Sambat ↔ Gregorian") }
        item {
            Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(26.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Text(if (bsToAd) "BS → AD" else "AD → BS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(input, { input = it; error = "" }, Modifier.fillMaxWidth(), label = { Text(if (bsToAd) "BS date · YYYY-MM-DD" else "AD date · YYYY-MM-DD") }, singleLine = true)
                    if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            scope.launch {
                                busy = true
                                result = ""
                                error = ""
                                try {
                                    val p = parseDateInput(input)
                                    val raw = if (bsToAd) api.bsToAd(p[0], p[1], p[2]) else api.adToBs(p[0], p[1], p[2])
                                    result = extractSummary(raw)
                                } catch (_: Throwable) { error = "Enter a valid date and check your connection." }
                                busy = false
                            }
                        }, modifier = Modifier.weight(1f)) {
                            if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Convert")
                        }
                        OutlinedButton(onClick = { bsToAd = !bsToAd; result = "" }) { Icon(Icons.Outlined.SwapHoriz, "Swap") }
                    }
                    TextButton(onClick = { input = ""; result = ""; error = "" }, modifier = Modifier.align(Alignment.End)) { Text("Clear") }
                }
            }
        }
        if (result.isNotBlank()) item {
            Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(18.dp)) { Text("Result", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text(result, style = MaterialTheme.typography.titleMedium) } }
        }
    }
}

@Composable
private fun More(dark: Boolean, onDarkChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var events by remember { mutableStateOf(loadEvents(context)) }
    var editing by remember { mutableStateOf<Event?>(null) }
    var showEventDialog by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenTitle("More", "Settings, personal events and app information") }
        item { ActionCard("Add personal event", Icons.Outlined.Add) { editing = null; showEventDialog = true } }
        item {
            Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Personal events", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    if (events.isEmpty()) Text("No events yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    events.sortedBy { it.date }.forEach { event ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(event.title, fontWeight = FontWeight.SemiBold); Text("${event.date}${if (event.time.isNotBlank()) " • ${event.time}" else ""}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            IconButton({ editing = event; showEventDialog = true }) { Icon(Icons.Outlined.Event, "Edit event") }
                            IconButton({ events = events.filterNot { it.id == event.id }; saveEvents(context, events) }) { Icon(Icons.Outlined.DeleteOutline, "Delete event") }
                        }
                    }
                }
            }
        }
        item { ActionCard("App settings", Icons.Outlined.Settings) { showSettings = true } }
        item {
            Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Nepali Calendar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("by Rajeshwor Maharjan", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp)); Divider(); Spacer(Modifier.height(12.dp))
                    Text("Calendar data is retrieved from the configured online services. The app caches the latest successful calendar data for use when a request fails.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (showSettings) SettingsDialog(dark, onDarkChanged) { showSettings = false }
    if (showEventDialog) {
        EventDialog(
            initial = editing,
            onSave = { saved ->
                events = if (events.any { it.id == saved.id }) {
                    events.map { if (it.id == saved.id) saved else it }
                } else {
                    events + saved
                }
                saveEvents(context, events)
                showEventDialog = false
            },
            onDismiss = { showEventDialog = false }
        )
    }
}

@Composable
private fun SettingsDialog(dark: Boolean, onDarkChanged: (Boolean) -> Unit, onDismiss: () -> Unit) {
    var showHolidays by rememberSaveable { mutableStateOf(true) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Settings") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingRow("Dark theme", dark) { onDarkChanged(!dark) }
            SettingRow("Show holidays", showHolidays) { showHolidays = !showHolidays }
            Text("Version 1.0.0", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } })
}

@Composable
private fun SettingRow(label: String, checked: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        FilterChip(checked, onClick, label = { Text(if (checked) "On" else "Off") })
    }
}

@Composable
private fun EventDialog(initial: Event?, onSave: (Event) -> Unit, onDismiss: () -> Unit) {
    var title by remember(initial) { mutableStateOf(initial?.title ?: "") }
    var date by remember(initial) { mutableStateOf(initial?.date ?: "") }
    var time by remember(initial) { mutableStateOf(initial?.time ?: "") }
    var notes by remember(initial) { mutableStateOf(initial?.notes ?: "") }
    var category by remember(initial) { mutableStateOf(initial?.category ?: "Personal") }
    var reminder by remember(initial) { mutableStateOf(initial?.reminder ?: false) }
    var error by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (initial == null) "Add event" else "Edit event") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Title") }, singleLine = true)
            OutlinedTextField(date, { date = it }, Modifier.fillMaxWidth(), label = { Text("BS date · YYYY-MM-DD") }, singleLine = true)
            OutlinedTextField(time, { time = it }, Modifier.fillMaxWidth(), label = { Text("Time · optional") }, singleLine = true)
            OutlinedTextField(category, { category = it }, Modifier.fillMaxWidth(), label = { Text("Category") }, singleLine = true)
            SettingRow("Reminder", reminder) { reminder = !reminder }
            OutlinedTextField(notes, { notes = it }, Modifier.fillMaxWidth(), label = { Text("Notes") }, minLines = 2, maxLines = 4)
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = { Button(onClick = {
        if (title.isBlank() || !Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(date.trim())) error = "Enter a title and valid BS date."
        else onSave(Event(initial?.id ?: System.currentTimeMillis(), title.trim(), notes.trim(), date.trim(), time.trim(), category.trim().ifBlank { "Personal" }, initial?.color ?: 0xFF18251DL, reminder))
    }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun StatusCard(message: String) {
    Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(20.dp)) { Text(message, Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

private fun parseDateInput(value: String): IntArray {
    val p = value.trim().split("-").map { it.toInt() }
    require(p.size == 3 && p[0] in 1900..2200 && p[1] in 1..12 && p[2] in 1..32)
    return intArrayOf(p[0], p[1], p[2])
}

private fun parseCalendar(year: Int, month: Int, raw: String, firstAd: String?): CalendarMonth {
    val arr = mutableListOf<CalendarDay>()
    val root: Any? = try { JSONObject(raw) } catch (_: Throwable) { try { JSONArray(raw) } catch (_: Throwable) { null } }
    fun text(o: JSONObject, vararg keys: String): String = keys.firstNotNullOfOrNull { key -> o.optString(key).takeIf { it.isNotBlank() } } ?: ""
    fun walk(v: Any?) {
        when (v) {
            is JSONObject -> {
                val day = parseDay(text(v, "n"), v.optInt("day", 0))
                if (day in 1..32) {
                    val apiWeekday = v.optInt("d", v.optInt("weekday", 0))
                    val weekday = if (apiWeekday in 1..7) apiWeekday - 1 else apiWeekday.coerceIn(0, 6)
                    arr += CalendarDay(day, if (firstAd != null) addDays(firstAd, day - 1) else text(v, "ad", "gregorian", "englishDate"), weekday, text(v, "f", "festival", "event", "name"), v.optBoolean("h", v.optBoolean("holiday", false)), text(v, "t", "tithi"))
                }
                val keys = v.keys(); while (keys.hasNext()) walk(v.get(keys.next()))
            }
            is JSONArray -> for (i in 0 until v.length()) walk(v.get(i))
        }
    }
    walk(root)
    val unique = arr.groupBy { it.day }.values.map { it.first() }.sortedBy { it.day }
    val days = unique.maxOfOrNull { it.day } ?: 30
    val start = unique.firstOrNull()?.weekday ?: 0
    val fallback = (1..days).map { d -> CalendarDay(d, if (firstAd != null) addDays(firstAd, d - 1) else "", (start + d - 1) % 7) }
    return CalendarMonth(year, month, days, start, unique.ifEmpty { fallback })
}

private fun parseDay(value: String, fallback: Int): Int {
    if (value.isBlank()) return fallback
    val normalized = value.map { c -> when (c) { '०' -> '0'; '१' -> '1'; '२' -> '2'; '३' -> '3'; '४' -> '4'; '५' -> '5'; '६' -> '6'; '७' -> '7'; '८' -> '8'; '९' -> '9'; else -> c } }.joinToString("")
    return normalized.toIntOrNull() ?: fallback
}

private fun extractIsoDate(raw: String): String? = Regex("\\b(\\d{4}-\\d{2}-\\d{2})\\b").find(raw)?.groupValues?.get(1)

private fun addDays(iso: String, days: Int): String {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false; timeZone = TimeZone.getTimeZone("UTC") }
    val date = format.parse(iso) ?: return iso
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US).apply { time = date; add(Calendar.DAY_OF_MONTH, days) }
    return format.format(calendar.time)
}

private fun extractSummary(raw: String): String {
    if (raw.isBlank()) return "Unavailable"
    return try {
        val o = JSONObject(raw)
        listOf("formatted", "bs", "date", "nepali", "today").firstNotNullOfOrNull { key -> o.optString(key).takeIf { it.isNotBlank() } } ?: o.toString().replace("\\n", " ").take(220)
    } catch (_: Throwable) { raw.replace("\\n", " ").take(220) }
}

private fun prefs(context: Context) = context.getSharedPreferences("events", Context.MODE_PRIVATE)
private fun loadEvents(context: Context): List<Event> = try {
    val array = JSONArray(prefs(context).getString("items", "[]") ?: "[]")
    (0 until array.length()).mapNotNull { i ->
        val o = array.optJSONObject(i) ?: return@mapNotNull null
        Event(o.optLong("id"), o.optString("title"), o.optString("notes"), o.optString("date"), o.optString("time"), o.optString("category", "Personal"), o.optLong("color", 0xFF18251DL), o.optBoolean("reminder", false))
    }
} catch (_: Throwable) { emptyList() }

private fun saveEvents(context: Context, events: List<Event>) {
    val array = JSONArray()
    events.forEach { event -> array.put(JSONObject().apply { put("id", event.id); put("title", event.title); put("notes", event.notes); put("date", event.date); put("time", event.time); put("category", event.category); put("color", event.color); put("reminder", event.reminder) }) }
    prefs(context).edit().putString("items", array.toString()).apply()
}
