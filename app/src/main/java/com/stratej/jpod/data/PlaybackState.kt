package com.stratej.jpod.data

/**
 * Enum representing the current playback state
 */
enum class PlaybackState {
    STOPPED,
    PLAYING,
    PAUSED,
    LOADING,
    ERROR
}

/**
 * Data class representing the current player state
 */
data class PlayerState(
    val playbackState: PlaybackState = PlaybackState.STOPPED,
    val currentSong: Song? = null,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val isRepeatEnabled: Boolean = false
) {
    /**
     * Get the progress as a float between 0 and 1
     */
    fun getProgress(): Float {
        return if (duration > 0) {
            (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
    
    /**
     * Format current position to MM:SS
     */
    fun getFormattedPosition(): String {
        val totalSeconds = currentPosition / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    /**
     * Format duration to MM:SS
     */
    fun getFormattedDuration(): String {
        val totalSeconds = duration / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
