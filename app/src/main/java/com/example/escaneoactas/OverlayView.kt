package com.example.escaneoactas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

class OverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val rectPaint = Paint().apply {
        color = 0xFFFF0000.toInt()  // Rojo
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private var rectangles: List<Rect> = emptyList()

    fun setRects(rects: List<Rect>) {
        rectangles = rects
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        rectangles.forEach {
            canvas.drawRect(it, rectPaint)
        }
    }
}
