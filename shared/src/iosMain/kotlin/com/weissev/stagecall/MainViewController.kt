package com.weissev.stagecall

import androidx.compose.ui.window.ComposeUIViewController
import io.github.jan.supabase.auth.handleDeeplinks
import platform.Foundation.NSURL

fun MainViewController() = ComposeUIViewController { App() }

fun handleAuthUrl(url: NSURL) { AuthSession.client?.handleDeeplinks(url) }
