with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'r') as f:
    content = f.read()

old_block = """
    val artworkBitmap: androidx.compose.ui.graphics.ImageBitmap? = null
        }
    }
"""

new_block = """
    val artworkBitmap: androidx.compose.ui.graphics.ImageBitmap? = null
"""

content = content.replace(old_block, new_block)
with open('app/src/main/java/com/example/ui/screens/LibraryScreen.kt', 'w') as f:
    f.write(content)
