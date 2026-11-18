package com.weissev.stagecall

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

// Open the desktop window and hand its content to App.kt.
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "StageCall") { App() }
}
