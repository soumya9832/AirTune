package com.soumyadip.airtune.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils

object PermissionUtils {


    fun isNotificationServiceEnabled(context: Context): Boolean {
        val pkgName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && TextUtils.equals(pkgName, cn.packageName)) {

                    return true
                }
            }
        }

        return false
    }

    /**
     * Launches the system settings screen to prompt the user to enable
     * Notification Listener Service access for this app.
     */
    fun requestNotificationServiceAccess(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        try {
            context.startActivity(intent)

        } catch (e: Exception) {

            // Fallback for some devices or specific ROMs that might not handle this intent
            // You might inform the user to navigate manually.
        }
    }

}