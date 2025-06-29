package com.example.escaneoactas

data class Recognition(
    val label: Int,        // Dígito reconocido (0-9)
    val confidence: Float, // Confianza (0.0-1.0)
    val timeCost: Long     // Tiempo de inferencia en ms
) {
    val confidencePercent: String
        get() = "%.1f%%".format(confidence * 100)
}