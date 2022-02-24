package com.vila.cameraxtest

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.vila.cameraxtest.MainActivity.Companion.HEIGHT_CROPPED_BOTTOM
import com.vila.cameraxtest.MainActivity.Companion.HEIGHT_CROPPED_TOP
import com.vila.cameraxtest.MainActivity.Companion.WIDTH_CROPPED
import com.vila.cameraxtest.MainActivity.Companion.scanMode

class ScannerAnalisys(private val listener: (String) -> Unit): ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {

            if (scanMode == MainActivity.MODO_TEXTO){
                //*****************************************************
                // ESTE CODIGO SE USABA PARA EL RECONOCIMIENTO DE TEXTO
                val imageTemp = processImage(image)
                val textScanner = TextRecognition.getClient(
                    TextRecognizerOptions.DEFAULT_OPTIONS
                )


                val result = textScanner.process(imageTemp)
                    .addOnSuccessListener { text ->

                        Log.d("controlMio", "estoy en success TEXTO......")

                        showTextResult(text)
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

                val imageTemp = processImage(image)
                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE, Barcode.FORMAT_CODE_128
                    )
                    .build()
                val scanner = BarcodeScanning.getClient()

                val result = scanner.process(imageTemp)
                    .addOnSuccessListener { barcodes ->
                        Log.d("controlMio", "estoy en onsuccess ......" )

                        showBarcodeResult(barcodes)
                    }
                    .addOnFailureListener {

                        Log.d("controlMio", "estoy en onfailure ......" + it.message)

                    }.addOnCompleteListener {
                        image.close()
                    }
                //  image.close()
            }

    }


    @SuppressLint("UnsafeOptInUsageError")
    private fun processImage(image: ImageProxy) : InputImage {

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
        if(scanMode == MainActivity.MODO_BARCODE)
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
                listener(barcode.displayValue.toString())
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
           // barcodeBoxView.setRect(RectF())
        }

    }

    // Funcion para mostrar el resultado cuando reconocemos el texto
    // que no se esta usando
    private fun showTextResult(textTemp : Text){
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
