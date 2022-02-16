package com.vila.cameraxtest

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text

import com.vila.cameraxtest.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding



    private lateinit var cameraExecutor: ExecutorService
    private lateinit var camera: Camera
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var preview: Preview
    private lateinit var textAnalisis: ImageAnalysis
    private lateinit var cameraSelector: CameraSelector
    private lateinit var barcodeBoxView: BarcodeSquareView
    private var scanMode = MODO_vacio
    private val WIDTH_CROPPED : Int = 8
    private val HEIGHT_CROPPED_TOP : Int = 10
    private val HEIGHT_CROPPED_BOTTOM : Int = 65

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(viewBinding.root)
        barcodeBoxView = BarcodeSquareView(this)
        addContentView(barcodeBoxView,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT))

        barcodeBoxView.bringToFront()

        viewBinding.surfaceView.apply {
            setZOrderOnTop(true)
            holder.setFormat(PixelFormat.TRANSPARENT)
            holder.addCallback(object : SurfaceHolder.Callback
            {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    holder.let {

                        Util.drawOverlay(it, HEIGHT_CROPPED_TOP
                            ,HEIGHT_CROPPED_BOTTOM
                            , WIDTH_CROPPED
                            ,this@MainActivity) }
                }

                override fun surfaceChanged(holder: SurfaceHolder, p1: Int, p2: Int, p3: Int) {

                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    holder.removeCallback(this)
                }


            })
        }

        init()

    }

    private fun init() {

        viewBinding.result.bringToFront()
        initListeners()
        cameraExecutor = Executors.newSingleThreadExecutor()
        if (allPermissionGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }


    }


    private fun allPermissionGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun initListeners() {
        viewBinding.btnFlash.setOnClickListener { turnFlash() }


        // no le presten a esta warning , habria que crear un boton que herede de Button
        // que implemente el metodo performClick , pero no es necesario para este ejemplo
        viewBinding.btnBarcodeScan.setOnTouchListener(object : View.OnTouchListener {
            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(view: View?, motionEvent: MotionEvent?): Boolean {

                when (motionEvent?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        Log.d("controlMio", "presionando boton barcode ......")

                        scanMode = MODO_CON_FILTRO
                        viewBinding.btnBarcodeScan.background = getDrawable(R.drawable.background_button_pressed)

                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        scanMode = MODO_vacio
                        viewBinding.btnBarcodeScan.background = getDrawable(R.drawable.button_background)

                        return true
                    }
                    else -> {}
                }
                return false
            }

        })

        // no le presten a esta warning , habria que crear un boton que herede de Button
        // que implemente el metodo performClick , pero no es necesario para este ejemplo
        viewBinding.btnTextScan.setOnTouchListener(object : View.OnTouchListener {
            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(view: View?, motionEvent: MotionEvent?): Boolean {

                when (motionEvent?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        Log.d("controlMio", "presionando boton texto ......")

                        scanMode = MODO_SIN_FILTRO
                        viewBinding.btnTextScan.background = getDrawable(R.drawable.background_button_pressed)
                    }
                    MotionEvent.ACTION_UP -> {
                        scanMode = MODO_vacio
                        viewBinding.btnTextScan.background = getDrawable(R.drawable.button_background)

                    }
                    else -> {}
                }
                return true
            }

        })



    }




    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {

            cameraProvider = cameraProviderFuture.get()

            preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
            }



            textAnalisis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, TextAnalysis(this
                        ,barcodeBoxView
                        ,viewBinding.viewFinder.width.toFloat()
                        ,viewBinding.viewFinder.height.toFloat()) { text ->

                        Log.d("webservice", "Resultado  .... $text")

                        viewBinding.result.text = text
                    })
                }


            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, textAnalisis
                )
            } catch (e: Exception) {

                Log.d("webservice", "Error en cameraProvider ${e.message}")
            }


        }, ContextCompat.getMainExecutor(this))
    }


    private fun turnFlash() {

        if (camera.cameraInfo.hasFlashUnit()) {
            if (camera.cameraInfo.torchState.value == TorchState.OFF) {
                camera.cameraControl.enableTorch(true)
                viewBinding.btnFlash.background =
                    AppCompatResources.getDrawable(this,R.drawable.button_background)
            } else {
                camera.cameraControl.enableTorch(false)
                viewBinding.btnFlash.background =
                    AppCompatResources.getDrawable(this,R.drawable.torch_background)

            }
        }

    }

    companion object {
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
        private const val REQUEST_CODE_PERMISSIONS = 1000
        private const val MODO_SIN_FILTRO = 1
        private const val MODO_CON_FILTRO = 2
        private const val MODO_vacio = 0


    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (REQUEST_CODE_PERMISSIONS == requestCode) {
            if (allPermissionGranted()) {
                startCamera()
            } else {
                Log.d("webservice", "No se ha dado permiso para la camera")
            }

        }
    }




    inner class TextAnalysis(
        private val context: Context,
        private val barcodeBoxView: BarcodeSquareView,
        private val previewViewWidth: Float,
        private val previewViewHeight: Float,
        private val listener: (String) -> Unit
    ) : ImageAnalysis.Analyzer {


        // Estas funciones se utilizan para dibuhar el cuadrado
        // pero para simplicidad estan comentadas
      /*private var scaleX = 1f
        private var scaleY = 1f

        private fun translateX(x: Float) = x * scaleX
        private fun translateY(y: Float) = y * scaleY

        private fun adjustBoundingRect(rect: Rect) = RectF(
            translateX(rect.left.toFloat()),
            translateY(rect.top.toFloat()),
            translateX(rect.right.toFloat()),
            translateY(rect.bottom.toFloat())
        )*/

        @SuppressLint("UnsafeOptInUsageError")
        override fun analyze(image: ImageProxy) {

            Log.d("controlMio", "dentro TextAnalisis")

          //  val image2 =
          //      InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)


            if (scanMode == MODO_SIN_FILTRO){

                val imageTemp = processImage(image)
                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE,Barcode.FORMAT_CODE_128
                    )
                    .build()
                val scanner = BarcodeScanning.getClient(options)

                val result = scanner.process(imageTemp)
                    .addOnSuccessListener { barcodes ->

                        showBarcodeResult(barcodes)
                    }
                    .addOnFailureListener {

                        Log.d("controlMio", "estoy en onfailure ......" + it.message)

                    }.addOnCompleteListener {
                        image.close()
                    }
                //*****************************************************
                // ESTE CODIGO SE USABA PARA EL RECONOCIMIENTO DE TEXTO
                /*val textScanner = TextRecognition.getClient(
                    TextRecognizerOptions.DEFAULT_OPTIONS
                )


                val result = textScanner.process(imageTemp)
                    .addOnSuccessListener { text ->

                        //Log.d("controlMio", "estoy en success ......")

                        showTextResult(text)
                    }
                    .addOnFailureListener {

                        Log.d("controlMio", "estoy en onfailure ......" + it.message)

                    }.addOnCompleteListener {
                        image.close()
                    }*/

            }else if (scanMode == MODO_CON_FILTRO){
                val imageTemp = processImage(image)

                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE, Barcode.FORMAT_CODE_128
                    )
                    .build()
                val scanner = BarcodeScanning.getClient(options)

                val result = scanner.process(imageTemp)
                    .addOnSuccessListener { barcodes ->

                        showBarcodeResult(barcodes)
                    }
                    .addOnFailureListener {

                        Log.d("controlMio", "estoy en onfailure ......" + it.message)

                    }.addOnCompleteListener {
                        image.close()
                    }
            }else{
               /* barcodeBoxView.setRect(
                    adjustBoundingRect(
                        Rect()
                    ))*/
                image.close()
            }

            }

        @SuppressLint("UnsafeOptInUsageError")
        private fun processImage(image: ImageProxy) :InputImage{

            // REALIZAMOS EL CROP ******************************
            // esta prate del codigo escanea solo el recuadro

            val mediaImage = image.image
            val bitmapTemp = Util.convertYuv420888ImageToBitmap(mediaImage!!)


            val height = bitmapTemp.height
            val width = bitmapTemp.width



            val left = mediaImage.height * WIDTH_CROPPED  / 2 / 100f
            val top = mediaImage.width * HEIGHT_CROPPED_TOP /100f
            val right = mediaImage.height * (1- WIDTH_CROPPED / 2 / 100f)
            val bottom = mediaImage .width * (1- HEIGHT_CROPPED_BOTTOM / 100f)

            val matrix = Matrix()
            matrix.preRotate(90f)

            val bitmap = Bitmap.createBitmap(bitmapTemp,0,0,width,height,matrix,true)


         //   scaleX = previewViewWidth / mediaImage.height.toFloat()
         //   scaleY = previewViewHeight / mediaImage.width.toFloat()
            val rect = Rect(left.toInt(),top.toInt(),right.toInt(),bottom.toInt())
            val rect2 = Rect(0,0,mediaImage.width,mediaImage.height)

            rect2.inset((height * (HEIGHT_CROPPED_BOTTOM/100f) /2).toInt(),(width * (WIDTH_CROPPED/100f) /2).toInt())
            val bitmapToAnalyse : Bitmap
            if(scanMode == MODO_CON_FILTRO)
            {
                bitmapToAnalyse = Util.rotateAndCropWithFilter(bitmap,
                    image.imageInfo.rotationDegrees,rect)
            }else{
                bitmapToAnalyse = Util.rotateAndCropwithOutFiletr(bitmap,
                    image.imageInfo.rotationDegrees,rect)
            }

            mediaImage.close()
            return  InputImage.fromBitmap(bitmapToAnalyse,image.imageInfo.rotationDegrees)


        }




        private fun showBarcodeResult(barcodes :List<Barcode>){

            if (barcodes.isNotEmpty()) {
                for (barcode in barcodes) {
                    // Handle received barcodes...
                    Log.d(
                        "controlMio",
                        "estoy en showresult BARCODE......${barcode.rawValue}"
                    )
                    listener(barcode.rawValue.toString())
                    // Update bounding rect
                    // el dibujo del cuadrado esta comentado por que habria que calcular
                    // las coordenadas dentro del cuadrado

                /*    barcode.boundingBox?.let { rect ->
                        barcodeBoxView.setRect(
                            adjustBoundingRect(
                                rect
                            )
                        )
                    }*/
                }
            } else {
                // Remove bounding rect
                barcodeBoxView.setRect(RectF())
            }

        }

        // Funcion para mostrar el resultado cuando reconocemos el texto
        // que no se esta usando
        private fun showTextResult(textTemp :Text){
            val resultText = textTemp.text
            if(resultText.isNotEmpty()){
                for (block in textTemp.textBlocks) {
                    val blockText = block.text
                    val blockCornerPoints = block.cornerPoints
                    val blockFrame = block.boundingBox
                    blockFrame?.let { rect ->

                        /*barcodeBoxView.setRect(
                            adjustBoundingRect(
                                rect
                            )
                        )*/
                    }
                    Log.d(
                        "controlMio",
                        "estoy en showresult blocktext......${blockText}"
                    )
                    listener("TEXTO $blockText")
                    /*
                    for (line in block.lines) {
                        val lineText = line.text
                        val lineCornerPoints = line.cornerPoints
                        val lineFrame = line.boundingBox
                        Log.d(
                            "controlMio",
                            "estoy en showresult line......${blockText}"
                        )
                        for (element in line.elements) {
                            val elementText = element.text
                            val elementCornerPoints = element.cornerPoints
                            val elementFrame = element.boundingBox
                            Log.d(
                                "controlMio",
                                "estoy en showresult element......${elementText}"
                            )
                            elementFrame?.let { rect ->
                                barcodeBoxView.setRect(
                                    adjustBoundingRect(
                                        rect
                                    )
                                )
                            }
                        }
                    }*/
                }
            }else{
             /*   barcodeBoxView.setRect(
                    adjustBoundingRect(
                        Rect()
                    ))*/
            }


        }
    }
}