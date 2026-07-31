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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import com.tenisturnuva.app.data.model.Participant
import com.tenisturnuva.app.data.model.Tournament
import com.tenisturnuva.app.data.repository.TournamentRepository
import com.tenisturnuva.app.ui.components.AppScaffold
import kotlinx.coroutines.launch

private val repository = TournamentRepository()

@Composable
fun TournamentSetupScreen(
    tournamentId: String,
    onBracketGenerated: () -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    var tournament by remember { mutableStateOf<Tournament?>(null) }
    var newPlayerName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun refresh() {
        try {
            tournament = repository.getTournament(tournamentId)
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = "Turnuva yuklenemedi: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(tournamentId) { refresh() }

    AppScaffold(title = tournament?.name ?: "Turnuva Kurulumu", onBack = onBack, onLogout = onLogout) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Katilimci ekle, sonra bracket'i olustur",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(modifier = Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newPlayerName,
                    onValueChange = { newPlayerName = it },
                    label = { Text("Oyuncu adi") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val name = newPlayerName.trim()
                        val clubId = tournament?.clubId
                        if (name.isNotEmpty() && clubId != null) {
                            scope.launch {
                                try {
                                    val player = repository.createPlayer(clubId, name)
                                    repository.addParticipant(tournamentId, player.id)
                                    newPlayerName = ""
                                    refresh()
                                } catch (e: Exception) {
                                    errorMessage = "Oyuncu eklenemedi: ${e.message}"
                                }
                            }
                        }
                    },
                    modifier = Modifier.height(56.dp)
                ) { Text("Ekle") }
            }

            Spacer(modifier = Modifier.height(16.dp))
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            val participants = tournament?.participants ?: emptyList()
            Text("Katilimcilar (${participants.size})", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(participants) { p: Participant ->
                        ParticipantRow(p)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (participants.size < 2) {
                    Text(
                        "Bracket olusturmak icin en az 2 katilimci gerekir.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        isGenerating = true
                        scope.launch {
                            try {
                                repository.generateBracket(tournamentId)
                                onBracketGenerated()
                            } catch (e: Exception) {
                                errorMessage = "Bracket olusturulamadi: ${e.message}"
                            } finally {
                                isGenerating = false
                            }
                        }
                    },
                    enabled = participants.size >= 2 && !isGenerating,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(if (isGenerating) "Olusturuluyor..." else "Bracket'i Olustur (Tek Eleme)")
                }
            }
        }
    }
}

@Composable
private fun ParticipantRow(participant: Participant) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(participant.player?.name ?: participant.playerId, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
