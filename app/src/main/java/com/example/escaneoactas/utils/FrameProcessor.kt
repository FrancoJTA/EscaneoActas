package com.example.escaneoactas.vision

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer

/**
 * Convierte un frame YUV (llega como ImageProxy) en un Bitmap RGBA
 * con la máscara binaria procesada.
 *
 *  ▸ Desenfoque Gauss  ▸ Adaptive Threshold  ▸ Cierre morfológico
 */
class FrameProcessor {

    /* parámetros fijos (tócalos aquí y recompila) */
    private val GAUSS_BLUR = 11          // impar
    private val TH_BLOCK   = 11          // impar
    private val TH_C       = 2.0         // -50…50
    private val MORPH_K    = 3           // 1…99

    fun process(proxy: ImageProxy): Bitmap {
        val w = proxy.width
        val h = proxy.height

        /* ---------- plano Y → Mat gris ---------- */
        val gray = Mat(h, w, CvType.CV_8UC1)
        val row = ByteArray(w)
        val buf = proxy.planes[0].buffer
        val stride = proxy.planes[0].rowStride
        for (r in 0 until h) {
            buf.position(r * stride); buf.get(row, 0, w)
            gray.put(r, 0, row)
        }

        /* ---------- pipeline ---------- */
        // 1· Blur
        Imgproc.GaussianBlur(gray, gray, Size(GAUSS_BLUR.toDouble(), GAUSS_BLUR.toDouble()), 0.0)

        // 2· Adaptive threshold
        Imgproc.adaptiveThreshold(
            gray, gray, 255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV,
            TH_BLOCK, TH_C
        )

        // 3· Morph close
        val k = Imgproc.getStructuringElement(
            Imgproc.MORPH_RECT,
            Size(MORPH_K.toDouble(), MORPH_K.toDouble())
        )
        Imgproc.morphologyEx(gray, gray, Imgproc.MORPH_CLOSE, k)

        /* ---------- Mat gris → Bitmap RGBA ---------- */
        val rgba = Mat()
        Imgproc.cvtColor(gray, rgba, Imgproc.COLOR_GRAY2RGBA)

        val bmp = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(rgba, bmp)

        gray.release()
        rgba.release()
        return bmp
    }
}
