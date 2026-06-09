package com.sports.redaccion.data

expect object SettingsStorage {
    fun saveString(key: String, value: String)
    fun getString(key: String, defaultValue: String = ""): String
}
