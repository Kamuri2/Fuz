import re

with open("app/src/main/java/com/example/data/MetadataReader.kt", "r") as f:
    content = f.read()

target = """            // 3. Buscar archivos sueltos (.lrc / .txt) en la misma carpeta
            if (extractedLyrics.isBlank() && track.path.isNotBlank() && !track.path.startsWith("content://")) {
                extractedLyrics = searchSidecarLyrics(track.path)
            }"""

replacement = """            // 3. Buscar archivos sueltos (.lrc / .txt) en la misma carpeta
            if (extractedLyrics.isBlank() && track.path.isNotBlank() && !track.path.startsWith("content://")) {
                extractedLyrics = searchSidecarLyrics(track.path)
            }

            // 4. Fallback a API de Internet (LrcLib) si no hay letras locales
            if (extractedLyrics.isBlank()) {
                val fetched = LrcLibHelper.fetchLyrics(title, artist)
                if (fetched != null) {
                    extractedLyrics = fetched
                }
            }"""

new_content = content.replace(target, replacement)

with open("app/src/main/java/com/example/data/MetadataReader.kt", "w") as f:
    f.write(new_content)
print("MetadataReader updated")
