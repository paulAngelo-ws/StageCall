package org.example.arena_or_classroom_scout

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform