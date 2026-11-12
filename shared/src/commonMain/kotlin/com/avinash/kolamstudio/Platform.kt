package com.avinash.kolamstudio

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform