package com.sports.redaccion

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

import com.sports.redaccion.data.PlatformTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun main() {
    PlatformTime.formatTime = {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "SportsRedaccion116",
        ) {
            App()
        }
    }
}