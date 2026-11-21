package com.weissev.stagecall

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.weissev.stagecall.data.auth.AuthSession
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Pass the launch intent to the client in AuthSession.kt before App.kt starts observing the session.
        AuthSession.client?.handleDeeplinks(intent)
        setContent {
            App { onBack -> BackHandler(onBack = onBack) }
        }
    }
    // When Android reuses this activity, send the new callback intent to the same client in AuthSession.kt.
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
