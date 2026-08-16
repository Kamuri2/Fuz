with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    lines = f.readlines()

new_lines = []
skip = False
for i, line in enumerate(lines):
    # Start skipping after Song Title Column closes
    if i == 326:
        # line 326 is: '                    }\n'
        new_lines.append(line)
        skip = True
        continue
    
    if skip:
        # Check if we reached RIGHT SIDE
        if "// RIGHT SIDE: Playback Controls OR Lyrics Display" in line:
            # We need to add the closing brace for Column(LEFT SIDE)
            new_lines.append("                }\n")
            new_lines.append(line)
            skip = False
        continue

    new_lines.append(line)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.writelines(new_lines)
print("Cleaned up orphaned buttons")
