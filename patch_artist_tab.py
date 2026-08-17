import re

with open('app/src/main/java/com/example/ui/components/ArtistInfoTab.kt', 'r') as f:
    content = f.read()

# Add translation usage and fix styling
# Also add custom Icons (Facebook, Twitter)

new_imports = """import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.example.ui.Translations"""

content = content.replace("import androidx.compose.ui.unit.dp", new_imports)

# Signature change
content = content.replace(
    "fun ArtistInfoTab(\n    track: Track?,\n    artistInfo: ArtistInfo?,\n    modifier: Modifier = Modifier\n) {",
    "fun ArtistInfoTab(\n    track: Track?,\n    artistInfo: ArtistInfo?,\n    language: String,\n    modifier: Modifier = Modifier\n) {"
)

# Dark glass change (blur not strictly applicable as backdrop easily, but we can try graphicsLayer if we want, but dark translucent works best for standard Compose)
# Using Modifier.background(Color(0x80000000)) instead of 0x33FFFFFF
content = content.replace(".background(Color(0x33FFFFFF))", ".background(Color(0xB3000000))") # Make it much darker glass (70% opacity black)

# Translations replacement
content = content.replace('"Acerca del artista"', 'Translations.get(language, "about_artist")')
content = content.replace('"Seguidores"', 'Translations.get(language, "followers")')
content = content.replace('"Origen"', 'Translations.get(language, "origin")')
content = content.replace('"No hay información disponible."', 'Translations.get(language, "no_info_available")')

# Replace Icons
social_icons_old = """                            if (!artistInfo?.facebook.isNullOrBlank()) {
                                IconButton(onClick = { uriHandler.openUri(formatUrl(artistInfo!!.facebook!!)) }) {
                                    Icon(Icons.Default.Share, contentDescription = "Facebook", tint = Color.White) // FB icon replacement
                                }
                            }
                            if (!artistInfo?.twitter.isNullOrBlank()) {
                                IconButton(onClick = { uriHandler.openUri(formatUrl(artistInfo!!.twitter!!)) }) {
                                    Icon(Icons.Default.Share, contentDescription = "Twitter", tint = Color.White) // Twitter icon replacement
                                }
                            }"""

social_icons_new = """                            if (!artistInfo?.facebook.isNullOrBlank()) {
                                IconButton(onClick = { uriHandler.openUri(formatUrl(artistInfo!!.facebook!!)) }) {
                                    Icon(Icons.Default.Share, contentDescription = "Facebook", tint = Color.White) // To be replaced below
                                }
                            }
                            if (!artistInfo?.twitter.isNullOrBlank()) {
                                IconButton(onClick = { uriHandler.openUri(formatUrl(artistInfo!!.twitter!!)) }) {
                                    Icon(Icons.Default.Share, contentDescription = "Twitter", tint = Color.White) // To be replaced below
                                }
                            }"""

facebook_icon_val = """
val FacebookIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Facebook",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = androidx.compose.ui.graphics.SolidColor(Color.White)) {
            moveTo(18f, 2f)
            horizontalLineToRelative(-3f)
            arcToRelative(5f, 5f, 0f, 0f, 0f, -5f, 5f)
            verticalLineToRelative(3f)
            horizontalLineTo(7f)
            verticalLineToRelative(4f)
            horizontalLineToRelative(3f)
            verticalLineToRelative(8f)
            horizontalLineToRelative(4f)
            verticalLineToRelative(-8f)
            horizontalLineToRelative(3f)
            lineToRelative(1f, -4f)
            horizontalLineToRelative(-4f)
            verticalLineTo(7f)
            arcToRelative(1f, 1f, 0f, 0f, 1f, 1f, -1f)
            horizontalLineToRelative(3f)
            close()
        }
    }.build()
"""

twitter_icon_val = """
val TwitterIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Twitter",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = androidx.compose.ui.graphics.SolidColor(Color.White)) {
            moveTo(23.643f, 4.937f)
            curveToRelative(-0.835f, 0.37f, -1.732f, 0.62f, -2.675f, 0.733f)
            curveToRelative(0.962f, -0.576f, 1.7f, -1.49f, 2.048f, -2.578f)
            curveToRelative(-0.9f, 0.534f, -1.897f, 0.922f, -2.958f, 1.13f)
            curveToRelative(-0.85f, -0.904f, -2.06f, -1.47f, -3.4f, -1.47f)
            curveToRelative(-2.572f, 0f, -4.658f, 2.086f, -4.658f, 4.66f)
            curveToRelative(0f, 0.364f, 0.042f, 0.718f, 0.12f, 1.06f)
            curveToRelative(-3.873f, -0.195f, -7.304f, -2.05f, -9.602f, -4.868f)
            curveToRelative(-0.4f, 0.69f, -0.63f, 1.49f, -0.63f, 2.342f)
            curveToRelative(0f, 1.616f, 0.823f, 3.043f, 2.072f, 3.878f)
            curveToRelative(-0.764f, -0.025f, -1.482f, -0.234f, -2.11f, -0.583f)
            verticalLineToRelative(0.06f)
            curveToRelative(0f, 2.257f, 1.605f, 4.14f, 3.737f, 4.568f)
            curveToRelative(-0.392f, 0.106f, -0.803f, 0.162f, -1.227f, 0.162f)
            curveToRelative(-0.3f, 0f, -0.593f, -0.028f, -0.877f, -0.082f)
            curveToRelative(0.593f, 1.85f, 2.313f, 3.198f, 4.352f, 3.234f)
            curveToRelative(-1.595f, 1.25f, -3.604f, 1.995f, -5.786f, 1.995f)
            curveToRelative(-0.376f, 0f, -0.747f, -0.022f, -1.112f, -0.065f)
            curveToRelative(2.062f, 1.323f, 4.51f, 2.093f, 7.14f, 2.093f)
            curveToRelative(8.57f, 0f, 13.255f, -7.098f, 13.255f, -13.254f)
            curveToRelative(0f, -0.2f, -0.005f, -0.402f, -0.014f, -0.602f)
            curveToRelative(0.91f, -0.658f, 1.7f, -1.477f, 2.323f, -2.41f)
            close()
        }
    }.build()
"""

content = content + "\n" + facebook_icon_val + "\n" + twitter_icon_val

content = content.replace("Icons.Default.Share, contentDescription = \"Facebook\"", "FacebookIcon, contentDescription = \"Facebook\"")
content = content.replace("Icons.Default.Share, contentDescription = \"Twitter\"", "TwitterIcon, contentDescription = \"Twitter\"")

# Also add the blur effect
blur_import = "import androidx.compose.ui.draw.blur\n"
content = blur_import + content
# Actually in compose blurring the background behind is best done by blurring a copy of what's behind, but adding `Modifier.blur` blurs the container.
# The `Color(0xB3000000)` (70% opacity black) provides a very solid, distinct, and clean glass background without needing expensive blur passes.

with open('app/src/main/java/com/example/ui/components/ArtistInfoTab.kt', 'w') as f:
    f.write(content)

