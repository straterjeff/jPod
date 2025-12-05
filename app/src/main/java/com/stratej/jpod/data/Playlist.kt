package com.stratej.jpod.data

import java.util.UUID

/**
 * Data class representing a playlist
 */
data class Playlist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val songIds: List<Long> = emptyList(),
    val dateCreated: Long = System.currentTimeMillis(),
    val dateModified: Long = System.currentTimeMillis(),
    val coverArtUri: String? = null
) {
    /**
     * Get the number of songs in this playlist
     */
    fun getSongCount(): Int = songIds.size
    
    /**
     * Get formatted duration if we have access to songs
     */
    fun getFormattedDuration(songs: List<Song>): String {
        val totalMillis = songs.filter { song -> songIds.contains(song.id) }
            .sumOf { it.duration }
        val totalSeconds = totalMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, totalSeconds % 60)
            else -> String.format("%02d:%02d", minutes, totalSeconds % 60)
        }
    }
    
    /**
     * Add a song to this playlist
     */
    fun addSong(songId: Long): Playlist {
        return if (!songIds.contains(songId)) {
            copy(
                songIds = songIds + songId,
                dateModified = System.currentTimeMillis()
            )
        } else {
            this
        }
    }
    
    /**
     * Remove a song from this playlist
     */
    fun removeSong(songId: Long): Playlist {
        return copy(
            songIds = songIds - songId,
            dateModified = System.currentTimeMillis()
        )
    }
    
    /**
     * Move a song to a new position in the playlist
     */
    fun moveSong(fromIndex: Int, toIndex: Int): Playlist {
        if (fromIndex < 0 || fromIndex >= songIds.size || toIndex < 0 || toIndex >= songIds.size) {
            return this
        }
        
        val mutableList = songIds.toMutableList()
        val item = mutableList.removeAt(fromIndex)
        mutableList.add(toIndex, item)
        
        return copy(
            songIds = mutableList,
            dateModified = System.currentTimeMillis()
        )
    }
    
    /**
     * Check if playlist contains a specific song
     */
    fun containsSong(songId: Long): Boolean = songIds.contains(songId)
}

/**
 * Enum for different browse categories
 */
enum class BrowseCategory {
    ALL_SONGS,
    ARTISTS,
    ALBUMS,
    GENRES,
    PLAYLISTS,
    RECENTLY_ADDED,
    RECENTLY_PLAYED,
    FAVORITES
}

/**
 * Data class for category items (artist, album, genre groupings)
 */
data class CategoryItem(
    val id: String,
    val name: String,
    val songCount: Int,
    val category: BrowseCategory,
    val description: String = "",
    val imageUri: String? = null
) {
    /**
     * Get display text for song count
     */
    fun getSongCountText(): String {
        return "$songCount song${if (songCount != 1) "s" else ""}"
    }
}

/**
 * Data class representing an artist with their albums grouped together
 */
data class ArtistGroup(
    val artistName: String,
    val albums: List<CategoryItem>,
    val totalSongs: Int,
    var isExpanded: Boolean = false
) {
    /**
     * Get display text for album and song count
     */
    fun getAlbumCountText(): String {
        return "${albums.size} album${if (albums.size != 1) "s" else ""}, $totalSongs song${if (totalSongs != 1) "s" else ""}"
    }
}

/**
 * Data class to track the current playlist context for proper shuffle/repeat behavior
 */
data class PlaylistContext(
    val type: BrowseCategory,
    val itemId: String?, // null for ALL_SONGS, otherwise artist/album/genre ID
    val itemName: String?, // human-readable name for the context
    val allSongs: List<Song>, // all songs in this context for shuffling
    val originalOrder: List<Song> // original order for when shuffle is turned off
)
