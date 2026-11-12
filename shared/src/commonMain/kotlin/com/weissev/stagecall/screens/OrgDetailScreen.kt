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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OrgDetailScreen(
    org: Organization,
    isBookmarked: Boolean,
    hasApplied: Boolean,
    onToggleBookmark: (String) -> Unit,
    onApplyClick: (Organization) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedDetailTab by remember { mutableStateOf("About") }
    val detailTabs = listOf("About", "Activities", "Updates")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9F9))
            .padding(horizontal = 16.dp)
    ) {
        // Top Header with Back Button & Bookmark
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
                            contentDescription = "Back",
                            tint = Color(0xFF1E1E1E)
                        )
                    }
                    Text(
                        text = "Organization details",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1E1E)
                    )
                }

                IconButton(onClick = { onToggleBookmark(org.id) }) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark Organization",
                        tint = Color(0xFF1E1E1E)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Info Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${org.category.uppercase()} · UNIVERSITY RECOGNIZED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF757575),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = org.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1E1E)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = org.shortTagline,
                        fontSize = 13.sp,
                        color = Color(0xFF424242),
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${org.memberCount} members",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E1E1E)
                        )
                        Text(
                            text = if (org.isRecruiting) "Applications open" else "Applications closed",
                            fontSize = 12.sp,
                            color = if (org.isRecruiting) Color(0xFF1E1E1E) else Color(0xFF757575),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-Navigation Tabs (About | Activities | Updates)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                detailTabs.forEach { tab ->
                    val isSelected = selectedDetailTab == tab
                    Column(
                        modifier = Modifier
                            .clickable { selectedDetailTab = tab }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = tab,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF1E1E1E) else Color(0xFF757575)
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(2.dp)
                                    .background(Color(0xFF1E1E1E))
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = Color(0xFFE0E0E0))
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Tab Body Content
        item {
            when (selectedDetailTab) {
                "About" -> {
                    Text(
                        text = org.aboutHeadline,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1E1E)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = org.fullDescription,
                        fontSize = 13.sp,
                        color = Color(0xFF424242),
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Membership requirements",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1E1E)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    org.requirements.forEach { req ->
                        Text(
                            text = "• $req",
                            fontSize = 13.sp,
                            color = Color(0xFF424242),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Application deadline: ${org.deadline}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E1E1E)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Next Activity Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "NEXT ACTIVITY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF757575)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = org.nextActivityTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E1E1E)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = org.nextActivitySchedule,
                                fontSize = 12.sp,
                                color = Color(0xFF757575)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Questions? Contact an officer",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E1E1E)
                        )
                        Text(
                            text = "Message →",
                            fontSize = 12.sp,
                            color = Color(0xFF616161),
                            modifier = Modifier.clickable {
                                println("Message officer clicked for ${org.name}")
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Apply CTA Button
                    Button(
                        onClick = { onApplyClick(org) },
                        enabled = org.isRecruiting && !hasApplied,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E1E1E),
                            contentColor = Color(0xFFFFFFFF),
                            disabledContainerColor = Color(0xFFBDBDBD),
                            disabledContentColor = Color(0xFFFFFFFF)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = when {
                                hasApplied -> "Application Submitted (Under Review)"
                                org.isRecruiting -> "Apply for membership"
                                else -> "Recruitment Closed"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                "Activities" -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = org.nextActivityTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E1E1E))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = org.nextActivitySchedule, fontSize = 13.sp, color = Color(0xFF757575))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Open to all SITizens and prospective applicants.", fontSize = 13.sp, color = Color(0xFF424242))
                        }
                    }
                }

                "Updates" -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Semester Recruitment Drive", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E1E1E))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Posted recently · Official Announcement", fontSize = 12.sp, color = Color(0xFF757575))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = org.shortTagline, fontSize = 13.sp, color = Color(0xFF424242))
                        }
                    }
                }
            }
        }
    }
}