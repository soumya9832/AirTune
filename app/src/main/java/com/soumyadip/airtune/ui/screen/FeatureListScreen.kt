package com.soumyadip.airtune.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.soumyadip.airtune.model.GestureFeature

// --- Composable for the Feature List Screen (NO SERVICE TOGGLE HERE) ---
@Composable
fun FeatureListScreen(navController: NavController) {
    val features = listOf(
        GestureFeature(
            "Volume Control",
            "Adjust media volume by hand gesture.",
            "volume_control_screen"
        ),
        GestureFeature(
            "Brightness Control",
            "Control screen brightness using intuitive gesture.",
            "brightness_control_screen"
        ),
        GestureFeature(
            "Media Control",
            "Control Media Playback using intuitive gesture.",
            "media_control_screen"
        )


        // Add more GestureFeature objects here as your app expands
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Gestures",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(features) { feature ->
                FeatureListItem(feature = feature) {
                    navController.navigate(feature.route) // Navigate to the selected feature's screen
                }
            }
        }
    }
}


// --- Composable for an individual Feature Item in the list ---
@Composable
fun FeatureListItem(feature: GestureFeature, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            feature.icon?.let { icon ->
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column {
                Text(text = feature.title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = feature.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Preview(showBackground = true, name = "Feature List Item Preview")
@Composable
fun FeatureListItemPreview() {
    // A sample GestureFeature for the preview
    val sampleFeature = GestureFeature(
        title = "Volume Control",
        description = "Adjust volume with hand gestures.",
        route = "volume_control_route"
    )

    // Call your FeatureListItem with the sample data
    FeatureListItem(feature = sampleFeature) {

    }
}

