package com.weissev.stagecall.ui.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.ui.components.StageCallBrand
import com.weissev.stagecall.ui.components.StageCallBrandBackground

@Composable
internal fun LoginScreen(error: String?, enabled: Boolean, signingIn: Boolean = false, onSignIn: () -> Unit) {
    // StageCallLoadingScreen.kt supplies the same logo and background used while the app loads.
    StageCallBrandBackground {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            StageCallBrand()
            Text("Discover your campus community", modifier = Modifier.padding(top = 8.dp, bottom = 24.dp), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Text("Use your University of Baguio Google account (for example, id@s.ubaguio.edu).", modifier = Modifier.padding(bottom = 16.dp), textAlign = TextAlign.Center)
            // Return the button tap to App.kt, which starts Google sign-in in AuthSession.kt.
            OutlinedButton(onClick = onSignIn, enabled = enabled, // App.kt receives the tap and starts sign-in through AuthSession.kt.
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Color(0xFF1F1F1F))) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val googlePaths = remember {
                        listOf(
                            Color(0xFF4285F4) to "M21.35,12.25c0,-0.68 -0.06,-1.18 -0.19,-1.7H12v3.09h5.38c-0.11,0.77 -0.69,1.93 -1.98,2.71l-0.02,0.1 2.9,2.25 0.2,0.02c1.83,-1.69 2.87,-4.17 2.87,-6.47z",
                            Color(0xFF34A853) to "M12,21.75c2.62,0 4.83,-0.86 6.44,-2.35l-3.08,-2.37c-0.83,0.57 -1.94,0.97 -3.36,0.97 -2.57,0 -4.75,-1.69 -5.53,-4.02l-0.1,0.01 -3.02,2.34 -0.04,0.1A9.73,9.73 0,0 0,12 21.75z",
                            Color(0xFFFBBC05) to "M6.47,13.98a5.99,5.99 0,0 1,0 -3.96l-0.01,-0.11 -3.06,-2.38 -0.1,0.05a9.75,9.75 0,0 0,0 8.85z",
                            Color(0xFFEA4335) to "M12,6c1.82,0 3.05,0.79 3.75,1.45l2.74,-2.67A9.35,9.35 0,0 0,12 2.25a9.73,9.73 0,0 0,-8.7 5.33l3.17,2.44C7.25,7.69 9.43,6 12,6z",
                        ).map { (color, path) -> color to PathParser().parsePathString(path).toPath() }
                    }
                    Canvas(Modifier.size(20.dp)) {
                        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
                            googlePaths.forEach { (color, path) -> drawPath(path, color) }
                        }
                    }
                    Text(if (signingIn) "Opening Google…" else "Continue with Google")
                }
            }
            if (error != null) Text(error, modifier = Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.error)
        }
    }
}
