package com.example.escaneoactas

import android.app.Application
import com.example.escaneoactas.session.AuthManager

class EscaneoActasApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Inicializar el AuthManager una vez para toda la aplicación
        AuthManager.init(applicationContext)
    }
} 