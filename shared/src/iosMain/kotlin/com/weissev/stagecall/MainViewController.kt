package com.weissev.stagecall

import androidx.compose.ui.window.ComposeUIViewController
import com.weissev.stagecall.data.auth.AuthSession
import io.github.jan.supabase.auth.handleDeeplinks
import platform.Foundation.NSURL

// ContentView.swift embeds this controller to show the shared UI in App.kt.
fun MainViewController() = ComposeUIViewController { App() }

// iOSApp.swift forwards the Google callback URL here. AuthSession.kt owns the client that handles it.
fun handleAuthUrl(url: NSURL) { AuthSession.client?.handleDeeplinks(url) }
