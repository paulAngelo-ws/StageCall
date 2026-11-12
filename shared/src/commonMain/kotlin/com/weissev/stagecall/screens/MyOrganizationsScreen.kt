package com.weissev.stagecall.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MyOrganizationsScreen(
    allOrganizations: List<Organization>,
    bookmarkedIds: Set<String>,
    appliedOrgIds: Set<String>,
    onOpenOrgClick: (Organization) -> Unit,
    onBackClick: () -> Unit
) {
    val bookmarkedOrgs = allOrganizations.filter { it.id in bookmarkedIds }
    val appliedOrgs = allOrganizations.filter { it.id in appliedOrgIds }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9F9))
            .padding(horizontal = 16.dp)
    ) {
        // Header with Back Arrow
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Directory",
                            tint = Color(0xFF1E1E1E)
                        )
                    }
                    Text(
                        text = "My organizations",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1E1E)
                    )
                }
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = "Saved",
                    tint = Color(0xFF1E1E1E)
                )
            }

            Text(
                text = "Your memberships, applications, and saved organizations in one place.",
                fontSize = 13.sp,
                color = Color(0xFF757575),
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
        }

        // Section 1: Memberships
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Memberships", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E1E))
                Text(text = "1 active", fontSize = 12.sp, color = Color(0xFF757575))
            }
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "ACTIVE MEMBER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF757575))
                        Text(text = "Since Oct 2026", fontSize = 11.sp, color = Color(0xFF757575))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Honor Society", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E1E))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Member · Peer tutoring & study support", fontSize = 12.sp, color = Color(0xFF424242))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            allOrganizations.firstOrNull()?.let { onOpenOrgClick(it) }
                        },
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Color(0xFF424242)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1E1E1E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Open organization", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Section 2: Applications (Dynamic based on user applying + default SSVG)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Applications", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E1E))
                Text(text = "${appliedOrgs.size} pending", fontSize = 12.sp, color = Color(0xFF757575))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(appliedOrgs) { org ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "UNDER REVIEW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF757575))
                        Text(text = "Submitted Nov 2026", fontSize = 11.sp, color = Color(0xFF757575))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = org.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E1E))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your application was received. An officer will review it soon.",
                        fontSize = 12.sp,
                        color = Color(0xFF424242)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Submitted → In review", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E1E))
                        Text(
                            text = "View details →",
                            fontSize = 11.sp,
                            color = Color(0xFF616161),
                            modifier = Modifier.clickable { onOpenOrgClick(org) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Section 3: Saved / Bookmarked Organizations
        if (bookmarkedOrgs.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Saved / Bookmarked", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E1E))
                    Text(text = "${bookmarkedOrgs.size} saved", fontSize = 12.sp, color = Color(0xFF757575))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(bookmarkedOrgs) { org ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenOrgClick(org) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = org.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E1E))
                            Text(text = org.category, fontSize = 12.sp, color = Color(0xFF757575))
                        }
                        Text(text = "View org →", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E1E1E))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Section 4: Coming Up
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Coming up", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E1E))
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEEEEEE), shape = RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(text = "Programming study session", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E1E))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Honor Society Nov 12 · 5:00 PM · IT Lab", fontSize = 12.sp, color = Color(0xFF616161))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "For active members", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E1E1E))
                        Text(text = "View activity →", fontSize = 11.sp, color = Color(0xFF616161))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Decisions and club updates appear in Feed.",
                fontSize = 12.sp,
                color = Color(0xFF757575)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}