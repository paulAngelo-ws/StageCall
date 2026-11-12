package com.weissev.stagecall.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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

data class Organization(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val shortTagline: String,
    val aboutHeadline: String,
    val fullDescription: String,
    val memberCount: Int,
    val isRecruiting: Boolean,
    val requirements: List<String>,
    val deadline: String,
    val nextActivityTitle: String,
    val nextActivitySchedule: String
)

val ubSoitOrganizations = listOf(
    Organization(
        id = "1",
        name = "Google Developer Groups - UB",
        category = "Academic / Tech",
        description = "Build real-world web, mobile, and AI software projects with a collaborative student developer community.",
        shortTagline = "Empowering SITizens through Google technologies, peer workshops, and hackathons.",
        aboutHeadline = "Build solutions that matter",
        fullDescription = "Google Developer Groups on Campus at the University of Baguio connects student developers interested in Android, Kotlin Multiplatform, Web, Cloud, and AI. Members collaborate on real campus projects and host technical study jams.",
        memberCount = 86,
        isRecruiting = true,
        requirements = listOf("Currently enrolled UB SOIT student", "Attend the developer onboarding session"),
        deadline = "November 25, 2026",
        nextActivityTitle = "Kotlin & Compose Study Jam",
        nextActivitySchedule = "Nov 16 · 3:00 PM · SIT AVR"
    ),
    Organization(
        id = "2",
        name = "UB Cybersecurity Society",
        category = "Academic / Tech",
        description = "Explore ethical hacking, Capture the Flag (CTF) competitions, network defense, and digital forensics.",
        shortTagline = "Training the next generation of ethical hackers and information security defenders.",
        aboutHeadline = "Defend, analyze, and secure",
        fullDescription = "Supervised by the University of Baguio School of Information Technology, the Cybersecurity Society prepares students for national CTF tournaments while promoting campus-wide digital safety and penetration testing workshops.",
        memberCount = 54,
        isRecruiting = true,
        requirements = listOf("Currently enrolled UB student", "Complete basic Linux & networking assessment"),
        deadline = "November 22, 2026",
        nextActivityTitle = "Intro to Capture the Flag Workshop",
        nextActivitySchedule = "Nov 19 · 4:30 PM · IT Lab 3"
    ),
    Organization(
        id = "3",
        name = "SSVG",
        category = "Community Service",
        description = "Student-led tech outreach, digital literacy training, and peer academic support for SITizens.",
        shortTagline = "Student-led tech outreach and peer support for SITizens.",
        aboutHeadline = "Put IT skills to work",
        fullDescription = "Supervised by the University of Baguio SIT Student Body, SSVG connects IT learning with community service, tech assistance, university events, and peer mentoring.",
        memberCount = 42,
        isRecruiting = true,
        requirements = listOf("Currently enrolled SIT student", "Attend one orientation session"),
        deadline = "November 20, 2026",
        nextActivityTitle = "Volunteer orientation",
        nextActivitySchedule = "Nov 18 · 4:00 PM · Student Center"
    ),
    Organization(
        id = "4",
        name = "Association for Computing",
        category = "Academic / Tech",
        description = "Connect through algorithmic coding contests, software engineering seminars, and research colloquiums.",
        shortTagline = "Advancing computing as a science and profession across the UB campus.",
        aboutHeadline = "Code, compete, and innovate",
        fullDescription = "The UB Association for Computing organizes competitive programming bootcamps, algorithm problem-solving sessions, and industry guest lectures for Computer Science and Information Technology majors.",
        memberCount = 68,
        isRecruiting = true,
        requirements = listOf("Open to all BSCS and BSIT students", "Submit membership registration form"),
        deadline = "November 28, 2026",
        nextActivityTitle = "Algorithmic Coding Sprint",
        nextActivitySchedule = "Nov 21 · 1:30 PM · IT Lab 1"
    ),
    Organization(
        id = "5",
        name = "UB Game Development Club",
        category = "Creative / Media",
        description = "Design 2D/3D games, digital art assets, and interactive experiences using Unity, Godot, and Blender.",
        shortTagline = "Bringing stories and interactive worlds to life through game design and programming.",
        aboutHeadline = "Create playable worlds",
        fullDescription = "A multidisciplinary collective of programmers, digital artists, and sound designers building indie games for annual university showcases and game jams.",
        memberCount = 49,
        isRecruiting = false,
        requirements = listOf("Portfolio or sample project submission", "Interview with core team leads"),
        deadline = "Closed for Semester 1",
        nextActivityTitle = "Game Jam Showcase",
        nextActivitySchedule = "Dec 04 · 2:00 PM · FGB Hall"
    )
)

