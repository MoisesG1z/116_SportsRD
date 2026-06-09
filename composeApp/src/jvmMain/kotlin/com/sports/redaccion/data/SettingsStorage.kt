package com.sports.redaccion.data

import java.util.prefs.Preferences

actual object SettingsStorage {
    private val prefs = Preferences.userNodeForPackage(SocialFeedMonitor::class.java)

    actual fun saveString(key: String, value: String) {
        prefs.put(key, value)
    }

    actual fun getString(key: String, defaultValue: String): String {
        return prefs.get(key, defaultValue)
    }
}
