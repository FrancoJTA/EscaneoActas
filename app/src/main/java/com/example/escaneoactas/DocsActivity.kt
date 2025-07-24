package com.example.escaneoactas

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.escaneoactas.data.ApiModule
import com.example.escaneoactas.data.MesaDto
import com.example.escaneoactas.data.MesaRepository
import com.example.escaneoactas.data.UpdateVotosRequest
import com.google.gson.Gson
import kotlinx.coroutines.launch

class DocsActivity : AppCompatActivity() {

    private var mesa: MesaDto? = null
    private val mesaRepo by lazy { MesaRepository() }

    // Header
    private lateinit var tvMesaTitle: TextView
    private lateinit var tvRecinto: TextView

    // Votos Presidenciales
    private lateinit var etMas: EditText
    private lateinit var etUnidad: EditText
    private lateinit var etMorena: EditText
    private lateinit var etPdc: EditText
    private lateinit var etLibre: EditText
    private lateinit var etLaFuerzaDelPueblo: EditText
    private lateinit var etAlianzaPopular: EditText
    private lateinit var etAdn: EditText
    private lateinit var etApb: EditText
    private lateinit var etNgp: EditText
    private lateinit var etNulo: EditText
    private lateinit var etBlanco: EditText

    // Votos Diputados
    private lateinit var etMasDiputado: EditText
    private lateinit var etUnidadDiputado: EditText
    private lateinit var etMorenaDiputado: EditText
    private lateinit var etPdcDiputado: EditText
    private lateinit var etLibreDiputado: EditText
    private lateinit var etLaFuerzaDelPuebloDiputado: EditText
    private lateinit var etAlianzaPopularDiputado: EditText
    private lateinit var etAdnDiputado: EditText
    private lateinit var etApbDiputado: EditText
    private lateinit var etNgpDiputado: EditText
    private lateinit var etNuloDiputado: EditText
    private lateinit var etBlancoDiputado: EditText

    // Estados
    private lateinit var cbVerificado: CheckBox
    private lateinit var cbObservado: CheckBox

