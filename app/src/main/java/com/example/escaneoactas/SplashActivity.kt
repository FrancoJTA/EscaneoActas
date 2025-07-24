package com.example.escaneoactas

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.escaneoactas.session.AuthManager

class SplashActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Breve delay para mostrar la pantalla de splash
        Handler(Looper.getMainLooper()).postDelayed({
            checkSession()
        }, 1000) // 1 segundo de delay
    }
    
    private fun checkSession() {
        // Verificar si hay una sesión activa
        if (AuthManager.isLoggedIn) {
            // Hay una sesión activa, ir a la actividad principal
            startActivity(Intent(this, MesasActivity::class.java))
        } else {
            // No hay sesión activa, ir a login
            startActivity(Intent(this, LoginActivity::class.java))
        }
        
        finish() // Cerrar esta actividad
    }
} 