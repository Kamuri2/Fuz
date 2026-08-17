import re

with open('app/src/main/java/com/example/ui/components/ArtistInfoTab.kt', 'r') as f:
    content = f.read()

# Add blur to the modifier for backdrop filter effect (if supported, else it just blurs the component, so let's use a border instead for the glassy look)
content = content.replace("import androidx.compose.foundation.background", "import androidx.compose.foundation.background\nimport androidx.compose.foundation.border")

# Note: Color(0x33FFFFFF) is used. Let's add border(1.dp, Color.White.copy(alpha=0.2f), RoundedCornerShape(...))
content = content.replace(".clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))\\n            .background(Color(0x33FFFFFF))", 
""".clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Color(0x40FFFFFF)) // Lighter background
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))""")

# In Python replace string needs exact match
content = content.replace(
    ".clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))\n            .background(Color(0x33FFFFFF))",
    ".clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))\n            .background(Color(0x40000000))\n            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))"
)
# Wait, 0x40000000 is 25% black. But the user asked for lighter and blurry.
# I'll use Color(0x33FFFFFF) for white translucent.
content = content.replace(".background(Color(0x40000000))", ".background(Color(0x2AFFFFFF))")

with open('app/src/main/java/com/example/ui/components/ArtistInfoTab.kt', 'w') as f:
    f.write(content)
