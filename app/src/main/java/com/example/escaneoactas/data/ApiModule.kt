// data/ApiModule.kt
package com.example.escaneoactas.data

import android.content.Context
import com.example.escaneoactas.session.AuthManager
import com.example.escaneoactas.session.GlobalTokenHandler
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiModule {

    // URL del servidor
    private const val BASE_URL = "http://192.168.0.102:3000/"  // Actualiza a tu IP correcta

    // EventBus para notificar cuando el token expire
    object TokenExpiredEvent

    // Interceptor para agregar automáticamente el JWT
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        
        // Solo agregar token si no es el endpoint de login
        val isLoginRequest = originalRequest.url.encodedPath.contains("/auth/login")
        
        if (isLoginRequest) {
            chain.proceed(originalRequest)
        } else {
            val token = AuthManager.token
            if (token != null) {
                val authenticatedRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
                chain.proceed(authenticatedRequest)
            } else {
                chain.proceed(originalRequest)
            }
        }
    }

    // Interceptor para manejar respuestas 401 (token expirado)
    private val tokenExpirationInterceptor = Interceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)
        
        // Si la respuesta es 401 Unauthorized, el token probablemente expiró
        if (response.code == 401) {
            // Notificar a la aplicación que el token expiró
            GlobalTokenHandler.notifyTokenExpired()
        }
        
        response
    }

    val api: ApiService by lazy {
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)  // ← Interceptor de JWT
            .addInterceptor(tokenExpirationInterceptor)  // ← Interceptor para token expirado
            .addInterceptor(logger)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create()) // ← Gson converter
            .build()
            .create(ApiService::class.java)
    }
}

/* ---------- simple repository ---------- */
class AuthRepository(private val ctx: Context) {

    suspend fun login(codigo: String, ci: String): Result<UserDto> = runCatching {
        val response = ApiModule.api.login(LoginRequest(codigo, ci))
        
        // Guardar sesión completa usando AuthManager
        AuthManager.saveSession(ctx, response.user, response.token)
        
        response.user
    }

    fun getSavedToken(): String? = AuthManager.token
    
    fun getCurrentUser(): UserDto? = AuthManager.user
    
    fun isLoggedIn(): Boolean = AuthManager.isLoggedIn
}
