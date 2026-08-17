with open('app/src/main/java/com/example/ui/screens/ArtistsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("} else {\\n                        TrackImage(track = track, modifier = Modifier.fillMaxSize())\\n                    }", "} else if (track.albumArtUri != null) {\\n                        AsyncImage(model = track.albumArtUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())\\n                    } else {\\n                        Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))\\n                    }")
content = content.replace("} else {\n                        TrackImage(track = track, modifier = Modifier.fillMaxSize())\n                    }", "} else if (track.albumArtUri != null) {\n                        AsyncImage(model = track.albumArtUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())\n                    } else {\n                        Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))\n                    }")

with open('app/src/main/java/com/example/ui/screens/ArtistsScreen.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ui/components/QueueBottomSheet.kt', 'r') as f:
    qcontent = f.read()

qcontent = qcontent.replace("true -> TrackImage(track = track, modifier = Modifier.fillMaxSize())", "track.albumArtUri != null -> AsyncImage(model = track.albumArtUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())")

with open('app/src/main/java/com/example/ui/components/QueueBottomSheet.kt', 'w') as f:
    f.write(qcontent)
