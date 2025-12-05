package com.stratej.jpod.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility class to scan for audio files on the device
 */
class MusicScanner(private val context: Context) {
    
    private val contentResolver: ContentResolver = context.contentResolver
    
    companion object {
        // jPod Music directory - dedicated folder for clean music filtering
        const val JPOD_MUSIC_DIR = "/storage/emulated/0/Music/jPod/"
    }
    
    /**
     * Scan for all audio files on the device
     */
    suspend fun scanForAudioFiles(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            "genre" // MediaStore.Audio.Genres.NAME - using string as it's not always available
        )
        
        // Filter to show only music files from jPod directory - simple and clean!
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} = 1 AND " +
                "${MediaStore.Audio.Media.DATA} LIKE '${JPOD_MUSIC_DIR}%'"
        
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
        
        val cursor: Cursor? = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )

        cursor?.use { cur ->
            if (cur.count == 0) {
                return@withContext emptyList()
            }
            
            val idColumn = cur.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cur.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cur.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cur.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cur.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = cur.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val dateAddedColumn = cur.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val sizeColumn = cur.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val mimeTypeColumn = cur.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val albumIdColumn = cur.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val trackColumn = cur.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearColumn = cur.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val genreColumn = cur.getColumnIndex("genre") // May not exist on all devices
            
            while (cur.moveToNext()) {
                val id = cur.getLong(idColumn)
                val title = cur.getString(titleColumn) ?: "Unknown Title"
                val artist = cur.getString(artistColumn) ?: "Unknown Artist"
                val album = cur.getString(albumColumn) ?: "Unknown Album"
                val duration = cur.getLong(durationColumn)
                val data = cur.getString(dataColumn) ?: ""
                val dateAdded = cur.getLong(dateAddedColumn)
                val size = cur.getLong(sizeColumn)
                val mimeType = cur.getString(mimeTypeColumn) ?: ""
                val albumId = cur.getLong(albumIdColumn)
                val track = cur.getInt(trackColumn)
                val year = cur.getInt(yearColumn)
                val genre = if (genreColumn >= 0) cur.getString(genreColumn) ?: "" else ""
                
                // Create content URI for the audio file
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                
                // Create album art URI
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )
                
                val song = Song(
                    id = id,
                    title = title,
                    artist = artist,
                    album = album,
                    genre = genre,
                    track = track,
                    year = year,
                    duration = duration,
                    uri = contentUri,
                    albumArt = albumArtUri,
                    dateAdded = dateAdded,
                    size = size,
                    mimeType = mimeType
                )
                
                songs.add(song)
            }
        }
        
        songs
    }
    
    /**
     * Get songs by artist
     */
    suspend fun getSongsByArtist(artist: String): List<Song> = withContext(Dispatchers.IO) {
        scanForAudioFiles().filter { it.artist.equals(artist, ignoreCase = true) }
    }
    
    /**
     * Get songs by album
     */
    suspend fun getSongsByAlbum(album: String): List<Song> = withContext(Dispatchers.IO) {
        scanForAudioFiles().filter { it.album.equals(album, ignoreCase = true) }
    }
    
    /**
     * Get songs by genre
     */
    suspend fun getSongsByGenre(genre: String): List<Song> = withContext(Dispatchers.IO) {
        scanForAudioFiles().filter { it.genre.equals(genre, ignoreCase = true) }
    }
    
    /**
     * Search songs by title, artist, album, or genre
     */
    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        scanForAudioFiles().filter { 
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true) ||
            it.album.contains(query, ignoreCase = true) ||
            it.genre.contains(query, ignoreCase = true)
        }
    }
    
    /**
     * Get all unique artists
     */
    suspend fun getAllArtists(): List<CategoryItem> = withContext(Dispatchers.IO) {
        scanForAudioFiles()
            .groupBy { it.artist }
            .map { (artist, songs) ->
                CategoryItem(
                    id = artist,
                    name = if (artist.isBlank()) "Unknown Artist" else artist,
                    songCount = songs.size,
                    category = BrowseCategory.ARTISTS,
                    description = songs.firstOrNull()?.album ?: "",
                    imageUri = songs.firstOrNull()?.albumArt?.toString()
                )
            }
            .sortedBy { it.name }
    }
    
    /**
     * Get all unique albums
     */
    suspend fun getAllAlbums(): List<CategoryItem> = withContext(Dispatchers.IO) {
        scanForAudioFiles()
            .groupBy { it.album } // Group by album only - album metadata takes precedence
            .map { (albumName, songs) ->
                val album = songs.first()
                // For description, show primary artist (most common) + "& others" if mixed
                val artists = songs.map { it.artist }.distinct()
                val description = if (artists.size == 1) {
                    artists.first()
                } else {
                    "${artists.first()} & others"
                }
                
                CategoryItem(
                    id = albumName, // Use album name directly as ID
                    name = if (albumName.isBlank()) "Unknown Album" else albumName,
                    songCount = songs.size,
                    category = BrowseCategory.ALBUMS,
                    description = description,
                    imageUri = album.albumArt?.toString()
                )
            }
            .sortedBy { it.name }
    }
    
    /**
     * Get albums grouped by artist for better organization
     */
    suspend fun getAlbumsGroupedByArtist(): List<ArtistGroup> = withContext(Dispatchers.IO) {
        val allSongs = scanForAudioFiles()
        
        // First group songs by album to create album objects
        val albums = allSongs
            .groupBy { it.album }
            .map { (albumName, songs) ->
                val album = songs.first()
                // For each album, find the primary artist (most common)
                val artistCounts = songs.groupBy { it.artist }.mapValues { it.value.size }
                val primaryArtist = artistCounts.maxByOrNull { it.value }?.key ?: "Unknown Artist"
                
                // Create album category item with primary artist as description
                val artists = songs.map { it.artist }.distinct()
                val description = if (artists.size == 1) {
                    artists.first()
                } else {
                    "${primaryArtist} & others"
                }
                
                CategoryItem(
                    id = albumName,
                    name = if (albumName.isBlank()) "Unknown Album" else albumName,
                    songCount = songs.size,
                    category = BrowseCategory.ALBUMS,
                    description = description,
                    imageUri = album.albumArt?.toString()
                ) to primaryArtist
            }
        
        // Now group albums by their primary artist
        albums
            .groupBy { it.second } // Group by primary artist
            .map { (artistName, albumPairs) ->
                val artistAlbums = albumPairs.map { it.first }.sortedBy { it.name }
                val totalSongs = artistAlbums.sumOf { it.songCount }
                
                ArtistGroup(
                    artistName = if (artistName.isBlank()) "Unknown Artist" else artistName,
                    albums = artistAlbums,
                    totalSongs = totalSongs,
                    isExpanded = false
                )
            }
            .sortedBy { it.artistName }
    }
    
    /**
     * Get all unique genres
     */
    suspend fun getAllGenres(): List<CategoryItem> = withContext(Dispatchers.IO) {
        scanForAudioFiles()
            .groupBy { it.genre }
            .filter { it.key.isNotBlank() } // Filter out empty genres
            .map { (genre, songs) ->
                CategoryItem(
                    id = genre,
                    name = genre,
                    songCount = songs.size,
                    category = BrowseCategory.GENRES,
                    description = "${songs.map { it.artist }.distinct().size} artists"
                )
            }
            .sortedBy { it.name }
    }
    
    /**
     * Get recently added songs (last 30 days)
     */
    suspend fun getRecentlyAddedSongs(): List<Song> = withContext(Dispatchers.IO) {
        val thirtyDaysAgo = System.currentTimeMillis() / 1000 - (30 * 24 * 60 * 60)
        scanForAudioFiles()
            .filter { it.dateAdded > thirtyDaysAgo }
            .sortedByDescending { it.dateAdded }
    }
    
    /**
     * Get songs by year
     */
    suspend fun getSongsByYear(year: Int): List<Song> = withContext(Dispatchers.IO) {
        scanForAudioFiles().filter { it.year == year }
    }
    
    /**
     * Get all unique years
     */
    suspend fun getAllYears(): List<CategoryItem> = withContext(Dispatchers.IO) {
        scanForAudioFiles()
            .filter { it.year > 0 }
            .groupBy { it.year }
            .map { (year, songs) ->
                CategoryItem(
                    id = year.toString(),
                    name = year.toString(),
                    songCount = songs.size,
                    category = BrowseCategory.ALL_SONGS, // Using ALL_SONGS for year category
                    description = "${songs.map { it.artist }.distinct().size} artists"
                )
            }
            .sortedByDescending { it.name.toIntOrNull() ?: 0 }
    }
    
    /**
     * Check if jPod Music directory exists
     */
    fun isJPodDirectoryAvailable(): Boolean {
        return try {
            val jPodDirectory = java.io.File(JPOD_MUSIC_DIR)
            jPodDirectory.exists() && jPodDirectory.isDirectory
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get the recommended directory structure message for users
     */
    fun getRecommendedStructure(): String {
        return """
            jPod uses a dedicated folder for clean, reliable music discovery.
            
            To use jPod with your music, organize files in this structure:
            
            /sdcard/Music/jPod/
            ├── [Artist Name]/
            │   ├── 01 Song Name.mp3
            │   ├── 02 Another Song.m4a
            │   └── ... (more songs by this artist)
            ├── [Another Artist]/
            │   ├── 01 Track Name.mp3
            │   └── ... (more songs by this artist)
            └── [More Artists]/
                    
            Supported formats: MP3, M4A/AAC, WAV, FLAC, OGG Vorbis, WMA
                    
            Use ADB to transfer music:
            adb push /path/to/music/ /sdcard/Music/jPod/[ArtistName]/
            adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///storage/emulated/0/Music/jPod
        """.trimIndent()
    }
}
