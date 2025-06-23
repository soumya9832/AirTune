package com.soumyadip.airtune.data.app_data

import android.content.Context

object AppPreferences {

    // Default values for each feature
    const val DEFAULT_CALL_DIALER_ENABLED = false
    const val DEFAULT_BRIGHTNESS_ENABLED = false
    const val DEFAULT_VOLUME_ENABLED = false
    const val DEFAULT_SPOTIFY_ENABLED = false


    // Map gestures to their corresponding preference keys
    val GESTURE_TO_FEATURE_KEY_MAP = mapOf(
        "Thumb_Up" to PrefManager.KEY_VOLUME_CONTROL_ENABLED,
        "Victory" to PrefManager.KEY_BRIGHTNESS_CONTROL_ENABLED,
        "Open_Palm" to PrefManager.KEY_MEDIA_CONTROL_ENABLED,
        "Closed_Fist" to PrefManager.KEY_MEDIA_CONTROL_ENABLED
    )

    val ALL_FEATURE_KEYS = listOf(
        PrefManager.KEY_VOLUME_CONTROL_ENABLED,
        PrefManager.KEY_BRIGHTNESS_CONTROL_ENABLED,
        PrefManager.KEY_MEDIA_CONTROL_ENABLED
        // Ensure all future feature keys are added to this list
    )

    // Provide a way to get the PrefManager instance
    private var prefManagerInstance: PrefManager? = null

    fun getPrefManager(context: Context): PrefManager {
        if (prefManagerInstance == null) {
            prefManagerInstance = PrefManager(context.applicationContext) // Use applicationContext
        }
        return prefManagerInstance!!
    }

    // Now, your helper functions use PrefManager
    fun isFeatureEnabled(context: Context, key: String, defaultValue: Boolean): Boolean {
        return getPrefManager(context).isFeatureEnabled(key, defaultValue)
    }

    fun setFeatureEnabled(context: Context, key: String, isEnabled: Boolean) {
        getPrefManager(context).saveFeatureEnabled(key, isEnabled)
    }

    fun anyGestureFeatureEnabled(context: Context): Boolean {
        // Iterate through all known feature keys
        for (key in ALL_FEATURE_KEYS) {
            // Check if this feature is enabled.
            // Since we know all defaults are false, we can simplify this.
            if (isFeatureEnabled(context, key,false)) { // Calling the simplified isFeatureEnabled
                return true // Found at least one enabled feature
            }
        }
        return false // No enabled features found
    }


}