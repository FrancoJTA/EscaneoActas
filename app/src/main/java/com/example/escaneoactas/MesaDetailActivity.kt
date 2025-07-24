package com.example.escaneoactas

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.escaneoactas.data.MesaDto
import com.example.escaneoactas.data.MesaRepository
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class MesaDetailActivity : AppCompatActivity() {

    private val mesaRepo by lazy { MesaRepository() }

    // Referencias a las vistas
    private lateinit var progress: ProgressBar
    private lateinit var scroll: ScrollView
    private lateinit var tvMesaTitle: TextView
    private lateinit var tvRecinto: TextView
    private lateinit var tvVerificado: TextView
    private lateinit var tvObservado: TextView
    private lateinit var tvTotalVotos: TextView
    private lateinit var tvVotosValidos: TextView
    private lateinit var tvVotosInvalidos: TextView
    private lateinit var tvMesaId: TextView
    private lateinit var tvCapacidad: TextView
    private lateinit var llVotosPresidenciales: LinearLayout
    private lateinit var llVotosDiputados: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mesa_detail)

        initViews()
        loadMesaData()
    }

    private fun initViews() {
        progress = findViewById(R.id.progress)
        scroll = findViewById(R.id.scroll)
        tvMesaTitle = findViewById(R.id.tvMesaTitle)
        tvRecinto = findViewById(R.id.tvRecinto)
        tvVerificado = findViewById(R.id.tvVerificado)
        tvObservado = findViewById(R.id.tvObservado)
        tvTotalVotos = findViewById(R.id.tvTotalVotos)
        tvVotosValidos = findViewById(R.id.tvVotosValidos)
        tvVotosInvalidos = findViewById(R.id.tvVotosInvalidos)
        tvMesaId = findViewById(R.id.tvMesaId)
        tvCapacidad = findViewById(R.id.tvCapacidad)
        llVotosPresidenciales = findViewById(R.id.llVotosPresidenciales)
        llVotosDiputados = findViewById(R.id.llVotosDiputados)
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
                
                result.onSuccess { mesa ->
                    displayMesaDetails(mesa)
                }.onFailure { error ->
                    showErrorAndFinish("Error al cargar la mesa: ${error.localizedMessage}")
                }
                
            } catch (e: Exception) {
                showErrorAndFinish("Error al cargar la mesa: ${e.localizedMessage}")
            }
        }
    }

    private fun displayMesaDetails(mesa: MesaDto) {
        // Mostrar contenido y ocultar loading
        progress.isVisible = false
        scroll.isVisible = true

        // Header información básica
        tvMesaTitle.text = "Mesa ${mesa.nrMesa ?: "Sin número"}"
        tvRecinto.text = mesa.recintoNombre.ifEmpty { "Recinto no especificado" }
        
        // Indicadores de estado
        tvVerificado.isVisible = mesa.verificado
        tvObservado.isVisible = mesa.observado

        // Calcular estadísticas
        val votosValidos = calcularVotosValidos(mesa)
        val votosInvalidos = mesa.nulo + mesa.blanco
        val totalVotos = votosValidos + votosInvalidos

        // Mostrar estadísticas formateadas
        val formatter = NumberFormat.getNumberInstance(Locale("es", "ES"))
        tvTotalVotos.text = formatter.format(totalVotos)
        tvVotosValidos.text = formatter.format(votosValidos)
        tvVotosInvalidos.text = formatter.format(votosInvalidos)

        // Información adicional
        tvMesaId.text = "ID: ${mesa.id}"
        tvCapacidad.text = formatter.format(mesa.cantMaxima)

        // Generar secciones de votos
        generateVotosPresidenciales(mesa, votosValidos)
        generateVotosDiputados(mesa)
    }

    private fun generateVotosPresidenciales(mesa: MesaDto, totalValidos: Int) {
        val partidos = listOf(
            "Alianza Popular" to mesa.alianzaPopular,
            "ADN" to mesa.adn,
            "APB" to mesa.apb,
            "NGP" to mesa.ngp,
            "LIBRE" to mesa.libre,
            "Fuerza del Pueblo" to mesa.laFuerzaDelPueblo,
            "MAS" to mesa.mas,
            "MORENA" to mesa.morena,
            "UNIDAD" to mesa.unidad,
            "PDC" to mesa.pdc,
            "Nulo" to mesa.nulo,
            "Blanco" to mesa.blanco
        )

        // Limpiar contenedor
        llVotosPresidenciales.removeAllViews()

        // Agregar cada partido
        partidos.forEach { (nombre, votos) ->
            if (votos > 0 || nombre == "Nulo" || nombre == "Blanco") {
                val partidoView = createPartidoView(nombre, votos, totalValidos + mesa.nulo + mesa.blanco)
                llVotosPresidenciales.addView(partidoView)
            }
        }
    }

    private fun generateVotosDiputados(mesa: MesaDto) {
        val totalDiputados = mesa.masDiputado + mesa.unidadDiputado + mesa.morenaDiputado + 
                           mesa.pdcDiputado + mesa.libreDiputado + mesa.laFuerzaDelPuebloDiputado +
                           mesa.alianzaPopularDiputado + mesa.adnDiputado + mesa.apbDiputado + 
                           mesa.ngpDiputado + mesa.nuloDiputado + mesa.blancoDiputado

        val partidos = listOf(
            "Alianza Popular" to mesa.alianzaPopularDiputado,
            "ADN" to mesa.adnDiputado,
            "APB" to mesa.apbDiputado,
            "NGP" to mesa.ngpDiputado,
            "LIBRE" to mesa.libreDiputado,
            "Fuerza del Pueblo" to mesa.laFuerzaDelPuebloDiputado,
            "MAS" to mesa.masDiputado,
            "MORENA" to mesa.morenaDiputado,
            "UNIDAD" to mesa.unidadDiputado,
            "PDC" to mesa.pdcDiputado,
            "Nulo" to mesa.nuloDiputado,
            "Blanco" to mesa.blancoDiputado
        )

        // Limpiar contenedor
        llVotosDiputados.removeAllViews()

        // Agregar cada partido
        partidos.forEach { (nombre, votos) ->
            if (votos > 0 || nombre == "Nulo" || nombre == "Blanco") {
                val partidoView = createPartidoView(nombre, votos, totalDiputados)
                llVotosDiputados.addView(partidoView)
            }
        }
    }

    private fun createPartidoView(nombrePartido: String, votos: Int, totalVotos: Int): LinearLayout {
        val partidoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24
            }
            setPadding(0, 8, 0, 8)
        }

        // Contenedor de texto (izquierda)
        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        // Nombre del partido
        val tvNombre = TextView(this).apply {
            text = nombrePartido
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@MesaDetailActivity, R.color.colorOnSurface))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        // Información de votos
        val porcentaje = if (totalVotos > 0) (votos * 100.0 / totalVotos) else 0.0
        val formatter = NumberFormat.getNumberInstance(Locale("es", "ES"))
        val tvInfo = TextView(this).apply {
            text = "${formatter.format(votos)} votos (${String.format("%.1f", porcentaje)}%)"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@MesaDetailActivity, R.color.light_gray))
        }

        textContainer.addView(tvNombre)
        textContainer.addView(tvInfo)

        // Barra de progreso visual (derecha)
        val progressContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(100, LinearLayout.LayoutParams.WRAP_CONTENT)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val progressBar = android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                24
            )
            max = 100
            progress = porcentaje.toInt()

            // Color según el tipo de voto
            val color = when (nombrePartido) {
                "Nulo", "Blanco" -> R.color.warning
                else -> R.color.colorPrimary
            }
            progressDrawable?.setColorFilter(
                ContextCompat.getColor(this@MesaDetailActivity, color),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        }

        progressContainer.addView(progressBar)

        partidoLayout.addView(textContainer)
        partidoLayout.addView(progressContainer)

        return partidoLayout
    }

    private fun calcularVotosValidos(mesa: MesaDto): Int {
        return mesa.alianzaPopular + mesa.adn + mesa.apb + mesa.ngp + mesa.libre + 
               mesa.laFuerzaDelPueblo + mesa.mas + mesa.morena + mesa.unidad + mesa.pdc
    }

    private fun showErrorAndFinish(message: String) {
        Toast.makeText(this, "Error: $message", Toast.LENGTH_LONG).show()
        finish()
    }
}