@Composable
fun DirectoryScreen(
    bookmarkedIds: Set<String>,
    onToggleBookmark: (String) -> Unit,
    onBookmarkBannerClick: () -> Unit,
    onViewOrgClick: (Organization) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // Dropdown States
    var selectedCategory by remember { mutableStateOf("All") }
    var categoryExpanded by remember { mutableStateOf(false) }
    val categories = listOf("All", "Academic / Tech", "Community Service", "Creative / Media")

    var selectedStatus by remember { mutableStateOf("Recruiting Only") }
    var statusExpanded by remember { mutableStateOf(false) }
    val statuses = listOf("Recruiting Only", "All Statuses", "Closed")

    var selectedSort by remember { mutableStateOf("Recommended") }
    var sortExpanded by remember { mutableStateOf(false) }
    val sortOptions = listOf("Recommended", "A - Z", "Most Members")

    // Filter + Sort Logic
    val filteredOrgs = ubSoitOrganizations
        .filter { org ->
            val matchesQuery = searchQuery.isBlank() ||
                    org.name.contains(searchQuery, ignoreCase = true) ||
                    org.category.contains(searchQuery, ignoreCase = true) ||
                    org.description.contains(searchQuery, ignoreCase = true)

            val matchesCategory = selectedCategory == "All" || org.category == selectedCategory

            val matchesStatus = when (selectedStatus) {
                "Recruiting Only" -> org.isRecruiting
                "Closed" -> !org.isRecruiting
                else -> true
            }

            matchesQuery && matchesCategory && matchesStatus
        }
        .let { list ->
            when (selectedSort) {
                "A - Z" -> list.sortedBy { it.name }
                "Most Members" -> list.sortedByDescending { it.memberCount }
                else -> list
            }
        }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9F9))
            .padding(horizontal = 16.dp)
    ) {
        // 1. Header Title & Working Bookmark Banner Button
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Campus Organizations",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1E1E)
                )
                IconButton(onClick = onBookmarkBannerClick) {
                    Icon(
                        imageVector = if (bookmarkedIds.isNotEmpty()) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "My Organizations & Saved",
                        tint = Color(0xFF1E1E1E)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Working Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(text = "Search org name or category...", fontSize = 13.sp, color = Color(0xFF757575))
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF757575))
                },
                shape = RoundedCornerShape(50),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFBDBDBD),
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedContainerColor = Color(0xFFFFFFFF),
                    unfocusedContainerColor = Color(0xFFFFFFFF)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Working Filter Dropdown Pills Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Category Dropdown
                Box {
                    Row(
                        modifier = Modifier
                            .background(
                                color = if (selectedCategory != "All") Color(0xFFE0E0E0) else Color(0xFFFFFFFF),
                                shape = RoundedCornerShape(50)
                            )
                            .border(1.dp, Color(0xFFBDBDBD), shape = RoundedCornerShape(50))
                            .clickable { categoryExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Category: $selectedCategory", fontSize = 12.sp, color = Color(0xFF1E1E1E))
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF757575))
                    }

                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                        containerColor = Color(0xFFFFFFFF)
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(text = category, fontSize = 13.sp, color = Color(0xFF1E1E1E)) },
                                onClick = {
                                    selectedCategory = category
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Status Dropdown
                Box {
                    Row(
                        modifier = Modifier
                            .background(
                                color = if (selectedStatus != "All Statuses") Color(0xFFE0E0E0) else Color(0xFFFFFFFF),
                                shape = RoundedCornerShape(50)
                            )
                            .border(1.dp, Color(0xFFBDBDBD), shape = RoundedCornerShape(50))
                            .clickable { statusExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Status: $selectedStatus", fontSize = 12.sp, color = Color(0xFF1E1E1E))
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF757575))
                    }

                    DropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false },
                        containerColor = Color(0xFFFFFFFF)
                    ) {
                        statuses.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(text = status, fontSize = 13.sp, color = Color(0xFF1E1E1E)) },
                                onClick = {
                                    selectedStatus = status
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Dynamic Result Count & Working Sort Dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredOrgs.size} organizations found",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E1E1E)
                )

                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { sortExpanded = true }
                    ) {
                        Text(text = selectedSort, fontSize = 13.sp, color = Color(0xFF757575))
                        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF757575))
                    }

                    DropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false },
                        containerColor = Color(0xFFFFFFFF)
                    ) {
                        sortOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(text = option, fontSize = 13.sp, color = Color(0xFF1E1E1E)) },
                                onClick = {
                                    selectedSort = option
                                    sortExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // 5. Organization Cards List with Complete Descriptions
        items(filteredOrgs) { org ->
            OrgCard(
                org = org,
                isBookmarked = org.id in bookmarkedIds,
                onToggleBookmark = { onToggleBookmark(org.id) },
                onViewOrgClick = onViewOrgClick
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun OrgCard(
    org: Organization,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onViewOrgClick: (Organization) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Rounded Square Logo Placeholder
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFEAEAEA), shape = RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Org Logo",
                    tint = Color(0xFF424242),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Organization Info Column (Full Description Included)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = org.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E1E1E)
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Category Tag Pill
                Box(
                    modifier = Modifier
                        .background(Color(0xFFF1F1F1), shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = org.category,
                        fontSize = 10.sp,
                        color = Color(0xFF616161),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = org.description,
                    fontSize = 12.sp,
                    color = Color(0xFF424242),
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Action Column (Recruiting Badge + Bookmark + View Org Button)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (org.isRecruiting) Color(0xFFF1F1F1) else Color(0xFFEEEEEE),
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (org.isRecruiting) "Recruiting" else "Closed",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (org.isRecruiting) Color(0xFF1E1E1E) else Color(0xFF757575)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Save Org",
                        tint = Color(0xFF1E1E1E),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onToggleBookmark() }
                    )
                }

                OutlinedButton(
                    onClick = { onViewOrgClick(org) },
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color(0xFFBDBDBD)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1E1E1E)),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "View org",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}