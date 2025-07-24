// data/MesaDto.kt
package com.example.escaneoactas.data

import com.google.gson.annotations.SerializedName

data class MesaDto(

    /* ----- claves y metadatos ----- */
    val id: Int = 0,

    @SerializedName("usuario_id")
    val usuarioId: Int = 0,

    @SerializedName("recinto_id")
    val recintoId: Int = 0,

    @SerializedName("nr_mesa")
    val nrMesa: String? = null,

    @SerializedName("cant_maxima")  
    val cantMaxima: Int = 0,

    /* ----- totales presidenciales ----- */
    val nulo: Int = 0,
    val blanco: Int = 0,
    
    @SerializedName("alianzapopular")
    val alianzaPopular: Int = 0,
    
    @SerializedName("adn")
    val adn: Int = 0,
    
    @SerializedName("apb")
    val apb: Int = 0,
    
    @SerializedName("ngp")
    val ngp: Int = 0,
    
    @SerializedName("libre")
    val libre: Int = 0,

    @SerializedName("lafuerzadelpueblo")
    val laFuerzaDelPueblo: Int = 0,

    @SerializedName("mas")
    val mas: Int = 0,
    
    @SerializedName("morena")
    val morena: Int = 0,
    
    @SerializedName("unidad")
    val unidad: Int = 0,
    
    @SerializedName("pdc")
    val pdc: Int = 0,

    /* ----- totales diputad@s ----- */
    @SerializedName("nulo_diputado")                
    val nuloDiputado: Int = 0,
    
    @SerializedName("blanco_diputado")              
    val blancoDiputado: Int = 0,
    
    @SerializedName("alianzapopular_diputado")      
    val alianzaPopularDiputado: Int = 0,
    
    @SerializedName("adn_diputado")                 
    val adnDiputado: Int = 0,
    
    @SerializedName("apb_diputado")                 
    val apbDiputado: Int = 0,
    
    @SerializedName("ngp_diputado")                 
    val ngpDiputado: Int = 0,
    
    @SerializedName("libre_diputado")               
    val libreDiputado: Int = 0,
    
    @SerializedName("lafuerzadelpueblo_diputado")   
    val laFuerzaDelPuebloDiputado: Int = 0,
    
    @SerializedName("mas_diputado")                 
    val masDiputado: Int = 0,
    
    @SerializedName("morena_diputado")              
    val morenaDiputado: Int = 0,
    
    @SerializedName("unidad_diputado")              
    val unidadDiputado: Int = 0,
    
    @SerializedName("pdc_diputado")                 
    val pdcDiputado: Int = 0,

    /* ----- estado ----- */
    val verificado: Boolean = false,
    val observado: Boolean = false,

    @SerializedName("url_imagen")
    val urlImagen: String? = null,

    /* ----- información adicional del JOIN ----- */
    @SerializedName("recinto_nombre")
    val recintoNombre: String = "",

    @SerializedName("usuario_nombre")
    val usuarioNombre: String = "",

    @SerializedName("municipio_nombre")
    val municipioNombre: String = "",

    @SerializedName("departamento_nombre")
    val departamentoNombre: String = "",

    @SerializedName("circunscripcion_numero")
    val circunscripcionNumero: Int = 0
)
