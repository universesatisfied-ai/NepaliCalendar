package com.rajeshwor.nepalicalendar.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class ApiClient {
    private val client = OkHttpClient()
    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().url(url).build()).execute().use { r ->
            if (!r.isSuccessful) error("HTTP ${r.code}")
            r.body?.string() ?: ""
        }
    }
    suspend fun month(year: Int, month: Int) = get("https://api-nepalicalendar.leapcell.app/calendar/$year/$month")
    suspend fun year(year: Int) = get("https://api-nepalicalendar.leapcell.app/calendar/$year")
    suspend fun today() = get("https://npdates.org/api/today")
    suspend fun bsToAd(y: Int, m: Int, d: Int) = get("https://npdates.org/api/convert/bs-to-ad?year=$y&month=$m&day=$d")
    suspend fun adToBs(y: Int, m: Int, d: Int) = get("https://npdates.org/api/convert/ad-to-bs?year=$y&month=$m&day=$d")
    fun prettyJson(raw: String): String = try { JSONObject(raw).toString(2) } catch (_: Throwable) { try { JSONArray(raw).toString(2) } catch (_: Throwable) { raw } }
}
