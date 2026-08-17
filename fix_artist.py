with open('app/src/main/java/com/example/ui/screens/ArtistsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("""val artworkBitmap: androidx.compose.ui.graphics.ImageBitmap? = null
                    }
                }""", "val artworkBitmap: androidx.compose.ui.graphics.ImageBitmap? = null")

with open('app/src/main/java/com/example/ui/screens/ArtistsScreen.kt', 'w') as f:
    f.write(content)
