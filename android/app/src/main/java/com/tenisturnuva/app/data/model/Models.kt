package com.tenisturnuva.app.data.model

/** role: "ADMIN" | "PLAYER" */
data class UserDto(
    val id: String,
    val email: String,
    val name: String,
    val role: String
)

data class AuthResponse(
    val token: String,
    val user: UserDto
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val role: String
)

data class LoginRequest(val email: String, val password: String)

data class Club(
    val id: String,
    val name: String
)

data class Player(
    val id: String,
    val clubId: String,
    val name: String,
    val phone: String? = null,
    val rating: Int = 1000
)

data class Participant(
    val id: String,
    val tournamentId: String,
    val playerId: String,
    val seed: Int? = null,
    val player: Player? = null
)

/** status: "PENDING" | "SCHEDULED" | "COMPLETED" */
data class Match(
    val id: String,
    val tournamentId: String,
    val round: Int,
    val position: Int,
    val player1Id: String?,
    val player2Id: String?,
    val winnerId: String?,
    val score: String?,
    val status: String,
    val nextMatchId: String?
)

/** status: "DRAFT" | "ACTIVE" | "COMPLETED" */
data class Tournament(
    val id: String,
    val clubId: String,
    val name: String,
    val format: String,
    val seedingType: String,
    val drawSize: Int,
    val status: String,
    val participants: List<Participant> = emptyList(),
    val matches: List<Match> = emptyList()
)

// --- İstek gövdeleri (request bodies) ---

data class CreateClubRequest(val name: String)

data class CreatePlayerRequest(
    val name: String,
    val phone: String? = null,
    val rating: Int? = null
)

data class CreateTournamentRequest(
    val clubId: String,
    val name: String,
    val seedingType: String = "random"
)

data class AddParticipantRequest(val playerId: String, val seed: Int? = null)

data class MatchResultRequest(val winnerId: String, val score: String? = null)
