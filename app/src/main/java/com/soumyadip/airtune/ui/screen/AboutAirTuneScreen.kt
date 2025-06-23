package com.soumyadip.airtune.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAirTuneScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About AirTune") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to settings",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "AirTune",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Version: ${1.0}", // Dynamically gets your app version
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "AirTune provides intuitive, touchless hand gesture controls for your Android device. Effortlessly manage media playback, adjust volume, and control screen brightness, all without touching your screen.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            val uriHandler = LocalUriHandler.current
            val instagramUsername = "soumya_030303"
            val instagramProfileUrl = "https://www.instagram.com/$instagramUsername/"

            // Build an AnnotatedString to make part of the text clickable
            val annotatedText = buildAnnotatedString {
                // Append the non-clickable part
                append("Follow me on ")

                // Apply a style and tag to the clickable part
                pushStringAnnotation(
                    tag = "URL", // A unique tag to identify this clickable part
                    annotation = instagramProfileUrl
                )
                withStyle(style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary, // Make it primary color to look like a link
                    textDecoration = TextDecoration.Underline // Optional: add an underline for classic link look
                    // You can also add other styles like fontWeight = FontWeight.Bold, fontSize = 16.sp
                )
                ) {
                    append("@$instagramUsername")
                }
                pop() // Pop the SpanStyle and StringAnnotation
            }

            ClickableText(
                text = annotatedText,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface), // Apply base style to the whole text
                onClick = { offset ->
                    // Check if the clicked offset is within the "URL" annotation
                    annotatedText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            uriHandler.openUri(annotation.item) // Open the URL associated with the annotation
                        }
                }
            )

            // Optional: Add links to your website, social media, etc.
        }
    }
}