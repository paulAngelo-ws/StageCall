package com.weissev.stagecall.data

import androidx.compose.runtime.Composable

internal interface RepositoryCache {
    fun read(): String?
    fun write(value: String)
}

@Composable
internal expect fun rememberRepositoryCache(): RepositoryCache
