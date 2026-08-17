import re

with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'r') as f:
    content = f.read()

# Remove artworkBitmap decoding
old_effect = """    var artworkBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(currentTrack?.id) {
        if (currentTrack != null && currentTrack.path.isNotBlank() && !currentTrack.path.startsWith("content://")) {
            withContext(Dispatchers.IO) {
                try {
                    val audioFile = org.jaudiotagger.audio.AudioFileIO.read(java.io.File(currentTrack.path))
                    val artwork = audioFile.tag?.firstArtwork
                    val rawBytes = artwork?.binaryData
                    if (rawBytes != null) {
                        val options = android.graphics.BitmapFactory.Options().apply {
                            inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                            inDither = false
                            inMutable = false // allow OS optimizations
                        }
                        val bitmap = android.graphics.BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)
                        artworkBitmap = bitmap?.asImageBitmap()
                    } else {
                        artworkBitmap = null
                    }
                } catch (e: Exception) {
                    artworkBitmap = null
                }
            }
        } else {
            artworkBitmap = null
        }
    }"""
new_effect = """    // artworkBitmap removed in favor of TrackImage"""
content = content.replace(old_effect, new_effect)

# Replace in mini player / upper view?
old_art_1 = """                        when {
                            artworkBitmap != null -> {
                                Image(
                                    bitmap = artworkBitmap!!,
                                    contentDescription = currentTrack?.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            currentTrack?.albumArtUri != null -> {
                                AsyncImage(
                                    model = currentTrack.albumArtUri,
                                    contentDescription = currentTrack.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(96.dp)
                                )
                            }
                        }"""
new_art_1 = """                        if (currentTrack != null) {
                            com.example.ui.components.TrackImage(
                                track = currentTrack,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(96.dp)
                            )
                        }"""
content = content.replace(old_art_1, new_art_1)

# Replace in main player view
old_art_2 = """                                when {
                                    artworkBitmap != null -> {
                                        Image(
                                            bitmap = artworkBitmap!!,
                                            contentDescription = targetTrack?.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    targetTrack?.albumArtUri != null -> {
                                        AsyncImage(
                                            model = targetTrack.albumArtUri,
                                            contentDescription = targetTrack.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    else -> {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(96.dp)
                                        )
                                    }
                                }"""
new_art_2 = """                                if (targetTrack != null) {
                                    com.example.ui.components.TrackImage(
                                        track = targetTrack,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(96.dp)
                                    )
                                }"""
content = content.replace(old_art_2, new_art_2)

with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'w') as f:
    f.write(content)
