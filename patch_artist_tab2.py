import re

with open('app/src/main/java/com/example/ui/components/ArtistInfoTab.kt', 'r') as f:
    content = f.read()

# Make background lighter
content = content.replace("Color(0xB3000000)", "Color(0x33FFFFFF)")

# Add instagram logic
socials_check_old = "val hasSocials = !artistInfo?.website.isNullOrBlank() || !artistInfo?.facebook.isNullOrBlank() || !artistInfo?.twitter.isNullOrBlank()"
socials_check_new = "val hasSocials = !artistInfo?.website.isNullOrBlank() || !artistInfo?.facebook.isNullOrBlank() || !artistInfo?.twitter.isNullOrBlank() || !artistInfo?.instagram.isNullOrBlank()"
content = content.replace(socials_check_old, socials_check_new)

insta_icon_val = """
val InstagramIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Instagram",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = androidx.compose.ui.graphics.SolidColor(Color.White)) {
            moveTo(12f, 2.163f)
            curveToRelative(3.204f, 0f, 3.584f, 0.012f, 4.85f, 0.07f)
            curveToRelative(3.252f, 0.148f, 4.771f, 1.691f, 4.919f, 4.919f)
            curveToRelative(0.058f, 1.265f, 0.069f, 1.645f, 0.069f, 4.849f)
            curveToRelative(0f, 3.205f, -0.012f, 3.584f, -0.069f, 4.849f)
            curveToRelative(-0.149f, 3.225f, -1.664f, 4.771f, -4.919f, 4.919f)
            curveToRelative(-1.266f, 0.058f, -1.644f, 0.07f, -4.85f, 0.07f)
            curveToRelative(-3.204f, 0f, -3.584f, -0.012f, -4.849f, -0.07f)
            curveToRelative(-3.26f, -0.149f, -4.771f, -1.699f, -4.919f, -4.92f)
            curveToRelative(-0.058f, -1.265f, -0.07f, -1.644f, -0.07f, -4.849f)
            curveToRelative(0f, -3.204f, 0.012f, -3.584f, 0.07f, -4.849f)
            curveToRelative(0.149f, -3.227f, 1.664f, -4.771f, 4.919f, -4.919f)
            curveToRelative(1.266f, -0.057f, 1.645f, -0.069f, 4.849f, -0.069f)
            close()
            moveTo(12f, 0f)
            curveTo(8.741f, 0f, 8.333f, 0.014f, 7.053f, 0.072f)
            curveTo(2.695f, 0.272f, 0.273f, 2.69f, 0.073f, 7.052f)
            curveTo(0.014f, 8.333f, 0f, 8.741f, 0f, 12f)
            curveToRelative(0f, 3.259f, 0.014f, 3.668f, 0.072f, 4.948f)
            curveToRelative(0.2f, 4.358f, 2.618f, 6.78f, 6.98f, 6.98f)
            curveTo(8.333f, 23.986f, 8.741f, 24f, 12f, 24f)
            curveToRelative(3.259f, 0f, 3.668f, -0.014f, 4.948f, -0.072f)
            curveToRelative(4.358f, -0.2f, 6.78f, -2.618f, 6.98f, -6.98f)
            curveTo(23.986f, 15.668f, 24f, 15.259f, 24f, 12f)
            curveToRelative(0f, -3.259f, -0.014f, -3.668f, -0.072f, -4.948f)
            curveToRelative(-0.2f, -4.358f, -2.618f, -6.78f, -6.98f, -6.98f)
            curveTo(15.668f, 0.014f, 15.259f, 0f, 12f, 0f)
            close()
            moveTo(12f, 5.838f)
            arcToRelative(6.162f, 6.162f, 0f, true, false, 0f, 12.324f)
            arcToRelative(6.162f, 6.162f, 0f, false, false, 0f, -12.324f)
            close()
            moveTo(12f, 16f)
            arcToRelative(4f, 4f, 0f, true, true, 0f, -8f)
            arcToRelative(4f, 4f, 0f, false, true, 0f, 8f)
            close()
            moveTo(18.406f, 5.594f)
            arcToRelative(1.44f, 1.44f, 0f, true, false, -2.88f, 0f)
            arcToRelative(1.44f, 1.44f, 0f, false, false, 2.88f, 0f)
            close()
        }
    }.build()
"""
content = content + "\n" + insta_icon_val

twitter_block = """                            if (!artistInfo?.twitter.isNullOrBlank()) {
                                IconButton(onClick = { uriHandler.openUri(formatUrl(artistInfo!!.twitter!!)) }) {
                                    Icon(TwitterIcon, contentDescription = "Twitter", tint = Color.White) // To be replaced below
                                }
                            }"""

insta_block = """                            if (!artistInfo?.instagram.isNullOrBlank()) {
                                IconButton(onClick = { uriHandler.openUri(formatUrl(artistInfo!!.instagram!!)) }) {
                                    Icon(InstagramIcon, contentDescription = "Instagram", tint = Color.White)
                                }
                            }"""

content = content.replace(twitter_block, twitter_block + "\n" + insta_block)

# Remove the 'To be replaced below' comment
content = content.replace("// To be replaced below", "")

with open('app/src/main/java/com/example/ui/components/ArtistInfoTab.kt', 'w') as f:
    f.write(content)
