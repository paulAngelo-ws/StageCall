package com.weissev.stagecall.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberRepositoryCache(): RepositoryCache {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val preferences = context.getSharedPreferences("stagecall_cache", Context.MODE_PRIVATE)
        object : RepositoryCache {
            override fun read(): String? = preferences.getString("state", null)
            override fun write(value: String) {
                check(preferences.edit().putString("state", value).commit()) { "Cache write failed" }
            }
        }
    }
}
