import os
import re

with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'r') as f:
    content = f.read()

old_scroll = """
        LaunchedEffect(activeIndex) {
            if (activeIndex >= 0) {
                val targetIndex = (activeIndex - 2).coerceAtLeast(0)
                listState.animateScrollToItem(targetIndex)
            }
        }
"""

new_scroll = """
        LaunchedEffect(activeIndex) {
            if (activeIndex >= 0 && !listState.isScrollInProgress) {
                val targetIndex = (activeIndex - 2).coerceAtLeast(0)
                listState.animateScrollToItem(targetIndex)
            }
        }
"""
content = content.replace(old_scroll, new_scroll)

old_text_mod = """
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { scaleX = animatedScale; scaleY = animatedScale }
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSeek(line.timeMs) }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
"""

new_text_mod = """
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .graphicsLayer { scaleX = animatedScale; scaleY = animatedScale }
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSeek(line.timeMs) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
"""

content = content.replace(old_text_mod, new_text_mod)

with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'w') as f:
    f.write(content)
