package com.stratej.jpod.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.stratej.jpod.data.BrowseCategory
import com.stratej.jpod.data.CategoryItem

/**
 * Composable for displaying a category card (Artist, Album, Genre, etc.)
 */
@Composable
fun CategoryCard(
    categoryItem: CategoryItem,
    onClick: (CategoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(categoryItem) }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon or image
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (categoryItem.imageUri != null) {
                    AsyncImage(
                        model = categoryItem.imageUri,
                        contentDescription = "${categoryItem.category.name} image",
                        modifier = Modifier.size(56.dp)
                    )
                } else {
                    Icon(
                        imageVector = getCategoryIcon(categoryItem.category),
                        contentDescription = categoryItem.category.name,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Category info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = categoryItem.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = categoryItem.getSongCountText(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (categoryItem.description.isNotBlank()) {
                    Text(
                        text = categoryItem.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Forward arrow
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Go to ${categoryItem.name}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Get icon for different browse categories
 */
private fun getCategoryIcon(category: BrowseCategory): ImageVector {
    return when (category) {
        BrowseCategory.ARTISTS -> Icons.Default.Person
        BrowseCategory.ALBUMS -> Icons.Default.Album
        BrowseCategory.GENRES -> Icons.Default.Category
        BrowseCategory.PLAYLISTS -> Icons.Default.QueueMusic
        BrowseCategory.FAVORITES -> Icons.Default.Favorite
        BrowseCategory.RECENTLY_ADDED -> Icons.Default.NewReleases
        BrowseCategory.RECENTLY_PLAYED -> Icons.Default.History
        else -> Icons.Default.LibraryMusic
    }
}
