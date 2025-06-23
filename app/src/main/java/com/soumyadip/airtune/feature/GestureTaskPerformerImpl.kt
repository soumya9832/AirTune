package com.soumyadip.airtune.feature

import android.content.ContentResolver
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import com.soumyadip.airtune.ui.ux.BrightnessComposeOverlay
import com.soumyadip.airtune.util.MediaControlManager

class GestureTaskPerformerImpl(context: Context):GestureTaskPerformer {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager


    private var brightnessOverlay: BrightnessComposeOverlay? = null // Compose-based overlay for brightness

    init {
        // Initialize Compose-based brightness overlay here
        brightnessOverlay = BrightnessComposeOverlay(context)
    }

    // Create a Handler associated with the main thread's Looper
    private val mainHandler = Handler(Looper.getMainLooper())

    // --- State variables for Media Control Gestures ---
    // Stores the X coordinate of the hand when an "Open_Palm" gesture was first recognized.
    // Used to track movement for swipes. Null when no open palm is detected or gesture completed.
    private var lastOpenPalmX: Float? = null

    // Stores the timestamp when the "Open_Palm" gesture was first detected or reset.
    // Used to measure how long the open palm has been relatively static.
    private var startTimeOfOpenPalm: Long = 0L

    // Stores the timestamp of the last *major* media control action (play/pause/skip).
    // Used for cooldown to prevent rapid, unintended actions.
    private var lastMediaActionTime: Long = 0L

    // --- Constants for Media Gesture Logic ---
    private val COOLDOWN_MILLIS = 100L // 0.1 seconds cooldown between major media actions

    // Swipe threshold in normalized coordinates (0.0 to 1.0)
    // A movement of 10% of the screen width is often 4a good starting point for a "solid" swipe.
    private val SWIPE_THRESHOLD_NORMALIZED = 0.1f // e.g., 0.1 means 10% of screen width

    // How long an open palm must be held *relatively still* to trigger play/pause.
    private val STATIC_OPEN_PALM_DELAY_MILLIS = 50L // 0.05 seconds

    // Maximum movement allowed for an open palm to be considered "static" (not a swipe).
    private val STATIC_OPEN_PALM_MOVEMENT_TOLERANCE_NORMALIZED = 0.03f // e.g., 3% of screen width



