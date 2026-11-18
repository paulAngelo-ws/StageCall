package com.weissev.stagecall

import android.os.Bundle
import android.content.Intent
import io.github.jan.supabase.auth.handleDeeplinks
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        AuthSession.client?.handleDeeplinks(intent)
        setContent { App() }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        AuthSession.client?.handleDeeplinks(intent)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
