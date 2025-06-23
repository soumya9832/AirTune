package com.soumyadip.airtune.data.app_data

import android.content.Context
import android.content.SharedPreferences

class PrefManager(context:Context) {
    private val prefName = "my_app_prefs" // Your main preference file name
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()

    companion object{
        const val KEY_VOLUME_CONTROL_ENABLED = "volume_control_enabled"
        const val KEY_BRIGHTNESS_CONTROL_ENABLED = "brightness_control_enabled"
        const val KEY_MEDIA_CONTROL_ENABLED = "media_control_enabled"

    }


    fun saveFeatureEnabled(key: String, enabled: Boolean) {
        editor.putBoolean(key, enabled).apply()
    }

    fun isFeatureEnabled(key: String, defaultValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    fun saveVolumeControlEnabled(enabled: Boolean) {
        editor.putBoolean(KEY_VOLUME_CONTROL_ENABLED, enabled).apply()
    }

    fun isVolumeControlEnabled(): Boolean {
        // Default to false if the preference isn't found
        return sharedPreferences.getBoolean(KEY_VOLUME_CONTROL_ENABLED, false)
    }
}