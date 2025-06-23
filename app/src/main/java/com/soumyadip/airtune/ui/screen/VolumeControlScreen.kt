package com.soumyadip.airtune.ui.screen

import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.soumyadip.airtune.R
import com.soumyadip.airtune.service.GestureControlService
import com.soumyadip.airtune.ui.theme.AirTuneTheme
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext
import com.soumyadip.airtune.data.app_data.AppPreferences
import com.soumyadip.airtune.data.app_data.PrefManager


@Composable
fun VolumeControlScreen() {
    // This screen acts as the wrapper for your animated UI.
    VolumeGestureGuide()
}

@Composable
fun VolumeGestureGuide() {

    val context = LocalContext.current

    // NEW: Instantiate PrefManager using remember
    // This creates one instance of PrefManager for the Composable's lifecycle
    val prefManager = remember { PrefManager(context) }

    // Load the initial state from PrefManager
    var isVolumeControlEnabled by remember {
        mutableStateOf(prefManager.isVolumeControlEnabled())
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        Spacer(modifier = Modifier.height(12.dp))

        // --- Card Component at the top ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(bottom = 24.dp), // Padding below the card to separate it from the next element
            shape = RoundedCornerShape(12.dp), // Card shape
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), // Card elevation for shadow
            colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)) // Card background color changed to green with some transparency
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp), // Inner padding for the card content
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween // Distribute content horizontally
            ) {
                Text(
                    text = "Enable Volume Control",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f) // Text takes available space, pushing switch to end
                )
                Spacer(modifier = Modifier.width(16.dp)) // Space between text and switch
                Switch(
                    checked = isVolumeControlEnabled,
                    onCheckedChange = { checked ->
                        isVolumeControlEnabled = checked

                        // NEW: Save the new state using PrefManager's method
                        prefManager.saveVolumeControlEnabled(checked)

                        // --- SERVICE START/STOP LOGIC HERE ---
                        val serviceIntent = Intent(context, GestureControlService::class.java)
                        if (checked) {

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                context.startService(serviceIntent)
                            }
                            Toast.makeText(context, "Gesture Service Started", Toast.LENGTH_SHORT).show()
                        } else {


                            if(!AppPreferences.anyGestureFeatureEnabled(context)){
                                context.stopService(serviceIntent)
                            }
                            Toast.makeText(context, "Volume Control Service Stopped", Toast.LENGTH_SHORT).show()
                        }
                        // --- END SERVICE START/STOP LOGIC ---
                    },
                    colors = SwitchDefaults.colors( // Customizing switch colors
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color.Green.copy(alpha = 0.9f), // Slightly lighter green for track
                        uncheckedThumbColor = Color.DarkGray,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.8f)
                    ),

                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
        // --- END CARD ---

        Spacer(modifier = Modifier.height(45.dp))

        // Dummy Phone Screen and Animation Area
        Box(
            modifier = Modifier
                .width(200.dp) // Adjust size as needed
                .height(370.dp) // Adjust size as needed
                .background(Color.DarkGray, shape = RoundedCornerShape(20.dp))
                .border(2.dp, Color.LightGray, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            // This is where your animations will go
            VolumeAnimationContent()
        }

        Spacer(Modifier.height(40.dp))

        Text(
            text = "Volume Control Tip: Gesture Guide!\n" +
                    "• 👍⬆️ Thumb Up (Move Up): Increase Volume\n" +
                    "• 👍⬇️ Thumb Up (Move Down): Decrease Volume\n",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start
        )
    }
}


