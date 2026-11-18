package com.weissev.stagecall.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults

@Composable
internal actual fun rememberRepositoryCache(): RepositoryCache = remember {
    object : RepositoryCache {
        override fun read(): String? = NSUserDefaults.standardUserDefaults.stringForKey("stagecall_state_v1")
        override fun write(value: String) {
            NSUserDefaults.standardUserDefaults.setObject(value, forKey = "stagecall_state_v1")
        }
    }
}
