package com.stratej.jpod.data

import android.net.Uri

/**
 * Data class representing a song/audio file
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String = "",
    val track: Int = 0,
    val year: Int = 0,
    val duration: Long, // Duration in milliseconds
    val uri: Uri,
    val albumArt: Uri? = null,
    val dateAdded: Long = 0L,
    val size: Long = 0L,
    val mimeType: String = ""
) {
    /**
     * Format duration from milliseconds to MM:SS format
     */
    fun getFormattedDuration(): String {
        val totalSeconds = duration / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    /**
     * Get display name for the song
     */
    fun getDisplayName(): String {
        return if (title.isNotBlank()) title else "Unknown Title"
    }
    
    /**
     * Get display artist name
     */
    fun getDisplayArtist(): String {
        return if (artist.isNotBlank()) artist else "Unknown Artist"
    }
    
    /**
     * Get display album name
     */
    fun getDisplayAlbum(): String {
        return if (album.isNotBlank()) album else "Unknown Album"
    }
    
    /**
     * Get display genre name
     */
    fun getDisplayGenre(): String {
        return if (genre.isNotBlank()) genre else "Unknown Genre"
    }
    
    /**
     * Get formatted year
     */
    fun getDisplayYear(): String {
        return if (year > 0) year.toString() else "Unknown Year"
    }
    
    /**
     * Get formatted track number
     */
    fun getDisplayTrack(): String {
        return if (track > 0) track.toString() else ""
    }
}
