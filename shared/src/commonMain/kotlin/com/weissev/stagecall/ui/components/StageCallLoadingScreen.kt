package com.weissev.stagecall.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.ui.theme.StageCallTheme

@Composable
internal fun StageCallBrandBackground(content: @Composable () -> Unit) {
    // LoginScreen.kt uses this same background so the sign-in and loading screens share the same artwork.
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(brush = Brush.verticalGradient(listOf(Color(0xFFE7F3FF), Color(0xFFF8FBFF), Color.White)))
                val unit = minOf(size.width, size.height)
                drawCircle(Color(0xFF8DC8F4).copy(alpha = 0.22f), radius = unit * 0.44f, center = Offset(size.width * 0.94f, size.height * 0.05f))
                drawCircle(Color(0xFF6CAEE5).copy(alpha = 0.18f), radius = unit * 0.28f, center = Offset(size.width * 0.04f, size.height * 0.78f))
                drawCircle(Color(0xFF70B5ED).copy(alpha = 0.24f), radius = unit * 0.16f, center = Offset(size.width * 0.10f, size.height * 0.16f), style = Stroke(width = unit * 0.008f))
                drawCircle(Color(0xFF72B5E9).copy(alpha = 0.22f), radius = unit * 0.09f, center = Offset(size.width * 0.88f, size.height * 0.81f), style = Stroke(width = unit * 0.006f))
            }
            content()
        }
    }
}

@Composable
internal fun StageCallBrand() {
    Icon(
        Icons.Filled.TheaterComedy,
        contentDescription = null,
        modifier = Modifier.padding(bottom = 16.dp).size(96.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
    Text("StageCall", style = MaterialTheme.typography.headlineLarge)
}

@Composable
internal fun StageCallLoadingScreen(message: String) {
    // App.kt sends the status message for session restoration or the first data fetch.
    StageCallTheme {
        StageCallBrandBackground {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StageCallBrand()
                CircularProgressIndicator(Modifier.padding(top = 24.dp).size(24.dp), strokeWidth = 2.dp) // The moving ring shows that the current step is still running.
                Text(
                    message,
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
