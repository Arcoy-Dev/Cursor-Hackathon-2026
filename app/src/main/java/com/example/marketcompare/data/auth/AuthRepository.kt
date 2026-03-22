package com.example.marketcompare.data.auth

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID

private val Context.authDataStore by preferencesDataStore(name = "auth_state")

data class UserSession(
    val username: String,
    val role: String,
    val token: String
)

class AuthRepository(private val context: Context) {
    private val usernameKey = stringPreferencesKey("username")
    private val roleKey = stringPreferencesKey("role")
    private val tokenKey = stringPreferencesKey("token")

    fun observeSession(): Flow<UserSession?> {
        return context.authDataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .map(::toSessionOrNull)
    }

    suspend fun signIn(username: String, password: String): Boolean {
        val normalizedUsername = username.trim().lowercase()
        val normalizedPassword = password.trim()
        val role = when {
            normalizedUsername == "admin" && normalizedPassword == "passwort" -> "admin"
            normalizedUsername == "user" && normalizedPassword == "passwort" -> "user"
            else -> null
        }
        if (role == null) {
            return false
        }

        val token = "token_${UUID.randomUUID()}"
        context.authDataStore.edit { prefs ->
            prefs[usernameKey] = normalizedUsername
            prefs[roleKey] = role
            prefs[tokenKey] = token
        }
        return true
    }

    suspend fun signOut() {
        context.authDataStore.edit { it.clear() }
    }

    private fun toSessionOrNull(preferences: Preferences): UserSession? {
        val username = preferences[usernameKey] ?: return null
        val role = preferences[roleKey] ?: "user"
        val token = preferences[tokenKey] ?: return null
        if (role != "admin" && role != "user") return null
        return UserSession(username = username, role = role, token = token)
    }
}
