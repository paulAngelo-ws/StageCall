package com.weissev.stagecall

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform