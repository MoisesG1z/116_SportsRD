package com.sports.redaccion.data

import kotlinx.browser.window

actual object SettingsStorage {
    actual fun saveString(key: String, value: String) {
        window.localStorage.setItem(key, value)
    }

    actual fun getString(key: String, defaultValue: String): String {
        return window.localStorage.getItem(key) ?: defaultValue
    }
}
