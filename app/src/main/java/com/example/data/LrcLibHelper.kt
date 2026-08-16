package com.example.data

import android.net.Uri
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object LrcLibHelper {
    fun fetchLyrics(title: String, artist: String): String? {
        if (title.isBlank() || artist.isBlank()) return null
        try {
            val urlString = "https://lrclib.net/api/get?track_name=" + Uri.encode(title) + "&artist_name=" + Uri.encode(artist)
            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "FuzionPlayer/1.0")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val syncedLyrics = json.optString("syncedLyrics", "")
                if (syncedLyrics.isNotBlank()) return syncedLyrics
                val plainLyrics = json.optString("plainLyrics", "")
                if (plainLyrics.isNotBlank()) return plainLyrics
            }
        } catch (e: Exception) {
            Log.e("LrcLibHelper", "Error fetching lyrics", e)
        }
        return null
    }
}
