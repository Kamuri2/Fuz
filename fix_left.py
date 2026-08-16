import re

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target_left_side = """                // LEFT SIDE: Cover, Title, Artist - Album, and Actions
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {"""

new_left_side = """                // LEFT SIDE: Cover, Title, Artist - Album, and Actions
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {"""

content = content.replace(target_left_side, new_left_side)

# Replace the cover box to be larger and have rounded corners of 12.dp instead of 24.dp
target_box = """                    // Artwork Cover
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.68f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0x1AFFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {"""

new_box = """                    // Artwork Cover
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x1AFFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {"""

content = content.replace(target_box, new_box)

# Now, we need to inject the action icons row INSIDE the Box, at the bottom.
# To do this, we need to find where the `when` block ends.

target_when_end = """                            else -> {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(72.dp)
                                )
                            }
                        }
                    }"""

new_when_end = """                            else -> {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(72.dp)
                                )
                            }
                        }
                        
                        // Bottom Action Icons Row (Overlayed inside the cover)
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                ))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onFavoriteToggle, modifier = Modifier.testTag("player_favorite_btn")) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFavorite) Color(0xFFFF5C00) else Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            IconButton(onClick = onDislikeToggle) {
                                Icon(
                                    imageVector = if (isDisliked) Icons.Default.ThumbDown else Icons.Default.ThumbDownOffAlt,
                                    contentDescription = "Dislike",
                                    tint = if (isDisliked) Color(0xFFEF4444) else Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            IconButton(onClick = { showAddToPlaylistModal = true }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add to playlist",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            IconButton(onClick = onOpenQueue, modifier = Modifier.testTag("player_queue_btn")) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = "Queue",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))"""

content = content.replace(target_when_end, new_when_end)

# Now remove the old bottom action icons row which is right after the Song Title block
old_row_regex = re.compile(r"( {20}// Bottom Action Icons Row \(Like, Dislike, Add to playlist, Queue, Close/Back\)\n {20}Row\([\s\S]*? {20}\}\n)")

content = old_row_regex.sub("", content)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
print("Left side fixed")
