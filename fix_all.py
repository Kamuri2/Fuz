import re

# 1. Fix ArtistInfoFetcher.kt
with open('app/src/main/java/com/example/data/ArtistInfoFetcher.kt', 'r') as f:
    content = f.read()

old_data_class = """data class ArtistInfo(
    val imageUrl: String?,
    val bio: String?,
    val followers: String,
    val listeners: String,
    val origin: String,
    val website: String? = null,
    val facebook: String? = null,
    val twitter: String? = null,
    val instagram: String? = null
)"""

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

content = content.replace(old_data_class, new_data_class)

with open('app/src/main/java/com/example/data/ArtistInfoFetcher.kt', 'w') as f:
    f.write(content)

# 2. Fix MetadataReader.kt escape sequences
with open('app/src/main/java/com/example/data/MetadataReader.kt', 'r') as f:
    content = f.read()

content = content.replace('Regex("\\[\d{1,2}:\d{2}")', 'Regex("\\\\[\\\\d{1,2}:\\\\d{2}")')
# also fix regex in convertSubtitleToLrc
content = content.replace('Regex("(?:\d{2}:)?(\d{2}):(\d{2})[,.](\d{2,3})\s*-->.*")', 'Regex("(?:\\\\d{2}:)?(\\\\d{2}):(\\\\d{2})[,.](\\\\d{2,3})\\\\s*-->.*")')
content = content.replace('Regex("^\d+$")', 'Regex("^\\\\d+$")')
content = content.replace('.append("\\n")', '.append("\\n")') # replace any double backslash n with just backslash n if it happened? Wait, it was "\\n". Let's just use "\n"
content = content.replace('append("\\\\n")', 'append("\\n")')
content = content.replace('Regex("\\[\\\\d', 'Regex("\\\\[\\\\d')

with open('app/src/main/java/com/example/data/MetadataReader.kt', 'w') as f:
    f.write(content)

# 3. Fix ArtistInfoTab.kt SolidColor import
with open('app/src/main/java/com/example/ui/components/ArtistInfoTab.kt', 'r') as f:
    content = f.read()

content = content.replace("import androidx.compose.ui.graphics.Color", "import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.graphics.SolidColor")

with open('app/src/main/java/com/example/ui/components/ArtistInfoTab.kt', 'w') as f:
    f.write(content)

