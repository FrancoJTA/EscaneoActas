// session/AuthManager.kt
package com.example.escaneoactas.session

import android.content.Context
import androidx.core.content.edit
import com.example.escaneoactas.data.UserDto
import com.google.gson.Gson

object AuthManager {

    private const val PREFS = "auth"
    private const val KEY_TOKEN = "jwt"
    private const val KEY_USER = "user_data"

    private var tokenCache: String? = null
    private var userCache: UserDto? = null
    
    // Para acceso global desde el interceptor
    var appContext: Context? = null
        private set

    private val gson = Gson()

    /** Se llama una vez en Application o donde quieras  */
    fun init(ctx: Context) {
        // Guardar el contexto de aplicación
        appContext = ctx.applicationContext
        
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        tokenCache = p.getString(KEY_TOKEN, null)
        
        // Recuperar datos del usuario desde SharedPreferences
        val userJson = p.getString(KEY_USER, null)
        userCache = userJson?.let { 
            try {
                gson.fromJson(it, UserDto::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    /* ---------- getters rápidos ---------- */
    val token get() = tokenCache
    val user get() = userCache
    val userId get() = userCache?.id
    val isLoggedIn get() = tokenCache != null && userCache != null

    /* ---------- setters ---------- */
    fun saveSession(ctx: Context, user: UserDto, jwt: String) {
        tokenCache = jwt
        userCache = user
        
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(KEY_TOKEN, jwt)
            putString(KEY_USER, gson.toJson(user))
        }
    }

    fun logout(ctx: Context) {
        tokenCache = null
        userCache = null
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { clear() }
    }

    /* ---------- helpers para scopes ---------- */
    fun hasScope(nivel: String, scopeId: Int? = null): Boolean {
        return userCache?.scopes?.any { scope ->
            scope.nivel == nivel && (scopeId == null || scope.scopeId == scopeId)
        } ?: false
    }

    fun getScopesByNivel(nivel: String) = userCache?.scopes?.filter { it.nivel == nivel } ?: emptyList()
}
