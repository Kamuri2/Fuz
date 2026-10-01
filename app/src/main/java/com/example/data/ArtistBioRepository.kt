package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Repository implementing the two-step Wikipedia search strategy with anchor terms
 * ("cantante" / "banda") to resolve musical artist biographies without dictionary definitions
 * or disambiguation pages.
 */
class ArtistBioRepository {
    companion object {
        private val instance = ArtistBioRepository()
        suspend fun getArtistBiography(artistName: String): String? = instance.getArtistBiography(artistName)
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private val musicKeywords = listOf(
        "banda", "música", "musica", "music", "músico", "musico", "cantante", "singer",
        "álbum", "album", "canción", "cancion", "song", "rock", "pop", "hip hop",
        "metal", "jazz", "grupo", "band", "vocalista", "guitarrista", "compositor",
        "cantautor", "discografía", "discografia", "sencillo", "single", "orquesta",
        "discográfica", "gira", "concierto", "baterista", "bajista", "rap", "trap",
        "reggaetón", "reggaeton", "solista", "productor", "discográfico"
    )

    suspend fun getArtistBiography(artistName: String): String? = withContext(Dispatchers.IO) {
        val cleanArtist = artistName.trim()
        if (cleanArtist.isBlank() || isGenericArtist(cleanArtist)) {
            return@withContext null
        }

        try {
            // 1. Intento directo en la API de resúmenes de Wikipedia (Español)
            val encodedName = URLEncoder.encode(cleanArtist, "UTF-8")
            var url = "https://es.wikipedia.org/api/rest_v1/page/summary/$encodedName"
            var responseJson = fetchJson(url)

            // 2. Validar si es desambiguación, error de no encontrado, o definición no musical
            var type = responseJson?.optString("type")
            var title = responseJson?.optString("title")
            var extract = responseJson?.optString("extract")

            val needsRefinedSearch = type == "disambiguation" ||
                    title == "Not found" ||
                    type?.contains("not_found", ignoreCase = true) == true ||
                    responseJson == null ||
                    extract.isNullOrBlank() ||
                    isNonMusicDefinition(extract)

            if (needsRefinedSearch) {
                // 3. Búsqueda Refinada: agregamos "cantante", "banda" o "músico" al query
                val anchorTerms = listOf("cantante", "banda", "músico")
                for (anchor in anchorTerms) {
                    val searchName = URLEncoder.encode("$cleanArtist $anchor", "UTF-8")
                    val searchUrl = "https://es.wikipedia.org/w/api.php?action=query&list=search&srsearch=$searchName&utf8=&format=json&origin=*"
                    val searchJson = fetchJson(searchUrl)

                    val searchResults = searchJson?.optJSONObject("query")?.optJSONArray("search")
                    if (searchResults != null && searchResults.length() > 0) {
                        // Tomamos el título del resultado más relevante
                        val bestTitle = searchResults.getJSONObject(0).optString("title")
                        if (bestTitle.isNotBlank()) {
                            val encodedBestTitle = URLEncoder.encode(bestTitle, "UTF-8")
                            // 4. Volvemos a consultar el resumen con el título exacto
                            val refinedUrl = "https://es.wikipedia.org/api/rest_v1/page/summary/$encodedBestTitle"
                            val refinedJson = fetchJson(refinedUrl)
                            val candidateType = refinedJson?.optString("type")
                            val candidateExtract = refinedJson?.optString("extract")

                            if (refinedJson != null &&
                                !candidateExtract.isNullOrBlank() &&
                                candidateType != "disambiguation" &&
                                !isNonMusicDefinition(candidateExtract)
                            ) {
                                responseJson = refinedJson
                                extract = candidateExtract
                                break
                            }
                        }
                    }
                }
            }

            // Validamos que el resultado final sea válido y extraemos el texto
            val finalType = responseJson?.optString("type")
            val finalExtract = responseJson?.optString("extract")
            if (responseJson != null &&
                !finalExtract.isNullOrBlank() &&
                finalType != "disambiguation" &&
                !isNonMusicDefinition(finalExtract)
            ) {
                return@withContext sanitizeExtract(finalExtract)
            }

            // Fallback a Wikipedia en inglés si en español falló o no se encontró
            val enBio = fetchEnglishWikipediaBio(cleanArtist)
            if (!enBio.isNullOrBlank()) {
                return@withContext sanitizeExtract(enBio)
            }

            return@withContext null
        } catch (e: Exception) {
            Log.e("ArtistBioRepository", "Error resolving biography for '$cleanArtist': ${e.message}")
            return@withContext null
        }
    }

    private fun fetchEnglishWikipediaBio(artistName: String): String? {
        return try {
            val encodedName = URLEncoder.encode(artistName, "UTF-8")
            var url = "https://en.wikipedia.org/api/rest_v1/page/summary/$encodedName"
            var responseJson = fetchJson(url)

            var type = responseJson?.optString("type")
            var title = responseJson?.optString("title")
            var extract = responseJson?.optString("extract")

            val needsRefinement = type == "disambiguation" ||
                    title == "Not found" ||
                    type?.contains("not_found", ignoreCase = true) == true ||
                    responseJson == null ||
                    extract.isNullOrBlank() ||
                    isNonMusicDefinition(extract)

            if (needsRefinement) {
                val anchorTerms = listOf("band", "singer", "musician")
                for (anchor in anchorTerms) {
                    val searchName = URLEncoder.encode("$artistName $anchor", "UTF-8")
                    val searchUrl = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$searchName&utf8=&format=json&origin=*"
                    val searchJson = fetchJson(searchUrl)
                    val searchResults = searchJson?.optJSONObject("query")?.optJSONArray("search")
                    if (searchResults != null && searchResults.length() > 0) {
                        val bestTitle = searchResults.getJSONObject(0).optString("title")
                        if (bestTitle.isNotBlank()) {
                            val encodedBestTitle = URLEncoder.encode(bestTitle, "UTF-8")
                            val refinedUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/$encodedBestTitle"
                            val refinedJson = fetchJson(refinedUrl)
                            val candidateType = refinedJson?.optString("type")
                            val candidateExtract = refinedJson?.optString("extract")
                            if (refinedJson != null &&
                                !candidateExtract.isNullOrBlank() &&
                                candidateType != "disambiguation" &&
                                !isNonMusicDefinition(candidateExtract)
                            ) {
                                responseJson = refinedJson
                                break
                            }
                        }
                    }
                }
            }

            val finalType = responseJson?.optString("type")
            val finalExtract = responseJson?.optString("extract")
            if (responseJson != null &&
                !finalExtract.isNullOrBlank() &&
                finalType != "disambiguation" &&
                !isNonMusicDefinition(finalExtract)
            ) {
                finalExtract
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun isNonMusicDefinition(text: String?): Boolean {
        if (text.isNullOrBlank()) return true
        val lower = text.lowercase().trim()

        // Discard placeholder errors or raw search output
        if (lower.startsWith("searched for") || lower.contains("searched for \"")) return true
        if (lower.contains("puede referirse a:") || lower.contains("may refer to:")) return true
        if (lower.contains("desambiguación") || lower.contains("disambiguation")) return true

        // Discard common dictionary definitions
        if (lower.startsWith("a tool is a device") || lower.startsWith("tool is a device")) return true
        if (lower.contains("herramienta de mano") || lower.contains("dispositivo o instrumento que se utiliza")) return true
        if (lower.contains("primer libro de la biblia") || lower.contains("primer libro del pentateuco")) return true
        if (lower.contains("libro del génesis") || lower.contains("libro de génesis")) return true
        if (lower.contains("paraje de un desierto") || lower.contains("oasis es un paraje")) return true

        // Verify musical relevance: valid artist extracts contain at least one music-related anchor keyword
        val hasMusicKeyword = musicKeywords.any { lower.contains(it) }
        if (!hasMusicKeyword) {
            return true
        }

        return false
    }

    private fun sanitizeExtract(extract: String): String {
        return extract.trim()
    }

    private fun isGenericArtist(name: String): Boolean {
        val lower = name.lowercase().trim()
        return lower == "unknown artist" ||
                lower == "unknown" ||
                lower == "<unknown>" ||
                lower == "desconocido" ||
                lower == "artista desconocido" ||
                lower == "various artists" ||
                lower == "varios artistas"
    }

    private fun fetchJson(url: String): JSONObject? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "LiquidMusicPlayer/1.0 (Android; contact@example.com)")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 404) return null
                val body = response.body?.string()
                if (body != null) JSONObject(body) else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
