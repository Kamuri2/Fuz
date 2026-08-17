import re

with open('app/src/main/java/com/example/data/MetadataReader.kt', 'r') as f:
    content = f.read()

# 1. Direct standard field check via FieldKey
std_search = """                        if (clean.contains(Regex("\\[\\d{1,2}:\\d{2}"))) {
                            return clean // Synchronized!
                        }"""
std_replace = """                        val converted = convertSubtitleToLrc(clean)
                        if (converted.contains(Regex("\\\\[\\\\d{1,2}:\\\\d{2}"))) {
                            return converted // Synchronized!
                        }"""
content = content.replace(std_search, std_replace)

# 3. Fallback candidate keys lookup
cand_search = """                        if (clean.contains(Regex("\\[\\d{1,2}:\\d{2}"))) return clean"""
cand_replace = """                        val converted = convertSubtitleToLrc(clean)
                        if (converted.contains(Regex("\\\\[\\\\d{1,2}:\\\\d{2}"))) return converted"""
content = content.replace(cand_search, cand_replace)

with open('app/src/main/java/com/example/data/MetadataReader.kt', 'w') as f:
    f.write(content)

