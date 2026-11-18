package com.weissev.stagecall.data

import android.content.Context
import android.util.AtomicFile
import java.io.File
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberCacheStorage(): CacheStorage {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val preferences = context.getSharedPreferences("stagecall_cache", Context.MODE_PRIVATE)
        CacheStorage { key ->
            // Keep the legacy preference entry readable. New cache files stay on the device and use atomic writes.
            if (key == "state") {
                object : RepositoryCache {
                    override fun read(): String? = preferences.getString(key, null)
                    override fun write(value: String) {
                        check(preferences.edit().putString(key, value).commit()) { "Cache write failed" }
                    }
                }
            } else {
                val file = AtomicFile(File(context.noBackupFilesDir, "$key.json")) // Give this entry its own device-only file.
                object : RepositoryCache {
                    override fun read(): String? =
                        if (file.baseFile.exists()) file.readFully().decodeToString() else null

                    override fun remove() = file.delete()

                    override fun write(value: String) {
                        // Finish the temporary write before replacing the saved file so an interrupted write can keep the previous copy.
                        val output = file.startWrite()
                        try {
                            output.write(value.encodeToByteArray())
                            file.finishWrite(output) // Commit the new copy after the write finishes.
                        } catch (failure: Exception) {
                            file.failWrite(output)
                            throw failure
                        }
                    }
                }
            }
        }
    }
}
