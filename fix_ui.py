import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

# 1. Remove format badge in landscape
landscape_badge_regex = re.compile(r" {28}// Format Badge\n {28}Box\([\s\S]*? {28}\}\n")
content = landscape_badge_regex.sub("", content)

# 2. Remove format badge in portrait
portrait_badge_regex = re.compile(r" {16}// Audio Quality Pill Tag\n {16}Box\([\s\S]*? {16}\}\n")
content = portrait_badge_regex.sub("", content)

# 3. Landscape right side: Change Arrangement to Center and add Spacers
landscape_right_column_regex = re.compile(r"( {24}// Playback Controls View in Landscape\n {24}Column\([\s\S]*?verticalArrangement = )Arrangement\.SpaceEvenly,")
content = landscape_right_column_regex.sub(r"\1Arrangement.Center,", content)

# Add spacing between controls in landscape
landscape_slider_regex = re.compile(r"( {28}// Progress bar & Timestamps\n {28}Column[\s\S]*?modifier = Modifier\.fillMaxWidth\(\)\n {32}\)\n {28}\}\n)")
content = landscape_slider_regex.sub(r"\1                            Spacer(modifier = Modifier.height(24.dp))\n", content)

landscape_playback_regex = re.compile(r"( {28}// Main Playback Controls Row[\s\S]*? {28}\}\n)")
content = landscape_playback_regex.sub(r"\1                            Spacer(modifier = Modifier.height(24.dp))\n", content)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
print("UI fixed step 1")
