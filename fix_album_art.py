import re

with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'r') as f:
    content = f.read()

# Fix 1: Restore the Album Art Box
old_top = """                            // ==================== ALBUM ART MODE ====================
                            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Spacer(modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.91f)
                                        .aspectRatio(1f)"""

new_top = """                            // ==================== ALBUM ART MODE ====================
                            // 1:1 Square Album Art Area with Action Buttons overlaid at bottom
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.91f)
                                        .aspectRatio(1f)"""
content = content.replace(old_top, new_top)

# Fix 2: Remove the badly placed LiveSyncedLyricSnippet from inside the Row/Crossfade end
# and restore the proper end of the Box.
old_bottom = """                                IconButton(onClick = onOpenQueue, modifier = Modifier.testTag("player_queue_btn")) {
                                    Icon(
                                        imageVector = Icons.Default.QueueMusic,
                                        contentDescription = "Queue",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(22.dp)
                                    )
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

new_bottom = """                                IconButton(onClick = onOpenQueue, modifier = Modifier.testTag("player_queue_btn")) {
                                    Icon(
                                        imageVector = Icons.Default.QueueMusic,
                                        contentDescription = "Queue",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(22.dp)
                                    )
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
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LiveSyncedLyricSnippet(
                            parsedLyrics = parsedLyrics,
                            rawLyrics = currentTrack?.lyrics ?: "",
                            currentPositionMs = currentPositionMs,
                            onClick = { showLyricsMode = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Title and Artist Centered
"""
content = content.replace(old_bottom, new_bottom)

with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'w') as f:
    f.write(content)
