package com.sports.redaccion.data

import platform.Foundation.NSUserDefaults

actual object SettingsStorage {
    actual fun saveString(key: String, value: String) {
        NSUserDefaults.standardUserDefaults.setObject(value, forKey = key)
    }

    actual fun getString(key: String, defaultValue: String): String {
        return NSUserDefaults.standardUserDefaults.stringForKey(key) ?: defaultValue
    }
}
