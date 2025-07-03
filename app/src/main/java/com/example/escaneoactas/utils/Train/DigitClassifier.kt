package com.example.escaneoactas.utils.Train

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DigitClassifier(context: Context) {
    private val inputSize = 28
    private val interpreter: Interpreter
    private val inputBuffer: ByteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize).apply {
        order(ByteOrder.nativeOrder())
    }

    init {
        val model = loadModelFile(context, "mnist.tflite")
        val options = Interpreter.Options().apply {
            setNumThreads(4)
        }

        interpreter = try {
            Interpreter(model, options)
        } catch (e: Exception) {
            Log.e("DigitClassifier", "Error al crear el intérprete", e)
            throw RuntimeException("Error al inicializar TensorFlow Lite", e)
        }
    }

    private fun loadModelFile(context: Context, filename: String): ByteBuffer {
        return context.assets.open(filename).use { inputStream ->
            ByteArrayOutputStream().use { outputStream ->
                val buffer = ByteArray(1024)
                var length: Int
                while (inputStream.read(buffer).also { length = it } != -1) {
                    outputStream.write(buffer, 0, length)
                }
                val modelBytes = outputStream.toByteArray()
                ByteBuffer.allocateDirect(modelBytes.size).apply {
                    order(ByteOrder.nativeOrder())
                    put(modelBytes)
                    position(0)
                }
            }
        }
    }

    fun classify(bitmap: Bitmap): Recognition {
        if (bitmap.width != inputSize || bitmap.height != inputSize) {
            throw IllegalArgumentException("El bitmap debe tener tamaño 28x28")
        }
        // Limpieza del buffer con ceros antes de escribir
        inputBuffer.rewind()
        for (i in 0 until inputSize * inputSize) {
            inputBuffer.putFloat(0f)
        }
        inputBuffer.rewind()

        // Preprocesamiento: usar promedio RGB e invertir
        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = (pixel and 0xFF)

                val avg = (r + g + b) / 3f
                inputBuffer.putFloat(avg / 255.0f)
            }
        }

        val output = Array(1) { FloatArray(10) }
        val startTime = System.nanoTime()

        try {
            interpreter.run(inputBuffer, output)
        } catch (e: Exception) {
            Log.e("DigitClassifier", "Error en inferencia: ${e.message}")
            return Recognition(-1, 0f, -1)
        }

        val timeCost = (System.nanoTime() - startTime) / 1_000_000
        val prediction = output[0].withIndex().maxByOrNull { it.value }

        return Recognition(
            prediction?.index ?: -1,
            prediction?.value ?: 0f,
            timeCost
        )
    }

    fun close() {
        interpreter.close()
    }
}
