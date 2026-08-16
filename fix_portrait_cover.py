import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

# Replace border radius
target_radius = ".clip(RoundedCornerShape(28.dp))"
new_radius = ".clip(RoundedCornerShape(16.dp))"
content = content.replace(target_radius, new_radius)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
print("Portrait radius fixed")
