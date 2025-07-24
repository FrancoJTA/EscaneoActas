package com.example.escaneoactas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.escaneoactas.data.AuthRepository
import com.example.escaneoactas.data.MesaDto
import com.example.escaneoactas.data.MesaRepository
import com.example.escaneoactas.session.AuthManager
import com.example.escaneoactas.ui.MesaAdapter
import kotlinx.coroutines.launch

class MesasActivity : AppCompatActivity() {

    private val mesaRepo by lazy { MesaRepository() }
    private val authRepo by lazy { AuthRepository(this) }

    // Referencias a las vistas
    private lateinit var rvMesas: RecyclerView
    private lateinit var tvTotalMesas: TextView
    private lateinit var pbLoading: ProgressBar
    private lateinit var llEmptyState: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mesas)

        initViews()
        setupRecyclerView()
        checkAuthAndLoadMesas()
    }

    private fun initViews() {
        rvMesas = findViewById(R.id.rvMesas)
        tvTotalMesas = findViewById(R.id.tvTotalMesas)
        pbLoading = findViewById(R.id.pbLoading)
        llEmptyState = findViewById(R.id.llEmptyState)
        
        // Configurar botón de cerrar sesión
        findViewById<View>(R.id.btnLogout).setOnClickListener {
            // Cerrar sesión
            AuthManager.logout(this)
            
            // Redirigir al login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setupRecyclerView() {
        rvMesas.layoutManager = LinearLayoutManager(this)
    }

    private fun checkAuthAndLoadMesas() {
        val userId = AuthManager.userId
        if (userId == null) {
            // Sesión perdida, redirigir al login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        loadMesas()
    }

    private fun loadMesas() {
        showLoadingState()

        lifecycleScope.launch {
            try {
                val result = mesaRepo.fetchMesas()
                
                result.onSuccess { mesas ->
                    showDataState(mesas)
                }.onFailure { error ->
                    showErrorState(error)
                }
                
            } catch (e: Exception) {
                showErrorState(e)
            }
        }
    }

    private fun showLoadingState() {
        pbLoading.isVisible = true
        rvMesas.isVisible = false
        llEmptyState.isVisible = false
        tvTotalMesas.text = "Cargando mesas..."
    }

    private fun showDataState(mesas: List<MesaDto>) {
        pbLoading.isVisible = false
        
        if (mesas.isEmpty()) {
            showEmptyState()
        } else {
            rvMesas.isVisible = true
            llEmptyState.isVisible = false
            
            // Configurar adapter
            rvMesas.adapter = MesaAdapter(mesas) { mesa ->
                // Click en mesa individual (opcional, ya manejado en el adapter)
            }
            
            // Actualizar contador
            updateMesasCounter(mesas)
        }
    }

    private fun showEmptyState() {
        rvMesas.isVisible = false
        llEmptyState.isVisible = true
        tvTotalMesas.text = "No hay mesas asignadas"
    }

    private fun showErrorState(error: Throwable) {
        pbLoading.isVisible = false
        rvMesas.isVisible = false
        llEmptyState.isVisible = false
        
        val errorMessage = error.localizedMessage ?: "Error desconocido"
        tvTotalMesas.text = "Error al cargar"
        
        Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_LONG).show()
            }

    private fun updateMesasCounter(mesas: List<MesaDto>) {
        val total = mesas.size
        val verificadas = mesas.count { it.verificado }
        val observadas = mesas.count { it.observado }
        
        val counterText = buildString {
            append("Total: $total mesas")
            if (verificadas > 0 || observadas > 0) {
                append(" • ")
                if (verificadas > 0) append("✓$verificadas ")
                if (observadas > 0) append("⚠$observadas")
            }
        }
        
        tvTotalMesas.text = counterText
    }
}
