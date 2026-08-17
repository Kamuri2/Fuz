package com.example.ui.components

import androidx.compose.material3.MaterialTheme
import android.graphics.BitmapFactory
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Track
import com.example.ui.theme.GlassTextMuted
import com.example.ui.theme.GlassTextSecondary


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    queue: List<Track>,
    currentIndex: Int,
    onDismissRequest: () -> Unit,
    onTrackSelect: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onSetPlayNext: (Track) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color(0xFF1A1A1A),
        scrimColor = Color.Black.copy(alpha = 0.5f),
        dragHandle = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.4f))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Header area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Queue",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                val remainingCount = (queue.size - (currentIndex + 1)).coerceAtLeast(0)
                Text(
                    text = "$remainingCount songs remaining",
                    fontSize = 14.sp,
                    color = GlassTextMuted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay canciones en la cola", color = GlassTextMuted)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.65f)
                        .padding(horizontal = 20.dp)
                ) {
                    val visibleQueue = queue.drop(currentIndex)
                    itemsIndexed(
                        items = visibleQueue,
                        key = { index, track -> "${track.id}_${index + currentIndex}" }
                    ) { index, track ->
                        val actualIndex = index + currentIndex
                        val isCurrent = actualIndex == currentIndex
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                when (value) {
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        onRemoveFromQueue(actualIndex)
                                        true
                                    }
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        onSetPlayNext(track)
                                        true
                                    }
                                    else -> false
                                }
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val direction = dismissState.dismissDirection
                                val color by animateColorAsState(
                                    when (direction) {
                                        SwipeToDismissBoxValue.EndToStart -> Color(0xFFDC2626) // Red for ELIMINAR
                                        SwipeToDismissBoxValue.StartToEnd -> Color(0xFF10B981) // Green for SIGUIENTE
                                        else -> Color.Transparent
                                    },
                                    label = "swipe_bg"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(color)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = if (direction == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart
                                ) {
                                    if (direction == SwipeToDismissBoxValue.EndToStart) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.White)
                                        }
                                    } else if (direction == SwipeToDismissBoxValue.StartToEnd) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.SkipNext, contentDescription = null, tint = Color.White)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "SIGUIENTE",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            },
                            content = {
                                QueueTrackRowItem(
                                    track = track,
                                    isCurrent = isCurrent,
                                    onClick = { onTrackSelect(actualIndex) }
                                )
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QueueTrackRowItem(
    track: Track,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val artworkBitmap: androidx.compose.ui.graphics.ImageBitmap? = null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color(0x1AFFFFFF))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x1AFFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    artworkBitmap != null -> Image(bitmap = artworkBitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    track.albumArtUri != null -> AsyncImage(model = track.albumArtUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else -> Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    fontSize = 12.sp,
                    color = GlassTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // ELIMINAR button was here, I removed it per request.
        }
    }
}
