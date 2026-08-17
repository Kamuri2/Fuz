with open('app/src/main/java/com/example/ui/screens/AlbumsScreen.kt', 'r') as f:
    content = f.read()

bad_block = """
                        val artworkBitmap: androidx.compose.ui.graphics.ImageBitmap? = null
                            }
                        val artworkBitmap: androidx.compose.ui.graphics.ImageBitmap? = null
"""

good_block = """
                        val artworkBitmap: androidx.compose.ui.graphics.ImageBitmap? = null
"""
content = content.replace(bad_block, good_block)

with open('app/src/main/java/com/example/ui/screens/AlbumsScreen.kt', 'w') as f:
    f.write(content)
