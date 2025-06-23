package com.soumyadip.airtune.ui.screen


import android.content.Intent
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.soumyadip.airtune.R
import com.soumyadip.airtune.data.app_data.AppPreferences
import com.soumyadip.airtune.service.GestureControlService
import com.soumyadip.airtune.ui.theme.AirTuneTheme
import com.soumyadip.airtune.data.app_data.PrefManager
import com.soumyadip.airtune.util.PermissionUtils
import kotlinx.coroutines.delay


@Composable
fun MediaControlScreen() {
    MediaGestureGuide()
}

@Composable
fun MediaGestureGuide() {
    val context = LocalContext.current
    val prefManager = remember { PrefManager(context) }

    // Load initial state for media control
    var isMediaControlEnabled by remember {
        mutableStateOf(prefManager.isFeatureEnabled(PrefManager.KEY_MEDIA_CONTROL_ENABLED,false))
    }

    // State for Notification Listener Service permission check
    var hasNotificationAccess by remember {
        mutableStateOf(PermissionUtils.isNotificationServiceEnabled(context))
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



    // Re-check permission status and manage feature state onResume
    DisposableEffect(Unit) {
        val lifecycleObserver = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {

                val wasHasNotificationAccess = hasNotificationAccess
                hasNotificationAccess = PermissionUtils.isNotificationServiceEnabled(context)

                // Only handle service/feature state if permission was lost
                if (isMediaControlEnabled && !hasNotificationAccess && wasHasNotificationAccess) {

                    isMediaControlEnabled = false
                    prefManager.saveFeatureEnabled(PrefManager.KEY_MEDIA_CONTROL_ENABLED, false)
                    // If all features are now disabled, stop the service
                    val serviceIntent = Intent(context, GestureControlService::class.java)
                    if (!AppPreferences.anyGestureFeatureEnabled(context)) {
                        context.stopService(serviceIntent)
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
                    .padding(bottom = 16.dp),
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
                        text = "Enable Media Control",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = isMediaControlEnabled,
                        onCheckedChange = { checked ->

                            if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasNotificationAccess) {

                                PermissionUtils.requestNotificationServiceAccess(context)

                            } else {
                                // Either permission is granted, or we are disabling the feature
                                isMediaControlEnabled = checked
                                prefManager.saveFeatureEnabled(
                                    PrefManager.KEY_MEDIA_CONTROL_ENABLED,
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasNotificationAccess) {
                Text(
                    "Status: 'Notification Access' permission is REQUIRED for this feature to work.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            // --- END NEW ---

            Spacer(modifier = Modifier.height(30.dp))

            // Dummy Phone Screen and Animation Area
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(370.dp)
                    .background(Color.DarkGray, shape = RoundedCornerShape(20.dp))
                    .border(2.dp, Color.LightGray, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                MediaAnimationContent() // Placeholder for Media animation
            }

            Spacer(Modifier.height(30.dp))

            Text(
                text = "Media Control Tip: Gesture Guide!\n" +
                        "• Open Palm (🖐️): Resume Music\n" +
                        "• Closed Fist (✊): Pause Music\n" +
                        "• Slide Open Palm Right (🖐️➡️): Next Song\n" +
                        "• Slide Open Palm Left (🖐️⬅️): Previous Song\n",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start
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





@Composable
fun MediaAnimationContent() {
    val infiniteTransition = rememberInfiniteTransition(label = "mediaGuideOverallTransition")

    val density = LocalDensity.current

    var isPlayingAnimated by remember { mutableStateOf(false) }
    var currentSongIndex by remember { mutableStateOf(0) }
    val songs = remember {
        listOf(
            "Stress Relief",
            "Morning",
            "Deep Focus"
        )
    }
    val artists = remember {
        listOf(
            "Fazle Studios",
            "Ambient ",
            "Zen Harmonies"
        )
    }

    // --- Durations for gestures (Long type for delay) ---
    val gestureTransitionTime: Long = 200 // Time for hand to appear/disappear (fade in/out)
    val gestureHoldTime: Long = 2000 // Time hand stays visible in place for static gestures
    val swipeMoveTime: Long = 1000 // Time for the actual swipe movement

    // Define total time for each gesture phase
    val playGestureTotalTime = gestureTransitionTime * 2 + gestureHoldTime // Appear + Hold + Disappear
    val pauseGestureTotalTime = gestureTransitionTime * 2 + gestureHoldTime // Appear + Hold + Disappear
    val swipeGestureTotalTime = gestureTransitionTime * 2 + swipeMoveTime + 500 // Appear + Move + Disappear + buffer

    // Calculate total animation duration
    val finalAnimationDuration = playGestureTotalTime + pauseGestureTotalTime + (swipeGestureTotalTime * 2) + 500  // Added total cycle buffer

    // Current time points for clarity in keyframes
    val playStartTime = 0L
    val playAppearTime = playStartTime + gestureTransitionTime
    val playHoldEndTime = playAppearTime + gestureHoldTime
    val playDisappearTime = playHoldEndTime + gestureTransitionTime

    val pauseStartTime = playDisappearTime // Starts immediately after play ends
    val pauseAppearTime = pauseStartTime + gestureTransitionTime
    val pauseHoldEndTime = pauseAppearTime + gestureHoldTime
    val pauseDisappearTime = pauseHoldEndTime + gestureTransitionTime

    val nextStartTime = pauseDisappearTime // Starts immediately after pause ends
    val nextAppearTime = nextStartTime + gestureTransitionTime
    val nextSwipeMoveEndTime = nextAppearTime + swipeMoveTime
    val nextDisappearTime = nextSwipeMoveEndTime + gestureTransitionTime

    val previousStartTime = nextDisappearTime+500// Starts immediately after next ends
    val previousAppearTime = previousStartTime + gestureTransitionTime
    val previousSwipeMoveEndTime = previousAppearTime + swipeMoveTime
    val previousDisappearTime = previousSwipeMoveEndTime + gestureTransitionTime

    val gestureCycle by infiniteTransition.animateValue(
        initialValue = 0,
        targetValue = 4, // 0, 1, 2, 3 (Play, Pause, Next, Previous)
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = finalAnimationDuration.toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "gestureCycle"
    )

    // Hand X position animation
    val handNormalizedX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                this.durationMillis = finalAnimationDuration.toInt()

                // Ensure initial state is off-screen (left)
                0.0f at 0

                // Gesture 0: Play (Open Palm) - Appears directly at center, stays, then disappears in place
                0.5f at playStartTime.toInt() // Ensure off-screen before appearance starts
                0.5f at playAppearTime.toInt() // **Jumps to center as it appears**
                0.5f at playHoldEndTime.toInt() // Stays at center
                0.5f at playDisappearTime.toInt() // Stays at center as it disappears
                // After it's fully invisible, move off-screen for the next gesture
                0.5f at playDisappearTime.toInt() + 1 // Move off-screen immediately after becoming invisible

                // Gesture 1: Pause (Closed Fist) - Appears directly at center, stays, then disappears in place
                0.5f at pauseStartTime.toInt() // Ensure off-screen before appearance starts
                0.5f at pauseAppearTime.toInt() // **Jumps to center as it appears**
                0.5f at pauseHoldEndTime.toInt() // Stays at center
                0.5f at pauseDisappearTime.toInt() // Stays at center as it disappears
                // After it's fully invisible, move off-screen for the next gesture
                0.5f at pauseDisappearTime.toInt() + 1 // Move off-screen immediately after becoming invisible

                // Gesture 2: Next (Open Palm Swipe Right) - Same as before
                0.5f at nextStartTime.toInt() // Starts off-screen (from previous state)
                0.5f at nextAppearTime.toInt() // Arrives at center
                0.8f at nextSwipeMoveEndTime.toInt() // Swipes right
                0.8f at nextDisappearTime.toInt() // Moves off-screen left

                // Gesture 3: Previous (Open Palm Swipe Left) - Same as before
                0.5f at previousStartTime.toInt() // Starts off-screen (from previous state)
                0.5f at previousAppearTime.toInt() // Arrives at center
                0.2f at previousSwipeMoveEndTime.toInt() // Swipes left
                0.2f at previousDisappearTime.toInt() // Moves off-screen left

                // Ensure it's off-screen at the very end of the cycle
                0.0f at finalAnimationDuration.toInt()
            },
            repeatMode = RepeatMode.Restart
        ), label = "handNormalizedX"
    )

    // Hand Alpha (Visibility) animation - No major changes here, as it controls the fade
    val handAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                this.durationMillis = finalAnimationDuration.toInt()

                // Initial state (invisible)
                0f at 0

                // Gesture 0: Play
                0f at playStartTime.toInt() // Invisible just before appearing
                1f at playAppearTime.toInt() // Appears quickly
                1f at playHoldEndTime.toInt() // Stays visible
                0f at playDisappearTime.toInt() // Disappears quickly

                // Gesture 1: Pause
                0f at pauseStartTime.toInt() // Invisible just before appearing
                1f at pauseAppearTime.toInt() // Appears quickly
                1f at pauseHoldEndTime.toInt() // Stays visible
                0f at pauseDisappearTime.toInt() // Disappears quickly

                // Gesture 2: Next
                0f at nextStartTime.toInt() // Invisible just before appearing
                1f at nextAppearTime.toInt() // Appears quickly
                1f at nextSwipeMoveEndTime.toInt() // Stays visible during swipe
                0f at nextDisappearTime.toInt() // Disappears quickly

                // Gesture 3: Previous
                0f at previousStartTime.toInt() // Invisible just before appearing
                1f at previousAppearTime.toInt() // Appears quickly
                1f at previousSwipeMoveEndTime.toInt() // Stays visible during swipe
                0f at previousDisappearTime.toInt() // Disappears quickly

                // Ensure it's invisible at the very end of the cycle
                0f at finalAnimationDuration.toInt()
            },
            repeatMode = RepeatMode.Restart
        ), label = "handAlpha"
    )




    val seekBarLottieComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_music_wave_animation))
    val seekBarLottieProgress by animateLottieCompositionAsState(
        composition = seekBarLottieComposition,
        isPlaying = true,
        iterations = LottieConstants.IterateForever,
        speed = if (gestureCycle == 1) 0f else 1f,
        clipSpec = LottieClipSpec.Progress(
            min = if (isPlayingAnimated) 0.5f else 0f,
            max = if (isPlayingAnimated) 1f else 0.5f
        )
    )

    val musicPlayingLottieComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_music_playing_animation))
    val musicPlayingLottieProgress by animateLottieCompositionAsState(
        composition = musicPlayingLottieComposition,
        isPlaying = true,
        iterations = LottieConstants.IterateForever,
        speed = 1f,
        clipSpec = LottieClipSpec.Progress(
            min = if (isPlayingAnimated) 0.5f else 0f,
            max = if (isPlayingAnimated) 1f else 0.5f
        )
    )



// Previous Button States
    var skipPreviousButtonIsClicked by remember { mutableStateOf(false) }
    val skipPreviousButtonScale by animateFloatAsState(
        targetValue = if (skipPreviousButtonIsClicked) 0.5f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "skipPreviousButtonScale"
    )

// Next Button States
    var skipNextButtonIsClicked by remember { mutableStateOf(false) }
    val skipNextButtonScale by animateFloatAsState(
        targetValue = if (skipNextButtonIsClicked) 0.5f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "skipNextButtonScale"
    )





    LaunchedEffect(gestureCycle) {
        skipNextButtonIsClicked = false
        skipPreviousButtonIsClicked = false

        when (gestureCycle) {
            0 -> { // Play/Resume (Open Palm)

                isPlayingAnimated = false
                //delay(playAppearTime + gestureHoldTime/2) // Trigger click in middle of hold time
                delay(100)
                isPlayingAnimated = true
            }
            1 -> { // Pause (Closed Fist)

                isPlayingAnimated = false
                //delay(pauseAppearTime + gestureHoldTime/2) // Trigger click in middle of hold time
                delay(100)

                isPlayingAnimated = true
            }
            2 -> { // Next (Open Palm Swipe Right)

                isPlayingAnimated = false
                //delay(nextAppearTime + swipeMoveTime/2) // Trigger click in middle of swipe
                delay(500)
                currentSongIndex = (currentSongIndex+1) % songs.size
                skipNextButtonIsClicked = true
                delay(300)
                skipNextButtonIsClicked = false
                isPlayingAnimated = true
            }
            3 -> { // Previous (Open Palm Swipe Left)

                isPlayingAnimated = false
                delay(500) // Trigger click in middle of swipe
                currentSongIndex = (currentSongIndex -1 + songs.size) % songs.size
                skipPreviousButtonIsClicked = true
                delay(300)
                skipPreviousButtonIsClicked = false
                isPlayingAnimated = true
            }
        }
    }


    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(260.dp)
                .height(480.dp)
                .background(Color.DarkGray, shape = RoundedCornerShape(24.dp))
                .border(2.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.8f))

            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .background(
                            Color.Black,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Top
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, start = 10.dp, end = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image( // <--- Use Image instead of Icon
                            painter = painterResource(id = R.drawable.player),
                            contentDescription = "Spotify Logo", // It's good practice to provide a meaningful contentDescription
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = songs[currentSongIndex],
                                color = Color(0xFF8F87F1),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = artists[currentSongIndex],
                                color = Color(0xFF00809D),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        LottieAnimation(
                            composition = musicPlayingLottieComposition,
                            progress = { musicPlayingLottieProgress },
                            modifier = Modifier
                                .width(30.dp)
                                .height(30.dp)
                                .padding(top = 8.dp)
                        )
                    }

                    LottieAnimation(
                        composition = seekBarLottieComposition,
                        progress = { seekBarLottieProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .padding(top = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .offset(y = (-8).dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "0:0${(currentSongIndex % 3) + 1}", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                        Text(text = "0:1${(currentSongIndex % 2) + 0}", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { /* Handled by animation logic */ },
                            modifier = Modifier
                                .size(40.dp)
                                .scale(skipPreviousButtonScale)
                        ) {

                            Icon(
                                painter = painterResource(id = R.drawable.ic_double_arrow_left),
                                contentDescription = "Previous",
                                tint = Color.White
                            )
                        }

                        // --- Highlighted Change: New IconButton for Play/Pause Toggle ---
                        IconButton(
                            onClick = { /* Handled by animation logic */ }, // Your existing onClick logic
                            modifier = Modifier
                                .size(64.dp) // Maintain the larger size of the original Lottie button

                        ) {

                            Icon(
                                // Choose the icon based on gestureCycle
                                painter = painterResource(
                                    id = if (gestureCycle == 1) R.drawable.ic_play_button else R.drawable.ic_pause_button
                                ),
                                contentDescription = if (gestureCycle == 1) "Pause" else "Play",
                                tint = Color.White // Keep the tint as white
                            )
                        }



                        IconButton(
                            onClick = { /* Handled by animation logic */ },
                            modifier = Modifier
                                .size(40.dp)
                                .scale(skipNextButtonScale)
                        ) {

                            Icon(
                                painter = painterResource(id = R.drawable.ic_double_arrow_right),
                                contentDescription = "Next",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        val handDrawableResId = when (gestureCycle) {
            0 -> R.drawable.open_palm
            1 -> R.drawable.closed_fist
            2, 3 -> R.drawable.open_palm
            else -> R.drawable.open_palm
        }

        val phoneWidthPx = with(density) { 260.dp.toPx() }
        val phoneHeightPx = with(density) { 480.dp.toPx() }
        val handSizePx = with(density) { 150.dp.toPx() }
        val playButtonRowCenterYDp = 400.dp
        val playButtonRowCenterYPx = with(density) { playButtonRowCenterYDp.toPx() }


        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = handAlpha

                    val xTranslationRelativeToParentCenter = (handNormalizedX - 0.5f) * size.width

                    val yTranslation =
                        -(phoneHeightPx / 2) + playButtonRowCenterYPx - (handSizePx / 2)

                    translationX = xTranslationRelativeToParentCenter
                    translationY = yTranslation
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = handDrawableResId),
                contentDescription = "Hand gesture guide",
                modifier = Modifier.size(100.dp)
            )

            if (gestureCycle == 2) { // Swipe Right
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Swipe Right Arrow",
                    tint = Color.White.copy(alpha = handAlpha),
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = 50.dp)
                )
            } else if (gestureCycle == 3) { // Swipe Left
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Swipe Left Arrow",
                    tint = Color.White.copy(alpha = handAlpha),
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterStart)
                        .offset(x = (-50).dp)
                )
            }
        }


    }



}



@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun MediaControlScreenPreview() {
    AirTuneTheme {
        MediaControlScreen()
    }
}

@Preview(showBackground = true, widthDp = 200, heightDp = 350)
@Composable
fun MediaAnimationContentPreview() {
    AirTuneTheme {
        // Wrap it in a Box with specific size to simulate the phone screen
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(350.dp)
                .background(Color.DarkGray, shape = RoundedCornerShape(20.dp))
                .border(2.dp, Color.LightGray, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            MediaAnimationContent()
        }
    }
}