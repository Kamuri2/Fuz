package com.example.ui.screens


import androidx.compose.material3.MaterialTheme
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

import androidx.compose.material.icons.filled.Add

import androidx.compose.material.icons.filled.Favorite

import androidx.compose.material.icons.filled.PlaylistPlay

import androidx.compose.material3.Button

import androidx.compose.material3.ButtonDefaults

import androidx.compose.material3.Card

import androidx.compose.material3.CardDefaults

import androidx.compose.material3.Icon

import androidx.compose.material3.OutlinedTextField

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler

import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember

import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import com.example.model.Track

import com.example.ui.theme.GlassTextMuted

import com.example.ui.theme.GlassTextSecondary

@Composable
fun PlaylistsScreen(
    settings: com.example.model.AppSettings,
    playlistsMap: Map<String, List<Track>>,
    favorites: Set<Long>,
    allTracks: List<Track>,
    currentTrack: Track?,
    onCreatePlaylist: (String) -> Unit,
    onPlayPlaylist: (List<Track>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var selectedPlaylistName by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = selectedPlaylistName != null) {
        selectedPlaylistName = null
    }

    val favoriteTracks = remember(allTracks, favorites) {
        allTracks.filter { favorites.contains(it.id) }
    }

    if (selectedPlaylistName != null) {
        val tracks = if (selectedPlaylistName == "Mis Favoritas") favoriteTracks else playlistsMap[selectedPlaylistName] ?: emptyList()
        com.example.ui.screens.AlbumDetailScreen(
            albumName = selectedPlaylistName!!,
            tracks = tracks,
            onBack = { selectedPlaylistName = null },
            onPlayTrack = { idx -> onPlayPlaylist(tracks, idx) },
            onShuffleAll = { onPlayPlaylist(tracks.shuffled(), 0) }
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedPlaylistName ?: com.example.ui.Translations.get(settings.appLanguage, "playlists"),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Text(com.example.ui.Translations.get(settings.appLanguage, "create"), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        if (showCreateDialog) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(com.example.ui.Translations.get(settings.appLanguage, "new_playlist"), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text(com.example.ui.Translations.get(settings.appLanguage, "list_name")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { showCreateDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                            Text(com.example.ui.Translations.get(settings.appLanguage, "cancel"))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            if (newPlaylistName.isNotBlank()) {
                                onCreatePlaylist(newPlaylistName)
                                newPlaylistName = ""
                                showCreateDialog = false
                            }
                        }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                            Text(com.example.ui.Translations.get(settings.appLanguage, "save"), color = Color.Black)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Favorites Special Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    selectedPlaylistName = "Mis Favoritas"
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1510))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(com.example.ui.Translations.get(settings.appLanguage, "favorite_songs"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("${favoriteTracks.size} " + com.example.ui.Translations.get(settings.appLanguage, "songs_marked"), fontSize = 13.sp, color = GlassTextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Custom User Playlists
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(playlistsMap.keys.toList()) { name ->
                val list = playlistsMap[name] ?: emptyList()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedPlaylistName = name
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.PlaylistPlay, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = "${list.size} " + com.example.ui.Translations.get(settings.appLanguage, "songs"), fontSize = 13.sp, color = GlassTextMuted)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}
}
