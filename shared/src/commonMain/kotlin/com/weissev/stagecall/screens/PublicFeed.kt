package com.weissev.stagecall.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.contentColorFor

data class FeedPost( // data class representing feed posts (idk probs this will be temporary for testing)
    val id: String,
    val orgName: String,
    val timeAgo: String,
    val title: String,
    val content: String,
    val statusBadge: String,
    val category: String
)

@Composable
fun PostCard(post: FeedPost) { //accepts the dataclass in variable named post idk
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically, // List all Post in vertical form
            ) {
                Box( // Handles Org Logo
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0E0E0))
                ) {
                    Icon( // Org Icon
                        imageVector = Icons.Default.Image,
                        contentDescription = "Org Logo",
                        tint = Color(0xFF424242),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp)
                ) {
                    Text( // Text for OrgName
                        text = post.orgName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1E1E)
                    )
                    Text( // Text for time Ago
                        text = post.timeAgo,
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )
                }

                Icon( // 3 Dot Icon
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More Options",
                    tint = Color(0xFF424242)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box( // Handles Image Post
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(165.dp)
                    .background(Color(0xFFE5E5E5), shape = RoundedCornerShape(8.dp))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Post Media",
                        tint = Color(0xFF424242),
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "POST MEDIA",
                        fontSize = 11.sp,
                        color = Color(0xFF616161)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text( // Text for Post Title
                text = post.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E1E1E)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = post.content, // Text for Content
                fontSize = 14.sp,
                color = Color(0xFF333333),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row( // CTA Buttons
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE5E5E5), shape = RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = post.statusBadge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E1E1E)
                    )
                }

                OutlinedButton(
                    onClick = {
                        println(post.orgName) // idk what to do here yet, so this is just temporary
                    },
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color(0xFF424242)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1E1E1E))
                ) {
                    Text(
                        text = "View Organization",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PublicFeed() {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Recruitment", "Events", "Announcements") //Filter Buttons

    val samplePosts = listOf(
        FeedPost(
            id = "1",
            orgName = "Google Developer Groups - UB",
            timeAgo = "2h ago",
            title = "Build with us this semester.",
            content = "Join the web, mobile or AI team. All skill levels welcome — curiosity comes first.",
            statusBadge = "Recruitment Open",
            category = "Recruitment"
        ),
        FeedPost(
            id = "2",
            orgName = "UB Cybersecurity Society",
            timeAgo = "5h ago",
            title = "Intro to Capture the Flag.",
            content = "Learn how to approach CTF challenges, practice binary exploitation, and team up.",
            statusBadge = "Event Upcoming",
            category = "Events"
        ),
        FeedPost(
            id = "3",
            orgName = "SOIT Student Council",
            timeAgo = "1d ago",
            title = "IT Week 2026 Registration Open",
            content = "Register now for the annual programming competition, UI/UX design contest, and e-sports tournament. Check out the mechanics!",
            statusBadge = "Registration Open",
            category = "Events"
        ),
        FeedPost(
            id = "4",
            orgName = "UB Coders Guild",
            timeAgo = "2d ago",
            title = "Peer Tutoring & Code Review Sessions",
            content = "Struggling with Data Structures or Object-Oriented Programming? Drop by the IT lab every Tuesday afternoon for assistance.",
            statusBadge = "Recruitment Open",
            category = "Recruitment"
        ),
        FeedPost(
            id = "5",
            orgName = "SOIT Dean's Office",
            timeAgo = "3d ago",
            title = "Midterm Examination Schedule Released",
            content = "Please check your student portals for the official room assignments and exam schedules for the upcoming midterms.",
            statusBadge = "Important",
            category = "Announcements"
        )
    )

    val filteredPosts = if (selectedCategory == "All") {
        samplePosts
    } else {
        samplePosts.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9F9))
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Public Feed",
                fontWeight = FontWeight.Bold,
                fontSize = 25.sp,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Browse announcements from campus organizations.",
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category

                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) Color(0xFFE0E0E0) else Color(0xFFFFFFFF),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFFBDBDBD) else Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = category,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = Color(0xFF1E1E1E)
                        )
                    }
                }
            }
        }

        //Shows all Sample Post // or Filtered if selected
        items(filteredPosts) { post ->
            PostCard(post = post)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}