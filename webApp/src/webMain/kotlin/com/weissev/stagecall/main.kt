package com.weissev.stagecall

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

@OptIn(ExperimentalComposeUiApi::class)
// The script loaded by index.html starts this viewport and renders App.kt inside it.
fun main() {
    ComposeViewport { App() }
}
