package com.tenisturnuva.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tenisturnuva.app.data.model.Club
import com.tenisturnuva.app.data.repository.TournamentRepository
import com.tenisturnuva.app.data.session.SessionManager
import com.tenisturnuva.app.ui.components.AppScaffold
import kotlinx.coroutines.launch

private val repository = TournamentRepository()

@Composable
fun ClubListScreen(onClubSelected: (String) -> Unit, onLogout: () -> Unit) {
    var clubs by remember { mutableStateOf<List<Club>>(emptyList()) }
    var newClubName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val isAdmin = SessionManager.isAdmin

    suspend fun refresh() {
        try {
            clubs = repository.getClubs()
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = "Kulupler yuklenemedi: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    AppScaffold(title = "Kulupler", onLogout = onLogout) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (isAdmin) {
                Text(
                    "Yeni bir kulup olustur",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newClubName,
                        onValueChange = { newClubName = it },
                        label = { Text("Kulup adi") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val name = newClubName.trim()
                            if (name.isNotEmpty()) {
                                scope.launch {
                                    try {
                                        repository.createClub(name)
                                        newClubName = ""
                                        refresh()
                                    } catch (e: Exception) {
                                        errorMessage = "Kulup olusturulamadi: ${e.message}"
                                    }
                                }
                            }
                        },
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            Text("Kulupler", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isLoading) {
                CircularProgressIndicator()
            } else if (clubs.isEmpty()) {
                Text(
                    if (isAdmin) "Henuz kulup yok — yukaridan bir tane olustur."
                    else "Henuz goruntulenecek kulup yok.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn {
                    items(clubs) { club ->
                        ClubRow(club = club, onClick = { onClubSelected(club.id) })
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ClubRow(club: Club, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Groups,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                club.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}
