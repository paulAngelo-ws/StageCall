package com.weissev.stagecall.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.weissev.stagecall.components.StageCallCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weissev.stagecall.data.StageCallRepository

// repository contains the data shared by all screens.
@Composable
fun MembersPage(repository: StageCallRepository, orgId: String, onShowMessage: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    // The screen scrolls vertically when its content is taller than the window.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Member directory",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search name or student email") },
            singleLine = true,
        )
        val members =
            repository.members.filter {
                it.organizationId == orgId &&
                    (it.student.name.contains(query, true) ||
                        it.student.email.contains(query, true))
            }
        Text("${members.size} members")
        if (members.isEmpty()) Text("No matching members.")
        for (member in members) {
            StageCallCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(member.student.name, style = MaterialTheme.typography.titleMedium)
                    Text("${member.role} - ${if (member.active) "Active" else "Inactive"}")
                    OutlinedButton(
                        onClick = {
                            onShowMessage(
                                "${member.student.name}\n${member.student.email}\n${member.student.studentId}\n${member.role}"
                            )
                        }
                    ) {
                        Text("Details")
                    }
                    OutlinedButton(
                        onClick = {
                            val index = repository.members.indexOf(member)
                            repository.members[index] = member.copy(active = !member.active)
                        }
                    ) {
                        Text(if (member.active) "Mark inactive" else "Mark active")
                    }
                }
            }
        }
    }
}
