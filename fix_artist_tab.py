import re

with open('app/src/main/java/com/example/ui/components/ArtistInfoTab.kt', 'r') as f:
    content = f.read()

has_socials_search = """val hasSocials = !artistInfo?.website.isNullOrBlank() || !artistInfo?.facebook.isNullOrBlank() || !artistInfo?.twitter.isNullOrBlank() || !artistInfo?.instagram.isNullOrBlank()"""
has_socials_replace = """val hasSocials = !artistInfo?.website.isNullOrBlank() || !artistInfo?.facebook.isNullOrBlank() || !artistInfo?.twitter.isNullOrBlank() || !artistInfo?.instagram.isNullOrBlank() || !artistInfo?.spotify.isNullOrBlank() || !artistInfo?.youtube.isNullOrBlank() || !artistInfo?.appleMusic.isNullOrBlank() || !artistInfo?.deezer.isNullOrBlank()"""
content = content.replace(has_socials_search, has_socials_replace)

icons_to_add_search = """                            if (!artistInfo?.instagram.isNullOrBlank()) {
                                IconButton(onClick = { uriHandler.openUri(formatUrl(artistInfo!!.instagram!!)) }) {
                                    Icon(InstagramIcon, contentDescription = "Instagram", tint = Color.White) // Instagram icon replacement
                                }
                            }
                        }"""

icons_to_add_replace = """                            if (!artistInfo?.instagram.isNullOrBlank()) {
                                IconButton(onClick = { uriHandler.openUri(formatUrl(artistInfo!!.instagram!!)) }) {
                                    Icon(InstagramIcon, contentDescription = "Instagram", tint = Color.White)
                                }
                            }
                            if (!artistInfo?.spotify.isNullOrBlank()) {
                                IconButton(onClick = { uriHandler.openUri(formatUrl(artistInfo!!.spotify!!)) }) {
                                    Icon(SpotifyIcon, contentDescription = "Spotify", tint = Color.White)
                                }
                            }
                            if (!artistInfo?.youtube.isNullOrBlank()) {
                                IconButton(onClick = { uriHandler.openUri(formatUrl(artistInfo!!.youtube!!)) }) {
                                    Icon(YouTubeIcon, contentDescription = "YouTube", tint = Color.White)
                                }
                            }
                            if (!artistInfo?.appleMusic.isNullOrBlank()) {
                                IconButton(onClick = { uriHandler.openUri(formatUrl(artistInfo!!.appleMusic!!)) }) {
                                    Icon(AppleMusicIcon, contentDescription = "Apple Music", tint = Color.White)
                                }
                            }
                        }"""
content = content.replace(icons_to_add_search, icons_to_add_replace)

