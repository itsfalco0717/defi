package com.tenisturnuva.app.data.repository

import com.tenisturnuva.app.data.model.AddParticipantRequest
import com.tenisturnuva.app.data.model.CreateClubRequest
import com.tenisturnuva.app.data.model.CreatePlayerRequest
import com.tenisturnuva.app.data.model.CreateTournamentRequest
import com.tenisturnuva.app.data.model.LoginRequest
import com.tenisturnuva.app.data.model.MatchResultRequest
import com.tenisturnuva.app.data.model.RegisterRequest
import com.tenisturnuva.app.data.network.RetrofitClient

class TournamentRepository {
    private val api = RetrofitClient.api

    suspend fun register(email: String, password: String, name: String, role: String) =
        api.register(RegisterRequest(email, password, name, role))

    suspend fun login(email: String, password: String) =
        api.login(LoginRequest(email, password))

    suspend fun getClubs() = api.getClubs()
    suspend fun createClub(name: String) = api.createClub(CreateClubRequest(name))

    suspend fun getPlayers(clubId: String) = api.getPlayers(clubId)
    suspend fun createPlayer(clubId: String, name: String) =
        api.createPlayer(clubId, CreatePlayerRequest(name))

    suspend fun getTournaments(clubId: String) = api.getTournaments(clubId)
    suspend fun createTournament(clubId: String, name: String, seedingType: String) =
        api.createTournament(CreateTournamentRequest(clubId, name, seedingType))

    suspend fun getTournament(id: String) = api.getTournament(id)

    suspend fun addParticipant(tournamentId: String, playerId: String) =
        api.addParticipant(tournamentId, AddParticipantRequest(playerId))

    suspend fun generateBracket(tournamentId: String) = api.generateBracket(tournamentId)

    suspend fun submitResult(tournamentId: String, matchId: String, winnerId: String, score: String?) =
        api.submitMatchResult(tournamentId, matchId, MatchResultRequest(winnerId, score))
}
