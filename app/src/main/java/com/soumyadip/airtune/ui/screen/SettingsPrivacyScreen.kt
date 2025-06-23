package com.soumyadip.airtune.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPrivacyScreen(navController: NavController) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Privacy") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to main",
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // Privacy Policy Link
            ListItem(
                headlineContent = { Text("Privacy Policy") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri("https://sites.google.com/view/airtune-privacy-policy/home") } // YOUR PRIVACY POLICY URL
            )
            HorizontalDivider()

            // About AirTune Link
            ListItem(
                headlineContent = { Text("About AirTune") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("about_airtune_route") }
            )
            HorizontalDivider()

            // Open Source Licenses Link
            ListItem(
                headlineContent = { Text("Open Source Licenses") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("open_source_licenses_route") }
            )
            HorizontalDivider()

            // You can add more settings options here if needed later
        }
    }
}