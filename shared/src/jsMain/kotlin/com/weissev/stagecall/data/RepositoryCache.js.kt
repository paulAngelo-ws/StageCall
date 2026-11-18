package com.weissev.stagecall.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

private fun readState(): String? = js("localStorage.getItem('stagecall_state_v1')") as String?
private fun writeState(value: String) { js("localStorage.setItem('stagecall_state_v1', value)") }

@Composable
internal actual fun rememberRepositoryCache(): RepositoryCache = remember {
    object : RepositoryCache {
        override fun read(): String? = readState()
        override fun write(value: String) = writeState(value)
    }
}
