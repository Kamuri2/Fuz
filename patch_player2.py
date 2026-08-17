import re

with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'r') as f:
    content = f.read()

bad_invocation = """                ArtistInfoTab(
                    track = currentTrack,
                    artistInfo = artistInfo,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )"""

good_invocation = """                ArtistInfoTab(
                    track = currentTrack,
                    artistInfo = artistInfo,
                    language = settings.appLanguage,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )"""

content = content.replace(bad_invocation, good_invocation)

with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'w') as f:
    f.write(content)
