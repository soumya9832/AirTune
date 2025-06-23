package com.soumyadip.airtune.ui.screen


import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.soumyadip.airtune.R
import com.soumyadip.airtune.data.app_data.AppPreferences
import com.soumyadip.airtune.service.GestureControlService
import com.soumyadip.airtune.ui.theme.AirTuneTheme
import com.soumyadip.airtune.data.app_data.PrefManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun BrightnessControlScreen() {
    BrightnessGestureGuide()
}

@Composable
fun BrightnessGestureGuide() {
    val context = LocalContext.current
    val prefManager = remember { PrefManager(context) }

    // Load initial state for brightness control
    var isBrightnessControlEnabled by remember {
        mutableStateOf(prefManager.isFeatureEnabled(PrefManager.KEY_BRIGHTNESS_CONTROL_ENABLED,false))
    }

    // --- NEW: State for WRITE_SETTINGS permission check ---
    var canWriteSettings by remember {
        mutableStateOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.System.canWrite(context) else true)
    }

    // --- NEW: State to hold the Lottie Composition currently being played (null if none) ---
    var currentLottieAnimationToPlay by remember { mutableStateOf<LottieComposition?>(null) }


    // --- NEW: Lottie Compositions for Permission Outcome ---
    val permissionGrantedComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_happy_emoji_animation)) // <--- Your success Lottie file/mb
    val permissionDeniedComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_sad_emoji_animation))


    // --- Lottie Animation Playback Progress (for the animation currently set in currentLottieAnimationToPlay) ---
    val lottieProgress by animateLottieCompositionAsState(
        currentLottieAnimationToPlay,
        isPlaying = currentLottieAnimationToPlay != null, // Play when a composition is set
        iterations = 1, // Play once
        speed = 1f
    )

    // --- NEW: Animation for blur radius ---
    val blurRadius by animateFloatAsState(
        targetValue = if (currentLottieAnimationToPlay != null) 16.dp.value else 0.dp.value, // 16.dp max blur, 0.dp no blur
        animationSpec = tween(durationMillis = 500), // Smooth blur transition over 0.5 seconds
        label = "blurAnimation"
    )

    // --- NEW: Animation for dimming alpha ---
    val dimmingAlpha by animateFloatAsState(
        targetValue = if (currentLottieAnimationToPlay != null) 0.5f else 0f, // 50% black overlay when active, 0% when inactive
        animationSpec = tween(durationMillis = 500), // Smooth dimming transition
        label = "dimmingAnimation"
    )


    // --- LaunchedEffect to manage animation duration ---
    LaunchedEffect(currentLottieAnimationToPlay) {

        if (currentLottieAnimationToPlay != null) {
            // Keep the animation visible/active for ~2.5 seconds, or until Lottie finishes if longer
            // Lottie's `animateLottieCompositionAsState` will handle playing it once.


            delay(2500)

            currentLottieAnimationToPlay = null
        }
    }

    // --- NEW: Launcher for WRITE_SETTINGS permission intent ---
    val writeSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {

        // After returning from settings, re-check the permission status
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            canWriteSettings = Settings.System.canWrite(context)
            if (canWriteSettings) {
                // If permission was just granted, and user wanted to enable it
                if (!isBrightnessControlEnabled) { // Only enable if not already enabled (e.g. from previous session)
                    isBrightnessControlEnabled = true // Update local state
                    prefManager.saveFeatureEnabled(PrefManager.KEY_BRIGHTNESS_CONTROL_ENABLED,true) // Save to preferences

                    val serviceIntent = Intent(context, GestureControlService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    Toast.makeText(context, "Brightness Control Enabled!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Permission not granted. Brightness Control cannot be fully enabled.", Toast.LENGTH_LONG).show()
                // If permission wasn't granted, ensure the switch reflects disabled state
                isBrightnessControlEnabled = false
                prefManager.saveFeatureEnabled(PrefManager.KEY_BRIGHTNESS_CONTROL_ENABLED,false)
            }
        }
    }

    // --- MODIFIED: Re-check permission status and TRIGGER ANIMATION onResume ---
    DisposableEffect(Unit) {
        val lifecycleObserver = object : DefaultLifecycleObserver {

            override fun onResume(owner: LifecycleOwner) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val wasCanWriteSettings = canWriteSettings
                    canWriteSettings = Settings.System.canWrite(context) // Keep this for general state update


                    // Only handle service/feature state if permission was lost,
                    // not for the initial permission request flow.
                    if (isBrightnessControlEnabled && !canWriteSettings && wasCanWriteSettings) {

                        isBrightnessControlEnabled = false
                        prefManager.saveFeatureEnabled(PrefManager.KEY_BRIGHTNESS_CONTROL_ENABLED, false)
                    }
                }
            }
        }
        (context as? LifecycleOwner)?.lifecycle?.addObserver(lifecycleObserver)
        onDispose {

            (context as? LifecycleOwner)?.lifecycle?.removeObserver(lifecycleObserver)
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .then(
                    if (currentLottieAnimationToPlay != null) {
                        // Apply blur only if API level is 31+
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12 is API 31 (S)
                            Modifier.blur(radius = blurRadius.dp)
                        } else {
                            Modifier // No blur on older APIs
                        }
                    } else Modifier // No blur when animation is inactive
                )
                // Apply dimming overlay conditionally
                .background(Color.Black.copy(alpha = dimmingAlpha)), // Dimming effect,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3)) // Changed to a blue color for brightness
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Enable Brightness Control",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = isBrightnessControlEnabled,
                        onCheckedChange = { checked ->

                            if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !canWriteSettings) {
                                // If enabling brightness and permission is NOT granted, request it
                                // --- MODIFIED: Set flag here, launch intent, but DON'T trigger animation yet ---
                                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                writeSettingsLauncher.launch(intent)
                                // Don't change isBrightnessControlEnabled state here yet,
                                // it will be updated in the launcher's callback if permission is granted.
                                Toast.makeText(
                                    context,
                                    "Please grant 'Modify system settings' permission.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                // Either permission is granted, or we are disabling the feature
                                isBrightnessControlEnabled = checked
                                prefManager.saveFeatureEnabled(
                                    PrefManager.KEY_BRIGHTNESS_CONTROL_ENABLED,
                                    checked
                                )

                                val serviceIntent =
                                    Intent(context, GestureControlService::class.java)
                                if (checked) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        context.startForegroundService(serviceIntent)
                                    } else {
                                        context.startService(serviceIntent)
                                    }
                                    currentLottieAnimationToPlay = permissionGrantedComposition

                                } else {
                                    if (!AppPreferences.anyGestureFeatureEnabled(context)) {
                                        context.stopService(serviceIntent)
                                    }
                                    currentLottieAnimationToPlay = permissionDeniedComposition
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color.Blue.copy(alpha = 0.9f),
                            uncheckedThumbColor = Color.DarkGray,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
            // --- END CARD ---

            // --- NEW: Permission status warning ---
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !canWriteSettings) {
                Text(
                    "Status: 'Modify system settings' permission is REQUIRED for this feature to work.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            // --- END NEW ---

            Spacer(modifier = Modifier.height(45.dp))

            // Dummy Phone Screen and Animation Area
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(370.dp)
                    .background(Color.DarkGray, shape = RoundedCornerShape(20.dp))
                    .border(2.dp, Color.LightGray, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                BrightnessAnimationContent() // Placeholder for brightness animation
            }

            Spacer(Modifier.height(40.dp))

            Text(
                text = "Brightness Control Tip: Gesture Guide!\n" +
                        "• 👋 Palm Facing Camera: Ensure your palm is clearly visible.\n" +
                        "• ✌️ Victory Gesture: Use the two-finger 'V' sign for control.\n" +
                        "• 🚫 Detection Limit: Gestures won't work if the back of your hand is facing the camera.\n",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start // Use TextAlign.Start for bullet points
            )
        }

        // --- Lottie Animation Overlay (for permission outcome) ---
        // This will appear on top of the blurred/dimmed background
        if (currentLottieAnimationToPlay != null) { // Only compose Lottie when active
            LottieAnimation(
                composition = currentLottieAnimationToPlay,
                progress = { lottieProgress }, // Use the animated progress
                modifier = Modifier.fillMaxSize(),
                alignment = Alignment.Center // Align the animation within its bounds
            )
        }


    }




}

