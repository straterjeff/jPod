package com.stratej.jpod.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.stratej.jpod.data.BrowseCategory
import com.stratej.jpod.data.CategoryItem
import com.stratej.jpod.data.ArtistGroup
import com.stratej.jpod.data.PlaylistContext
import com.stratej.jpod.data.MusicScanner
import com.stratej.jpod.data.PlayerState
import com.stratej.jpod.data.PlaybackState
import com.stratej.jpod.data.Playlist
import com.stratej.jpod.data.PlaylistManager
import com.stratej.jpod.data.Song
import com.stratej.jpod.service.MusicService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing music player state and operations
 */
class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        /**
         * Threshold in milliseconds for skip back behavior.
         * If the current position is less than this value, skip to previous song.
         * Otherwise, restart the current song from the beginning.
         */
        private const val SKIP_BACK_THRESHOLD_MS = 5000L // 5 seconds
    }
    
    private val context = getApplication<Application>()
    private val musicScanner = MusicScanner(context)
    private val playlistManager = PlaylistManager(context)
    
    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    
    // State flows
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()
    
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _currentPlaylist = MutableStateFlow<List<Song>>(emptyList())
    val currentPlaylist: StateFlow<List<Song>> = _currentPlaylist.asStateFlow()
    
    private val _currentSongIndex = MutableStateFlow(0)
    val currentSongIndex: StateFlow<Int> = _currentSongIndex.asStateFlow()
    
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()
    
    private val _categories = MutableStateFlow<Map<BrowseCategory, List<CategoryItem>>>(emptyMap())
    val categories: StateFlow<Map<BrowseCategory, List<CategoryItem>>> = _categories.asStateFlow()
    
    // Artist-grouped albums
    private val _artistGroups = MutableStateFlow<List<ArtistGroup>>(emptyList())
    val artistGroups: StateFlow<List<ArtistGroup>> = _artistGroups.asStateFlow()
    
    private val _currentCategory = MutableStateFlow(BrowseCategory.ALL_SONGS)
    val currentCategory: StateFlow<BrowseCategory> = _currentCategory.asStateFlow()
    
    private val _favorites = MutableStateFlow<List<Long>>(emptyList())
    val favorites: StateFlow<List<Long>> = _favorites.asStateFlow()
    
    private val _recentlyPlayed = MutableStateFlow<List<Song>>(emptyList())
    val recentlyPlayed: StateFlow<List<Song>> = _recentlyPlayed.asStateFlow()
    
    // Current playlist context for proper shuffle/repeat behavior
    private val _currentPlaylistContext = MutableStateFlow<PlaylistContext?>(null)
    val currentPlaylistContext: StateFlow<PlaylistContext?> = _currentPlaylistContext.asStateFlow()
    
    // Player listener for state changes
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            updatePlaybackState(playbackState)
        }
        
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val state = if (isPlaying) PlaybackState.PLAYING else PlaybackState.PAUSED
            _playerState.value = _playerState.value.copy(playbackState = state)
        }
        
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let {
                updateCurrentSong(it.mediaId)
            }
        }
        
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _playerState.value = _playerState.value.copy(isShuffleEnabled = shuffleModeEnabled)
        }
        
        override fun onRepeatModeChanged(repeatMode: Int) {
            val isRepeatEnabled = repeatMode != Player.REPEAT_MODE_OFF
            Log.d("MusicPlayerViewModel", "onRepeatModeChanged: repeatMode=$repeatMode, isRepeatEnabled=$isRepeatEnabled")
            _playerState.value = _playerState.value.copy(isRepeatEnabled = isRepeatEnabled)
        }
    }
    
    init {
        initializeMediaController()
        startPositionUpdates()
        loadSongs()
        loadCategories()
        loadPlaylists()
    }
    
    private fun initializeMediaController() {
        val sessionToken = SessionToken(context, android.content.ComponentName(context, MusicService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
            mediaController?.addListener(playerListener)
            
            // Sync initial shuffle/repeat states from player
            mediaController?.let { controller ->
                _playerState.value = _playerState.value.copy(
                    isShuffleEnabled = controller.shuffleModeEnabled,
                    isRepeatEnabled = controller.repeatMode != Player.REPEAT_MODE_OFF
                )
            }
        }, MoreExecutors.directExecutor())
    }
    
    private fun loadSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val scannedSongs = musicScanner.scanForAudioFiles()
                _songs.value = scannedSongs
                loadRecentlyPlayed() // Load recently played songs after songs are loaded
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val artists = musicScanner.getAllArtists()
                val albums = musicScanner.getAllAlbums()
                val genres = musicScanner.getAllGenres()
                val years = musicScanner.getAllYears()
                
                _categories.value = mapOf(
                    BrowseCategory.ARTISTS to artists,
                    BrowseCategory.ALBUMS to albums,
                    BrowseCategory.GENRES to genres,
                    BrowseCategory.ALL_SONGS to years // Using for years temporarily
                )
                
                // Load artist-grouped albums
                val artistGroups = musicScanner.getAlbumsGroupedByArtist()
                _artistGroups.value = artistGroups
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun loadPlaylists() {
        viewModelScope.launch {
            try {
                val allPlaylists = playlistManager.getAllPlaylists()
                _playlists.value = allPlaylists
                
                // Load favorites
                val favoritesPlaylist = allPlaylists.find { it.id == "favorites_playlist" }
                _favorites.value = favoritesPlaylist?.songIds ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun loadRecentlyPlayed() {
        viewModelScope.launch {
            try {
                val recentlyPlayedIds = playlistManager.getRecentlyPlayedSongIds()
                val allSongs = _songs.value
                val recentSongs = recentlyPlayedIds.mapNotNull { id ->
                    allSongs.find { it.id == id }
                }
                _recentlyPlayed.value = recentSongs
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun startPositionUpdates() {
        viewModelScope.launch {
            while (true) {
                mediaController?.let { controller ->
                    if (controller.isPlaying) {
                        val currentPosition = controller.currentPosition
                        val duration = controller.duration
                        val currentSong = getCurrentSong()
                        
                        _playerState.value = _playerState.value.copy(
                            currentPosition = currentPosition,
                            duration = duration,
                            currentSong = currentSong,
                            playbackState = if (controller.isPlaying) PlaybackState.PLAYING else PlaybackState.PAUSED
                        )
                    }
                }
                delay(1000) // Update every second
            }
        }
    }
    
    private fun getCurrentSong(): Song? {
        val playlist = _currentPlaylist.value
        val index = _currentSongIndex.value
        return if (playlist.isNotEmpty() && index in playlist.indices) {
            playlist[index]
        } else null
    }
    
    /**
     * Play a specific song
     */
    fun playSong(song: Song) {
        val mediaItem = MediaItem.Builder()
            .setUri(song.uri)
            .setMediaId(song.id.toString())
            .build()
        
        mediaController?.setMediaItem(mediaItem)
        mediaController?.prepare()
        mediaController?.play()
        
        _currentPlaylist.value = listOf(song)
        _currentSongIndex.value = 0
        updatePlayerState(PlaybackState.PLAYING, song)
        
        // Track recently played
        viewModelScope.launch {
            playlistManager.addToRecentlyPlayed(song.id)
            loadRecentlyPlayed()
        }
    }
    
    /**
     * Play a list of songs starting from a specific index
     */
    fun playPlaylist(playlist: List<Song>, startIndex: Int = 0, context: PlaylistContext? = null) {
        if (playlist.isNotEmpty() && startIndex in playlist.indices) {
            
            // Store the playlist context for proper shuffle/repeat behavior
            _currentPlaylistContext.value = context ?: PlaylistContext(
                type = BrowseCategory.ALL_SONGS,
                itemId = null,
                itemName = "All Songs",
                allSongs = playlist,
                originalOrder = playlist
            )
            
            // Apply current shuffle state to the playlist
            val playlistToUse = if (_playerState.value.isShuffleEnabled) {
                shufflePlaylist(playlist, startIndex)
            } else {
                playlist
            }
            
            val actualStartIndex = if (_playerState.value.isShuffleEnabled) 0 else startIndex
            
            val mediaItems = playlistToUse.map { song ->
                MediaItem.Builder()
                    .setUri(song.uri)
                    .setMediaId(song.id.toString())
                    .build()
            }
            
            mediaController?.setMediaItems(mediaItems, actualStartIndex, 0L)
            mediaController?.prepare()
            mediaController?.play()
            
            _currentPlaylist.value = playlistToUse
            _currentSongIndex.value = actualStartIndex
            updatePlayerState(PlaybackState.PLAYING, playlistToUse[actualStartIndex])
            
            // Track recently played
            viewModelScope.launch {
                playlistManager.addToRecentlyPlayed(playlistToUse[actualStartIndex].id)
                loadRecentlyPlayed()
            }
        }
    }
    
    /**
     * Create a shuffled playlist with the current song moved to the front
     */
    private fun shufflePlaylist(songs: List<Song>, currentIndex: Int): List<Song> {
        val mutableSongs = songs.toMutableList()
        val currentSong = mutableSongs.removeAt(currentIndex)
        mutableSongs.shuffle()
        return listOf(currentSong) + mutableSongs
    }
    
    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.isPlaying) {
                controller.pause()
            } else {
                controller.play()
            }
        }
    }
    
    /**
     * Stop playback
     */
    fun stop() {
        mediaController?.stop()
        _playerState.value = _playerState.value.copy(
            playbackState = PlaybackState.STOPPED,
            currentPosition = 0L
        )
    }
    
    /**
     * Skip to next track
     */
    fun skipToNext() {
        mediaController?.let { controller ->
            if (controller.hasNextMediaItem()) {
                controller.seekToNext()
                // Index update handled by onMediaItemTransition listener
            }
        }
    }
    
    /**
     * Skip to previous track
     * If the current position is less than SKIP_BACK_THRESHOLD_MS, skip to the previous song.
     * Otherwise, restart the current song from the beginning.
     */
    fun skipToPrevious() {
        mediaController?.let { controller ->
            // Get position from controller (most accurate)
            val currentPosition = controller.currentPosition
            val playlist = _currentPlaylist.value
            val currentIndex = _currentSongIndex.value
            
            if (currentPosition >= SKIP_BACK_THRESHOLD_MS) {
                // Restart the current song from the beginning
                controller.seekTo(0L)
                _playerState.value = _playerState.value.copy(currentPosition = 0L)
            } else {
                // Skip to previous song if available
                if (currentIndex > 0 && playlist.isNotEmpty()) {
                    // We have a previous song - seek to it directly using the media item index
                    val previousIndex = currentIndex - 1
                    val wasPlaying = controller.isPlaying
                    
                    // Seek to the previous media item
                    controller.seekTo(previousIndex, 0L)
                    
                    // Update our state immediately
                    _currentSongIndex.value = previousIndex
                    val previousSong = playlist[previousIndex]
                    _playerState.value = _playerState.value.copy(
                        currentSong = previousSong,
                        currentPosition = 0L
                    )
                    
                    // Restore playback state
                    if (wasPlaying) {
                        controller.play()
                    }
                    
                    // Track recently played
                    viewModelScope.launch {
                        playlistManager.addToRecentlyPlayed(previousSong.id)
                        loadRecentlyPlayed()
                    }
                } else if (controller.hasPreviousMediaItem()) {
                    // Fallback: use controller's seekToPrevious
                    controller.seekToPrevious()
                    // Index update handled by onMediaItemTransition listener
                } else {
                    // If no previous song, just restart the current song
                    controller.seekTo(0L)
                    _playerState.value = _playerState.value.copy(currentPosition = 0L)
                }
            }
        }
    }
    
    /**
     * Seek to a specific position
     */
    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _playerState.value = _playerState.value.copy(currentPosition = positionMs)
    }
    
    /**
     * Set shuffle mode - mutually exclusive with repeat
     */
    fun setShuffleEnabled(enabled: Boolean) {
        if (enabled) {
            // Turn off repeat when enabling shuffle (mutually exclusive)
            setRepeatEnabled(false)
        }
        
        val currentContext = _currentPlaylistContext.value
        val currentPlaylist = _currentPlaylist.value
        val currentSong = _playerState.value.currentSong
        
        if (currentContext != null && currentPlaylist.isNotEmpty() && currentSong != null) {
            val newPlaylist = if (enabled) {
                // Shuffle: get all songs from context and shuffle them
                val allSongs = currentContext.allSongs
                val currentIndex = allSongs.indexOfFirst { it.id == currentSong.id }
                if (currentIndex >= 0) {
                    shufflePlaylist(allSongs, currentIndex)
                } else {
                    allSongs.shuffled()
                }
            } else {
                // Un-shuffle: restore original order from context
                currentContext.originalOrder
            }
            
            // Update the playlist in the player
            val currentSongIndex = newPlaylist.indexOfFirst { it.id == currentSong.id }
            if (currentSongIndex >= 0) {
                val mediaItems = newPlaylist.map { song ->
                    MediaItem.Builder()
                        .setUri(song.uri)
                        .setMediaId(song.id.toString())
                        .build()
                }
                
                // Get current position to maintain playback
                val currentPosition = mediaController?.currentPosition ?: 0L
                
                mediaController?.let { controller ->
                    controller.setMediaItems(mediaItems, currentSongIndex, currentPosition)
                    controller.shuffleModeEnabled = enabled
                    controller.prepare()
                    if (_playerState.value.playbackState == PlaybackState.PLAYING) {
                        controller.play()
                    }
                }
                
                _currentPlaylist.value = newPlaylist
                _currentSongIndex.value = currentSongIndex
            }
        } else {
            // Fallback: just set the shuffle mode on the player
            mediaController?.shuffleModeEnabled = enabled
        }
        
        _playerState.value = _playerState.value.copy(isShuffleEnabled = enabled)
    }
    
    /**
     * Set repeat mode - mutually exclusive with shuffle
     */
    fun setRepeatEnabled(enabled: Boolean) {
        // Store the current shuffle state before turning it off
        val wasShuffleEnabled = _playerState.value.isShuffleEnabled
        
        if (enabled) {
            // Turn off shuffle when enabling repeat (mutually exclusive)
            mediaController?.shuffleModeEnabled = false
            _playerState.value = _playerState.value.copy(isShuffleEnabled = false)
            
            // If shuffle was on, restore original order
            val currentContext = _currentPlaylistContext.value
            val currentSong = _playerState.value.currentSong
            if (currentContext != null && currentSong != null && wasShuffleEnabled) {
                val originalPlaylist = currentContext.originalOrder
                val currentSongIndex = originalPlaylist.indexOfFirst { it.id == currentSong.id }
                
                if (currentSongIndex >= 0) {
                    val mediaItems = originalPlaylist.map { song ->
                        MediaItem.Builder()
                            .setUri(song.uri)
                            .setMediaId(song.id.toString())
                            .build()
                    }
                    
                    val currentPosition = mediaController?.currentPosition ?: 0L
                    
                    mediaController?.let { controller ->
                        controller.setMediaItems(mediaItems, currentSongIndex, currentPosition)
                        controller.prepare()
                        if (_playerState.value.playbackState == PlaybackState.PLAYING) {
                            controller.play()
                        }
                    }
                    
                    _currentPlaylist.value = originalPlaylist
                    _currentSongIndex.value = currentSongIndex
                }
            }
        }
        
        // Set repeat mode: REPEAT_MODE_ONE repeats the current song
        val repeatMode = if (enabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        Log.d("MusicPlayerViewModel", "setRepeatEnabled: enabled=$enabled, setting repeatMode=$repeatMode")
        mediaController?.repeatMode = repeatMode
        _playerState.value = _playerState.value.copy(isRepeatEnabled = enabled)
    }
    
    /**
     * Search songs
     */
    fun searchSongs(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val searchResults = musicScanner.searchSongs(query)
                _songs.value = searchResults
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Refresh song list
     */
    fun refreshSongs() {
        loadSongs()
        loadCategories()
    }
    
    // === Playlist Management Functions ===
    
    /**
     * Create a new playlist
     */
    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            try {
                playlistManager.createPlaylist(name, description)
                loadPlaylists()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Delete a playlist
     */
    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            try {
                playlistManager.deletePlaylist(playlistId)
                loadPlaylists()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Add song to playlist
     */
    fun addSongToPlaylist(playlistId: String, songId: Long) {
        viewModelScope.launch {
            try {
                playlistManager.addSongToPlaylist(playlistId, songId)
                loadPlaylists()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Remove song from playlist
     */
    fun removeSongFromPlaylist(playlistId: String, songId: Long) {
        viewModelScope.launch {
            try {
                playlistManager.removeSongFromPlaylist(playlistId, songId)
                loadPlaylists()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Add/remove song to/from favorites
     */
    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            try {
                val isFav = playlistManager.isFavorite(songId)
                if (isFav) {
                    playlistManager.removeFromFavorites(songId)
                } else {
                    playlistManager.addToFavorites(songId)
                }
                loadPlaylists()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Check if song is favorite
     */
    fun isFavorite(songId: Long): Boolean {
        return _favorites.value.contains(songId)
    }
    
    /**
     * Get songs for a specific playlist
     */
    fun getPlaylistSongs(playlistId: String): List<Song> {
        val playlist = _playlists.value.find { it.id == playlistId } ?: return emptyList()
        val allSongs = _songs.value
        return playlist.songIds.mapNotNull { id ->
            allSongs.find { it.id == id }
        }
    }
    
    // === Category Browsing Functions ===
    
    /**
     * Set current browsing category
     */
    fun setBrowseCategory(category: BrowseCategory) {
        _currentCategory.value = category
    }
    
    /**
     * Get songs for a specific category item (artist, album, genre, etc.)
     */
    fun getCategorySongs(category: BrowseCategory, itemId: String): List<Song> {
        return when (category) {
            BrowseCategory.ARTISTS -> {
                _songs.value.filter { it.artist.equals(itemId, ignoreCase = true) }
            }
            BrowseCategory.ALBUMS -> {
                // itemId is now just the album name (no longer artist|album format)
                _songs.value.filter { 
                    it.album.equals(itemId, ignoreCase = true) 
                }.sortedBy { it.track } // Sort by track number for proper album order
            }
            BrowseCategory.GENRES -> {
                _songs.value.filter { it.genre.equals(itemId, ignoreCase = true) }
            }
            BrowseCategory.RECENTLY_ADDED -> {
                _recentlyPlayed.value
            }
            BrowseCategory.FAVORITES -> {
                val favoriteIds = _favorites.value
                _songs.value.filter { favoriteIds.contains(it.id) }
            }
            else -> _songs.value
        }
    }
    
    /**
     * Filter songs by year
     */
    fun getSongsByYear(year: Int): List<Song> {
        return _songs.value.filter { it.year == year }
    }
    
    /**
     * Toggle expansion state of an artist group
     */
    fun toggleArtistGroupExpansion(artistName: String) {
        val currentGroups = _artistGroups.value.toMutableList()
        val groupIndex = currentGroups.indexOfFirst { it.artistName == artistName }
        if (groupIndex != -1) {
            currentGroups[groupIndex] = currentGroups[groupIndex].copy(
                isExpanded = !currentGroups[groupIndex].isExpanded
            )
            _artistGroups.value = currentGroups
        }
    }
    
    private fun updatePlayerState(playbackState: PlaybackState, song: Song) {
        _playerState.value = _playerState.value.copy(
            playbackState = playbackState,
            currentSong = song
        )
    }
    
    private fun updatePlaybackState(playbackState: Int) {
        val state = when (playbackState) {
            Player.STATE_IDLE -> PlaybackState.STOPPED
            Player.STATE_BUFFERING -> PlaybackState.LOADING
            Player.STATE_READY -> if (mediaController?.isPlaying == true) PlaybackState.PLAYING else PlaybackState.PAUSED
            Player.STATE_ENDED -> PlaybackState.STOPPED
            else -> PlaybackState.STOPPED
        }
        _playerState.value = _playerState.value.copy(playbackState = state)
    }
    
    private fun updateCurrentSong(mediaId: String?) {
        mediaId?.let { id ->
            val songId = id.toLongOrNull()
            val playlist = _currentPlaylist.value
            val song = playlist.find { it.id == songId }
            song?.let {
                _playerState.value = _playerState.value.copy(currentSong = it)
                _currentSongIndex.value = playlist.indexOf(it)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}
