import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

# The block to find:
#                 // Progress Bar & Duration Labels
#                 Column(modifier = Modifier.fillMaxWidth()) {
# ... to ...
#                     }
#                 }

regex = re.compile(r"( {16}// Progress Bar & Duration Labels\n {16}Column.*? {16}\}\n) {16}Spacer\(modifier = Modifier\.height\(6\.dp\)\)\n( {16}// Main Playback Controls\n {16}Row\([\s\S]*? \}\n {16}\}\n)", re.DOTALL)

def repl(match):
    return match.group(2) + "                Spacer(modifier = Modifier.height(6.dp))\n" + match.group(1)

new_content = regex.sub(repl, content)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(new_content)

print("Replaced!")
