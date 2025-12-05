# 🎵 jPod - Android Music Player

A feature-rich, modern Android music player built with **Kotlin** and **Jetpack Compose**. jPod provides a comprehensive music library experience with advanced categorization, playlist management, and professional-grade audio playback inspired by classic portable music players.

## ✨ Features

### 🎵 **Core Music Playback**
- **High-quality audio playback** using Media3 ExoPlayer
- **Background playback** with media session support
- **Full playback controls**: Play, Pause, Stop, Skip Next/Previous
- **Seek functionality** with interactive progress bar
- **Advanced Shuffle and Repeat modes**:
  - **Shuffle**: Randomizes playback order within the current context
    - When browsing all songs: shuffles all available songs
    - When browsing an album: shuffles only songs from that album
    - When browsing an artist: shuffles only songs from that artist
    - Mutually exclusive with repeat mode
  - **Repeat**: Repeats the current song continuously
    - When enabled, the current song will restart when it reaches the end
    - Mutually exclusive with shuffle mode
  - Both buttons provide visual feedback when active (highlighted background)
- **Audio focus management** and notification controls

### 📂 **Smart Music Organization**
- **Multi-format support**: MP3, WAV, WMA, FLAC, and more
- **Automatic library scanning** from device storage
- **Rich metadata display**: Title, Artist, Album, Genre, Year, Track Number
- **Album artwork** with fallback icons
- **Recently played tracking**

### 🗂️ **Category Browsing**
Browse your music collection by:
- **🎤 Artists** - Organized by performing artist
- **💿 Albums** - Grouped by album with artist information
- **🎵 Genres** - Musical genres (Pop, Rock, Jazz, Country, etc.)
- **⭐ Favorites** - Your liked songs
- **🕐 Recently Played** - Recently listened tracks
- **📅 Recently Added** - Newest songs on device

### 📋 **Advanced Playlist Management**
- **Create custom playlists** with names and descriptions
- **Add/Remove songs** from any playlist
- **Multiple playlist membership** - songs can belong to multiple playlists
- **Playlist reordering** - drag and drop song organization
- **Persistent storage** - playlists survive app restarts
- **Built-in Favorites** playlist with one-tap favoriting

### 🔍 **Search & Discovery**
- **Comprehensive search** across title, artist, album, and genre
- **Quick category filters** with material design chips
- **Instant results** as you type

### 🎨 **Modern UI/UX**
- **Material Design 3** with beautiful animations
- **Dark/Light theme support** (system-based)
- **Intuitive navigation** with clear visual hierarchy
- **Responsive design** for different screen sizes
- **Accessibility support** with proper content descriptions
- **Auto-scroll to currently playing song** - automatically brings the playing song to the top of any song list for easy visibility and navigation

## 📱 **Screenshots & UI**

### Main Screen
- Clean song list with album artwork
- Category filter chips for quick browsing
- Search functionality
- Player controls (when playing)

### Category Browsing
- Artist/Album/Genre cards with song counts
- Drill-down navigation to view category contents
- Beautiful category icons and metadata

### Playlist Management
- Create and manage custom playlists
- Add/remove songs with intuitive UI
- Playlist overview with song counts and descriptions

## 🛠️ **Technical Stack**

- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose with Material Design 3
- **Audio Engine**: Media3 ExoPlayer
- **Architecture**: MVVM with StateFlow
- **Persistence**: SharedPreferences with kotlinx.serialization
- **Image Loading**: Coil for album artwork
- **Permissions**: Accompanist Permissions
- **Navigation**: Jetpack Navigation Compose

## 🏗️ **Build Instructions**

### Prerequisites
- **Android Studio** Hedgehog (2023.1.1) or later
- **Java 17** or later
- **Android SDK** with minimum API 24 (Android 7.0)
- **Target SDK**: API 35 (Android 14)

### Setup
1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd jPod
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned directory

3. **Sync the project**
   - Android Studio will automatically sync Gradle dependencies
   - Wait for the sync to complete

4. **Build the project**
   ```bash
   ./gradlew build
   ```

5. **Run on device/emulator**
   - Connect an Android device or start an emulator
   - Click "Run" in Android Studio or use:
   ```bash
   ./gradlew installDebug
   ```

### Building APK
```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requires signing)
./gradlew assembleRelease
```

## 🎵 **Testing with Sample Music**

### ADB Setup for File Transfer

To test jPod with your own music files, you'll need **ADB (Android Debug Bridge)** to transfer MP3s to the emulator or device.

#### Installing ADB

**On macOS (recommended):**
```bash
# Install via Homebrew
brew install android-platform-tools
```

