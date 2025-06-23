package com.soumyadip.airtune.util

import android.content.Context
import android.media.session.MediaController
import android.media.session.PlaybackState
import com.soumyadip.airtune.service.NotificationListenerService

object MediaControlManager {


    private fun getPrimaryMediaController(context: Context): MediaController? {
        val activeControllers = NotificationListenerService.getActiveMediaControllers(context)

        val playingController = activeControllers.find { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        if (playingController != null) {

            return playingController
        }

        val pausedController = activeControllers.find { it.playbackState?.state == PlaybackState.STATE_PAUSED }
        if (pausedController != null) {

            return pausedController
        }


        return null
    }


    fun play(context: Context) {
        val controller = getPrimaryMediaController(context)
        if (controller != null) {
            controller.transportControls.play()

        }
    }

    fun pause(context: Context) {
        val controller = getPrimaryMediaController(context)
        if (controller != null) {
            controller.transportControls.pause()

        }
    }

    fun togglePlayPause(context: Context) {
        val controller = getPrimaryMediaController(context)
        if (controller != null) {
            val playbackState = controller.playbackState
            if (playbackState != null) {
                // Check if the current state is playing, buffering, fast forwarding, etc.
                // These states imply that media is currently active or about to be
                val isPlaying = playbackState.state == PlaybackState.STATE_PLAYING ||
                        playbackState.state == PlaybackState.STATE_BUFFERING ||
                        playbackState.state == PlaybackState.STATE_FAST_FORWARDING ||
                        playbackState.state == PlaybackState.STATE_REWINDING ||
                        playbackState.state == PlaybackState.STATE_SKIPPING_TO_NEXT ||
                        playbackState.state == PlaybackState.STATE_SKIPPING_TO_PREVIOUS ||
                        playbackState.state == PlaybackState.STATE_CONNECTING

                if (isPlaying) {
                    // If currently playing, send pause command
                    controller.transportControls.pause()

                } else {
                    // If not playing (paused, stopped, error, none), send play command
                    controller.transportControls.play()

                }
            } else {
                // If playbackState is null, we can try to play as a fallback or log a warning

                controller.transportControls.play()
            }
        } else {

        }
    }

    fun skipToNext(context: Context) {
        val controller = getPrimaryMediaController(context)
        if (controller != null) {
            if (controller.playbackState?.actions?.and(PlaybackState.ACTION_SKIP_TO_NEXT) != 0L) {
                controller.transportControls.skipToNext()

            }
        }
    }

    fun skipToPrevious(context: Context) {
        val controller = getPrimaryMediaController(context)
        if (controller != null) {
            if (controller.playbackState?.actions?.and(PlaybackState.STATE_SKIPPING_TO_PREVIOUS.toLong()) != 0L) { // Corrected constant
                controller.transportControls.skipToPrevious()

            }
        }
    }


}