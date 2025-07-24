// ui/MesaAdapter.kt
package com.example.escaneoactas.ui

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.escaneoactas.MesaOptionsActivity
import com.example.escaneoactas.R
import com.example.escaneoactas.data.MesaDto
import java.text.NumberFormat
import java.util.Locale

class MesaAdapter(
    private val items: List<MesaDto>,
    private val onClick: (MesaDto) -> Unit
) : RecyclerView.Adapter<MesaAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvMesaNumero      : TextView = v.findViewById(R.id.tvMesaNumero)
        val tvRecinto         : TextView = v.findViewById(R.id.tvRecinto)
        val tvVerificado      : TextView = v.findViewById(R.id.tvVerificado)
        val tvObservado       : TextView = v.findViewById(R.id.tvObservado)
        val tvTotalVotos      : TextView = v.findViewById(R.id.tvTotalVotos)
        val tvVotosValidos    : TextView = v.findViewById(R.id.tvVotosValidos)
        val tvVotosInvalidos  : TextView = v.findViewById(R.id.tvVotosInvalidos)
    }

    override fun onCreateViewHolder(p: ViewGroup, vType: Int): VH =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_mesa, p, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val m = items[pos]
        
        // Información básica
        h.tvMesaNumero.text = "Mesa ${m.nrMesa ?: "Sin número"}"
        h.tvRecinto.text = m.recintoNombre.ifEmpty { "Recinto no especificado" }
        
        // Indicadores de estado
        h.tvVerificado.isVisible = m.verificado
        h.tvObservado.isVisible = m.observado
        
        // Calcular totales
        val votosValidos = calcularVotosValidos(m)
        val votosInvalidos = m.nulo + m.blanco
        val totalVotos = votosValidos + votosInvalidos
        
        // Mostrar totales formateados
        val formatter = NumberFormat.getNumberInstance(Locale("es", "ES"))
        h.tvTotalVotos.text = formatter.format(totalVotos)
        h.tvVotosValidos.text = formatter.format(votosValidos)
        h.tvVotosInvalidos.text = formatter.format(votosInvalidos)
        
        // Click listener
        h.itemView.setOnClickListener { 
            val ctx = h.itemView.context
            ctx.startActivity(
                Intent(ctx, MesaOptionsActivity::class.java)
                    .putExtra("mesa_id", m.id)
            )
        }
    }
    
    /**
     * Calcula el total de votos válidos sumando todos los partidos presidenciales
     */
    private fun calcularVotosValidos(mesa: MesaDto): Int {
        return mesa.alianzaPopular + 
               mesa.adn + 
               mesa.apb + 
               mesa.ngp + 
               mesa.libre + 
               mesa.laFuerzaDelPueblo + 
               mesa.mas + 
               mesa.morena + 
               mesa.unidad + 
               mesa.pdc
    }
}
