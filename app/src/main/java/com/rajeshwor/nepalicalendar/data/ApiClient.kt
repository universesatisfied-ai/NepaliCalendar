package com.rajeshwor.nepalicalendar.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class ApiClient(context: Context? = null) {
    private val client = OkHttpClient.Builder().retryOnConnectionFailure(true).build()
    private val cache = context?.getSharedPreferences("api_cache", Context.MODE_PRIVATE)

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().url(url).header("Accept", "application/json").build()).execute().use { r ->
            if (!r.isSuccessful) error("HTTP ${r.code}")
            r.body?.string()?.takeIf { it.isNotBlank() } ?: error("Empty response")
        }
    }

    private suspend fun cachedGet(key: String, urls: List<String>): String {
        var last: Throwable? = null
        for (url in urls) {
            try {
                val value = get(url)
                cache?.edit()?.putString(key, value)?.apply()
                return value
            } catch (t: Throwable) { last = t }
        }
        cache?.getString(key, null)?.takeIf { it.isNotBlank() }?.let { return it }
        throw last ?: IllegalStateException("Calendar service unavailable")
    }

    suspend fun month(year: Int, month: Int) = cachedGet(
        "month_${year}_$month",
        listOf(
            "https://api-nepalicalendar.leapcell.app/calendar/$year/$month",
            "https://calendar.newsbihani.com/api/calendar?year=$year&month=$month"
        )
    )

    suspend fun today() = cachedGet("today", listOf("https://usemiti.com/api/today", "https://npdates.org/api/today"))

    suspend fun bsToAd(y: Int, m: Int, d: Int) = cachedGet(
        "bs_${y}_${m}_${d}",
        listOf(
            "https://calendar.newsbihani.com/api/convert?from=bs&date=%04d-%02d-%02d".format(y, m, d),
            "https://npdates.org/api/convert/bs-to-ad?year=$y&month=$m&day=$d"
        )
    )

    suspend fun adToBs(y: Int, m: Int, d: Int) = cachedGet(
        "ad_${y}_${m}_${d}",
        listOf(
            "https://calendar.newsbihani.com/api/convert?from=ad&date=%04d-%02d-%02d".format(y, m, d),
            "https://npdates.org/api/convert/ad-to-bs?year=$y&month=$m&day=$d"
        )
    )

    fun prettyJson(raw: String) = try { JSONObject(raw).toString(2) } catch (_: Throwable) { raw }
}
