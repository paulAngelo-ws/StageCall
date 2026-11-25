package com.weissev.stagecall.ui.organizations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.ui.components.StageCallCard
import com.weissev.stagecall.ui.components.OrganizationCardBackground
import androidx.compose.ui.graphics.Color
import com.weissev.stagecall.ui.components.OrganizationThumbnail
import com.weissev.stagecall.ui.components.organizationCardColors

@Composable
internal fun OrganizationDirectoryCard(
    org: com.weissev.stagecall.data.Organization,
    colorIndex: Int,
    onOpen: () -> Unit,
    trailingContent: @Composable (Color) -> Unit = {},
) {
    val hasBanner = !org.bannerPath.isNullOrBlank()
    val organizationContentColor = if (hasBanner) Color.White else MaterialTheme.colorScheme.onSurface
    // Cycle through the accent colors using the organization’s position in the full directory.
    val (_, barColor) = organizationCardColors[
        colorIndex.mod(organizationCardColors.size)
    ]
    StageCallCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(
                role = Role.Button,
                onClickLabel = "Open ${org.name}",
                // Send the selected ID to App.kt. It opens OrganizationScreen.kt with that ID.
                onClick = { onOpen() }, // Send this organization ID to App.kt when the card is tapped.
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = organizationContentColor,
        ),
    ) {
        OrganizationCardBackground(org.bannerPath, if (hasBanner) barColor else MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp)
                .drawBehind {
                    // Draw a 6 dp wide bar down the left side of the card.
                    if (!hasBanner) drawRect(barColor, size = Size(6.dp.toPx(), size.height))
                }
                .padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Box(Modifier.padding(top = 8.dp, end = 12.dp)) {
                    OrganizationThumbnail(org.photoPath)
                }
                Text(
                    org.name,
                    modifier = Modifier.weight(1f).padding(top = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = organizationContentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                trailingContent(organizationContentColor)
            }
            Text(
                "${org.category} - ${if (org.isRecruiting) "Recruiting" else "Closed"}",
                modifier = Modifier.padding(top = 16.dp, end = 12.dp),
                style = MaterialTheme.typography.labelMedium,
                color = organizationContentColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        }
    }
}
