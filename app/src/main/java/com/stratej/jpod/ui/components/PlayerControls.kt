package com.stratej.jpod.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stratej.jpod.data.PlayerState
import com.stratej.jpod.data.PlaybackState

/**
 * Composable for music player controls
 */
@Composable
fun PlayerControls(
    playerState: PlayerState,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Song information
        if (playerState.currentSong != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = playerState.currentSong.getDisplayName(),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                Text(
                    text = playerState.currentSong.getDisplayArtist(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Progress bar
        Column(
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            var sliderValue by remember { mutableFloatStateOf(playerState.getProgress()) }
            var isDragging by remember { mutableStateOf(false) }
            
            // Update slider value when not dragging
            LaunchedEffect(playerState.currentPosition) {
                if (!isDragging) {
                    sliderValue = playerState.getProgress()
                }
            }
            
            Slider(
                value = sliderValue,
                onValueChange = { value ->
                    isDragging = true
                    sliderValue = value
                },
                onValueChangeFinished = {
                    isDragging = false
                    val seekPosition = (sliderValue * playerState.duration).toLong()
                    onSeek(seekPosition)
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Time labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = playerState.getFormattedPosition(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = playerState.getFormattedDuration(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Main control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle button
            FilledIconButton(
                onClick = onShuffleToggle,
                modifier = Modifier.size(48.dp),
                colors = if (playerState.isShuffleEnabled) {
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                } else {
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = if (playerState.isShuffleEnabled) "Shuffle On" else "Shuffle Off",
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Previous button
            IconButton(
                onClick = onSkipPrevious,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Play/Pause button
            FilledIconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = when (playerState.playbackState) {
                        PlaybackState.PLAYING -> Icons.Default.Pause
                        PlaybackState.LOADING -> Icons.Default.Refresh
                        else -> Icons.Default.PlayArrow
                    },
                    contentDescription = when (playerState.playbackState) {
                        PlaybackState.PLAYING -> "Pause"
                        PlaybackState.LOADING -> "Loading"
                        else -> "Play"
                    },
                    modifier = Modifier.size(36.dp),
                    tint = Color.White
                )
            }
            
            // Next button
            IconButton(
                onClick = onSkipNext,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Repeat button
            FilledIconButton(
                onClick = onRepeatToggle,
                modifier = Modifier.size(48.dp),
                colors = if (playerState.isRepeatEnabled) {
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                } else {
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = if (playerState.isRepeatEnabled) "Repeat On" else "Repeat Off",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Stop button
        OutlinedButton(
            onClick = onStop,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = "Stop",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Stop")
        }
    }
}
