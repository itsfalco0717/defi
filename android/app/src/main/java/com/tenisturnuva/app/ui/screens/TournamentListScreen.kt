package com.tenisturnuva.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.tenisturnuva.app.data.model.Tournament
import com.tenisturnuva.app.data.repository.TournamentRepository
import com.tenisturnuva.app.data.session.SessionManager
import com.tenisturnuva.app.ui.components.AppScaffold
import kotlinx.coroutines.launch

private val repository = TournamentRepository()

@Composable
fun TournamentListScreen(
    clubId: String,
    onTournamentSelected: (tournamentId: String, status: String) -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    var tournaments by remember { mutableStateOf<List<Tournament>>(emptyList()) }
    var newName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val isAdmin = SessionManager.isAdmin

    suspend fun refresh() {
        try {
            tournaments = repository.getTournaments(clubId)
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = "Turnuvalar yuklenemedi: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(clubId) { refresh() }

    AppScaffold(title = "Turnuvalar", onBack = onBack, onLogout = onLogout) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (isAdmin) {
                Text("Yeni turnuva olustur", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Turnuva adi") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val name = newName.trim()
                            if (name.isNotEmpty()) {
                                scope.launch {
                                    try {
                                        val created = repository.createTournament(clubId, name, "manual")
                                        newName = ""
                                        onTournamentSelected(created.id, created.status)
                                    } catch (e: Exception) {
                                        errorMessage = "Turnuva olusturulamadi: ${e.message}"
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

            Text("Turnuvalar", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isLoading) {
                CircularProgressIndicator()
            } else if (tournaments.isEmpty()) {
                Text(
                    if (isAdmin) "Bu kulupte henuz turnuva yok — yukaridan olustur."
                    else "Bu kulupte henuz turnuva yok.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn {
                    items(tournaments) { t ->
                        TournamentRow(
                            tournament = t,
                            onClick = { onTournamentSelected(t.id, t.status) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentRow(tournament: Tournament, onClick: () -> Unit) {
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
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tournament.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                StatusChip(status = tournament.status)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (label, color) = when (status) {
        "DRAFT" -> "Taslak" to MaterialTheme.colorScheme.tertiary
        "ACTIVE" -> "Devam ediyor" to MaterialTheme.colorScheme.primary
        "COMPLETED" -> "Tamamlandi" to MaterialTheme.colorScheme.secondary
        else -> status to Color.Gray
    }
    Surface(color = color.copy(alpha = 0.18f), shape = MaterialTheme.shapes.small) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}
