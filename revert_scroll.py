with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'r') as f:
    content = f.read()

old_scroll = """
        LaunchedEffect(activeIndex) {
            if (activeIndex >= 0 && !listState.isScrollInProgress) {
                val targetIndex = (activeIndex - 2).coerceAtLeast(0)
                listState.animateScrollToItem(targetIndex)
            }
        }
"""

new_scroll = """
        LaunchedEffect(activeIndex) {
            if (activeIndex >= 0) {
                val targetIndex = (activeIndex - 2).coerceAtLeast(0)
                listState.animateScrollToItem(targetIndex)
            }
        }
"""

content = content.replace(old_scroll, new_scroll)

with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'w') as f:
    f.write(content)
