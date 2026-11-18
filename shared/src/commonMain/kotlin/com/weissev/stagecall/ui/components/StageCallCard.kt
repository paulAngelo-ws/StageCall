package com.weissev.stagecall.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** A quiet, flat surface shared by content sections across the app. */
@Composable
// Feature screens pass their card content here. StageCallTheme.kt sets the shared shapes and colors.
fun StageCallCard(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        colors = colors,
        // The theme sets the corner radius for every shared card.
        shape = MaterialTheme.shapes.medium, // Uses the medium corner radius from StageCallTheme.
        // A thin outline defines the card edge. Zero elevation keeps it flat.
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), // 1.dp sets the border thickness. outlineVariant sets its color.
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Increase this value to add a shadow under the card.
        // Each screen supplies what goes inside the card.
        content = content,
    )
}
