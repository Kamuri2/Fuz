import re

with open('app/src/main/java/com/example/ui/components/ArtistInfoTab.kt', 'r') as f:
    content = f.read()

icons_to_add_search = """                            if (!artistInfo?.appleMusic.isNullOrBlank()) {
                                IconButton(onClick = { uriHandler.openUri(formatUrl(artistInfo!!.appleMusic!!)) }) {
                                    Icon(AppleMusicIcon, contentDescription = "Apple Music", tint = Color.White)
                                }
                            }"""

icons_to_add_replace = """                            if (!artistInfo?.appleMusic.isNullOrBlank()) {
                                IconButton(onClick = { uriHandler.openUri(formatUrl(artistInfo!!.appleMusic!!)) }) {
                                    Icon(AppleMusicIcon, contentDescription = "Apple Music", tint = Color.White)
                                }
                            }
                            if (!artistInfo?.deezer.isNullOrBlank()) {
                                IconButton(onClick = { uriHandler.openUri(formatUrl(artistInfo!!.deezer!!)) }) {
                                    Icon(androidx.compose.material.icons.Icons.Default.LibraryMusic, contentDescription = "Deezer", tint = Color.White)
                                }
                            }"""

content = content.replace(icons_to_add_search, icons_to_add_replace)

with open('app/src/main/java/com/example/ui/components/ArtistInfoTab.kt', 'w') as f:
    f.write(content)

