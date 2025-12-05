package com.stratej.jpod.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Serializable version of Playlist for storage
 */
@Serializable
data class SerializablePlaylist(
    val id: String,
    val name: String,
    val description: String = "",
    val songIds: List<Long> = emptyList(),
    val dateCreated: Long = System.currentTimeMillis(),
    val dateModified: Long = System.currentTimeMillis(),
    val coverArtUri: String? = null
)

/**
 * Manager class for playlist persistence and operations
 */
class PlaylistManager(private val context: Context) {
    
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(
        "music_player_playlists", Context.MODE_PRIVATE
    )
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true 
    }
    
    companion object {
        private const val PLAYLISTS_KEY = "saved_playlists"
        private const val FAVORITES_PLAYLIST_ID = "favorites_playlist"
        private const val RECENTLY_PLAYED_KEY = "recently_played_songs"
        private const val MAX_RECENTLY_PLAYED = 100
    }
    
    /**
     * Get all saved playlists
     */
    suspend fun getAllPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        val playlistsJson = sharedPrefs.getString(PLAYLISTS_KEY, "[]") ?: "[]"
        try {
            val serializablePlaylists = json.decodeFromString<List<SerializablePlaylist>>(playlistsJson)
            serializablePlaylists.map { it.toPlaylist() } + getFavoritesPlaylist()
        } catch (e: Exception) {
            // If there's an error reading playlists, return just the favorites
            listOf(getFavoritesPlaylist())
        }
    }
    
    /**
     * Save a playlist
     */
    suspend fun savePlaylist(playlist: Playlist): Boolean = withContext(Dispatchers.IO) {
        try {
            val currentPlaylists = getAllPlaylists().filter { it.id != playlist.id && it.id != FAVORITES_PLAYLIST_ID }
            val updatedPlaylists = currentPlaylists + playlist
            val serializablePlaylists = updatedPlaylists.map { it.toSerializablePlaylist() }
            val playlistsJson = json.encodeToString(serializablePlaylists)
            sharedPrefs.edit().putString(PLAYLISTS_KEY, playlistsJson).apply()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Delete a playlist
     */
    suspend fun deletePlaylist(playlistId: String): Boolean = withContext(Dispatchers.IO) {
        if (playlistId == FAVORITES_PLAYLIST_ID) return@withContext false // Cannot delete favorites
        
        try {
            val currentPlaylists = getAllPlaylists().filter { it.id != playlistId && it.id != FAVORITES_PLAYLIST_ID }
            val serializablePlaylists = currentPlaylists.map { it.toSerializablePlaylist() }
            val playlistsJson = json.encodeToString(serializablePlaylists)
            sharedPrefs.edit().putString(PLAYLISTS_KEY, playlistsJson).apply()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get a specific playlist by ID
     */
    suspend fun getPlaylistById(playlistId: String): Playlist? = withContext(Dispatchers.IO) {
        if (playlistId == FAVORITES_PLAYLIST_ID) {
            return@withContext getFavoritesPlaylist()
        }
        getAllPlaylists().find { it.id == playlistId }
    }
    
    /**
     * Add song to favorites
     */
    suspend fun addToFavorites(songId: Long): Boolean = withContext(Dispatchers.IO) {
        val favorites = getFavoritesPlaylist()
        if (!favorites.containsSong(songId)) {
            val updatedFavorites = favorites.addSong(songId)
            saveFavoritesPlaylist(updatedFavorites)
        } else {
            true
        }
    }
    
    /**
     * Remove song from favorites
     */
    suspend fun removeFromFavorites(songId: Long): Boolean = withContext(Dispatchers.IO) {
        val favorites = getFavoritesPlaylist()
        if (favorites.containsSong(songId)) {
            val updatedFavorites = favorites.removeSong(songId)
            saveFavoritesPlaylist(updatedFavorites)
        } else {
            true
        }
    }
    
    /**
     * Check if song is in favorites
     */
    suspend fun isFavorite(songId: Long): Boolean = withContext(Dispatchers.IO) {
        getFavoritesPlaylist().containsSong(songId)
    }
    
    /**
     * Add song to recently played
     */
    suspend fun addToRecentlyPlayed(songId: Long) = withContext(Dispatchers.IO) {
        val recentlyPlayedJson = sharedPrefs.getString(RECENTLY_PLAYED_KEY, "[]") ?: "[]"
        try {
            val recentlyPlayed = json.decodeFromString<List<Long>>(recentlyPlayedJson).toMutableList()
            
            // Remove if already exists to avoid duplicates
            recentlyPlayed.remove(songId)
            // Add to beginning
            recentlyPlayed.add(0, songId)
            // Keep only the most recent items
            val trimmed = recentlyPlayed.take(MAX_RECENTLY_PLAYED)
            
            val updatedJson = json.encodeToString(trimmed)
            sharedPrefs.edit().putString(RECENTLY_PLAYED_KEY, updatedJson).apply()
        } catch (e: Exception) {
            // If there's an error, start fresh with just this song
            val newList = listOf(songId)
            val newJson = json.encodeToString(newList)
            sharedPrefs.edit().putString(RECENTLY_PLAYED_KEY, newJson).apply()
        }
    }
    
    /**
     * Get recently played song IDs
     */
    suspend fun getRecentlyPlayedSongIds(): List<Long> = withContext(Dispatchers.IO) {
        val recentlyPlayedJson = sharedPrefs.getString(RECENTLY_PLAYED_KEY, "[]") ?: "[]"
        try {
            json.decodeFromString<List<Long>>(recentlyPlayedJson)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Create a new playlist
     */
    suspend fun createPlaylist(name: String, description: String = ""): Playlist = withContext(Dispatchers.IO) {
        val playlist = Playlist(
            name = name,
            description = description
        )
        savePlaylist(playlist)
        playlist
    }
    
    /**
     * Add song to playlist
     */
    suspend fun addSongToPlaylist(playlistId: String, songId: Long): Boolean = withContext(Dispatchers.IO) {
        if (playlistId == FAVORITES_PLAYLIST_ID) {
            return@withContext addToFavorites(songId)
        }
        
        val playlist = getPlaylistById(playlistId) ?: return@withContext false
        val updatedPlaylist = playlist.addSong(songId)
        savePlaylist(updatedPlaylist)
    }
    
    /**
     * Remove song from playlist
     */
    suspend fun removeSongFromPlaylist(playlistId: String, songId: Long): Boolean = withContext(Dispatchers.IO) {
        if (playlistId == FAVORITES_PLAYLIST_ID) {
            return@withContext removeFromFavorites(songId)
        }
        
        val playlist = getPlaylistById(playlistId) ?: return@withContext false
        val updatedPlaylist = playlist.removeSong(songId)
        savePlaylist(updatedPlaylist)
    }
    
    /**
     * Move song within playlist
     */
    suspend fun moveSongInPlaylist(playlistId: String, fromIndex: Int, toIndex: Int): Boolean = withContext(Dispatchers.IO) {
        val playlist = getPlaylistById(playlistId) ?: return@withContext false
        val updatedPlaylist = playlist.moveSong(fromIndex, toIndex)
        savePlaylist(updatedPlaylist)
    }
    
    private fun getFavoritesPlaylist(): Playlist {
        val favoritesJson = sharedPrefs.getString("favorites_songs", "[]") ?: "[]"
        val favoriteIds = try {
            json.decodeFromString<List<Long>>(favoritesJson)
        } catch (e: Exception) {
            emptyList()
        }
        
        return Playlist(
            id = FAVORITES_PLAYLIST_ID,
            name = "Favorites",
            description = "Your favorite songs",
            songIds = favoriteIds
        )
    }
    
    private fun saveFavoritesPlaylist(playlist: Playlist): Boolean {
        return try {
            val favoritesJson = json.encodeToString(playlist.songIds)
            sharedPrefs.edit().putString("favorites_songs", favoritesJson).apply()
            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Extension functions for converting between Playlist and SerializablePlaylist
 */
private fun Playlist.toSerializablePlaylist(): SerializablePlaylist {
    return SerializablePlaylist(
        id = id,
        name = name,
        description = description,
        songIds = songIds,
        dateCreated = dateCreated,
        dateModified = dateModified,
        coverArtUri = coverArtUri
    )
}

private fun SerializablePlaylist.toPlaylist(): Playlist {
    return Playlist(
        id = id,
        name = name,
        description = description,
        songIds = songIds,
        dateCreated = dateCreated,
        dateModified = dateModified,
        coverArtUri = coverArtUri
    )
}
