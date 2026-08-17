with open('app/src/main/java/com/example/data/LrcParser.kt', 'r') as f:
    content = f.read()

# Make timeTagRegex match both [] and <>
content = content.replace('Regex("""\\[\\s*(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?\\s*\\]""")',
                          'Regex("""(?:\\[|<)\\s*(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?\\s*(?:\\]|>)""")')

with open('app/src/main/java/com/example/data/LrcParser.kt', 'w') as f:
    f.write(content)
