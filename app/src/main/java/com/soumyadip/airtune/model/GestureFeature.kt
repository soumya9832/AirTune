package com.soumyadip.airtune.model

import androidx.compose.ui.graphics.vector.ImageVector

data class GestureFeature(
    val title: String,
    val description: String,
    val route: String, // Navigation route for this feature's detail screen
    val icon: ImageVector? = null // Optional icon for the list item
)
