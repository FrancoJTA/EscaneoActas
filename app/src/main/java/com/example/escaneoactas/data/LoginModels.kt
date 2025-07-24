// data/LoginModels.kt
package com.example.escaneoactas.data

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val codigo: String,
    val ci: String
)

data class ScopeDto(
    val nivel: String,
    @SerializedName("scope_id") val scopeId: Int
)

data class UserDto(
    val id: Int,
    val codigo: String,
    val nombre: String,
    val ci: String,
    val telefono: String,
    val scopes: List<ScopeDto>
)

data class LoginResponse(
    val user: UserDto,
    val token: String
)
