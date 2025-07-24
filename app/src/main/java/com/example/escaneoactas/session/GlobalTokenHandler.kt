package com.example.escaneoactas.session

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.escaneoactas.LoginActivity

/**
 * Manejador global para eventos relacionados con el token
 */
object GlobalTokenHandler {
    
    private var isTokenExpiredHandling = false
    
    /**
     * Notifica que el token ha expirado y redirige al usuario al login
     */
    fun notifyTokenExpired() {
        // Evitar múltiples notificaciones simultáneas
        if (isTokenExpiredHandling) return
        isTokenExpiredHandling = true
        
        Handler(Looper.getMainLooper()).post {
            // Obtener el contexto de la aplicación (si está disponible)
            val appContext = AuthManager.appContext
            if (appContext != null) {
                // Mostrar mensaje al usuario
                Toast.makeText(
                    appContext, 
                    "Su sesión ha expirado. Por favor inicie sesión nuevamente.", 
                    Toast.LENGTH_LONG
                ).show()
                
                // Cerrar la sesión actual
                AuthManager.logout(appContext)
                
                // Iniciar la actividad de login con bandera NEW_TASK
                val intent = Intent(appContext, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                appContext.startActivity(intent)
            }
            
            // Restablecer la bandera después de un tiempo
            Handler(Looper.getMainLooper()).postDelayed({
                isTokenExpiredHandling = false
            }, 3000) // 3 segundos para evitar múltiples redirecciones
        }
    }
} 