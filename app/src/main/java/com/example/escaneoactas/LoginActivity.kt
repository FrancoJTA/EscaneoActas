package com.example.escaneoactas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.escaneoactas.data.AuthRepository
import androidx.lifecycle.lifecycleScope
import com.example.escaneoactas.session.AuthManager

class LoginActivity : AppCompatActivity() {

    private lateinit var etUser: EditText
    private lateinit var etPass: EditText
    private lateinit var btnLogin: Button
    private val repo by lazy { AuthRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etUser  = findViewById(R.id.etUser)
        etPass  = findViewById(R.id.etPass)
        btnLogin= findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val code = etUser.text.toString().trim()
            val ci   = etPass.text.toString().trim()

            if (code.isBlank() || ci.isBlank()) {
                Toast.makeText(this, "Completa ambos campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launchWhenStarted {
                btnLogin.isEnabled = false
                try {
                    val result = repo.login(code, ci)
                    result.onSuccess { user ->
                        Toast.makeText(this@LoginActivity,
                            "¡Bienvenido ${user.nombre}!", Toast.LENGTH_SHORT).show()

                        // ▶  ya no pasamos userId; lo leerá MesasActivity de AuthManager
                        startActivity(Intent(this@LoginActivity, MesasActivity::class.java))
                        finish()
                    }.onFailure { e ->
                        val msg = if (e is retrofit2.HttpException && e.code() == 401)
                            "Credenciales incorrectas"
                        else "Error de red: ${e.localizedMessage}"
                        Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                } finally { btnLogin.isEnabled = true }
            }
        }
    }
}
