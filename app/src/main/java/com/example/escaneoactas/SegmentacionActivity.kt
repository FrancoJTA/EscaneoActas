package com.example.escaneoactas

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.escaneoactas.utils.Train.DigitClassifier
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

class SegmentacionActivity : AppCompatActivity() {

    private lateinit var segmentedImageView: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_segmentacion)
        segmentedImageView = findViewById(R.id.segmentedImage)


        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV no se pudo inicializar")
            finish()
            return
        }

        // Obtener imagen desde URI
        val uriString = intent.getStringExtra("imageUri")
        uriString?.let {
            try {
                val uri = Uri.parse(it)
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    val segmented = segmentarYReconocer(bitmap)
                    segmentedImageView.setImageBitmap(segmented)
                }
            } catch (e: Exception) {
                Log.e("Segmentacion", "Error al procesar imagen: ${e.message}")
            }
        } ?: run {
            Log.e("Segmentacion", "URI de imagen no proporcionada")
            finish()
        }
    }

    private fun segmentarYReconocer(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        val processed = Mat()

        // Convertir a escala de grises
        org.opencv.android.Utils.bitmapToMat(bitmap, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2GRAY)

        // Aplicar desenfoque gaussiano para reducir ruido
        // Tamaño del kernel (5.0, 5.0) mejora suavizado pero puede afectar bordes pequeños
        Imgproc.GaussianBlur(mat, mat, Size(47.0, 47.0), 0.0)

        // Umbralización adaptativa mejora segmentación bajo distintas iluminaciones
        Imgproc.adaptiveThreshold(
            mat, processed, 255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV,
            29,  // tamaño de bloque: aumentar si hay mucho ruido
            2.0  // constante que se resta: subir si hay muchos falsos positivos
        )

        // Morfología: cerrar huecos y mejorar forma de los números
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(17.0, 17.0))
        Imgproc.morphologyEx(processed, processed, Imgproc.MORPH_CLOSE, kernel)

        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(processed, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        val result = Mat()
        Imgproc.cvtColor(processed, result, Imgproc.COLOR_GRAY2RGBA)

        val classifier = DigitClassifier(this)
        val containerLayout = findViewById<LinearLayout>(R.id.roiContainer)
        runOnUiThread { containerLayout.removeAllViews() }

        for (cnt in contours) {
            val rect = Imgproc.boundingRect(cnt)

            // Filtrado por tamaño: descarta ruido (ajusta según tamaño promedio del número)
            if (rect.area() > 5000 && rect.width > 30 && rect.height > 30) {
                val roi = Mat(processed, rect)
                val rawBitmap = Bitmap.createBitmap(roi.cols(), roi.rows(), Bitmap.Config.ARGB_8888)
                org.opencv.android.Utils.matToBitmap(roi, rawBitmap)

                // Fondo negro cuadrado con padding
                val maxDim = maxOf(roi.cols(), roi.rows())
                val paddingRatio = 0.7f // Mayor valor = más espacio negro alrededor
                val paddedSize = (maxDim * (1.0f + paddingRatio)).toInt()
                val paddedBitmap = Bitmap.createBitmap(paddedSize, paddedSize, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(paddedBitmap)
                canvas.drawColor(android.graphics.Color.BLACK)
                val left = (paddedSize - roi.cols()) / 2
                val top = (paddedSize - roi.rows()) / 2
                canvas.drawBitmap(rawBitmap, left.toFloat(), top.toFloat(), null)

                // Dilatar para engrosar trazos
                val thickenedMat = Mat()
                val kernel1 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(10.0, 10.0)) // Más grande = trazos más gruesos
                org.opencv.android.Utils.bitmapToMat(paddedBitmap, thickenedMat)
                Imgproc.dilate(thickenedMat, thickenedMat, kernel1)

                // Convertir y escalar a 28x28 para clasificación
                val thickenedBitmap = Bitmap.createBitmap(thickenedMat.cols(), thickenedMat.rows(), Bitmap.Config.ARGB_8888)
                org.opencv.android.Utils.matToBitmap(thickenedMat, thickenedBitmap)
                val resized = Bitmap.createScaledBitmap(thickenedBitmap, 28, 28, true)

                // Solo vista previa para UI (no se usa en clasificación)
                val previewBitmap = Bitmap.createBitmap(28, 28, Bitmap.Config.ARGB_8888)
                for (y in 0 until 28) {
                    for (x in 0 until 28) {
                        val pixel = resized.getPixel(x, y)
                        val r = (pixel shr 16) and 0xFF
                        val g = (pixel shr 8) and 0xFF
                        val b = (pixel and 0xFF)
                        val avg = (r + g + b) / 3f
                        val gray = avg.toInt().coerceIn(0, 255)
                        val grayPixel = 0xFF shl 24 or (gray shl 16) or (gray shl 8) or gray
                        previewBitmap.setPixel(x, y, grayPixel)
                    }
                }

                val recognition = classifier.classify(resized)
                Log.d("DigitClassifier", "Dígito: ${recognition.label}, Conf: ${"%.2f".format(recognition.confidence)}")

                if (recognition.label != -1 && recognition.confidence > 0.5f) {
                    runOnUiThread {
                        val imageView = ImageView(this)
                        imageView.setImageBitmap(previewBitmap)
                        val params = LinearLayout.LayoutParams(100, 100)
                        params.setMargins(10, 10, 10, 10)
                        imageView.layoutParams = params
                        containerLayout.addView(imageView)
                    }

                    Imgproc.rectangle(
                        result,
                        Point(rect.x.toDouble(), rect.y.toDouble()),
                        Point((rect.x + rect.width).toDouble(), (rect.y + rect.height).toDouble()),
                        Scalar(0.0, 255.0, 0.0, 255.0), 2
                    )
                    Imgproc.putText(
                        result,
                        recognition.label.toString(),
                        Point(rect.x.toDouble(), rect.y - 5.0),
                        Imgproc.FONT_HERSHEY_SIMPLEX,
                        1.5,
                        Scalar(255.0, 0.0, 0.0, 255.0),
                        2
                    )
                }

                thickenedMat.release()
            }
        }

        classifier.close()
        mat.release()
        processed.release()
        hierarchy.release()

        val resultBitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888)
        org.opencv.android.Utils.matToBitmap(result, resultBitmap)
        result.release()
        return resultBitmap
    }




}