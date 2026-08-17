import re

with open('app/src/main/java/com/example/data/LrcLibHelper.kt', 'r') as f:
    content = f.read()

clean_title_old = """        // Remove trailing tags like "(Official Video)", "[Lyrics]", "(Remastered 2021)", "(HD)"
        t = t.replace(Regex("(?i)\\\\s*[\\\\[\\\\(](?:official|audio|video|lyrics|hd|4k|remaster(?:ed)?|live|bonus track|feat|ft)[^\\\\]\\\\)]*[\\\\]\\\\)]"), "")"""

clean_title_new = """        // Remove trailing tags like "(Official Video)", "[Lyrics]", "(Remastered 2021)", "(HD)"
        t = t.replace(Regex("(?i)\\\\s*[\\\\[\\\\(](?:official|audio|video|lyrics|hd|4k|remaster(?:ed)?|live|bonus track|feat|ft)[^\\\\]\\\\)]*[\\\\]\\\\)]"), "")
        // Remove dash tags like "- Remastered 2011"
        t = t.replace(Regex("(?i)\\\\s*-\\\\s*(?:Remaster(?:ed)?|Live|Mono|Stereo|Bonus Track).*$"), "")"""

content = content.replace(clean_title_old, clean_title_new)

with open('app/src/main/java/com/example/data/LrcLibHelper.kt', 'w') as f:
    f.write(content)
