package com.tenisturnuva.app.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.tenisturnuva.app.data.model.AuthResponse
import com.tenisturnuva.app.data.model.UserDto
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "session")

/**
 * Oturum bilgisini (JWT + kullanici) hem kalici olarak (DataStore — uygulama
 * kapatilip acilsa da kaybolmaz) hem bellek-ici bir onbellekte tutar.
 *
 * Bellek-ici onbellek gerekli, cunku Retrofit/OkHttp'nin istek interceptor'i
 * senkron calisir (suspend fonksiyon cagiramaz) — token'i her istekte
 * DataStore'dan okumak yerine buradan aninda okur.
 */
object SessionManager {

    @Volatile
    var token: String? = null
        private set

    @Volatile
    var currentUser: UserDto? = null
        private set

    private val TOKEN_KEY = stringPreferencesKey("token")
    private val USER_JSON_KEY = stringPreferencesKey("user_json")
    private val gson = Gson()

    val isAdmin: Boolean get() = currentUser?.role == "ADMIN"
    val isLoggedIn: Boolean get() = token != null

    /** Uygulama acilirken bir kere cagrilir — kalici depodan bellege yukler. */
    suspend fun restore(context: Context) {
        val prefs = context.dataStore.data.first()
        token = prefs[TOKEN_KEY]
        currentUser = prefs[USER_JSON_KEY]?.let {
            runCatching { gson.fromJson(it, UserDto::class.java) }.getOrNull()
        }
    }

    suspend fun saveSession(context: Context, auth: AuthResponse) {
        token = auth.token
        currentUser = auth.user
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = auth.token
            prefs[USER_JSON_KEY] = gson.toJson(auth.user)
        }
    }

    suspend fun clear(context: Context) {
        token = null
        currentUser = null
        context.dataStore.edit { it.clear() }
    }
}
