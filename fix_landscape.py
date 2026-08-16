import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

landscape_start = """            // ==================== LANDSCAPE MODE ====================
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {"""

landscape_replacement = """            // ==================== LANDSCAPE MODE ====================
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {"""

# Also need to find the end of the Row to close the Box.
# The row closes around line 335... wait, I'll just find the end of the Landscape mode block
# which is right before `} else {`
# Let's see:

regex_end = re.compile(r"( {12}\}\n) {8}\} else \{\n {12}// ==================== PORTRAIT MODE ====================")
def repl_end(match):
    return "            }\n" + match.group(0)

new_content = content.replace(landscape_start, landscape_replacement)
new_content = regex_end.sub(repl_end, new_content)

# Now add the button right after the Box starts:
box_start = """            Box(modifier = Modifier.fillMaxSize()) {"""
button_str = """            Box(modifier = Modifier.fillMaxSize()) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp).testTag("close_player_landscape")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Deslizar para cerrar",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }"""
new_content = new_content.replace(box_start, button_str)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(new_content)

print("Landscape fixed!")