new_icons = """
val SpotifyIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Spotify",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 2f)
            curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
            curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
            curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
            curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
            close()
            moveTo(17.37f, 17.29f)
            curveTo(17.15f, 17.65f, 16.68f, 17.77f, 16.32f, 17.55f)
            curveTo(13.56f, 15.86f, 10.07f, 15.48f, 5.92f, 16.42f)
            curveTo(5.51f, 16.51f, 5.09f, 16.25f, 5.0f, 15.84f)
            curveTo(4.91f, 15.43f, 5.17f, 15.01f, 5.58f, 14.92f)
            curveTo(10.12f, 13.88f, 14.0f, 14.33f, 17.07f, 16.21f)
            curveTo(17.43f, 16.43f, 17.55f, 16.91f, 17.37f, 17.29f)
            close()
            moveTo(18.82f, 14.0f)
            curveTo(18.52f, 14.47f, 17.89f, 14.62f, 17.42f, 14.33f)
            curveTo(14.23f, 12.38f, 9.47f, 11.81f, 5.67f, 12.96f)
            curveTo(5.13f, 13.12f, 4.56f, 12.82f, 4.39f, 12.28f)
            curveTo(4.23f, 11.73f, 4.53f, 11.17f, 5.08f, 11.0f)
            curveTo(9.44f, 9.68f, 14.7f, 10.33f, 18.33f, 12.56f)
            curveTo(18.8f, 12.85f, 18.96f, 13.48f, 18.82f, 14.0f)
            close()
            moveTo(18.99f, 10.51f)
            curveTo(15.17f, 8.24f, 8.79f, 8.03f, 5.12f, 9.15f)
            curveTo(4.45f, 9.35f, 3.75f, 8.97f, 3.55f, 8.3f)
            curveTo(3.35f, 7.63f, 3.73f, 6.93f, 4.4f, 6.73f)
            curveTo(8.65f, 5.43f, 15.68f, 5.68f, 20.14f, 8.33f)
            curveTo(20.75f, 8.69f, 20.95f, 9.48f, 20.59f, 10.09f)
            curveTo(20.23f, 10.7f, 19.44f, 10.9f, 18.83f, 10.53f)
            close()
        }
    }.build()

val YouTubeIcon: ImageVector
    get() = ImageVector.Builder(
        name = "YouTube",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(21.58f, 7.19f)
            curveTo(21.34f, 6.27f, 20.61f, 5.54f, 19.69f, 5.29f)
            curveTo(18.02f, 4.84f, 12.0f, 4.84f, 12.0f, 4.84f)
            curveTo(12.0f, 4.84f, 5.98f, 4.84f, 4.31f, 5.29f)
            curveTo(3.39f, 5.54f, 2.66f, 6.27f, 2.42f, 7.19f)
            curveTo(1.97f, 8.86f, 1.97f, 12.0f, 1.97f, 12.0f)
            curveTo(1.97f, 12.0f, 1.97f, 15.14f, 2.42f, 16.81f)
            curveTo(2.66f, 17.73f, 3.39f, 18.46f, 4.31f, 18.71f)
            curveTo(5.98f, 19.16f, 12.0f, 19.16f, 12.0f, 19.16f)
            curveTo(12.0f, 19.16f, 18.02f, 19.16f, 19.69f, 18.71f)
            curveTo(20.61f, 18.46f, 21.34f, 17.73f, 21.58f, 16.81f)
            curveTo(22.03f, 15.14f, 22.03f, 12.0f, 22.03f, 12.0f)
            curveTo(22.03f, 12.0f, 22.03f, 8.86f, 21.58f, 7.19f)
            close()
            moveTo(9.97f, 14.86f)
            lineTo(9.97f, 9.14f)
            lineTo(14.97f, 12.0f)
            lineTo(9.97f, 14.86f)
            close()
        }
    }.build()

val AppleMusicIcon: ImageVector
    get() = ImageVector.Builder(
        name = "AppleMusic",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(20.5f, 4.0f)
            lineTo(9.5f, 6.0f)
            curveTo(8.67f, 6.15f, 8.0f, 6.87f, 8.0f, 7.71f)
            lineTo(8.0f, 15.54f)
            curveTo(7.48f, 15.2f, 6.78f, 15.0f, 6.0f, 15.0f)
            curveTo(3.79f, 15.0f, 2.0f, 16.34f, 2.0f, 18.0f)
            curveTo(2.0f, 19.66f, 3.79f, 21.0f, 6.0f, 21.0f)
            curveTo(8.21f, 21.0f, 10.0f, 19.66f, 10.0f, 18.0f)
            lineTo(10.0f, 9.71f)
            lineTo(19.0f, 8.07f)
            lineTo(19.0f, 13.54f)
            curveTo(18.48f, 13.2f, 17.78f, 13.0f, 17.0f, 13.0f)
            curveTo(14.79f, 13.0f, 13.0f, 14.34f, 13.0f, 16.0f)
            curveTo(13.0f, 17.66f, 14.79f, 19.0f, 17.0f, 19.0f)
            curveTo(19.21f, 19.0f, 21.0f, 17.66f, 21.0f, 16.0f)
            lineTo(21.0f, 5.5f)
            curveTo(21.0f, 4.67f, 20.33f, 4.0f, 20.5f, 4.0f)
            close()
        }
    }.build()
"""

content = content + "\n" + new_icons

with open('app/src/main/java/com/example/ui/components/ArtistInfoTab.kt', 'w') as f:
    f.write(content)

