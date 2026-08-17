with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'r') as f:
    content = f.read()

old_box = """                                        .fillMaxWidth(0.91f)
                                        .aspectRatio(1f)"""

new_box = """                                        .fillMaxWidth(0.96f)
                                        .aspectRatio(1f)"""
content = content.replace(old_box, new_box)

with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'w') as f:
    f.write(content)
