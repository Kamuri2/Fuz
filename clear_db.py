with open('app/src/main/java/com/example/data/TrackRepository.kt', 'r') as f:
    content = f.read()

content = content.replace('val cached = db.trackDao().getAllTracks().map { it.toTrack() }', 
'''// Forcing a DB clear to remove any corrupted lyrics cache from previous builds
                db.trackDao().clear()
                val cached = db.trackDao().getAllTracks().map { it.toTrack() }''')

with open('app/src/main/java/com/example/data/TrackRepository.kt', 'w') as f:
    f.write(content)
