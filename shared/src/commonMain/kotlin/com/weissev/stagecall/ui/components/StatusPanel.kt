package com.weissev.stagecall.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
// App.kt and feature screens supply a message and optional retry callback. The screen or store decides what retry does.
internal fun StatusPanel(
    message: String,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    isError: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        if (loading) CircularProgressIndicator() // Show the spinner only while the calling screen is waiting.
        Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionLabel != null && onAction != null) TextButton(onClick = onAction) { Text(actionLabel) } // Show a button only if the caller supplied both its text and its action.
    }
}
