package com.soumyadip.airtune.service

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log


class NotificationListenerService : NotificationListenerService() {

    companion object{

        private var currentListenerComponent:ComponentName? = null

        fun setListenerComponent(componentName: ComponentName){
            currentListenerComponent = componentName
        }

        fun getActiveMediaControllers(context: Context) : List<MediaController>{
            val component = currentListenerComponent

            return try {
                val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
                manager.getActiveSessions(component)
            } catch (e: SecurityException) {

                emptyList()
            } catch (e: Exception) {

                emptyList()
            }
        }

    }

    override fun onCreate() {
        super.onCreate()
        setListenerComponent(ComponentName(this, NotificationListenerService::class.java))

    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // Not directly used for media control, but shows the service is active
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Not directly used for media control
    }

    override fun onListenerConnected() {
        super.onListenerConnected()

        setListenerComponent(ComponentName(this, NotificationListenerService::class.java))
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()

        currentListenerComponent = null
    }




}