with open('app/src/main/java/com/example/data/MetadataReader.kt', 'r') as f:
    lines = f.readlines()

out = []
in_find = False
for line in lines:
    if 'private fun findLyricsInTag(tag: Tag): String {' in line:
        in_find = True
        out.append(line)
        continue
    if in_find:
        if line.strip() == '}':
            if len([l for l in out if 'private fun extractTextFromRawField' in l]) > 0:
                pass # oops
        if 'private fun extractTextFromRawField' in line:
            in_find = False
            
    if not in_find:
        out.append(line)

# Let's just use standard string replacement for the block we care about
