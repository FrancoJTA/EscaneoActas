package com.example.escaneoactas

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.graphics.Matrix
import android.widget.SeekBar
import org.opencv.core.CvType
import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.core.Scalar

class MainActivity : AppCompatActivity() {

    private lateinit var imageOverlay: ImageView
    private lateinit var captureButton: Button
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var overlayRects: OverlayView
    private lateinit var paramSelector: ParamSelectorView
    private var currentIdx = 0
    private lateinit var paramList: List<ParamConfig>
    private lateinit var btnToggleContours: Button
    private lateinit var seekMinArea: SeekBar
    private lateinit var seekMaxArea: SeekBar

    private var contoursEnabled = true
    private var minArea = 1000f
    private var maxArea = 200000f
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnToggleContours = findViewById(R.id.btnToggleContours)
        seekMinArea = findViewById(R.id.seekMinArea)
        seekMaxArea = findViewById(R.id.seekMaxArea)
        overlayRects = findViewById(R.id.overlayRects)
        btnToggleContours.setOnClickListener {
            contoursEnabled = !contoursEnabled
            btnToggleContours.text = if (contoursEnabled) "Contornos: ON" else "Contornos: OFF"
        }

        seekMinArea.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                minArea = p.toFloat()
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        seekMaxArea.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                maxArea = p.toFloat().coerceAtLeast(minArea)
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        imageOverlay = findViewById(R.id.imageOverlay)
        captureButton = findViewById(R.id.captureButton)
        paramSelector = findViewById(R.id.paramSelector)

        val pressAnim = AnimationUtils.loadAnimation(this, R.anim.button_press)
        captureButton.setOnTouchListener { v, _ ->
            v.startAnimation(pressAnim)
            false
        }
        captureButton.setOnClickListener { takePhoto() }

        paramList = listOf(
            ParamConfig("Gaussian Blur", 11f, 3f, 99f, 2f, mustBeOdd = true) {},
            ParamConfig("Threshold Block", 11f, 3f, 99f, 2f, mustBeOdd = true) {},
            ParamConfig("Threshold C", 2f, -50f, 50f, 0.5f, allowNegative = true) {},
            ParamConfig("Morph Close", 3f, 1f, 99f, 1f) {}
        )

        paramSelector.onPrev = {
            currentIdx = (currentIdx - 1 + paramList.size) % paramList.size
            paramSelector.bind(paramList[currentIdx])
        }
        paramSelector.onNext = {
            currentIdx = (currentIdx + 1) % paramList.size
            paramSelector.bind(paramList[currentIdx])
        }
        paramSelector.bind(paramList[currentIdx])

        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "No se pudo inicializar")
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            startCamera()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(android.util.Size(640, 480)) // Optimiza el frame rate
                .build().also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        val rotation = imageProxy.imageInfo.rotationDegrees
                        var bmp = processFrame(imageProxy)
                        if (rotation != 0) {
                            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                        }

                        runOnUiThread { imageOverlay.setImageBitmap(bmp) }
                        imageProxy.close()
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalyzer)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processFrame(imageProxy: ImageProxy): Bitmap {
        val width = imageProxy.width
        val height = imageProxy.height
        val yBuffer = imageProxy.planes[0].buffer
        val yRowStride = imageProxy.planes[0].rowStride

        // 1. Construcción del Mat en escala de grises directamente desde Y
        val grayMat = Mat(height, width, CvType.CV_8UC1)
        val row = ByteArray(width)
        for (i in 0 until height) {
            yBuffer.position(i * yRowStride)
            yBuffer.get(row, 0, width)
            grayMat.put(i, 0, row)
        }

        // 2. Procesamiento: blur, threshold, close
        val blur = paramList[0].value.toDouble()
        Imgproc.GaussianBlur(grayMat, grayMat, Size(blur, blur), 0.0)

        val blockSize = (paramList[1].value.toInt() or 1).coerceAtLeast(3)
        val c = paramList[2].value.toDouble()
        Imgproc.adaptiveThreshold(
            grayMat, grayMat, 255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV,
            blockSize, c
        )

        val morph = paramList[3].value.toDouble()
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(morph, morph))
        Imgproc.morphologyEx(grayMat, grayMat, Imgproc.MORPH_CLOSE, kernel)

        // 3. Convertir a RGBA para superponer contornos
        val rgbaMat = Mat()
        Imgproc.cvtColor(grayMat, rgbaMat, Imgproc.COLOR_GRAY2RGBA)

        // 4. Detectar y dibujar contornos si están activos
        if (contoursEnabled) {
            val contours = mutableListOf<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(grayMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

            for (cnt in contours) {
                val r = Imgproc.boundingRect(cnt)
                val area = r.area().toFloat()
                if (area in minArea..maxArea && r.width > 0 && r.height > 0) {
                    Imgproc.rectangle(
                        rgbaMat,
                        Point(r.x.toDouble(), r.y.toDouble()),
                        Point((r.x + r.width).toDouble(), (r.y + r.height).toDouble()),
                        Scalar(0.0, 0.0, 255.0, 255.0),
                        2
                    )
                }
            }
            hierarchy.release()
        }

        // 5. Enviar rectángulos al overlayView (opcional)
        // Si sigues usando overlayRects para dibujar otra capa, llámalo aquí
        // runOnUiThread { overlayRects.setRects(detectedRects) }

        // 6. Convertir Mat final a Bitmap
        val outputBitmap = Bitmap.createBitmap(rgbaMat.cols(), rgbaMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(rgbaMat, outputBitmap)

        grayMat.release()
        rgbaMat.release()
        return outputBitmap
    }



    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    imageProxy.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(applicationContext, "Error al capturar", Toast.LENGTH_SHORT).show()
                    Log.e("CameraX", "Captura fallida", exception)
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
    }
}
