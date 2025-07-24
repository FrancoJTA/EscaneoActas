package com.example.escaneoactas

import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.scale
import com.example.escaneoactas.utils.Train.DigitClassifier
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc

class SegmentacionActivity : AppCompatActivity() {

    private data class DigitRec(val x: Int, val y: Int, val label: Int)
    private lateinit var mainImage: ImageView
    private lateinit var roiContainer: LinearLayout
    private lateinit var btnBack     : ImageButton
    private lateinit var btnDocs     : ImageButton
    /* nuevo rango de área ─ más permisivo */
    private val minFrac = 0.001   // 0.3 % del frame
    private val maxFrac = 0.50    // 50 %

    /* padding negro extra (40 % del lado mayor) */
    private val padRatio = 0.40f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_segmentacion)

        mainImage    = findViewById(R.id.segmentedImage)
        roiContainer = findViewById(R.id.roiContainer)
        btnBack      = findViewById(R.id.btnBack)
        btnDocs      = findViewById(R.id.btnDocs)

        btnBack.setOnClickListener { finish() }   // vuelve a la cámara

        val digits = mutableListOf<DigitRec>()


        /* 1·  abre la máscara */
        val uriStr = intent.getStringExtra("imageUri") ?: run { finish(); return }
        val uri    = Uri.parse(uriStr)
        val maskBmp = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            ?: run { finish(); return }

        /* 2·  RGBA -> gris -> binario */
        val matRgba = Mat(); Utils.bitmapToMat(maskBmp, matRgba)
        val matGray = Mat(); Imgproc.cvtColor(matRgba, matGray, Imgproc.COLOR_RGBA2GRAY)
        val matBin  = Mat(); Imgproc.threshold(matGray, matBin, 128.0, 255.0, Imgproc.THRESH_BINARY)

        /* 3·  contornos externos */
        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(matBin, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        val frameArea = matBin.total()
        val minArea = (frameArea * minFrac).toInt()
        val maxArea = (frameArea * maxFrac).toInt()

        val result = matRgba.clone()
        val classifier = DigitClassifier(this)
        roiContainer.removeAllViews()

        for (c in contours) {
            val rect = Imgproc.boundingRect(c)
            val area = rect.area().toInt()
            if (area !in minArea..maxArea) { c.release(); continue }

            /* ---------- ROI con padding estilo MNIST ---------- */
            val roi = Mat(matBin, rect)

            // bitmap del ROI binario
            val roiBmp = Bitmap.createBitmap(roi.cols(), roi.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(roi, roiBmp)

            // lado mayor + extra padding
            val side = (maxOf(roiBmp.width, roiBmp.height) * (1 + padRatio)).toInt()
            val square = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
            Canvas(square).apply {
                drawColor(Color.BLACK)
                val dx = (side - roiBmp.width) / 2f
                val dy = (side - roiBmp.height) / 2f
                drawBitmap(roiBmp, dx, dy, null)
            }

            val resized = square.scale(28, 28, false)

            /* ---------- clasificación ---------- */
            val res = classifier.classify(resized)
            if (res.label != -1 && res.confidence > 0.5f) {
                digits += DigitRec(rect.x, rect.y, res.label)
                // miniatura en la tira horizontal
                val thumb = resized.scale(56, 56, false)
                val iv = ImageView(this).apply {
                    setImageBitmap(thumb)
                    layoutParams = LinearLayout.LayoutParams(72, 72).also { it.setMargins(8,8,8,8) }
                }
                roiContainer.addView(iv)

                // dibuja caja + texto
                Imgproc.rectangle(result, rect, Scalar(0.0,255.0,0.0,255.0), 2)
                Imgproc.putText(result, res.label.toString(),
                    Point(rect.x.toDouble(), rect.y - 6.0),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 1.2,
                    Scalar(255.0,0.0,0.0,255.0), 2)
            }
            c.release()
        }

        val values = composeRowValues(digits)
        val mesaId = intent.getIntExtra("mesa_id", -1)
        
        btnDocs.setOnClickListener {
            val intent = Intent(this, DocsActivity::class.java)
            intent.putExtra("mesa_id", mesaId)
            intent.putExtra("suggested_values", values.toTypedArray())
            startActivity(intent)
        }

        classifier.close()
        matGray.release(); matBin.release(); matRgba.release()

        /* 4·  muestra resultado */
        val outBmp = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(result, outBmp); result.release()
        mainImage.setImageBitmap(outBmp)


    }
    private fun composeRowValues(list: List<DigitRec>): List<String> {
        if (list.isEmpty()) return listOf("","","")

        // 1· ordena por y (arriba-abajo)
        val sorted = list.sortedBy { it.y }

        // 2· agrupa: nuevo grupo si diferencia Y > umbral
        val groups  = mutableListOf<MutableList<DigitRec>>()
        val threshY = 50          // píxeles de tolerancia entre filas
        var current = mutableListOf<DigitRec>()
        var lastY   = sorted.first().y
        for (d in sorted) {
            if (kotlin.math.abs(d.y - lastY) > threshY && current.isNotEmpty()) {
                groups += current
                current = mutableListOf()
            }
            current += d
            lastY = d.y
        }
        if (current.isNotEmpty()) groups += current

        // Tomamos las 3 primeras filas
        val rows = groups.take(3)

        // 3· para cada fila: ordenar por X (izq-der) y concatenar (máximo 3 dígitos)
        val values = rows.map { row ->
            val sortedDigits = row.sortedBy { it.x }
            val limitedDigits = sortedDigits.take(3)  // ← LIMITAR A MÁXIMO 3 DÍGITOS
            val numberString = limitedDigits.joinToString("") { it.label.toString() }
            
            // Log para debug (opcional)
            if (sortedDigits.size > 3) {
                Log.d("SegmentacionActivity", 
                    "Fila tenía ${sortedDigits.size} dígitos, usando solo los primeros 3: $numberString")
            }
            
            // Validar que el número sea razonable (0-999)
            val number = numberString.toIntOrNull()
            if (number != null && number <= 999) {
                numberString
            } else {
                Log.w("SegmentacionActivity", "Número inválido detectado: $numberString, ignorando")
                ""  // Devolver string vacío si el número no es válido
            }
        }

        // completa hasta 3 filas con cadenas vacías
        while (values.size < 3) (values as MutableList).add("")
        return values
    }
}


