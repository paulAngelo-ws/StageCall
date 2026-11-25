package com.weissev.stagecall.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.delay

/** Transient feedback that also stays visible above a form's dialog. */
@Composable
internal fun ErrorNotice(message: String?, onDismiss: () -> Unit = {}) {
    var visible by remember(message) { mutableStateOf(!message.isNullOrBlank()) }
    val dismiss by rememberUpdatedState(onDismiss)
    val accessibility = androidx.compose.ui.platform.LocalAccessibilityManager.current
    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            delay(accessibility?.calculateRecommendedTimeoutMillis(6_000L,
                containsIcons = false, containsText = true, containsControls = true) ?: 6_000L)
            visible = false
            dismiss()
        }
    }
    if (visible && message != null) {
        Popup(alignment = Alignment.BottomCenter) {
            Snackbar(modifier = Modifier.padding(16.dp).widthIn(max = 560.dp),
                action = { TextButton(onClick = { visible = false; dismiss() }) { Text("Dismiss") } }) {
                Text(message)
            }
        }
    }
}