**On Windows:**
1. Download [Android Platform Tools](https://developer.android.com/studio/releases/platform-tools)
2. Extract and add to your system PATH
3. Or use Android Studio's built-in ADB: `~/Library/Android/sdk/platform-tools/`

**On Linux:**
```bash
# Ubuntu/Debian
sudo apt install android-tools-adb

# Arch Linux
sudo pacman -S android-tools
```

#### Verify ADB Installation
```bash
adb version
# Should show: Android Debug Bridge version X.X.X
```

### Managing Music in jPod

**jPod uses a dedicated, organized folder structure for reliable music discovery and management.**

#### Prerequisites
1. **ADB installed and working** (see ADB Installation section above)
2. **Device/emulator connected** via ADB
3. **Music files organized locally** in the correct structure

#### Required Folder Structure

**Local computer reference structure:**
```
~/jPod/
├── [Artist Name]/
│   ├── [Album 1]/
│   │   ├── 01 - Track One.mp3
│   │   ├── 02 - Track Two.mp3
│   │   └── 03 - Track Three.m4a
│   ├── [Album 2 - EP]/
│   │   ├── 01 - EP Track.m4a
│   │   └── 02 - EP Track Two.m4a
│   └── [Singles Collection]/
│       ├── Single Song.mp3
│       └── Another Single.m4a
└── [Another Artist]/
    ├── [Their Album]/
    │   ├── track1.mp3
    │   └── track2.m4a
    └── [Their EP]/
        └── ep_track.m4a
```

**Android device target structure:**
```
/sdcard/Music/jPod/
├── [Artist Name]/
│   ├── [Album 1]/
│   │   ├── 01 - Track One.mp3
│   │   ├── 02 - Track Two.mp3
│   │   └── 03 - Track Three.m4a
│   ├── [Album 2 - EP]/
│   │   ├── 01 - EP Track.m4a
│   │   └── 02 - EP Track Two.m4a
│   └── [Singles Collection]/
│       ├── Single Song.mp3
│       └── Another Single.m4a
└── [Another Artist]/
    ├── [Their Album]/
    │   ├── track1.mp3
    │   └── track2.m4a
    └── [Their EP]/
        └── ep_track.m4a
```

#### Why Hierarchical Artist/Album Structure?

The **Artist/Album** folder structure provides several important benefits:
- **📚 Proper Album Grouping:** Albums with featured artists or collaborations appear as unified entities (no duplicate albums)
- **🎵 Metadata Precedence:** Album metadata takes priority over individual track artists for organization
- **📁 Clean Organization:** Music is logically organized and easy to browse by both artist and album
- **🔍 Enhanced Discoverability:** Makes it easier to find specific albums and maintain large collections
- **🎯 Album Cohesion:** Tracks from the same album stay together regardless of featuring different artists

#### Supported Audio Formats
- **MP3** (.mp3) - Recommended for compatibility
- **M4A/AAC** (.m4a) - Excellent quality, includes album art
- **FLAC** (.flac) - Lossless quality
- **WAV** (.wav) - Uncompressed
- **OGG Vorbis** (.ogg) - Open source
- **WMA** (.wma) - Windows Media Audio

#### Step-by-Step Music Transfer Process

#### 1. Check Connected Devices
```bash
adb devices
# Should show your emulator or device listed
# Example output:
# emulator-5554   device
# ABC123DEF456    device  (physical device)
```

#### 2. Organize Music Files Locally
Create the proper hierarchical structure on your computer:
```bash
# Create local jPod directory (recommended location)
mkdir -p ~/jPod

# Create artist and album directories
mkdir -p ~/jPod/"Dave Hause"/"Kick"
mkdir -p ~/jPod/"Dave Hause"/"Paddy - EP"  
mkdir -p ~/jPod/"Dave Hause"/"Patty - EP"
mkdir -p ~/jPod/"Fantastic Cat"/"The Very Best of Fantastic Cat"

# Copy your music files into appropriate Artist/Album folders
# Maintain the hierarchical Artist/Album structure for proper organization
```

#### 3. Transfer Music to Device
```bash
# Transfer entire collection (recommended - maintains Artist/Album hierarchy):
adb push ~/jPod/ /sdcard/Music/

# Multiple devices - specify device ID:
# Emulator:
adb -s emulator-5554 push ~/jPod/ /sdcard/Music/

# Physical device (replace ABC123DEF456 with your device ID):
adb -s ABC123DEF456 push ~/jPod/ /sdcard/Music/

# Transfer single artist (maintains album structure):
adb -s emulator-5554 push ~/jPod/"Dave Hause"/ /sdcard/Music/jPod/"Dave Hause"/

# Transfer single album:
adb -s emulator-5554 push ~/jPod/"Dave Hause"/"Kick"/ /sdcard/Music/jPod/"Dave Hause"/"Kick"/
```

#### 4. Trigger Media Scanner (Critical Step)
Android's MediaStore must be updated to index the new files:
```bash
# Single device:
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Music/jPod

# Multiple devices:
adb -s emulator-5554 shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Music/jPod
adb -s ABC123DEF456 shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Music/jPod
```

#### 5. Restart jPod App
```bash
# Force stop app to clear any cached music data:
adb shell am force-stop com.stratej.jpod

# Launch jPod:
adb shell am start -n com.stratej.jpod/.MainActivity
```

#### Troubleshooting Common Issues

##### "No song found" Error
1. **Check folder structure:** Verify files are in `/sdcard/Music/jPod/[Artist]/`
2. **Trigger media scan:** Run the MediaStore broadcast command
3. **Check file permissions:** Files must be readable by MediaStore
4. **Restart app:** Force-stop and restart jPod

##### Duplicate Songs Appearing
If you see duplicate versions of the same song (e.g., both MP3 and M4A):
```bash
# List duplicates:
adb shell 'find /sdcard/Music/jPod -name "*Song Title*"'

# Remove unwanted format (keep highest quality):
adb shell 'rm "/sdcard/Music/jPod/Artist/Song Title.mp3"'  # Remove MP3, keep M4A
# OR
adb shell 'rm "/sdcard/Music/jPod/Artist/Song Title.m4a"'  # Remove M4A, keep MP3

# Trigger rescan after cleanup:
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Music/jPod
```

##### Verify Transfer Success
```bash
# Check directory structure:
adb shell 'find /sdcard/Music/jPod -type d | sort'

# Count files per artist:
adb shell 'ls /sdcard/Music/jPod/"Artist Name"/ | wc -l'

# List specific files:
adb shell 'ls /sdcard/Music/jPod/"Artist Name"/'
```

#### Advanced: Multiple Device Management
For managing multiple devices simultaneously:
```bash
# Get all connected device IDs:
adb devices | grep -v "List" | awk '{print $1}' > device_list.txt

# Transfer to all devices (bash script):
while read device; do
  echo "Transferring to $device..."
  adb -s "$device" push ~/jPod/. /sdcard/Music/jPod/
  adb -s "$device" shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Music/jPod
done < device_list.txt
```

#### Future: Automated Music Management
*Note: This is planned for future development*

The current manual ADB process will eventually be replaced with automated tools:
- **Desktop app** for drag-and-drop music management
- **Automated download scripts** for acquiring and organizing music
- **Batch processing** for format conversion and metadata cleanup  
- **Device synchronization** across multiple Android devices
- **Music library backup** and restoration tools

The foundation is in place with the standardized folder structure and reliable transfer process documented above.

#### 5. Install and Test jPod
```bash
# Install the app
./gradlew installDebug

# Launch jPod and grant permissions when prompted
# Your music files should appear automatically!
```

### jPod Music Directory Structure
**jPod uses a dedicated folder for clean, reliable music discovery.** Only music in the jPod folder will appear in the app:

```
/sdcard/Music/jPod/
├── [Artist Name]/
│   ├── 01 Song Name.mp3
│   ├── 02 Another Song.m4a
│   └── ... (more songs by this artist)
├── [Another Artist]/
│   ├── 01 Track Name.mp3
│   └── ... (more songs by this artist)
└── [More Artists]/
```

**Benefits of jPod Folder:**
- ✅ **Clean Discovery**: Only your curated music appears (no system sounds, notifications, etc.)
- ✅ **Future-Proof**: Scales to 1000+ artists without performance issues  
- ✅ **Simple Management**: Easy to organize and transfer music collections
- ✅ **No Interference**: Other apps can't contaminate your music library

**Supported Formats:** MP3, M4A/AAC, WAV, FLAC, OGG Vorbis, WMA

**Transfer and Setup (proven on multiple devices):**
```bash
# Transfer music using ADB
adb push /path/to/music/ /sdcard/Music/jPod/[ArtistName]/

# Force MediaStore to recognize files as music
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///storage/emulated/0/Music/jPod
```

**Benefits of jPod's approach:**
- ✅ **Automatic Discovery**: Finds all music with proper ID3/metadata tags
- ✅ **Multi-Format Support**: Works with MP3, M4A, and other popular formats
- ✅ **Clean Organization**: Artist/Album browsing based on file metadata
- ✅ **Scalable**: Add any artist without code changes
- ✅ **MediaStore Compatible**: Works reliably across Android versions

### Testing Features
Once music is loaded, test these jPod capabilities:
- ✅ **Playback Controls**: Play, pause, skip, seek
- ✅ **Categories**: Browse by Artist, Album, Genre  
- ✅ **Search**: Find songs by title, artist, album
- ✅ **Playlists**: Create and manage custom playlists
- ✅ **Favorites**: Star/unstar songs
- ✅ **Recently Played**: Check playback history

### Troubleshooting
- **No music appears**: Check permissions and trigger media scanner
- **ADB not found**: Ensure platform-tools are in your PATH
- **Device not detected**: Enable USB Debugging on physical devices
- **Transfer fails**: Check available storage space on device

## 📋 **Permissions Required**

The app requires the following permissions:

### Storage Permissions
- **READ_MEDIA_AUDIO** (Android 13+) - Access audio files
- **READ_EXTERNAL_STORAGE** (Android 12 and below) - Legacy storage access

### Media Playback
- **FOREGROUND_SERVICE** - Background music playback
- **FOREGROUND_SERVICE_MEDIA_PLAYBACK** - Media-specific foreground service
- **WAKE_LOCK** - Prevent device sleep during playback

## 🎯 **Usage Instructions**

### First Launch
1. **Grant permissions** when prompted for storage access
2. The app will **automatically scan** your device for music files
3. **Browse your library** using category chips or the main song list

### Playing Music
1. **Tap any song** to start playback immediately
2. **Tap a category** (Artist, Album, Genre) to browse organized content
3. **Use player controls** that appear when music is playing
4. **Seek within tracks** using the progress bar

### Managing Favorites
1. **Tap the heart icon** on any song to add to favorites
2. **Access favorites** via the Favorites chip or Playlists screen
3. **Remove favorites** by tapping the filled heart icon

### Creating Playlists
1. **Tap the playlist icon** in the top toolbar
2. **Tap the + button** to create a new playlist
3. **Enter playlist name** and optional description
4. **Add songs** from the main library or category views

### Search Functionality
1. **Tap the search icon** in the top toolbar
2. **Type your query** - searches title, artist, album, and genre
3. **Tap any result** to play or view details

### Browsing by Categories
1. **Tap category chips** on the main screen (Artists, Albums, etc.)
2. **Browse organized lists** with song counts and descriptions
3. **Tap any category item** to view its songs
4. **Play entire categories** or individual tracks

## 🏛️ **Architecture Overview**

### Data Layer
- **`Song.kt`** - Core data model with metadata
- **`Playlist.kt`** - Playlist and category definitions
- **`MusicScanner.kt`** - MediaStore integration for file discovery
- **`PlaylistManager.kt`** - Playlist persistence and operations

### UI Layer (Jetpack Compose)
- **`MusicPlayerScreen.kt`** - Main interface with player controls
- **`BrowseScreen.kt`** - Category browsing interface
- **`PlaylistScreen.kt`** - Playlist management interface
- **Reusable components** for songs, categories, and controls

### Business Logic
- **`MusicPlayerViewModel.kt`** - State management and business logic
- **`MusicService.kt`** - Background audio playbook with Media3

### Key Features Implementation
- **State Management**: StateFlow for reactive UI updates
- **Media Session**: Integration with system media controls
- **Persistence**: JSON serialization for playlist storage
- **Performance**: Lazy loading and efficient list rendering

## 🔮 **Future Enhancements**

- **Equalizer** with preset and custom settings
- **Sleep timer** for bedtime listening
- **Crossfade** between tracks
- **Lyrics display** integration
- **Cloud sync** for playlists across devices
- **Custom themes** and color schemes
- **Advanced search filters**
- **Song statistics** and listening analytics

## 🐛 **Troubleshooting**

### Common Issues
1. **No songs found**: Ensure music files are in standard locations and formats
2. **Permission denied**: Grant storage permissions in Android Settings
3. **Playback issues**: Check if audio files are corrupted or unsupported format
4. **App crashes**: Ensure device meets minimum requirements (API 24+)

### Supported Audio Formats
- MP3, WAV, FLAC, AAC, OGG Vorbis, WMA
- Most common audio codecs via Media3 ExoPlayer

## 📄 **License**

This project is open source and available under the [MIT License](LICENSE).

## 🤝 **Contributing**

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

---

**jPod - Built with ❤️ using Kotlin and Jetpack Compose**