    // Botones y controles
    private lateinit var btnCancelar: Button
    private lateinit var btnGuardar: Button
    private lateinit var progressGuardar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_docs)

        initViews()
        loadMesaData()
        setupClickListeners()
    }

    private fun initViews() {
        // Header
        tvMesaTitle = findViewById(R.id.tvMesaTitle)
        tvRecinto = findViewById(R.id.tvRecinto)

        // Votos Presidenciales
        etMas = findViewById(R.id.etMas)
        etUnidad = findViewById(R.id.etUnidad)
        etMorena = findViewById(R.id.etMorena)
        etPdc = findViewById(R.id.etPdc)
        etLibre = findViewById(R.id.etLibre)
        etLaFuerzaDelPueblo = findViewById(R.id.etLaFuerzaDelPueblo)
        etAlianzaPopular = findViewById(R.id.etAlianzaPopular)
        etAdn = findViewById(R.id.etAdn)
        etApb = findViewById(R.id.etApb)
        etNgp = findViewById(R.id.etNgp)
        etNulo = findViewById(R.id.etNulo)
        etBlanco = findViewById(R.id.etBlanco)

        // Votos Diputados - Todos los campos
        etMasDiputado = findViewById(R.id.etMasDiputado)
        etUnidadDiputado = findViewById(R.id.etUnidadDiputado)
        etMorenaDiputado = findViewById(R.id.etMorenaDiputado)
        etPdcDiputado = findViewById(R.id.etPdcDiputado)
        etLibreDiputado = findViewById(R.id.etLibreDiputado)
        etLaFuerzaDelPuebloDiputado = findViewById(R.id.etLaFuerzaDelPuebloDiputado)
        etAlianzaPopularDiputado = findViewById(R.id.etAlianzaPopularDiputado)
        etAdnDiputado = findViewById(R.id.etAdnDiputado)
        etApbDiputado = findViewById(R.id.etApbDiputado)
        etNgpDiputado = findViewById(R.id.etNgpDiputado)
        etNuloDiputado = findViewById(R.id.etNuloDiputado)
        etBlancoDiputado = findViewById(R.id.etBlancoDiputado)

        // Estados
        cbVerificado = findViewById(R.id.cbVerificado)
        cbObservado = findViewById(R.id.cbObservado)

        // Botones
        btnCancelar = findViewById(R.id.btnCancelar)
        btnGuardar = findViewById(R.id.btnGuardar)
        progressGuardar = findViewById(R.id.progressGuardar)
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
                    applySuggestedValues()
                }.onFailure { error ->
                    showErrorAndFinish("Error al cargar la mesa: ${error.localizedMessage}")
                }
                
            } catch (e: Exception) {
                showErrorAndFinish("Error al cargar la mesa: ${e.localizedMessage}")
            }
        }
    }

    private fun displayMesaInfo(mesa: MesaDto) {
        // Header
        tvMesaTitle.text = "Mesa ${mesa.nrMesa ?: "Sin número"}"
        
        // Verificar si hay sugerencias del scanner
        val suggestedValues = intent.getStringArrayExtra("suggested_values")
        val hasSuggestions = suggestedValues?.any { it.isNotEmpty() } == true
        
        tvRecinto.text = if (hasSuggestions) {
            "${mesa.recintoNombre.ifEmpty { "Recinto no especificado" }} (📱 Con sugerencias del scanner)"
        } else {
            mesa.recintoNombre.ifEmpty { "Recinto no especificado" }
        }

        // Cargar votos presidenciales actuales - Nuevo orden
        etAlianzaPopular.setText(mesa.alianzaPopular.toString())
        etAdn.setText(mesa.adn.toString())
        etApb.setText(mesa.apb.toString())
        etNgp.setText(mesa.ngp.toString())
        etLibre.setText(mesa.libre.toString())
        etLaFuerzaDelPueblo.setText(mesa.laFuerzaDelPueblo.toString())
        etMas.setText(mesa.mas.toString())
        etMorena.setText(mesa.morena.toString())
        etUnidad.setText(mesa.unidad.toString())
        etPdc.setText(mesa.pdc.toString())
        etNulo.setText(mesa.nulo.toString())
        etBlanco.setText(mesa.blanco.toString())

        // Cargar votos diputados actuales - Nuevo orden
        etAlianzaPopularDiputado.setText(mesa.alianzaPopularDiputado.toString())
        etAdnDiputado.setText(mesa.adnDiputado.toString())
        etApbDiputado.setText(mesa.apbDiputado.toString())
        etNgpDiputado.setText(mesa.ngpDiputado.toString())
        etLibreDiputado.setText(mesa.libreDiputado.toString())
        etLaFuerzaDelPuebloDiputado.setText(mesa.laFuerzaDelPuebloDiputado.toString())
        etMasDiputado.setText(mesa.masDiputado.toString())
        etMorenaDiputado.setText(mesa.morenaDiputado.toString())
        etUnidadDiputado.setText(mesa.unidadDiputado.toString())
        etPdcDiputado.setText(mesa.pdcDiputado.toString())
        etNuloDiputado.setText(mesa.nuloDiputado.toString())
        etBlancoDiputado.setText(mesa.blancoDiputado.toString())

        // Estados
        cbVerificado.isChecked = mesa.verificado
        cbObservado.isChecked = mesa.observado
    }

    private fun setupClickListeners() {
        btnCancelar.setOnClickListener {
            finish()
        }

        btnGuardar.setOnClickListener {
            guardarCambios()
        }
    }

    private fun guardarCambios() {
        val currentMesa = mesa ?: return

        try {
            // Mostrar loading
            progressGuardar.isVisible = true
            btnGuardar.isEnabled = false
            btnCancelar.isEnabled = false

            // Crear request con todos los campos
            val updateRequest = UpdateVotosRequest(
                // Votos presidenciales
                nulo = getIntValue(etNulo),
                blanco = getIntValue(etBlanco),
                alianzapopular = getIntValue(etAlianzaPopular),
                adn = getIntValue(etAdn),
                apb = getIntValue(etApb),
                ngp = getIntValue(etNgp),
                libre = getIntValue(etLibre),
                lafuerzadelpueblo = getIntValue(etLaFuerzaDelPueblo),
                mas = getIntValue(etMas),
                morena = getIntValue(etMorena),
                unidad = getIntValue(etUnidad),
                pdc = getIntValue(etPdc),
                
                // Votos diputados - Todos los campos
                nulo_diputado = getIntValue(etNuloDiputado),
                blanco_diputado = getIntValue(etBlancoDiputado),
                alianzapopular_diputado = getIntValue(etAlianzaPopularDiputado),
                adn_diputado = getIntValue(etAdnDiputado),
                apb_diputado = getIntValue(etApbDiputado),
                ngp_diputado = getIntValue(etNgpDiputado),
                libre_diputado = getIntValue(etLibreDiputado),
                lafuerzadelpueblo_diputado = getIntValue(etLaFuerzaDelPuebloDiputado),
                mas_diputado = getIntValue(etMasDiputado),
                morena_diputado = getIntValue(etMorenaDiputado),
                unidad_diputado = getIntValue(etUnidadDiputado),
                pdc_diputado = getIntValue(etPdcDiputado),
                
                // Estados
                verificado = cbVerificado.isChecked,
                observado = cbObservado.isChecked
            )

            // Enviar actualización
            lifecycleScope.launch {
                try {
                    val updatedMesa = ApiModule.api.updateMesaVotos(currentMesa.id, updateRequest)
                    
                    // Ocultar loading
                    progressGuardar.isVisible = false
                    btnGuardar.isEnabled = true
                    btnCancelar.isEnabled = true
                    
                    Toast.makeText(this@DocsActivity, "✅ Votos guardados correctamente", Toast.LENGTH_SHORT).show()
                    
                    // Volver a la pantalla anterior
                    finish()
                    
                } catch (e: Exception) {
                    // Ocultar loading
                    progressGuardar.isVisible = false
                    btnGuardar.isEnabled = true
                    btnCancelar.isEnabled = true
                    
                    Toast.makeText(this@DocsActivity, "❌ Error al guardar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }

        } catch (e: Exception) {
            // Ocultar loading
            progressGuardar.isVisible = false
            btnGuardar.isEnabled = true
            btnCancelar.isEnabled = true
            
            Toast.makeText(this, "❌ Error en los datos: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getIntValue(editText: EditText): Int? {
        val text = editText.text.toString().trim()
        return if (text.isEmpty()) null else text.toIntOrNull()
    }

    private fun applySuggestedValues() {
        // Obtener sugerencias del scanner
        val suggestedValues = intent.getStringArrayExtra("suggested_values")
        if (suggestedValues == null || suggestedValues.isEmpty()) {
            return
        }

        // Mostrar notificación al usuario
        val validValues = suggestedValues.filter { value ->
            val number = value.toIntOrNull()
            value.isNotEmpty() && number != null && number in 0..999
        }
        
        if (validValues.isNotEmpty()) {
            Toast.makeText(this, 
                "📱 Scanner detectó: ${validValues.joinToString(", ")}\n" +
                "💡 Sugerencias aplicadas en campos vacíos (máx. 999 votos)", 
                Toast.LENGTH_LONG).show()
        } else if (suggestedValues.any { it.isNotEmpty() }) {
            Toast.makeText(this, 
                "⚠️ Scanner detectó valores, pero fueron descartados\n" +
                "(solo se permiten números de 0-999)", 
                Toast.LENGTH_SHORT).show()
        }

        // Aplicar sugerencias a campos específicos como ayuda inicial
        // Solo si el campo actual está vacío o es 0
        suggestedValues.forEachIndexed { index, value ->
            // Validar que sea un número válido entre 0 y 999
            val number = value.toIntOrNull()
            if (value.isNotEmpty() && number != null && number in 0..999) {
                when (index) {
                    0 -> { // Primera fila detectada → Alianza Popular (primer partido en nuevo orden)
                        if (etAlianzaPopular.text.toString() == "0") {
                            etAlianzaPopular.setText(value)
                            etAlianzaPopular.setBackgroundColor(0x2000FF00) // Verde claro
                        }
                    }
                    1 -> { // Segunda fila detectada → ADN  
                        if (etAdn.text.toString() == "0") {
                            etAdn.setText(value)
                            etAdn.setBackgroundColor(0x2000FF00) // Verde claro
                        }
                    }
                    2 -> { // Tercera fila detectada → APB
                        if (etApb.text.toString() == "0") {
                            etApb.setText(value)
                            etApb.setBackgroundColor(0x2000FF00) // Verde claro
                        }
                    }
                }
            }
        }
    }

    private fun showErrorAndFinish(message: String) {
        Toast.makeText(this, "Error: $message", Toast.LENGTH_LONG).show()
        finish()
    }
}