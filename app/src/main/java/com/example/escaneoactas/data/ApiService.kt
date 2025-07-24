// data/ApiService.kt
package com.example.escaneoactas.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @GET("api/control-mesas")  // ← Endpoint base que usa getMesasFiltered con JWT
    suspend fun getMesas(): List<MesaDto>
    
    @GET("api/control-mesas/{id}")  // ← Endpoint para obtener una mesa específica
    suspend fun getMesa(@Path("id") mesaId: Int): MesaDto
    
    @PUT("api/control-mesas/{id}/votos")
    suspend fun updateMesaVotos(@Path("id") mesaId: Int, @Body body: UpdateVotosRequest): MesaDto
    
}

// Data class para la actualización de votos
data class UpdateVotosRequest(
    // Votos presidenciales
    val nulo: Int? = null,
    val blanco: Int? = null,
    val alianzapopular: Int? = null,
    val adn: Int? = null,
    val apb: Int? = null,
    val ngp: Int? = null,
    val libre: Int? = null,
    val lafuerzadelpueblo: Int? = null,
    val mas: Int? = null,
    val morena: Int? = null,
    val unidad: Int? = null,
    val pdc: Int? = null,
    
    // Votos diputados
    val nulo_diputado: Int? = null,
    val blanco_diputado: Int? = null,
    val alianzapopular_diputado: Int? = null,
    val adn_diputado: Int? = null,
    val apb_diputado: Int? = null,
    val ngp_diputado: Int? = null,
    val libre_diputado: Int? = null,
    val lafuerzadelpueblo_diputado: Int? = null,
    val mas_diputado: Int? = null,
    val morena_diputado: Int? = null,
    val unidad_diputado: Int? = null,
    val pdc_diputado: Int? = null,
    
    // Estados
    val verificado: Boolean? = null,
    val observado: Boolean? = null
)
