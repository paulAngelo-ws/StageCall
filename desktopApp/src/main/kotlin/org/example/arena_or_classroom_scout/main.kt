package org.example.arena_or_classroom_scout

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Arena-or-Classroom-Scout",
    ) {
        App()
    }
}