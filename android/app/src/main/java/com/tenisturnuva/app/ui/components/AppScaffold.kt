package com.tenisturnuva.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tenisturnuva.app.data.session.SessionManager
import com.tenisturnuva.app.ui.theme.AdminBadge
import com.tenisturnuva.app.ui.theme.PlayerBadge

/**
 * Uygulamadaki her ekranin ortak dis cercevesi: baslik + (varsa) geri butonu
 * + giris yapan kullanicinin rol rozeti + cikis yap butonu.
 *
 * Rol rozetinin her zaman gorunur olmasi, "admin mi kullanici mi belli
 * degil" sorununu dogrudan cozuyor — hangi hesapla gezindigin her ekranda
 * ustte yaziyor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    onLogout: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Geri")
                        }
                    }
                },
                actions = {
                    val user = SessionManager.currentUser
                    if (user != null) {
                        RoleBadge(role = user.role)
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Filled.ExitToApp, contentDescription = "Cikis yap")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding -> content(padding) }
}

@Composable
private fun RoleBadge(role: String) {
    val label = if (role == "ADMIN") "Yonetici" else "Oyuncu"
    val color = if (role == "ADMIN") AdminBadge else PlayerBadge
    Surface(
        color = color,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