@Composable
fun VolumeAnimationContent() {
    // Define animation values for the infinite thumb movement
    val infiniteTransition = rememberInfiniteTransition(label = "volumeGuideTransition")

    // --- Intro Blink/Pop-up Animation at the start ---
    val popScale = remember { Animatable(initialValue = 0f) }
    val popAlpha = remember { Animatable(initialValue = 0f) }

    // Define very fast individual blink durations
    val popInDuration = 50 // ms
    val popOutDuration = 75 // ms

    val delayBetweenBlinks = 50 // ms (very short pause between the two pops)
    val clearDelayAfterBlinks = 500 // ms (e.g., 500ms or 1000ms as per your choice)

    // State to control when the thumb movement starts
    var startThumbMovement by remember { mutableStateOf(false) }

    // Thumb movement positions
    val thumbBottomY = 210f // 80% from top of inner screen (low volume)
    val thumbTopY = 90f    // 20% from top of inner screen (high volume)
    val thumbLoopDuration = 2000 // ms for one half-cycle (e.g., bottom to top)

    LaunchedEffect(Unit) {
        // This LaunchedEffect runs once when VolumeAnimationContent is composed
        repeat(2) { // Play the pop animation twice
            popScale.snapTo(0f) // Reset state for a fresh pop
            popAlpha.snapTo(0f)

            // Pop up phase (very fast)
            popScale.animateTo(
                targetValue = 1.2f, // Slightly larger than the icon
                animationSpec = tween(durationMillis = popInDuration, easing = LinearEasing)
            )
            popAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = popInDuration / 2, easing = LinearEasing)
            )

            delay(popInDuration.toLong()) // Wait for pop-in to complete

            // Pop down and fade out phase (very fast)
            popScale.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = popOutDuration, easing = LinearEasing)
            )
            popAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = popOutDuration / 2, easing = LinearEasing)
            )
            delay(popOutDuration.toLong()) // Wait for pop-out to complete

            if (it < 1) { // Only delay between the two blinks (not after the last blink)
                delay(delayBetweenBlinks.toLong())
            }
        }
        // AFTER the two blinks are COMPLETELY done, add the clear delay
        delay(clearDelayAfterBlinks.toLong())

        // Small buffer delay to ensure Compose is ready before starting the complex animation
        delay(50)

        startThumbMovement = true // Signal that thumb movement can now begin
    }

    // 1. Animate Thumb Up Icon Vertical Position
    val thumbOffsetY by if (startThumbMovement) {
        // Only start the infinite animation when 'startThumbMovement' is true
        infiniteTransition.animateFloat(
            initialValue = thumbBottomY, // Start at the visual bottom (low volume)
            targetValue = thumbTopY,  // Move up to the visual top (high volume)
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = thumbLoopDuration, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse // Go up and down
            ), label = "thumbOffset"
        )
    } else {
        // If not started yet, hold the thumb at its initial position (bottom of range)
        remember { mutableStateOf(thumbBottomY) }
    }

    // 2. Animate Volume Level based on thumb position
    val volumeLevel = remember(thumbOffsetY) {
        // Map thumbOffsetY (thumbBottomY at bottom, thumbTopY at top) to volume (0f to 1f)
        val normalizedPosition = (thumbOffsetY - thumbTopY) / (thumbBottomY - thumbTopY)
        // Invert it so thumbTopY (high volume) is 1 and thumbBottomY (low volume) is 0
        (1f - normalizedPosition).coerceIn(0f, 1f)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Dummy phone screen background (lighter inside area)
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(350.dp)
                .background(Color.Black.copy(alpha = 0.8f), shape = RoundedCornerShape(10.dp))
        )

        // Volume Bar (Vertical)
        VolumeBar(
            level = volumeLevel,
            modifier = Modifier
                .align(Alignment.CenterEnd) // Position on the right edge
                .padding(end = 20.dp)
        )

        // Box to contain the Thumb Up Icon and its blink effect
        Box(
            modifier = Modifier
                .size(60.dp) // Size of the icon and its effect area
                .graphicsLayer {
                    // Apply the main thumb vertical movement to this entire Box
                    translationY = thumbOffsetY.dp.toPx()
                }
                .align(Alignment.TopCenter) // Align this Box relative to its parent (dummy screen)
        ) {
            // "Blink" effect circle behind the thumb
            Box(
                modifier = Modifier
                    .fillMaxSize() // Fills the 60.dp sized parent Box
                    .graphicsLayer {
                        scaleX = popScale.value // Apply pop animation scale
                        scaleY = popScale.value
                        alpha = popAlpha.value // Apply pop animation alpha
                        transformOrigin = TransformOrigin.Center // Scale from center
                    }
                    .background(Color.Green.copy(alpha = 0.8f), shape = RoundedCornerShape(50)) // Green circle shape
            )

            // Thumb Up Icon
            Image(
                painter = painterResource(id = R.drawable.thumb_up), // Ensure this drawable exists
                contentDescription = "Thumb up gesture guide",
                modifier = Modifier
                    .fillMaxSize() // Fill the 60.dp sized parent Box
            )
        }
    }
}


@Composable
fun VolumeBar(level: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(20.dp) // Width of the volume bar
            .height(150.dp) // Height of the volume bar
            .background(Color.Gray, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp)) // Clip the content to rounded corners
    ) {
        // Filled part of the volume bar
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(level) // Fill based on the 'level'
                .align(Alignment.BottomCenter) // Starts filling from the bottom
                .background(Color.Green) // Color of the filled part
        )
    }
}


@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun VolumeGestureGuidePreview() {
    AirTuneTheme {
        VolumeGestureGuide()
    }
}

@Preview(showBackground = true, widthDp = 200, heightDp = 350)
@Composable
fun VolumeAnimationContentPreview() {
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
            VolumeAnimationContent()
        }
    }
}