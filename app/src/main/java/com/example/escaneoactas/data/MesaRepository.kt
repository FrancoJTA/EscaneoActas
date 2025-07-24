// data/MesaRepository.kt
package com.example.escaneoactas.data

class MesaRepository {
    suspend fun fetchMesas(): Result<List<MesaDto>> =
        runCatching { ApiModule.api.getMesas() }

    suspend fun fetchMesa(mesaId: Int): Result<MesaDto> =
        runCatching { ApiModule.api.getMesa(mesaId) }

    suspend fun updateMesaVotos(mesaId: Int, updateRequest: UpdateVotosRequest): Result<MesaDto> =
        runCatching { ApiModule.api.updateMesaVotos(mesaId, updateRequest) }
}
