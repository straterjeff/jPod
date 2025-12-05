package com.stratej.jpod.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stratej.jpod.data.BrowseCategory
import com.stratej.jpod.data.PlaybackState
import com.stratej.jpod.data.PlaylistContext
import com.stratej.jpod.ui.components.PlayerControls
import com.stratej.jpod.ui.components.SongListItem
import com.stratej.jpod.viewmodel.MusicPlayerViewModel

/**
 * Main music player screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    onBrowseCategory: (BrowseCategory) -> Unit = {},
    onViewPlaylists: () -> Unit = {},
    viewModel: MusicPlayerViewModel = viewModel()
) {
    val songs by viewModel.songs.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var showPlayerControls by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    
    // Show player controls when a song is selected and not stopped
    LaunchedEffect(playerState.currentSong, playerState.playbackState) {
        showPlayerControls = playerState.currentSong != null && 
                            playerState.playbackState != PlaybackState.STOPPED
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Music Player") },
            actions = {
                IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                IconButton(onClick = onViewPlaylists) {
                    Icon(Icons.Default.QueueMusic, contentDescription = "Playlists")
                }
                IconButton(onClick = { viewModel.refreshSongs() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        )
        
        // Search bar
        if (isSearchExpanded) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    if (it.isBlank()) {
                        viewModel.refreshSongs()
                    } else {
                        viewModel.searchSongs(it)
                    }
                },
                label = { Text("Search songs...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
        
        // Category chips for quick access
        if (!isSearchExpanded) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        onClick = { onBrowseCategory(BrowseCategory.ARTISTS) },
                        label = { Text("Artists") },
                        selected = false,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
                item {
                    FilterChip(
                        onClick = { onBrowseCategory(BrowseCategory.ALBUMS) },
                        label = { Text("Albums") },
                        selected = false,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Album,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
                item {
                    FilterChip(
                        onClick = { onBrowseCategory(BrowseCategory.GENRES) },
                        label = { Text("Genres") },
                        selected = false,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
                item {
                    FilterChip(
                        onClick = { onBrowseCategory(BrowseCategory.FAVORITES) },
                        label = { Text("Favorites") },
                        selected = false,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
                item {
                    FilterChip(
                        onClick = { onBrowseCategory(BrowseCategory.RECENTLY_PLAYED) },
                        label = { Text("Recent") },
                        selected = false,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        if (showPlayerControls) {
            // Player Controls Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                PlayerControls(
                    playerState = playerState,
                    onPlayPause = { viewModel.togglePlayPause() },
                    onStop = { 
                        viewModel.stop()
                        showPlayerControls = false
                    },
                    onSkipNext = { viewModel.skipToNext() },
                    onSkipPrevious = { viewModel.skipToPrevious() },
                    onSeek = { position -> viewModel.seekTo(position) },
                    onShuffleToggle = { viewModel.setShuffleEnabled(!playerState.isShuffleEnabled) },
                    onRepeatToggle = { viewModel.setRepeatEnabled(!playerState.isRepeatEnabled) },
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        // Songs List
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when {
                isLoading -> {
                    // Loading state
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading songs...")
                    }
                }
                
                songs.isEmpty() -> {
                    // Empty state
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No songs found",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Make sure you have audio files on your device and have granted storage permissions.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
                
                else -> {
                    // Songs list
                    val listState = rememberLazyListState()
                    
                    // Auto-scroll to currently playing song
                    LaunchedEffect(playerState.currentSong?.id) {
                        playerState.currentSong?.let { currentSong ->
                            val currentIndex = songs.indexOfFirst { it.id == currentSong.id }
                            if (currentIndex >= 0) {
                                // Scroll to the item, accounting for the header item (index + 1)
                                listState.animateScrollToItem(currentIndex + 1)
                            }
                        }
                    }
                    
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        item {
                            // Songs count header
                            Text(
                                text = "${songs.size} song${if (songs.size != 1) "s" else ""} found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        
                        items(
                            items = songs,
                            key = { song -> song.id }
                        ) { song ->
                            SongListItem(
                                song = song,
                                onSongClick = { clickedSong ->
                                    val context = PlaylistContext(
                                        type = BrowseCategory.ALL_SONGS,
                                        itemId = null,
                                        itemName = "All Songs",
                                        allSongs = songs,
                                        originalOrder = songs
                                    )
                                    viewModel.playPlaylist(songs, songs.indexOf(clickedSong), context)
                                    showPlayerControls = true
                                },
                                isCurrentlyPlaying = playerState.currentSong?.id == song.id && 
                                    playerState.playbackState == PlaybackState.PLAYING,
                                isFavorite = viewModel.isFavorite(song.id),
                                onFavoriteClick = { clickedSong ->
                                    viewModel.toggleFavorite(clickedSong.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
