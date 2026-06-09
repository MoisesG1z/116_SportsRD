package com.sports.redaccion

import com.sports.redaccion.data.PlatformTime
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

private fun getJsTime(): String = js("new Date().toTimeString().split(' ')[0]")

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    PlatformTime.formatTime = {
        try {
            getJsTime()
        } catch (e: Exception) {
            "00:00:00"
        }
    }
    ComposeViewport {
        App()
    }
}