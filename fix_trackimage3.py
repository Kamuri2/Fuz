with open('app/src/main/java/com/example/ui/components/TrackImage.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
skip = False
for l in lines:
    if '/*                        }' in l:
        skip = True
    if '*/' in l:
        skip = False
        continue
    if not skip:
        new_lines.append(l)

content = "".join(new_lines)
content = content.replace("                    if (rawBytes != null) {\n                            if (rawBytes != null) {", "                    if (rawBytes != null) {")
content = content.replace("                                }\n                        }\n                        val mmr", "                                }\n                        }\n                        // val mmr")

with open('app/src/main/java/com/example/ui/components/TrackImage.kt', 'w') as f:
    f.write(content)