    @RequiresApi(Build.VERSION_CODES.O)
    override fun performVolumeTask(result:GestureRecognizerResult) {
        val landmarks = result.landmarks().first()

        if (landmarks.isNotEmpty()) {
            val handY = landmarks[0].y() // Wrist Y coordinate


            val minY = 0.2f
            val maxY = 0.8f

            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val volumeLevel = ((maxY - handY) / (maxY - minY)).coerceIn(0f, 1f)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val newVolume = (volumeLevel * maxVolume).toInt()



            try {
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    newVolume,
                    AudioManager.FLAG_SHOW_UI
                )


                // Immediately check if the volume actually changed internally
                val volumeAfterSet = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)


            } catch (e: SecurityException) {
                // This catch block is important, though you said permissions are fine.
                // It's a good sanity check for unexpected OEM behavior.

            } catch (e: Exception) {

            }


        }
    }

    override fun performBrightnessTask(context: Context, result: GestureRecognizerResult) {
        val landmarks = result.landmarks().firstOrNull() // Get landmarks for the first detected hand

        if (landmarks != null && landmarks.isNotEmpty()) {
            val wristLandmark = landmarks[0] // Wrist is typically the first landmark in MediaPipe's hand model
            val handX = wristLandmark.x()    // Get the wrist's X coordinate (normalized 0.0 to 1.0)


            val GESTURE_MIN_X = 0.2f // Left extreme of the horizontal gesture
            val GESTURE_MAX_X = 0.8f // Right extreme of the horizontal gesture

            val MIN_BRIGHTNESS_VALUE = 0    // Android brightness range is 0-255
            val MAX_BRIGHTNESS_VALUE = 255


            // Calculate the brightness level (0.0f to 1.0f)
            // As handX moves from GESTURE_MIN_X (left) to GESTURE_MAX_X (right),
            // the brightnessLevel will smoothly increase from 0.0f to 1.0f.
            val brightnessLevel = ((handX - GESTURE_MIN_X) / (GESTURE_MAX_X - GESTURE_MIN_X)).coerceIn(0f, 1f)

            // Convert the normalized brightness level to Android's system brightness scale (0-255)
            val newBrightness = (brightnessLevel * MAX_BRIGHTNESS_VALUE).toInt().coerceIn(MIN_BRIGHTNESS_VALUE, MAX_BRIGHTNESS_VALUE)



            // --- Important: Check and Handle WRITE_SETTINGS Permission ---
            if (Settings.System.canWrite(context)) {
                try {
                    val contentResolver: ContentResolver = context.contentResolver

                    // Ensure the screen brightness mode is set to manual.
                    // If it's in automatic mode, your manual changes won't be applied.
                    if (Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                        Settings.System.putInt(
                            contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS_MODE,
                            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                        )
                    }

                    // Apply the new brightness value to the system settings
                    Settings.System.putInt(
                        contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS,
                        newBrightness
                    )


                    // --- INTEGRATION: Show the Compose-based brightness overlay ---
                    // This is the key line to display your visual feedback!
                    // Post the UI update to the main thread's message queue
                    mainHandler.post {
                        brightnessOverlay?.show(newBrightness)
                    }

                } catch (e: SecurityException) {
                    // This catch block is vital! It means WRITE_SETTINGS permission was required but not granted.

                } catch (e: Exception) {
                    // Catch any other unexpected errors during brightness setting
                }
            } else {
                // This is where you'd typically prompt the user to grant the permission

                // In your actual UI, you'd show a dialog and direct them to:
                // Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                // context.startActivity(intent)
            }
        } else {

        }
    }

    override fun performMediaTask(context: Context, result: GestureRecognizerResult) {

        val currentTime = System.currentTimeMillis()
        val handLandmarks = result.landmarks().firstOrNull() // Get landmarks for the primary hand
        val gestures = result.gestures() // Get recognized gestures

        val isOpenPalm = gestures.any { it.first().categoryName() == "Open_Palm"} // Adjust score threshold
        val isClosedFist = gestures.any { it.first().categoryName() == "Closed_Fist"} // Adjust score threshold

        if (handLandmarks == null || handLandmarks.isEmpty()) {
            // No hand detected, reset tracking
            lastOpenPalmX = null
            startTimeOfOpenPalm = 0L
            return
        }

        val currentHandX = handLandmarks[0].x() // Use wrist X for movement tracking (normalized)

        if (currentTime - lastMediaActionTime < COOLDOWN_MILLIS) {
            // Still in cooldown period, ignore new gestures for media

            return
        }

        if (isClosedFist) {

            MediaControlManager.pause(context)
            lastMediaActionTime = currentTime
            // Reset open palm state since a different gesture is active
            lastOpenPalmX = null
            startTimeOfOpenPalm = 0L
        } else if (isOpenPalm) {
            if (lastOpenPalmX == null) {
                // First detection of Open_Palm, initialize tracking
                lastOpenPalmX = currentHandX
                startTimeOfOpenPalm = currentTime

            } else {
                val deltaX = currentHandX - lastOpenPalmX!! // Non-null asserted because of check above

                // --- Check for Swipe Gesture ---
                if (Math.abs(deltaX) > SWIPE_THRESHOLD_NORMALIZED) {
                    if (deltaX > 0) { // Moved right significantly

                        MediaControlManager.skipToNext(context)
                    } else { // Moved left significantly

                        MediaControlManager.skipToPrevious(context)
                    }
                    lastMediaActionTime = currentTime // Action taken, apply cooldown
                    lastOpenPalmX = null // Reset tracking after a swipe
                    startTimeOfOpenPalm = 0L // Reset start time
                }
                // --- Check for Static Open Palm (Play/Resume) ---
                else if (currentTime - startTimeOfOpenPalm > STATIC_OPEN_PALM_DELAY_MILLIS &&
                    Math.abs(deltaX) < STATIC_OPEN_PALM_MOVEMENT_TOLERANCE_NORMALIZED) {
                    // Open palm held relatively still for the required duration

                    MediaControlManager.play(context)
                    lastMediaActionTime = currentTime // Action taken, apply cooldown
                    lastOpenPalmX = null // Reset tracking after play/resume
                    startTimeOfOpenPalm = 0L // Reset start time
                }
                // Else: Open_Palm detected, but not a significant swipe yet,
                // and not static enough/long enough for play/resume.
                // Continue tracking (do nothing and let the next frame update the state).
            }
        } else {
            // Neither Open_Palm nor Closed_Fist detected, or scores too low.
            // Reset tracking as the desired gesture is no longer active.
            lastOpenPalmX = null
            startTimeOfOpenPalm = 0L

        }
    }







    // You might also want a way to hide the overlay when the brightness feature is disabled
    fun hideBrightnessOverlay() {
        // Post the UI update to the main thread's message queue
        mainHandler.post {
            brightnessOverlay?.hide()
        }
    }

}