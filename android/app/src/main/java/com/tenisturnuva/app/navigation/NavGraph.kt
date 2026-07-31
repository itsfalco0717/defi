package com.tenisturnuva.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tenisturnuva.app.data.session.SessionManager
import com.tenisturnuva.app.ui.screens.BracketScreen
import com.tenisturnuva.app.ui.screens.ClubListScreen
import com.tenisturnuva.app.ui.screens.LoginScreen
import com.tenisturnuva.app.ui.screens.RegisterScreen
import com.tenisturnuva.app.ui.screens.TournamentListScreen
import com.tenisturnuva.app.ui.screens.TournamentSetupScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val onLogout: () -> Unit = {
        scope.launch {
            SessionManager.clear(context)
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(
                onReady = { loggedIn ->
                    val destination = if (loggedIn) "clubs" else "login"
                    navController.navigate(destination) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate("clubs") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onGoToRegister = { navController.navigate("register") }
            )
        }

        composable("register") {
            RegisterScreen(
                onRegistered = {
                    navController.navigate("clubs") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onGoToLogin = { navController.popBackStack() }
            )
        }

        composable("clubs") {
            ClubListScreen(
                onClubSelected = { clubId -> navController.navigate("tournaments/$clubId") },
                onLogout = onLogout
            )
        }

        composable("tournaments/{clubId}") { backStackEntry ->
            val clubId = backStackEntry.arguments?.getString("clubId") ?: return@composable
            TournamentListScreen(
                clubId = clubId,
                onTournamentSelected = { tournamentId, status ->
                    // Sadece yonetici, taslak (DRAFT) bir turnuvanin kurulum
                    // ekranina (katilimci ekleme) girebilir. Oyuncu icin DRAFT
                    // bir turnuva da dogrudan bracket ekranina gider — orada
                    // "henuz olusturulmadi" bos durumu gorunur.
                    val destination = if (status == "DRAFT" && SessionManager.isAdmin) {
                        "setup/$tournamentId"
                    } else {
                        "bracket/$tournamentId"
                    }
                    navController.navigate(destination)
                },
                onBack = { navController.popBackStack() },
                onLogout = onLogout
            )
        }

        composable("setup/{tournamentId}") { backStackEntry ->
            val tournamentId = backStackEntry.arguments?.getString("tournamentId") ?: return@composable
            TournamentSetupScreen(
                tournamentId = tournamentId,
                onBracketGenerated = {
                    navController.navigate("bracket/$tournamentId") {
                        popUpTo("setup/$tournamentId") { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
                onLogout = onLogout
            )
        }

        composable("bracket/{tournamentId}") { backStackEntry ->
            val tournamentId = backStackEntry.arguments?.getString("tournamentId") ?: return@composable
            BracketScreen(
                tournamentId = tournamentId,
                onBack = { navController.popBackStack() },
                onLogout = onLogout
            )
        }
    }
}

@Composable
private fun SplashScreen(onReady: (loggedIn: Boolean) -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        SessionManager.restore(context)
        onReady(SessionManager.isLoggedIn)
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
