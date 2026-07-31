package com.tenisturnuva.app.data.network

import com.tenisturnuva.app.data.model.AddParticipantRequest
import com.tenisturnuva.app.data.model.AuthResponse
import com.tenisturnuva.app.data.model.Club
import com.tenisturnuva.app.data.model.CreateClubRequest
import com.tenisturnuva.app.data.model.CreatePlayerRequest
import com.tenisturnuva.app.data.model.CreateTournamentRequest
import com.tenisturnuva.app.data.model.LoginRequest
import com.tenisturnuva.app.data.model.Match
import com.tenisturnuva.app.data.model.MatchResultRequest
import com.tenisturnuva.app.data.model.Participant
import com.tenisturnuva.app.data.model.Player
import com.tenisturnuva.app.data.model.RegisterRequest
import com.tenisturnuva.app.data.model.Tournament
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** apps/api/src/routes klasorundeki endpoint tanimlarinin Kotlin karsiligi. */
interface ApiService {

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("clubs")
    suspend fun createClub(@Body body: CreateClubRequest): Club

    @GET("clubs")
    suspend fun getClubs(): List<Club>

    @POST("clubs/{clubId}/players")
    suspend fun createPlayer(@Path("clubId") clubId: String, @Body body: CreatePlayerRequest): Player

    @GET("clubs/{clubId}/players")
    suspend fun getPlayers(@Path("clubId") clubId: String): List<Player>

    @POST("tournaments")
    suspend fun createTournament(@Body body: CreateTournamentRequest): Tournament

    @GET("tournaments/club/{clubId}")
    suspend fun getTournaments(@Path("clubId") clubId: String): List<Tournament>

    @GET("tournaments/{id}")
    suspend fun getTournament(@Path("id") id: String): Tournament

    @POST("tournaments/{id}/participants")
    suspend fun addParticipant(
        @Path("id") tournamentId: String,
        @Body body: AddParticipantRequest
    ): Participant

    @POST("tournaments/{id}/generate-bracket")
    suspend fun generateBracket(@Path("id") tournamentId: String): Tournament

    @POST("tournaments/{tournamentId}/matches/{matchId}/result")
    suspend fun submitMatchResult(
        @Path("tournamentId") tournamentId: String,
        @Path("matchId") matchId: String,
        @Body body: MatchResultRequest
    ): Map<String, Match?>
}
