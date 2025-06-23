// File: app/src/main/java/com/soumyadip/airtune/util/BrightnessComposeOverlay.kt
package com.soumyadip.airtune.ui.ux

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class BrightnessComposeOverlay(private val context: Context) {
    private val windowManager: WindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { hide() }

    private val _currentBrightness: MutableState<Int> = mutableStateOf(0)

    // A single class that implements all necessary "Owner" interfaces
    private class OverlayViewOwners : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        // Correct and ONLY declaration for viewModelStore
        override val viewModelStore: ViewModelStore = ViewModelStore()
        private val savedStateRegistryController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        // This getter references the single viewModelStore declared above
        override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

        init {
            // Restore SavedStateRegistry immediately, as we're not tied to an Activity's Bundle
            savedStateRegistryController.performRestore(null)

            // Register an observer to tie SavedStateRegistry to the lifecycle
            lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    // This is handled by the init block for SavedStateRegistryController
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    // Clear ViewModelStore when the lifecycle is destroyed
                    viewModelStore.clear()
                }
            })
        }

        // Methods to manually control the lifecycle state
        fun createAndResume() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        fun pauseAndDestroy() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
    }

    private var overlayOwners: OverlayViewOwners? = null

    /**
     * Shows or updates the brightness overlay.
     * @param brightnessLevel The current system brightness level (0-255).
     */
    fun show(brightnessLevel: Int) {
        _currentBrightness.value = brightnessLevel


        if (composeView == null) {
            // Create new owners and set their initial state
            overlayOwners = OverlayViewOwners()
            overlayOwners?.createAndResume() // Move to RESUMED state

            composeView = ComposeView(context).apply {
                id = View.generateViewId()

                // Set all three owners using the extension functions
                overlayOwners?.let {
                    this.setViewTreeLifecycleOwner(it)
                    this.setViewTreeViewModelStoreOwner(it)
                    this.setViewTreeSavedStateRegistryOwner(it)
                }

                // Set layout parameters for the WindowManager
                layoutParams = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    y = 150 // Adjust vertical position as needed
                }

                // Set the Compose content
                setContent {
                    BrightnessIndicator(brightnessLevel = _currentBrightness.value)
                }
            }

            try {
                windowManager.addView(composeView, composeView?.layoutParams)

            } catch (e: Exception) {

            }
        }

        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(hideRunnable, 1500)
    }

    /**
     * Hides the brightness overlay.
     */
    fun hide() {
        if (composeView != null && composeView?.isAttachedToWindow == true) {
            try {
                windowManager.removeView(composeView)

            } catch (e: Exception) {

            }

            composeView?.disposeComposition()
            // Clear all tree owners when the view is detached
            composeView?.setViewTreeLifecycleOwner(null)
            composeView?.setViewTreeViewModelStoreOwner(null)
            composeView?.setViewTreeSavedStateRegistryOwner(null)

            // Transition owner to destroyed state and clear its resources
            overlayOwners?.pauseAndDestroy()
            overlayOwners = null // Clear the owner reference

            composeView = null
        }
        handler.removeCallbacks(hideRunnable)
    }
}