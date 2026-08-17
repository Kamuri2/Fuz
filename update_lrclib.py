import re

with open('app/src/main/java/com/example/data/LrcLibHelper.kt', 'r') as f:
    content = f.read()

# Replace URLEncoder.encode(..., "UTF-8") with one that replaces + with %20
content = content.replace(
    'URLEncoder.encode(query, "UTF-8")',
    'URLEncoder.encode(query, "UTF-8").replace("+", "%20")'
)
content = content.replace(
    'URLEncoder.encode(title, "UTF-8")',
    'URLEncoder.encode(title, "UTF-8").replace("+", "%20")'
)
content = content.replace(
    'URLEncoder.encode(artist, "UTF-8")',
    'URLEncoder.encode(artist, "UTF-8").replace("+", "%20")'
)
content = content.replace(
    'URLEncoder.encode(album, "UTF-8")',
    'URLEncoder.encode(album, "UTF-8").replace("+", "%20")'
)

# And one more fix: make sure the search is more forgiving by just using artist + title without extra characters.
with open('app/src/main/java/com/example/data/LrcLibHelper.kt', 'w') as f:
    f.write(content)
