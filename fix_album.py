with open('app/src/main/java/com/example/ui/screens/AlbumsScreen.kt', 'r') as f:
    content = f.read()

bad_block = """
                        }

                        Card(
"""

good_block = """
                        val artworkBitmap: androidx.compose.ui.graphics.ImageBitmap? = null
                        Card(
"""
content = content.replace(bad_block, good_block)

# Fix items blocks if missing LazyVerticalGrid closure properly
content = content.replace("item { Spacer(modifier = Modifier.height(100.dp)) }", "item { Spacer(modifier = Modifier.height(100.dp)) }", 1)

with open('app/src/main/java/com/example/ui/screens/AlbumsScreen.kt', 'w') as f:
    f.write(content)
