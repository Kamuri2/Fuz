with open('app/src/main/java/com/example/data/TrackRepository.kt', 'r') as f:
    c = f.read()

old = """                val db = AppDatabase.getDatabase(context)
                // Forcing a DB clear to remove any corrupted lyrics cache from previous builds
                db.trackDao().clear()
                val cached = db.trackDao().getAllTracks().map { it.toTrack() }"""

new = """                val db = AppDatabase.getDatabase(context)
                val cached = db.trackDao().getAllTracks().map { it.toTrack() }"""

c = c.replace(old, new)

with open('app/src/main/java/com/example/data/TrackRepository.kt', 'w') as f:
    f.write(c)
