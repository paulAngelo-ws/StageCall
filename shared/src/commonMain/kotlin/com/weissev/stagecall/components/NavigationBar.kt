package com.weissev.stagecall.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun NavigationBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    // The list order matches the tab numbers used in App.kt.
    val labels = listOf("Feed", "Directory", "Applications", "Profile")
    val icons =
        listOf(
            Icons.Default.Home,
            Icons.Default.Search,
            Icons.AutoMirrored.Filled.List,
            Icons.Default.Person,
        )

    androidx.compose.material3.NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        for (index in labels.indices) {
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = { Icon(icons[index], contentDescription = null) },
                label = { Text(labels[index]) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
