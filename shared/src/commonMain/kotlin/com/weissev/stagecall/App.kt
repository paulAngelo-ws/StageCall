package com.weissev.stagecall

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.material3.Scaffold
import com.weissev.stagecall.components.TopHeader

@Composable
@Preview
fun App() {
    Scaffold(
        topBar = {
            TopHeader(
                onNotificationClick = {
                    println("TopHeader clicked")
                }
            )
        },

    ) {

    }
}