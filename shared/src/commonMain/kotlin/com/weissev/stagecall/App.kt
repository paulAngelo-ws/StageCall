package com.weissev.stagecall

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box

//Component Imports:
import com.weissev.stagecall.components.NavigationBar
import com.weissev.stagecall.components.TopHeader

//Screen Imports:
import com.weissev.stagecall.screens.PublicFeed
import com.weissev.stagecall.screens.DirectoryScreen
import com.weissev.stagecall.screens.MyOrganizationsScreen
import com.weissev.stagecall.screens.OrgDetailScreen
import com.weissev.stagecall.screens.Organization
import com.weissev.stagecall.screens.ubSoitOrganizations
@Composable
@Preview
fun App() {
    var selectedTab by remember { mutableStateOf(0) } // MutableState, to know what tab the user is in, default 1 (Feed Page)

    var selectedOrgForDetails by remember { mutableStateOf<Organization?>(null) }
    var showMyOrganizations by remember { mutableStateOf(false) }
    var bookmarkedOrgIds by remember { mutableStateOf(setOf("1")) }
    var appliedOrgIds by remember { mutableStateOf(setOf("3")) }

    Scaffold(
        topBar = {
            TopHeader(
                onNotificationClick = {
                    println("TopHeader clicked") // Idk what The Notification Does for Now, js print stuff
                }
            )
        },
        bottomBar = {
            NavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { newTab -> // when clicked it will pass the value of the tab we are in/going to selectedtab value
                    selectedTab = newTab

                    selectedOrgForDetails = null
                    showMyOrganizations = false
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> PublicFeed()
                1 -> {
                    when {
                        selectedOrgForDetails != null -> {
                            val currentOrg = selectedOrgForDetails!!
                            OrgDetailScreen(
                                org = currentOrg,
                                isBookmarked = currentOrg.id in bookmarkedOrgIds,
                                hasApplied = currentOrg.id in appliedOrgIds,
                                onToggleBookmark = { orgId ->
                                    bookmarkedOrgIds = if (orgId in bookmarkedOrgIds) {
                                        bookmarkedOrgIds - orgId
                                    } else {
                                        bookmarkedOrgIds + orgId
                                    }
                                },
                                onApplyClick = { org ->
                                    appliedOrgIds = appliedOrgIds + org.id
                                },
                                onBackClick = {
                                    selectedOrgForDetails = null
                                }
                            )
                        }

                        showMyOrganizations -> {
                            MyOrganizationsScreen(
                                allOrganizations = ubSoitOrganizations,
                                bookmarkedIds = bookmarkedOrgIds,
                                appliedOrgIds = appliedOrgIds,
                                onOpenOrgClick = { org ->
                                    selectedOrgForDetails = org
                                },
                                onBackClick = {
                                    showMyOrganizations = false
                                }
                            )
                        }

                        else -> {
                            DirectoryScreen(
                                bookmarkedIds = bookmarkedOrgIds,
                                onToggleBookmark = { orgId ->
                                    bookmarkedOrgIds = if (orgId in bookmarkedOrgIds) {
                                        bookmarkedOrgIds - orgId
                                    } else {
                                        bookmarkedOrgIds + orgId
                                    }
                                },
                                onBookmarkBannerClick = {
                                    showMyOrganizations = true
                                },
                                onViewOrgClick = { org ->
                                    selectedOrgForDetails = org
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}