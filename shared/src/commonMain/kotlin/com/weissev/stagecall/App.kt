package com.weissev.stagecall

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box

//Component Imports:
import com.weissev.stagecall.components.NavigationBar
import com.weissev.stagecall.components.TopHeader

//Screen Imports:
import com.weissev.stagecall.screens.PublicFeed

@Composable
@Preview
fun App() {
    var selectedTab by remember { mutableStateOf(0) } // MutableState, to know what tab the user is in, default 1 (Feed Page)

    Scaffold(
        topBar = {
            TopHeader(
                onNotificationClick = {
                    println("TopHeader clicked") // Idk what The Notification Does for Now, js print stuff
                }
            )
        },
        bottomBar = {
            NavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { newTab -> // when clicked it will pass the value of the tab we are in/going to selectedtab value
                    selectedTab = newTab
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> PublicFeed()
            }
        }
    }
}