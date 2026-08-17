package com.example.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Track
import com.example.ui.theme.GlassTextMuted
import com.example.ui.theme.GlassTextSecondary

@Composable
fun FoldersScreen(
    settings: com.example.model.AppSettings,
    tracks: List<Track>,
    currentTrack: Track?,
    onPlayFolder: (List<Track>, Int) -> Unit,
    onPickFolderUri: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = selectedFolder != null) {
        selectedFolder = null
    }

    val folderGroups = remember(tracks) {
        tracks.groupBy { it.folderName }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedFolder != null) {
                    IconButton(onClick = { selectedFolder = null }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = selectedFolder ?: com.example.ui.Translations.get(settings.appLanguage, "folders"),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedFolder == null) {
            // Folder Grid/List
            if (folderGroups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No music folders found", color = GlassTextMuted)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(folderGroups.keys.toList()) { folderName ->
                        val folderTracks = folderGroups[folderName] ?: emptyList()
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFolder = folderName },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x1AFFFFFF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = folderName,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${folderTracks.size} audio files",
                                        fontSize = 13.sp,
                                        color = GlassTextSecondary
                                    )
                                }

                                IconButton(onClick = { onPlayFolder(folderTracks, 0) }) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play Folder", tint = Color.White)
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        } else {
            // Folder Track List Details
            val activeTracks = folderGroups[selectedFolder] ?: emptyList()
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(activeTracks) { track ->
                    val isCurrent = currentTrack?.id == track.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayFolder(activeTracks, activeTracks.indexOf(track)) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color(0x1AFFFFFF))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White
                                )
                                Text(
                                    text = track.artist,
                                    fontSize = 13.sp,
                                    color = GlassTextSecondary
                                )
                            }
                            Text(text = track.bitrate, fontSize = 12.sp, color = GlassTextMuted)
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}
