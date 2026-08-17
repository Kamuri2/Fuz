import re

with open('app/src/main/java/com/example/data/ArtistInfoFetcher.kt', 'r') as f:
    content = f.read()

# Update ArtistInfo data class
new_data_class = """data class ArtistInfo(
    val imageUrl: String?,
    val bio: String?,
    val followers: String,
    val listeners: String,
    val origin: String,
    val website: String? = null,
    val facebook: String? = null,
    val twitter: String? = null,
    val instagram: String? = null,
    val spotify: String? = null,
    val youtube: String? = null,
    val appleMusic: String? = null,
    val deezer: String? = null
)"""

content = re.sub(r'data class ArtistInfo\(.*?instagram: String\? = null\)', new_data_class, content, flags=re.DOTALL)

# Update fetchArtistInfo variables
vars_search = """        var instagram: String? = null
        var followersStr = ""
        var origin = "Unknown"
        var listeners = """""

vars_replace = """        var instagram: String? = null
        var spotify: String? = null
        var youtube: String? = null
        var appleMusic: String? = null
        var deezer: String? = null
        
        var followersStr = ""
        var origin = "Unknown"
        var listeners = """""

content = content.replace(vars_search, vars_replace)

# Deezer fetch update
deezer_fetch_search = """                    if (bestMatch != null) {
                        imageUrl = bestMatch.optString("picture_xl").takeIf { it.isNotBlank() }
                        val fans = bestMatch.optInt("nb_fan", 0)
                        if (fans > 0) {
                            followersStr = "%,d".format(fans)
                        }
                    }"""

deezer_fetch_replace = """                    if (bestMatch != null) {
                        imageUrl = bestMatch.optString("picture_xl").takeIf { it.isNotBlank() }
                        deezer = bestMatch.optString("link").takeIf { it.isNotBlank() }
                        val fans = bestMatch.optInt("nb_fan", 0)
                        if (fans > 0) {
                            followersStr = "%,d".format(fans)
                        }
                    }"""

content = content.replace(deezer_fetch_search, deezer_fetch_replace)

# Add fallback custom links just before creating ArtistInfo
fallback_search = """        val info = ArtistInfo(imageUrl, bio, followersStr, listeners, origin, website, facebook, twitter, instagram)"""
fallback_replace = """        val encodedForLinks = java.net.URLEncoder.encode(artistName, "UTF-8").replace("+", "%20")
        if (spotify == null) spotify = "https://open.spotify.com/search/${encodedForLinks}/artists"
        if (youtube == null) youtube = "https://www.youtube.com/results?search_query=${encodedForLinks}+artist"
        if (appleMusic == null) appleMusic = "https://music.apple.com/search?term=${encodedForLinks}"

        val info = ArtistInfo(imageUrl, bio, followersStr, listeners, origin, website, facebook, twitter, instagram, spotify, youtube, appleMusic, deezer)"""

content = content.replace(fallback_search, fallback_replace)

# One more thing: fix any social connection errors.
# In AudioDB, sometimes facebook/twitter come back as just "1" or username instead of a URL.
# Let's fix that.
audiodb_search = """                        website = bestMatch.optString("strWebsite").takeIf { it.isNotBlank() && it.lowercase() != "null" }
                        facebook = bestMatch.optString("strFacebook").takeIf { it.isNotBlank() && it.lowercase() != "null" }
                        twitter = bestMatch.optString("strTwitter").takeIf { it.isNotBlank() && it.lowercase() != "null" }
                        instagram = bestMatch.optString("strInstagram").takeIf { it.isNotBlank() && it.lowercase() != "null" }"""

audiodb_replace = """                        website = bestMatch.optString("strWebsite").takeIf { it.isNotBlank() && it.lowercase() != "null" }
                        facebook = bestMatch.optString("strFacebook").takeIf { it.isNotBlank() && it.lowercase() != "null" }?.let { if (it.startsWith("http")) it else "https://facebook.com/$it" }
                        twitter = bestMatch.optString("strTwitter").takeIf { it.isNotBlank() && it.lowercase() != "null" }?.let { if (it.startsWith("http")) it else "https://twitter.com/$it" }
                        instagram = bestMatch.optString("strInstagram").takeIf { it.isNotBlank() && it.lowercase() != "null" }?.let { if (it.startsWith("http")) it else "https://instagram.com/$it" }"""
content = content.replace(audiodb_search, audiodb_replace)

with open('app/src/main/java/com/example/data/ArtistInfoFetcher.kt', 'w') as f:
    f.write(content)

