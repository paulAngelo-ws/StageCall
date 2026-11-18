package com.weissev.stagecall.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import com.weissev.stagecall.components.StageCallCard
import com.weissev.stagecall.components.organizationCardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.data.StageCallRepository

// repository contains the data shared by all screens.
@Composable
fun DirectoryPage(
    repository: StageCallRepository,
    onOpenOrganization: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf("Recommended") }
    var showSorting by remember { mutableStateOf(false) }
    val cardContentColor = MaterialTheme.colorScheme.onSurface
    // The screen scrolls vertically when its content is taller than the window.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Organizations",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        BasicTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                .semantics { contentDescription = "Search org name or category" },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = true,
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) Text(
                            "Search org name or category",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        innerTextField()
                    }
                }
            },
        )
        val searchText = query.trim()
        var results =
            repository.organizations.filter { organization ->
                organization.name.contains(searchText, ignoreCase = true) ||
                    organization.category.contains(searchText, ignoreCase = true)
            }
        results =
            when (sort) {
                "A-Z" -> results.sortedBy { it.name }
                "Most members" -> results.sortedByDescending { it.memberCount }
                else -> results
            }
        // Stable sorting keeps the selected order within each bookmark group.
        results = results.sortedByDescending { it.id in repository.bookmarks }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("${results.size} organizations found", modifier = Modifier.weight(1f))
            Box {
                TextButton(onClick = { showSorting = true }) {
                    Text(sort)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Sort organizations")
                }
                DropdownMenu(
                    expanded = showSorting,
                    onDismissRequest = { showSorting = false },
                ) {
                    for (option in listOf("Recommended", "A-Z", "Most members")) {
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                sort = option
                                showSorting = false
                            },
                        )
                    }
                }
            }
        }
        if (results.isEmpty()) Text("No matching organizations. Try a different search.")
        for (org in results) {
            val (_, barColor) = organizationCardColors[
                repository.organizations.indexOf(org) % organizationCardColors.size
            ]
            StageCallCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Open ${org.name}",
                        onClick = { onOpenOrganization(org.id) },
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = cardContentColor,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp)
                        .drawBehind {
                            drawRect(barColor, size = Size(6.dp.toPx(), size.height))
                        }
                        .padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            org.name,
                            modifier = Modifier.weight(1f).padding(top = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = cardContentColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val bookmarked = org.id in repository.bookmarks
                        IconToggleButton(
                            checked = bookmarked,
                            onCheckedChange = { repository.toggleBookmark(org.id) },
                        ) {
                            Icon(
                                imageVector = if (bookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (bookmarked) "Remove bookmark for ${org.name}" else "Bookmark ${org.name}",
                                tint = cardContentColor,
                            )
                        }
                    }
                    Text(
                        "${org.category} - ${if (org.isRecruiting) "Recruiting" else "Closed"}",
                        modifier = Modifier.padding(top = 16.dp, end = 12.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = cardContentColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
