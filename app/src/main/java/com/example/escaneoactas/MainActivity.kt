package com.example.escaneoactas

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.util.Rational
import android.view.OrientationEventListener
import android.view.Surface
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.window.layout.WindowMetricsCalculator
import com.example.escaneoactas.vision.FrameProcessor
import org.opencv.android.OpenCVLoader
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    /* ---------- UI ---------- */
    private lateinit var previewView: PreviewView
    private lateinit var imageOverlay: ImageView
    private lateinit var captureBtn: ImageButton
    private lateinit var flashBtn: ImageButton
    private lateinit var maskBtn: ImageButton
    private var orientListener: OrientationEventListener? = null

    /* ---------- CameraX ---------- */
    private var camera: Camera? = null
    private lateinit var exec: ExecutorService
    private val processor = FrameProcessor()
    private var phoneRotation = Surface.ROTATION_0
    /* ---------- flags ---------- */
    private var torchOn = false
    private var showMask = false

    /* ---------- buffer último bitmap ---------- */
    private var latestMask: Bitmap? = null        // se actualiza en el analizador

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        imageOverlay = findViewById(R.id.imageOverlay)
        captureBtn = findViewById(R.id.captureButton)
        flashBtn = findViewById(R.id.flashButton)
        maskBtn = findViewById(R.id.maskButton)

        /* ---- botón captura ---- */
        captureBtn.setOnClickListener { saveAndLaunch() }

        /* ---- linterna ---- */
        flashBtn.setOnClickListener {
            torchOn = !torchOn
            camera?.cameraControl?.enableTorch(torchOn)
            flashBtn.isSelected = torchOn
            flashBtn.setImageResource(
                if (torchOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off
            )
        }

        /* ---- toggle máscara ---- */
        maskBtn.setOnClickListener {
            showMask = !showMask
            imageOverlay.visibility = if (showMask) ImageView.VISIBLE else ImageView.GONE
            maskBtn.isSelected = showMask    // cambia tinte del icono
        }

        if (!OpenCVLoader.initDebug())
            Toast.makeText(this, "OpenCV init error", Toast.LENGTH_LONG).show()

        if (checkSelfPermission(Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) permLauncher.launch(Manifest.permission.CAMERA)
        else startCamera()

        exec = Executors.newSingleThreadExecutor()

        orientListener = object : OrientationEventListener(this) {

            // márgenes de tolerancia
            private val TOL_VERT  = 15          // ± 15° para vertical
            private val TOL_LAND  = 15          // ± 15° para horizontal
            private val LAND_LEFT  = 90
            private val LAND_RIGHT = 270

            override fun onOrientationChanged(o: Int) {

                // función auxiliar: ¿angle ≈ target ± tol?
                fun near(angle: Int, target: Int, tol: Int) : Boolean =
                    if (target == 0)
                        angle in (360 - tol)..360 || angle in 0..tol
                    else
                        angle in (target - tol)..(target + tol)

                val newRot = when (phoneRotation) {

                    /* ===== ya estamos en RETRATO normal ===== */
                    Surface.ROTATION_0 -> when {
                        near(o, LAND_LEFT , TOL_LAND) -> Surface.ROTATION_90   // gira CCW
                        near(o, LAND_RIGHT, TOL_LAND) -> Surface.ROTATION_270  // gira CW
                        else                          -> phoneRotation
                    }

                    /* ===== ya estamos en RETRATO invertido ===== */
                    Surface.ROTATION_180 -> when {
                        near(o, LAND_LEFT , TOL_LAND) -> Surface.ROTATION_90
                        near(o, LAND_RIGHT, TOL_LAND) -> Surface.ROTATION_270
                        else                          -> phoneRotation
                    }

                    /* ===== en PAISAJE IZQUIERDO (90°) ===== */
                    Surface.ROTATION_90 -> when {
                        near(o, LAND_LEFT, TOL_LAND) -> Surface.ROTATION_270          // cambia al otro landscape
                        near(o, 0  , TOL_VERT) || near(o, 180, TOL_VERT) -> Surface.ROTATION_0  // vuelve a portrait
                        else -> phoneRotation
                    }

                    /* ===== en PAISAJE DERECHO (270°) ===== */
                    Surface.ROTATION_270 -> when {
                        near(o, LAND_RIGHT, TOL_LAND) -> Surface.ROTATION_90
                        near(o, 0  , TOL_VERT) || near(o, 180, TOL_VERT) -> Surface.ROTATION_0
                        else -> phoneRotation
                    }

                    else -> phoneRotation
                }

                if (newRot != phoneRotation) {
                    phoneRotation = newRot            // guarda para saveAndLaunch()

                    val deg = when (newRot) {
                        Surface.ROTATION_0   -> 0f
                        Surface.ROTATION_90  -> 90f
                        Surface.ROTATION_180 -> 180f
                        else                 -> 270f
                    }

                    listOf(captureBtn, flashBtn, maskBtn).forEach { btn ->
                        btn.animate().rotation(deg).setDuration(200).start()
                    }
                }
            }
        }.apply { enable() }
    }

    /* ---------- cámara ---------- */
    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({

            val provider = providerFuture.get()

            /* Preview */
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            /* Analyzer */
            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also { ia ->
                    ia.setAnalyzer(exec) { px ->
                        var bmp = processor.process(px)
                        val rot = px.imageInfo.rotationDegrees
                        if (rot != 0) {
                            val m = Matrix().apply { postRotate(rot.toFloat()) }
                            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
                        }
                        latestMask = bmp            // guarda la última máscara

                        if (showMask) runOnUiThread { imageOverlay.setImageBitmap(bmp) }
                        px.close()
                    }
                }

            /* Viewport común */
            val bounds = WindowMetricsCalculator.getOrCreate()
                .computeCurrentWindowMetrics(this).bounds
            val vp = ViewPort.Builder(
                Rational(bounds.width(), bounds.height()), previewView.display.rotation
            ).setScaleType(ViewPort.FILL_CENTER).build()

            val group = UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(analyzer)
                .setViewPort(vp)
                .build()

            provider.unbindAll()
            camera = provider.bindToLifecycle(
                this, CameraSelector.DEFAULT_BACK_CAMERA, group
            )

        }, ContextCompat.getMainExecutor(this))
    }

    private fun saveAndLaunch() {

        val src = latestMask ?: run {
            Toast.makeText(this, "Aún no hay imagen", Toast.LENGTH_SHORT).show(); return
        }

        /* fuerza retrato según la última orientación leída */
        val portrait = when (phoneRotation) {
            Surface.ROTATION_90  -> src.rotate(-90f)   // landscape-left
            Surface.ROTATION_270 -> src.rotate( 90f)   // landscape-right
            Surface.ROTATION_180 -> src.rotate(180f)   // upside-down
            else                 -> src                // portrait
        }

        val file = File.createTempFile("mask_", ".png", cacheDir)
        FileOutputStream(file).use { portrait.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)

        // Obtener mesa_id del intent original  
        val mesaId = intent.getIntExtra("mesa_id", -1)
        
        startActivity(
            Intent(this, SegmentacionActivity::class.java).apply {
                putExtra("imageUri", uri.toString())
                putExtra("mesa_id", mesaId)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        )
    }

    /* helper */
    private fun Bitmap.rotate(deg: Float): Bitmap =
        Bitmap.createBitmap(this, 0, 0, width, height,
            Matrix().apply { postRotate(deg) }, true)

    /* ---------- limpieza ---------- */
    override fun onDestroy() {
        orientListener?.disable()
        exec.shutdown()
        super.onDestroy()
    }

    /* ---------- permisos ---------- */
    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok ->
        if (ok) startCamera()
        else Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
    }
}
