package com.example.escaneoactas

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.*
import androidx.core.widget.doAfterTextChanged

class ParamSelectorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val txtName: TextView
    private val edtValue: EditText
    private val btnMinus: Button
    private val btnPlus: Button
    private val btnPrev: Button
    private val btnNext: Button

    var onPrev: (() -> Unit)? = null
    var onNext: (() -> Unit)? = null

    private var config: ParamConfig? = null

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_param_selector, this, true)

        txtName = findViewById(R.id.txtParamName)
        edtValue = findViewById(R.id.edtParamValue)
        btnMinus = findViewById(R.id.btnMinus)
        btnPlus = findViewById(R.id.btnPlus)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)

        edtValue.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_NUMBER_FLAG_DECIMAL

        btnMinus.setOnClickListener { adjust(-config!!.step) }
        btnPlus.setOnClickListener { adjust(+config!!.step) }
        btnPrev.setOnClickListener { onPrev?.invoke() }
        btnNext.setOnClickListener { onNext?.invoke() }
        edtValue.doAfterTextChanged {
            val cfg = config ?: return@doAfterTextChanged
            val v = it.toString().toFloatOrNull() ?: return@doAfterTextChanged
            if (v != cfg.value) {
                updateValue(v)
            }
        }
    }

    fun bind(cfg: ParamConfig) {
        config = cfg
        txtName.text = cfg.name

        val currentText = edtValue.text.toString()
        val currentValue = currentText.toFloatOrNull()
        if (currentValue == null || currentValue != cfg.value) {
            edtValue.setText(cfg.value.toInt().toString())
        }
    }

    private fun adjust(delta: Float) {
        val cfg = config ?: return
        var v = (edtValue.text.toString().toFloatOrNull() ?: cfg.value) + delta
        if (!cfg.allowNegative && v < 0f) v = cfg.min
        if (cfg.mustBeOdd && v % 2f == 0f) v += delta
        v = v.coerceIn(cfg.min, cfg.max)
        updateValue(v)
    }

    private fun updateValue(value: Float) {
        val cfg = config ?: return
        var v = value.coerceIn(cfg.min, cfg.max)
        if (cfg.mustBeOdd && v % 2 == 0f) v += 1f
        if (!cfg.allowNegative && v < 0f) v = 0f
        if (cfg.value != v) {
            cfg.value = v
            cfg.onValueChanged(v)
            if (edtValue.text.toString() != v.toInt().toString()) {
                edtValue.setText(v.toInt().toString())
            }
        }
    }
}
