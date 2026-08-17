import re

with open('app/src/main/java/com/example/data/MetadataReader.kt', 'r') as f:
    content = f.read()

old_code = """
            // 3. Buscar archivos sueltos (.lrc / .txt) en la misma carpeta
            if (extractedLyrics.isBlank() && track.path.isNotBlank() && !track.path.startsWith("content://")) {
                extractedLyrics = searchSidecarLyrics(track.path)
            }

            // 4. Fallback a API de Internet (LrcLib) si no hay letras locales
            if (extractedLyrics.isBlank()) {
                val fetched = LrcLibHelper.fetchLyrics(
                    title = title,
                    artist = artist,
                    album = album,
                    durationSec = durationMs / 1000
                )
                if (fetched != null) {
                    extractedLyrics = fetched
                }
            }
"""

new_code = """
            // 3. Buscar archivos sueltos (.lrc / .txt) en la misma carpeta
            val isSyncedLocal = extractedLyrics.contains(Regex("\\\\[\\\\d{1,2}:\\\\d{2}"))
            if (!isSyncedLocal && track.path.isNotBlank() && !track.path.startsWith("content://")) {
                val sidecar = searchSidecarLyrics(track.path)
                if (sidecar.contains(Regex("\\\\[\\\\d{1,2}:\\\\d{2}"))) {
                    extractedLyrics = sidecar
                } else if (extractedLyrics.isBlank() && sidecar.isNotBlank()) {
                    extractedLyrics = sidecar
                }
            }

            // 4. Fallback a API de Internet (LrcLib) si no hay letras sincronizadas
            if (!extractedLyrics.contains(Regex("\\\\[\\\\d{1,2}:\\\\d{2}"))) {
                val fetched = LrcLibHelper.fetchLyrics(
                    title = title,
                    artist = artist,
                    album = album,
                    durationSec = durationMs / 1000
                )
                if (fetched != null) {
                    if (fetched.contains(Regex("\\\\[\\\\d{1,2}:\\\\d{2}")) || extractedLyrics.isBlank()) {
                        extractedLyrics = fetched
                    }
                }
            }
"""

content = content.replace(old_code.strip(), new_code.strip())

with open('app/src/main/java/com/example/data/MetadataReader.kt', 'w') as f:
    f.write(content)
