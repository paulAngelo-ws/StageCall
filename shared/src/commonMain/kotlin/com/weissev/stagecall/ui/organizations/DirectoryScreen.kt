package com.weissev.stagecall.ui.organizations

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.data.StageCallRepository
import com.weissev.stagecall.ui.components.StageCallCard
import com.weissev.stagecall.ui.components.OrganizationCardBackground
import androidx.compose.ui.graphics.Color
import com.weissev.stagecall.ui.components.OrganizationThumbnail
import com.weissev.stagecall.ui.components.organizationCardColors

// repository contains the data shared by all screens.
@Composable
fun DirectoryPage(
    repository: StageCallRepository,
    onOpenOrganization: (String) -> Unit,
    onRegisterOrganization: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf("Recommended") }
    var showSorting by remember { mutableStateOf(false) }
    // The screen scrolls vertically when its content is taller than the window.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Organizations",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            FilledTonalButton(
                onClick = onRegisterOrganization,
                modifier = Modifier.heightIn(min = 48.dp)
                    .semantics { contentDescription = "Register an organization" },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Register", fontWeight = FontWeight.SemiBold)
                }
            }
        }
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
        // Filter the directory with the typed search, then apply the selected sort order.
        val searchText = query.trim() // Ignore spaces typed at either end of the search.
        var results =
            when {
                query.isEmpty() -> repository.organizations.toList()
                searchText.isEmpty() -> emptyList()
                else -> repository.organizations.filter { organization ->
                    organization.name.contains(searchText, ignoreCase = true) ||
                        organization.category.contains(searchText, ignoreCase = true)
                }
            }
        results =
            when (sort) {
                "A-Z" -> results.sortedBy { it.name }
                "Most members" -> results.sortedByDescending { it.memberCount }
                else -> results
            }
        // Stable sorting keeps the selected order within each bookmark group.
        results = results.sortedByDescending { it.id in repository.bookmarks } // Put bookmarked organizations first while keeping the chosen order within each group.
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
            OrganizationDirectoryCard(org, repository.organizations.indexOf(org), { onOpenOrganization(org.id) }) { contentColor ->
                val bookmarked = org.id in repository.bookmarks
                IconToggleButton(checked = bookmarked, onCheckedChange = { repository.toggleBookmark(org.id) }) {
                    Icon(if (bookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (bookmarked) "Remove bookmark for ${org.name}" else "Bookmark ${org.name}", tint = contentColor)
                }
            }
        }
    }
}
