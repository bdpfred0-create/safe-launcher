package com.safelauncher.app.model

import android.graphics.drawable.Drawable

/**
 * Representa um aplicativo instalado no dispositivo.
 */
data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isSensitive: Boolean = false
)
