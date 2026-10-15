package com.safelauncher.app.data

import android.content.Context
import android.content.SharedPreferences

class SecurityPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("safe_launcher_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SENSITIVE_PACKAGES = "sensitive_packages"
        private const val KEY_PANIC_MODE = "panic_mode"
        private const val KEY_MASTER_PIN = "master_pin"
        private const val KEY_DURESS_PIN = "duress_pin"
        private const val KEY_LOCKSCREEN_ENABLED = "lockscreen_enabled"

        private const val DEFAULT_MASTER_PIN = "1212"
        private const val DEFAULT_DURESS_PIN = "2121"
    }

    var isPanicMode: Boolean
        get() = prefs.getBoolean(KEY_PANIC_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_PANIC_MODE, value).apply()

    var isLockscreenEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCKSCREEN_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_LOCKSCREEN_ENABLED, value).apply()

    var masterPin: String
        get() = prefs.getString(KEY_MASTER_PIN, DEFAULT_MASTER_PIN) ?: DEFAULT_MASTER_PIN
        set(value) = prefs.edit().putString(KEY_MASTER_PIN, value).apply()

    var duressPin: String
        get() = prefs.getString(KEY_DURESS_PIN, DEFAULT_DURESS_PIN) ?: DEFAULT_DURESS_PIN
        set(value) = prefs.edit().putString(KEY_DURESS_PIN, value).apply()

    fun getSensitivePackages(): Set<String> {
        return prefs.getStringSet(KEY_SENSITIVE_PACKAGES, emptySet()) ?: emptySet()
    }

    fun setSensitivePackages(packages: Set<String>) {
        prefs.edit().putStringSet(KEY_SENSITIVE_PACKAGES, packages).apply()
    }

    fun toggleAppSensitive(packageName: String): Boolean {
        val current = getSensitivePackages().toMutableSet()
        val isNowSensitive = if (current.contains(packageName)) {
            current.remove(packageName)
            false
        } else {
            current.add(packageName)
            true
        }
        setSensitivePackages(current)
        return isNowSensitive
    }
}
