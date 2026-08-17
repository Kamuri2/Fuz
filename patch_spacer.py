import re

with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'r') as f:
    content = f.read()

# Replace spacing between Title and Progress Bar
content = content.replace(
    "Spacer(modifier = Modifier.height(6.dp))\n\n                // Progress Bar & Duration Labels (Tightly attached right below Title)",
    "Spacer(modifier = Modifier.height(28.dp))\n\n                // Progress Bar & Duration Labels (Tightly attached right below Title)"
)

# Replace spacing between Progress Bar and Main Playback Controls
content = content.replace(
    "Spacer(modifier = Modifier.height(4.dp))\n\n                // Main Playback Controls",
    "Spacer(modifier = Modifier.height(20.dp))\n\n                // Main Playback Controls"
)

# Decrease overall bottom padding slightly to allow the push down without compressing
content = content.replace(
    ".padding(top = 8.dp, bottom = 80.dp),",
    ".padding(top = 8.dp, bottom = 54.dp),"
)

with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'w') as f:
    f.write(content)
