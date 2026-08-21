package com.stratej.jpod

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stratej.jpod.data.BrowseCategory
import com.stratej.jpod.data.CategoryItem
import com.stratej.jpod.ui.screens.BrowseScreen
import com.stratej.jpod.ui.screens.CategorySongsScreen
import com.stratej.jpod.ui.screens.MusicPlayerScreen
import com.stratej.jpod.ui.theme.Android_music_playerTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Android_music_playerTheme {
                MusicPlayerApp()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MusicPlayerApp() {
    // Request necessary permissions
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    
    val permissionsState = rememberMultiplePermissionsState(
        permissions = permissionsToRequest
    )
    
    LaunchedEffect(Unit) {
        permissionsState.launchMultiplePermissionRequest()
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            permissionsState.allPermissionsGranted -> {
                // All permissions granted, show the music player with navigation
                MusicPlayerAppContent()
            }
            permissionsState.shouldShowRationale -> {
                // Show rationale and request permissions again
                PermissionRationaleScreen(
                    onRequestPermissions = {
                        permissionsState.launchMultiplePermissionRequest()
                    }
                )
            }
            else -> {
                // Permissions denied permanently
                PermissionDeniedScreen()
            }
        }
    }
}

@Composable
fun MusicPlayerAppContent() {
    // Navigation state
    var currentScreen by remember { mutableStateOf("browse") }
    var currentCategory by remember { mutableStateOf<BrowseCategory?>(BrowseCategory.ALBUMS) }
    var currentCategoryItem by remember { mutableStateOf<CategoryItem?>(null) }

    // Scroll state hoisted here so it survives navigation to/from category_songs
    val browseListState = rememberLazyListState()
    val browseArtistListState = rememberLazyListState()
    
    when (currentScreen) {
        "home" -> {
            MusicPlayerScreen(
                onBrowseCategory = { category ->
                    currentCategory = category
                    currentScreen = "browse"
                }
            )
        }
        "browse" -> {
            currentCategory?.let { category ->
                BrowseScreen(
                    category = category,
                    onBackClick = {
                        currentScreen = "home"
                        currentCategory = null
                    },
                    onCategoryItemClick = { categoryItem ->
                        currentCategoryItem = categoryItem
                        currentScreen = "category_songs"
                    },
                    listState = browseListState,
                    artistListState = browseArtistListState
                )
            }
        }
        "category_songs" -> {
            currentCategory?.let { category ->
                currentCategoryItem?.let { categoryItem ->
                    CategorySongsScreen(
                        category = category,
                        itemId = categoryItem.id,
                        itemName = categoryItem.name,
                        onBackClick = {
                            currentScreen = "browse"
                            currentCategoryItem = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionRationaleScreen(
    onRequestPermissions: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Storage Permission Required",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "This app needs access to your device's storage to find and play music files. Please grant the storage permission to continue.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRequestPermissions,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant Permission")
        }
    }
}

@Composable
fun PermissionDeniedScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Permission Denied",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Storage permission is required to access music files. Please enable it manually in the app settings.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}