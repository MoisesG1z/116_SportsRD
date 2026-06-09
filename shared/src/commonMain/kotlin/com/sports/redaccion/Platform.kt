package com.sports.redaccion

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform