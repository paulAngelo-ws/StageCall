package com.weissev.stagecall.data

import kotlinx.serialization.Serializable

@Serializable
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
    val nextActivitySchedule: String,
)
