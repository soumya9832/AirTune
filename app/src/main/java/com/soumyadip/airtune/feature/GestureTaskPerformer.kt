package com.soumyadip.airtune.feature

import android.content.Context
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult

interface GestureTaskPerformer{
    fun performVolumeTask(result:GestureRecognizerResult)
    fun performBrightnessTask(context: Context,result: GestureRecognizerResult)
    fun performMediaTask(context: Context,result: GestureRecognizerResult)
}