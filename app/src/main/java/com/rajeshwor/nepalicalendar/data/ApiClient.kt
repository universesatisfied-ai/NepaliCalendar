package com.rajeshwor.nepalicalendar.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class ApiClient {
    private val client=OkHttpClient()
    private suspend fun get(url:String):String=withContext(Dispatchers.IO){
        client.newCall(Request.Builder().url(url).build()).execute().use { r -> if(!r.isSuccessful) error("HTTP ${r.code}"); r.body?.string() ?: "" }
    }
    suspend fun month(year:Int, month:Int):String = get("https://api-nepalicalendar.leapcell.app/calendar/$year/$month")
    suspend fun year(year:Int):String = get("https://api-nepalicalendar.leapcell.app/calendar/$year")
    suspend fun today():String = get("https://npdates.org/api/today")
    suspend fun convert(endpoint:String, body:String):String = withContext(Dispatchers.IO){
        val req=Request.Builder().url("https://npdates.org/api/$endpoint").post(okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/json"),body)).build()
        client.newCall(req).execute().use { r -> if(!r.isSuccessful) error("HTTP ${r.code}"); r.body?.string() ?: "" }
    }
    fun prettyJson(raw:String):String=try{JSONObject(raw).toString(2)}catch(_:Throwable){try{JSONArray(raw).toString(2)}catch(_:Throwable){raw}}
}
