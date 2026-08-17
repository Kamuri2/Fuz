with open('app/src/main/java/com/example/data/LrcParser.kt', 'r') as f:
    content = f.read()

old_code = """
    fun getCurrentLineIndex(lines: List<LrcLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var activeIndex = -1
        for (i in lines.indices) {
            if (positionMs >= lines[i].timeMs) {
                activeIndex = i
            } else {
                break
            }
        }
        return activeIndex
    }
"""

new_code = """
    fun getCurrentLineIndex(lines: List<LrcLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        return lines.indexOfLast { it.timeMs <= positionMs }
    }
"""

content = content.replace(old_code.strip(), new_code.strip())

with open('app/src/main/java/com/example/data/LrcParser.kt', 'w') as f:
    f.write(content)