// --- Placeholder for Brightness Animation Content ---
@Composable
fun BrightnessAnimationContent() {
    val infiniteTransition = rememberInfiniteTransition(label = "brightnessGuideTransition")

    // Gesture Movement and Brightness Level Animation
    val gestureStartX = 30f // Start position (left)
    val gestureEndX = 150f // End position (right)
    val gestureLoopDuration = 2000 // ms for one full sweep (e.g., left to right)

    // The gesture animation starts immediately
    val gestureOffsetX by infiniteTransition.animateFloat(
        initialValue = gestureStartX,
        targetValue = gestureEndX,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = gestureLoopDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse // Go left and right
        ), label = "gestureOffset"
    )

    // Map gesture position to brightness level (0.0 to 1.0)
    val brightnessLevel = remember(gestureOffsetX) {
        ((gestureOffsetX - gestureStartX) / (gestureEndX - gestureStartX)).coerceIn(0f, 1f)
    }

    // --- MODIFIED LOGIC FOR DIMMING OVERLAY ALPHA ---
    // When brightnessLevel is 0 (dim), dimmingOverlayAlpha is max (e.g., 0.95f or 1.0f) -> screen looks dark/black
    // When brightnessLevel is 1 (bright), dimmingOverlayAlpha is min (0.0f) -> screen looks very light gray/white
    val dimmingOverlayAlpha = remember(brightnessLevel) {
        // We want 0.95 alpha when brightnessLevel is 0, and 0.0 alpha when brightnessLevel is 1.
        // So, (1f - brightnessLevel) will go from 1 to 0. Multiply by desired max alpha.
        (0.95f * (1f - brightnessLevel)).coerceIn(0f, 0.95f) // <-- Changed max alpha to 0.95f for a darker black
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
            // Inner box for the actual "screen" display area
            // CHANGE THIS LINE: Make the base background of the 'screen' very light gray
            Box(
                modifier = Modifier
                    .width(180.dp) // Slightly smaller than outer box to create a bezel
                    .height(350.dp)
                    .background(
                        Color(0xFFEEEEEE),
                        shape = RoundedCornerShape(10.dp)
                    ) // <-- CHANGED to a very light gray (or Color.White)
                    .clip(RoundedCornerShape(10.dp)) // Clip the content to rounded corners
            ) {
                // "Real" Dummy Screen Brightness Animation: This Spacer now overlays a very light gray background.
                // As dimmingOverlayAlpha goes from 0.0 to 0.95, the screen will transition from very light gray to near black.
                Spacer(modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimmingOverlayAlpha))) // Still a black overlay
            }

            // Brightness Bar (Horizontal) - Top of Dummy Screen
            BrightnessBar(
                level = brightnessLevel, // Level based on gesture position
                modifier = Modifier
                    .align(Alignment.TopCenter) // Position relative to the inner screen (not the outer frame)
                    .padding(top = 15.dp) // Adjust padding if needed based on bezel size
            )

        // Peace Sign Icon - Adjust its position relative to the main Box
        Box(
            modifier = Modifier
                .size(60.dp) // Size of the icon and its effect area
                .graphicsLayer {
                    // Apply the main gesture horizontal movement to this entire Box
                    // Ensure it moves across the visible part of the dummy screen
                    translationX =
                        gestureOffsetX.dp.toPx() - (180.dp.toPx() / 2) // Roughly center the movement range
                }
                .align(Alignment.Center) // Aligns relative to the parent Box (which is fillMaxSize)
        ) {
            Image(
                // IMPORTANT: Use the correct drawable for Peace Sign/Victory symbol
                // If you have `baseline_peace_24`, use that. If not, replace with your actual resource.
                painter = painterResource(id = R.drawable.peace), // <--- CHECK THIS DRAWABLE NAME!
                contentDescription = "Peace sign gesture guide",
                modifier = Modifier.fillMaxSize()
            )
        }
    }


}


@Composable
fun BrightnessBar(level: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(150.dp) // Width of the brightness bar
            .height(20.dp) // Height of the brightness bar
            .background(
                Color.Gray.copy(alpha = 0.5f),
                RoundedCornerShape(10.dp)
            ) // Background of the bar
            .clip(RoundedCornerShape(10.dp)) // Clip the content (the fill) to rounded corners
    ) {
        // Filled part of the brightness bar
        Spacer(
            modifier = Modifier
                .fillMaxHeight() // Takes full height of the bar
                .fillMaxWidth(level) // Fills width based on the 'level' (0.0 to 1.0)
                .align(Alignment.CenterStart) // Ensures it starts filling from the left
                .background(Color.Yellow) // Color of the filled part (can be adjusted)
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun BrightnessControlScreenPreview() {
    AirTuneTheme {
        BrightnessControlScreen()
    }
}