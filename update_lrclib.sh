cat << 'INNER_EOF' > app/src/main/java/com/example/data/LrcLibHelper.kt
package com.example.data

import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object LrcLibHelper {
    private const val TAG = "LrcLibHelper"

    fun fetchLyrics(title: String, artist: String, album: String = "", durationSec: Long = 0): String? {
        if (title.isBlank()) return null

        val cleanTitle = cleanTrackTitle(title)
        val cleanArtist = cleanArtistName(artist)

        // 1. Direct GET request with exact match
        if (cleanArtist.isNotBlank() && !isUnknownArtist(cleanArtist)) {
            val directResult = tryDirectGet(cleanTitle, cleanArtist, album, durationSec)
            if (!directResult.isNullOrBlank()) return directResult
        }

        // 2. Search query endpoint with (artist + title)
        val query = if (cleanArtist.isNotBlank() && !isUnknownArtist(cleanArtist)) {
            "$cleanArtist $cleanTitle"
        } else {
            cleanTitle
        }

        val searchResult = trySearch(query)
        if (!searchResult.isNullOrBlank()) return searchResult

        // 3. Fallback: Search with raw title if cleaned title differed
        if (cleanTitle != title.trim()) {
            val rawSearchResult = trySearch(title.trim())
            if (!rawSearchResult.isNullOrBlank()) return rawSearchResult
        }
        
        // 4. Aggressive Fallback: Strip ALL parentheses/brackets and search again
        val ultraCleanTitle = title.replace(Regex("\\s*\\[.*?\\]\\s*"), " ").replace(Regex("\\s*\\(.*?\\)\\s*"), " ").trim()
        if (ultraCleanTitle.isNotBlank() && ultraCleanTitle != cleanTitle && ultraCleanTitle != title.trim()) {
            val ultraQuery = if (cleanArtist.isNotBlank() && !isUnknownArtist(cleanArtist)) "$cleanArtist $ultraCleanTitle" else ultraCleanTitle
            val ultraResult = trySearch(ultraQuery)
            if (!ultraResult.isNullOrBlank()) return ultraResult
        }
        
        // 5. Ultimate Fallback: Just search the clean title and take first result blindly
        val ultimateResult = trySearch(cleanTitle)
        if (!ultimateResult.isNullOrBlank()) return ultimateResult
        
        // 6. External plain lyrics fallback (lyrics.ovh)
        if (cleanArtist.isNotBlank() && !isUnknownArtist(cleanArtist)) {
            val ovhResult = tryOvhFallback(cleanArtist, cleanTitle)
            if (!ovhResult.isNullOrBlank()) return ovhResult
        }

        return null
    }
    
    private fun tryOvhFallback(artist: String, title: String): String? {
        try {
            val urlStr = "https://api.lyrics.ovh/v1/" + URLEncoder.encode(artist, "UTF-8") + "/" + URLEncoder.encode(title, "UTF-8")
            val connection = URL(urlStr).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 4000
            connection.readTimeout = 4000

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val plain = json.optString("lyrics", "").trim()
                if (plain.isNotBlank()) return plain
            }
        } catch (e: Exception) {
            Log.d(TAG, "OVH fallback error for $title: ${e.message}")
        }
        return null
    }

    private fun tryDirectGet(title: String, artist: String, album: String, durationSec: Long): String? {
        try {
            var urlStr = "https://lrclib.net/api/get?track_name=" + URLEncoder.encode(title, "UTF-8") + "&artist_name=" + URLEncoder.encode(artist, "UTF-8")
            if (album.isNotBlank() && !album.equals("Unknown Album", ignoreCase = true)) {
                urlStr += "&album_name=" + URLEncoder.encode(album, "UTF-8")
            }
            if (durationSec > 0) {
                urlStr += "&duration=" + durationSec
            }

            val connection = URL(urlStr).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "FuzionMusicPlayer/1.0 (Android; support@example.com)")
            connection.connectTimeout = 4000
            connection.readTimeout = 4000

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val synced = json.optString("syncedLyrics", "").trim()
                if (synced.isNotBlank()) return synced
                val plain = json.optString("plainLyrics", "").trim()
                if (plain.isNotBlank()) return plain
            }
        } catch (e: Exception) {
            Log.d(TAG, "Direct GET error for $title: ${e.message}")
        }
        return null
    }

    private fun trySearch(query: String): String? {
        try {
            val urlStr = "https://lrclib.net/api/search?q=" + URLEncoder.encode(query, "UTF-8")
            val connection = URL(urlStr).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "FuzionMusicPlayer/1.0 (Android; support@example.com)")
            connection.connectTimeout = 4000
            connection.readTimeout = 4000

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)
                if (jsonArray.length() == 0) return null

                // We want the most relevant result (first one) that has synced lyrics.
                // If none have synced, we take the first plain lyrics.
                var firstPlain: String? = null

                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val synced = item.optString("syncedLyrics", "").trim()
                    val plain = item.optString("plainLyrics", "").trim()

                    if (synced.isNotBlank()) return synced
                    if (firstPlain == null && plain.isNotBlank()) {
                        firstPlain = plain
                    }
                }
                
                return firstPlain
            }
        } catch (e: Exception) {
            Log.d(TAG, "Search query error for $query: ${e.message}")
        }
        return null
    }

    private fun isUnknownArtist(artist: String): Boolean {
        val a = artist.trim().lowercase()
        return a.isEmpty() || a == "unknown" || a == "unknown artist" || a == "<unknown>" || a == "local artist" || a == "artista desconocido"
    }

    private fun cleanTrackTitle(raw: String): String {
        var t = raw.trim()
        // Remove file extensions if in title
        val extRegex = Regex("\\.(mp3|flac|m4a|wav|ogg|opus|aac|wma)$", RegexOption.IGNORE_CASE)
        t = t.replace(extRegex, "")

        // Remove leading track numbers like "01. ", "01 - ", "1- ", "01 "
        t = t.replace(Regex("^[0-9]{1,3}[.\\-\\s_]+"), "")

        // Remove trailing tags like "(Official Video)", "[Lyrics]", "(Remastered 2021)", "(HD)"
        t = t.replace(Regex("(?i)\\s*[\\[\\(](?:official|audio|video|lyrics|hd|4k|remaster(?:ed)?|live|bonus track|feat|ft)[^\\]\\)]*[\\]\\)]"), "")
        
        return t.trim()
    }

    private fun cleanArtistName(raw: String): String {
        var a = raw.trim()
        if (isUnknownArtist(a)) return ""
        // Remove "feat. ..." or "ft. ..."
        a = a.replace(Regex("(?i)\\s+(?:feat\\.|ft\\.|featuring)\\s+.*$"), "")
        return a.trim()
    }
}
INNER_EOF
