package com.example.escaneoactas

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Rational
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.window.layout.WindowMetricsCalculator
import com.example.escaneoactas.vision.FrameProcessor
import org.opencv.android.OpenCVLoader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    /* ---------- UI ---------- */
    private lateinit var previewView : PreviewView
    private lateinit var imageOverlay: ImageView
    private lateinit var captureBtn : ImageButton
    private lateinit var flashBtn    : ImageButton
    private lateinit var maskBtn     : ImageButton

    /* ---------- CameraX ---------- */
    private var camera : Camera? = null
    private lateinit var exec  : ExecutorService
    private val processor = FrameProcessor()

    /* ---------- flags ---------- */
    private var torchOn  = false
    private var showMask = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView  = findViewById(R.id.previewView)
        imageOverlay = findViewById(R.id.imageOverlay)
        captureBtn   = findViewById(R.id.captureButton)
        flashBtn     = findViewById(R.id.flashButton)
        maskBtn      = findViewById(R.id.maskButton)

        captureBtn.setOnClickListener { /* futura foto */ }

        flashBtn.setOnClickListener {
            torchOn = !torchOn
            camera?.cameraControl?.enableTorch(torchOn)
            flashBtn.isSelected = torchOn          // cambia fondo por selector
            flashBtn.setImageResource(
                if (torchOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off
            )
        }

        maskBtn.setOnClickListener {
            showMask = !showMask
            imageOverlay.visibility = if (showMask) ImageView.VISIBLE else ImageView.GONE
            maskBtn.isSelected = showMask      // activa el estado del selector de color
        }


        if (!OpenCVLoader.initDebug())
            Toast.makeText(this,"OpenCV init error",Toast.LENGTH_LONG).show()

        if (checkSelfPermission(Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED)
            permLauncher.launch(Manifest.permission.CAMERA)
        else startCamera()

        exec = Executors.newSingleThreadExecutor()
    }

    /* ---------- cámara ---------- */
    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({

            val provider = providerFuture.get()

            /* ---------- 1·  Preview ---------- */
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            /* ---------- 2·  Analyzer ---------- */
            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build().also { ia ->
                    ia.setAnalyzer(exec) { px ->

                        /* procesa -> bitmap */
                        var bmp = processor.process(px)

                        /* rota según metadata */
                        val rot = px.imageInfo.rotationDegrees
                        if (rot != 0) {
                            val m = Matrix().apply { postRotate(rot.toFloat()) }
                            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
                        }

                        /* pinta sólo si la máscara está activa */
                        if (showMask) runOnUiThread { imageOverlay.setImageBitmap(bmp) }

                        px.close()
                    }
                }

            /* ---------- 3·  ViewPort (center-crop compartido) ---------- */
            val bounds = WindowMetricsCalculator.getOrCreate()
                .computeCurrentWindowMetrics(this).bounds
            val viewport = ViewPort.Builder(
                Rational(bounds.width(), bounds.height()),
                previewView.display.rotation
            )
                .setScaleType(ViewPort.FILL_CENTER)   // center-crop
                .build()

            /* ---------- 4·  UseCaseGroup ---------- */
            val group = UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(analyzer)
                .setViewPort(viewport)
                .build()

            provider.unbindAll()
            camera = provider.bindToLifecycle(
                this, CameraSelector.DEFAULT_BACK_CAMERA, group
            )

        }, ContextCompat.getMainExecutor(this))
    }

    /* ---------- limpieza ---------- */
    override fun onDestroy() {
        super.onDestroy()
        exec.shutdown()
    }

    /* ---------- permisos ---------- */
    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok ->
        if (ok) startCamera()
        else Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
    }
}
