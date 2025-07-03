package com.example.escaneoactas

data class ParamConfig(
    val name: String,
    var value: Float,
    val min: Float,
    val max: Float,
    val step: Float = 1f,
    val mustBeOdd: Boolean = false,
    val allowNegative: Boolean = false,
    val onValueChanged: (Float) -> Unit
)
