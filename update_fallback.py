import re
with open('app/src/main/java/com/example/ui/components/TrackImage.kt', 'r') as f:
    content = f.read()

# Add imports
imports = """import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
"""
if "import androidx.compose.foundation.layout.Box" not in content:
    content = content.replace("import androidx.compose.foundation.Image", imports + "import androidx.compose.foundation.Image")

new_render = """    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = track.title,
            contentScale = contentScale,
            modifier = modifier
        )
    } else if (useFallback) {
        Box(
            modifier = modifier.background(Color(0x1AFFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxSize(0.4f)
            )
        }
    }"""
content = re.sub(r'    if \(bitmap != null\) \{\n        Image\([\s\S]*?modifier = modifier\n        \)\n    \}', new_render, content)

with open('app/src/main/java/com/example/ui/components/TrackImage.kt', 'w') as f:
    f.write(content)
