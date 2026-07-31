package com.tenisturnuva.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.tenisturnuva.app.data.model.Match
import com.tenisturnuva.app.data.model.Tournament
import com.tenisturnuva.app.data.repository.TournamentRepository
import com.tenisturnuva.app.data.session.SessionManager
import com.tenisturnuva.app.ui.components.AppScaffold
import kotlinx.coroutines.launch

private val repository = TournamentRepository()

@Composable
fun BracketScreen(tournamentId: String, onBack: () -> Unit, onLogout: () -> Unit) {
    var tournament by remember { mutableStateOf<Tournament?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var matchForResult by remember { mutableStateOf<Match?>(null) }
    val scope = rememberCoroutineScope()
    val isAdmin = SessionManager.isAdmin

    suspend fun refresh() {
        try {
            tournament = repository.getTournament(tournamentId)
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = "Bracket yuklenemedi: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(tournamentId) { refresh() }

    // Not: m.player1Id / player2Id backend'de Participant.id degerlerini
    // tutar (Player.id degil) — bu yuzden isim goturmek icin katilimci
    // listesinde participant.id uzerinden eslestiriyoruz.
    fun playerLabel(participantId: String?): String {
        if (participantId == null) return "BYE"
        val participant = tournament?.participants?.find { it.id == participantId }
        return participant?.player?.name ?: participantId
    }

    fun recordWinner(match: Match, winnerId: String?) {
        if (winnerId == null) return
        scope.launch {
            try {
                repository.submitResult(tournamentId, match.id, winnerId, null)
                matchForResult = null
                refresh()
            } catch (e: Exception) {
                errorMessage = "Sonuc kaydedilemedi: ${e.message}"
                matchForResult = null
            }
        }
    }

    AppScaffold(title = tournament?.name ?: "Bracket", onBack = onBack, onLogout = onLogout) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                val matches = tournament?.matches ?: emptyList()
                if (matches.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(48.dp))
                        Icon(
                            Icons.Filled.HourglassEmpty,
                            contentDescription = null,
                            modifier = Modifier.height(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Bracket henuz olusturulmadi",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Yonetici katilimcilari ekleyip bracket'i olusturunca burada gorunecek.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val matchesByRound = matches.groupBy { it.round }.toSortedMap()
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        matchesByRound.forEach { (round, roundMatches) ->
                            item {
                                Text(
                                    "Tur $round",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                )
                            }
                            items(roundMatches.sortedBy { it.position }) { m ->
                                MatchCard(
                                    match = m,
                                    isAdmin = isAdmin,
                                    playerLabel = ::playerLabel,
                                    onEnterResult = { matchForResult = m }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    val currentMatch = matchForResult
    if (currentMatch != null) {
        AlertDialog(
            onDismissRequest = { matchForResult = null },
            title = { Text("Kazanani sec") },
            text = {
                Text("${playerLabel(currentMatch.player1Id)}  vs  ${playerLabel(currentMatch.player2Id)}")
            },
            confirmButton = {
                Row {
                    TextButton(onClick = { recordWinner(currentMatch, currentMatch.player1Id) }) {
                        Text(playerLabel(currentMatch.player1Id))
                    }
                    TextButton(onClick = { recordWinner(currentMatch, currentMatch.player2Id) }) {
                        Text(playerLabel(currentMatch.player2Id))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { matchForResult = null }) { Text("Iptal") }
            }
        )
    }
}

@Composable
private fun MatchCard(
    match: Match,
    isAdmin: Boolean,
    playerLabel: (String?) -> String,
    onEnterResult: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "${playerLabel(match.player1Id)}  vs  ${playerLabel(match.player2Id)}",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(6.dp))
            when {
                match.status == "COMPLETED" -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Kazanan: ${playerLabel(match.winnerId)}",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                match.player1Id != null && match.player2Id != null -> {
                    if (isAdmin) {
                        TextButton(onClick = onEnterResult) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.height(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sonucu Gir")
                        }
                    } else {
                        Text(
                            "Sonuc bekleniyor",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.HourglassEmpty,
                        contentDescription = null,
                        modifier = Modifier.height(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Rakip bekleniyor",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
