package com.example.terminal.ui

import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Configures the receiver [Window] to hide both the status and navigation bars using
 * immersive mode. The system bars remain hidden until the user performs an explicit gesture
 * to reveal them temporarily.
 */
fun Window.enterImmersiveMode() {
    WindowCompat.setDecorFitsSystemWindows(this, false)

    WindowInsetsControllerCompat(this, decorView).apply {
        hide(WindowInsetsCompat.Type.systemBars())
        systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
