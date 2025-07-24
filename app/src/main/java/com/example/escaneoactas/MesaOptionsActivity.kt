package com.example.escaneoactas

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.escaneoactas.data.MesaDto
import com.example.escaneoactas.data.MesaRepository
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class MesaOptionsActivity : AppCompatActivity() {

    private var mesa: MesaDto? = null
    private val mesaRepo by lazy { MesaRepository() }

    // Referencias a las vistas del header
    private lateinit var tvMesaTitle: TextView
    private lateinit var tvRecinto: TextView
    private lateinit var tvTotalVotos: TextView
    private lateinit var tvVerificado: TextView
    private lateinit var tvObservado: TextView

    // Referencias a las cards de opciones
    private lateinit var cardDetails: MaterialCardView
    private lateinit var cardScan: MaterialCardView
    private lateinit var cardManual: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mesa_options)

        initViews()
        loadMesaData()
        setupClickListeners()
    }

    private fun initViews() {
        // Header views
        tvMesaTitle = findViewById(R.id.tvMesaTitle)
        tvRecinto = findViewById(R.id.tvRecinto)
        tvTotalVotos = findViewById(R.id.tvTotalVotos)
        tvVerificado = findViewById(R.id.tvVerificado)
        tvObservado = findViewById(R.id.tvObservado)

        // Option cards
        cardDetails = findViewById(R.id.cardDetails)
        cardScan = findViewById(R.id.cardScan)
        cardManual = findViewById(R.id.cardManual)
    }

    private fun loadMesaData() {
        // Obtener el ID de la mesa desde el intent
        val mesaId = intent.getIntExtra("mesa_id", -1)
        if (mesaId == -1) {
            showErrorAndFinish("No se pudo obtener el ID de la mesa")
            return
        }

        // Cargar datos frescos desde la API
        lifecycleScope.launch {
            try {
                val result = mesaRepo.fetchMesa(mesaId)
                
                result.onSuccess { loadedMesa ->
                    mesa = loadedMesa
                    displayMesaInfo(loadedMesa)
                }.onFailure { error ->
                    showErrorAndFinish("Error al cargar la mesa: ${error.localizedMessage}")
                }
                
            } catch (e: Exception) {
                showErrorAndFinish("Error al cargar la mesa: ${e.localizedMessage}")
            }
        }
    }

    private fun displayMesaInfo(mesa: MesaDto) {
        // Información básica
        tvMesaTitle.text = "Mesa ${mesa.nrMesa ?: "Sin número"}"
        tvRecinto.text = mesa.recintoNombre.ifEmpty { "Recinto no especificado" }

        // Calcular y mostrar estadísticas
        val totalVotos = calcularTotalVotos(mesa)
        val formatter = NumberFormat.getNumberInstance(Locale("es", "ES"))
        tvTotalVotos.text = formatter.format(totalVotos)

        // Indicadores de estado
        tvVerificado.isVisible = mesa.verificado
        tvObservado.isVisible = mesa.observado
    }

    private fun setupClickListeners() {
        val mesaId = intent.getIntExtra("mesa_id", -1)
        if (mesaId == -1) return

        // Card Ver Detalles
        cardDetails.setOnClickListener {
            startActivity(Intent(this, MesaDetailActivity::class.java)
                .putExtra("mesa_id", mesaId))
        }

        // Card Escanear
        cardScan.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java)
                .putExtra("mesa_id", mesaId))
        }

        // Card Rellenar Manual
        cardManual.setOnClickListener {
            startActivity(Intent(this, DocsActivity::class.java)
                .putExtra("mesa_id", mesaId))
        }
    }

    private fun calcularTotalVotos(mesa: MesaDto): Int {
        val votosValidos = mesa.alianzaPopular + 
                          mesa.adn + 
                          mesa.apb + 
                          mesa.ngp + 
                          mesa.libre + 
                          mesa.laFuerzaDelPueblo + 
                          mesa.mas + 
                          mesa.morena + 
                          mesa.unidad + 
                          mesa.pdc
        
        val votosInvalidos = mesa.nulo + mesa.blanco
        
        return votosValidos + votosInvalidos
    }

    private fun showErrorAndFinish(message: String) {
        Toast.makeText(this, "Error: $message", Toast.LENGTH_LONG).show()
        finish()
    }
}
