package com.sports.redaccion.data

import android.content.Context

actual object SettingsStorage {
    private val context: Context by lazy {
        Class.forName("android.app.ActivityThread")
            .getMethod("currentApplication")
            .invoke(null) as Context
    }

    private val prefs by lazy {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }

    actual fun saveString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    actual fun getString(key: String, defaultValue: String): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }
}
