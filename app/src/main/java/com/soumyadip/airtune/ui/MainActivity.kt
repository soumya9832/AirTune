package com.soumyadip.airtune.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.soumyadip.airtune.ui.theme.AirTuneTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.soumyadip.airtune.ui.screen.AboutAirTuneScreen
import com.soumyadip.airtune.ui.screen.AnimatedSplashScreen
import com.soumyadip.airtune.ui.screen.BrightnessControlScreen
import com.soumyadip.airtune.ui.screen.FeatureListScreen
import com.soumyadip.airtune.ui.screen.MediaControlScreen
import com.soumyadip.airtune.ui.screen.OpenSourceLicensesScreen
import com.soumyadip.airtune.ui.screen.SettingsPrivacyScreen
import com.soumyadip.airtune.ui.screen.VolumeControlScreen

class MainActivity : ComponentActivity() {

    // Activity result launcher for the SYSTEM_ALERT_WINDOW permission
    private val overlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {

            } else {
                Toast.makeText(this, "Overlay permission is required for background gesture detection.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Activity result launcher for CAMERA permission
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Camera permission is required for gesture detection.", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to check and request CAMERA permission
    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Function to check and request SYSTEM_ALERT_WINDOW permission
    private fun checkOverlayPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"))
            overlayPermissionLauncher.launch(intent)
            return false // Permission not granted, launched request
        }
        return true // Permission already granted
    }





    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request permissions on app startup
        requestCameraPermission()
        checkOverlayPermission()

        setContent {

            //  State to control whether the splash screen is currently visible
            val splashViewModel: SplashViewModel = viewModel()
            val showSplashScreen by splashViewModel.showSplashScreen.collectAsState()


            AirTuneTheme {

                // If showSplashScreen is true, display the splash screen
                if (showSplashScreen) {
                    AnimatedSplashScreen()
                }else{

                    val navController = rememberNavController()
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(
                                    "AirTune",
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontStyle = FontStyle.Normal
                                    ))
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                                ),

                                actions = {
                                    var expanded by remember { mutableStateOf(false) }
                                    IconButton(onClick = { expanded = true }) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "More options",
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Settings & Privacy") },
                                            onClick = {
                                                expanded = false
                                                navController.navigate("settings_privacy_route")
                                            }
                                        )
                                        // You can add more DropdownMenuItem here if needed later
                                    }
                                }

                            )
                        }
                    ) { paddingValues ->
                        NavHost(
                            navController = navController,
                            startDestination = "feature_list", // The initial screen
                            modifier = Modifier.padding(paddingValues)
                        ) {
                            // 1. Route for the list of features (the main screen)
                            composable("feature_list") {
                                FeatureListScreen(navController = navController)
                            }

                            // 2. Route for Volume Control details screen
                            composable("volume_control_screen") {
                                VolumeControlScreen() // This is where your VolumeGestureGuide will be called
                            }

                            // 3. Route for Brightness Control details screen
                            composable("brightness_control_screen") {
                                BrightnessControlScreen() // Your BrightnessGestureGuide equivalent will be called here
                            }

                            // 4. Route for Media Control details screen
                            composable("media_control_screen") {
                                MediaControlScreen() // Your MediaGestureGuide equivalent will be called here
                            }

                            composable("settings_privacy_route") {
                                SettingsPrivacyScreen(navController = navController)
                            }
                            composable("about_airtune_route") {
                                AboutAirTuneScreen(navController = navController)
                            }
                            composable("open_source_licenses_route") {
                                OpenSourceLicensesScreen(navController = navController)
                            }

                            // Add more feature routes here as your app grows
                        }
                    }
                }
            }

        }


    }


}
