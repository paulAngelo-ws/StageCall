package com.weissev.stagecall.components

import androidx.compose.runtime.Composable

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import androidx.compose.material3.NavigationBar

@Composable
fun NavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFFFFFFFF),
        tonalElevation = 8.dp,
    ) {
        NavigationbarItem(
            selected = selectedTab == 0,
        )
    }
}