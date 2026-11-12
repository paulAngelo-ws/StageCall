package com.weissev.stagecall.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material.icons.Icons
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp

@Composable
private fun NavTabIcon( // Function for Icon States background pill
    icon: ImageVector,
    label: String,
    isSelected: Boolean
) {
    val pilColor = if (isSelected) Color(0xFFE0E0E0) else Color.Transparent
    val iconColor = if (isSelected) Color(0xFF1E1E1E) else Color(0xFF757575)

    Box(
        modifier = Modifier
            .background(color = pilColor, shape = RoundedCornerShape(percent = 50))
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor
        )
    }
}

@Composable
private fun NavTabLabel( // Text State Below the Icon, if selected bold text else not
    text: String,
    isSelected: Boolean
) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = if (isSelected) Color(0xFF1E1E1E) else Color(0xFF757575)
    )
}


@Composable
fun NavigationBar(
    selectedTab: Int, //Int Value to know which tab/page user is it or pressed
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFFFFFFFF),
        tonalElevation = 8.dp,
    ) {
        // Tab 0: Feed Page
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            icon = { NavTabIcon(icon = Icons.Default.Home, label = "Feed", isSelected = selectedTab == 0) },
            label = { NavTabLabel(text = "Feed", isSelected = selectedTab == 0) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )

        // Tab 1: Directory Page
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            icon = { NavTabIcon(icon = Icons.Default.Search, label = "Directory", isSelected = selectedTab == 1) },
            label = { NavTabLabel(text = "Directory", isSelected = selectedTab == 1) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )

        // Tab 2: Applications Page
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            icon = { NavTabIcon(icon = Icons.Default.List, label = "Applications", isSelected = selectedTab == 2) },
            label = { NavTabLabel(text = "Applications", isSelected = selectedTab == 2) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )

        // Tab 3: Profile Page
        NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            icon = { NavTabIcon(icon = Icons.Default.Person, label = "Profile", isSelected = selectedTab == 3) },
            label = { NavTabLabel(text = "Profile", isSelected = selectedTab == 3) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )
    }
}