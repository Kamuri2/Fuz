import re

with open('app/src/main/java/com/example/ui/components/ArtistInfoTab.kt', 'r') as f:
    content = f.read()

content = content.replace("Color(0x2AFFFFFF)", "Color(0x66000000)")
content = content.replace("Color.White.copy(alpha = 0.2f)", "Color.White.copy(alpha = 0.1f)")

with open('app/src/main/java/com/example/ui/components/ArtistInfoTab.kt', 'w') as f:
    f.write(content)
