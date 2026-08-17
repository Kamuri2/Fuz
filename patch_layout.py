import re

with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'r') as f:
    content = f.read()

# 1. Replace the outer Box of Album Art with a Column + Spacers
old_album_art_container = """
                            // ==================== ALBUM ART MODE ====================
                            // 1:1 Square Album Art Area with Action Buttons overlaid at bottom
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.91f)
"""

new_album_art_container = """
                            // ==================== ALBUM ART MODE ====================
                            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Spacer(modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.91f)
"""
content = content.replace(old_album_art_container, new_album_art_container)

# 2. Add the bottom spacer/lyrics container and close the Column
old_album_art_end = """
                                }
                            }
                        }
                    } }
// end of if/else isLyricsMode
                    } // end of Crossfade block
                }

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
// Compact Live Single-Line Lyric Snippet
                if (!showLyricsMode) {
                    LiveSyncedLyricSnippet(
                        parsedLyrics = parsedLyrics,
                        rawLyrics = currentTrack?.lyrics ?: "",
                        currentPositionMs = currentPositionMs,
                        onClick = { showLyricsMode = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 2.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Title and Artist Centered
"""

new_album_art_end = """
                                }
                                
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    LiveSyncedLyricSnippet(
                                        parsedLyrics = parsedLyrics,
                                        rawLyrics = targetTrack?.lyrics ?: "",
                                        currentPositionMs = currentPositionMs,
                                        onClick = { showLyricsMode = true },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    } }
// end of if/else isLyricsMode
                    } // end of Crossfade block
                }

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                // Title and Artist Centered
"""
content = content.replace(old_album_art_end, new_album_art_end)

with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'w') as f:
    f.write(content)
