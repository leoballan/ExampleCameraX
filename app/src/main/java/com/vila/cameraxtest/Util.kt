package com.vila.cameraxtest

import android.content.Context
import android.graphics.*
import android.media.Image
import android.util.Log
import android.view.SurfaceHolder
import androidx.annotation.ColorInt

object Util
{
    private val CHANNEL_RANGE = 0 until (1 shl 18)

    fun convertYuv420888ImageToBitmap(image: Image): Bitmap {
        require(image.format == ImageFormat.YUV_420_888) {
            "Unsupported image format $(image.format)"
        }

        val planes = image.planes

        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        val yuvBytes = planes.map { plane ->
            val buffer = plane.buffer
            val yuvBytes = ByteArray(buffer.capacity())
            buffer[yuvBytes]
            buffer.rewind()  // Be kind…
            yuvBytes
        }

        val yRowStride = planes[0].rowStride
        val uvRowStride = planes[1].rowStride
        val uvPixelStride = planes[1].pixelStride
        val width = image.width
        val height = image.height
        Log.d("webservice","--------------")
        Log.d("webservice","ancho ------ $width")
        Log.d("webservice","alto --------$height")


        @ColorInt val argb8888 = IntArray(width * height)
        var i = 0
        for (y in 0 until height) {
            val pY = yRowStride * y
            val uvRowStart = uvRowStride * (y shr 1)
            for (x in 0 until width) {
                val uvOffset = (x shr 1) * uvPixelStride
                argb8888[i++] =
                    yuvToRgb(
                        yuvBytes[0][pY + x].toIntUnsigned(),
                        yuvBytes[1][uvRowStart + uvOffset].toIntUnsigned(),
                        yuvBytes[2][uvRowStart + uvOffset].toIntUnsigned()
                    )
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(argb8888, 0, width, 0, 0, width, height)
        val matrix = Matrix()
        matrix.preRotate(90f)
        return bitmap
    }

    @ColorInt
    private fun yuvToRgb(nY: Int, nU: Int, nV: Int): Int {
        var nY = nY
        var nU = nU
        var nV = nV
        nY -= 16
        nU -= 128
        nV -= 128
        nY = nY.coerceAtLeast(0)

        // This is the floating point equivalent. We do the conversion in integer
        // because some Android devices do not have floating point in hardware.
        // nR = (int)(1.164 * nY + 2.018 * nU);
        // nG = (int)(1.164 * nY - 0.813 * nV - 0.391 * nU);
        // nB = (int)(1.164 * nY + 1.596 * nV);
        var nR = 1192 * nY + 1634 * nV
        var nG = 1192 * nY - 833 * nV - 400 * nU
        var nB = 1192 * nY + 2066 * nU

        // Clamp the values before normalizing them to 8 bits.
        nR = nR.coerceIn(CHANNEL_RANGE) shr 10 and 0xff
        nG = nG.coerceIn(CHANNEL_RANGE) shr 10 and 0xff
        nB = nB.coerceIn(CHANNEL_RANGE) shr 10 and 0xff
        return -0x1000000 or (nR shl 16) or (nG shl 8) or nB
    }

    private fun Byte.toIntUnsigned(): Int {
        return toInt() and 0xFF
    }

    fun rotateAndCropWithFilter(
        bitmap: Bitmap,
        imageRotationDegrees: Int,
        cropRect: Rect
    ): Bitmap
    {
        val matrix = Matrix()
        //matrix.preRotate(imageRotationDegrees.toFloat())
        val myBitmap = Bitmap.createBitmap(
            bitmap,
            cropRect.left,
            cropRect.top,
            cropRect.width(),
            cropRect.height(),
            matrix,
            true
        )

        val scale: Float = 35f + 1f
        val translate = (-.5f * scale + .5f) * 255f

        val cm = ColorMatrix(floatArrayOf(
            1f,1f,0f,0f,0f,
            1f,0f,0f,0f,0f,
            1f,2f,scale,0f,translate,
            0f,0f,0f,1f,0f
        ))


        val colorMatrixColorFilter = ColorMatrixColorFilter(cm)
        val canvas = Canvas(myBitmap)

        val paint = Paint()
        paint.colorFilter = colorMatrixColorFilter

        canvas.drawBitmap(myBitmap,0f,0f,paint)
        return myBitmap

    }


    fun rotateAndCropwithOutFiletr(
        bitmap: Bitmap,
        imageRotationDegrees: Int,
        cropRect: Rect
    ): Bitmap{
        val matrix = Matrix()
        return Bitmap.createBitmap(
            bitmap,
            cropRect.left,
            cropRect.top,
            cropRect.width(),
            cropRect.height(),
            matrix,
            true
        )
    }
    fun drawOverlay(holder: SurfaceHolder
                            , heightTopCropped: Int
                            ,heightBottomCropped: Int
                            , widthCropped: Int
                            , context:Context)
    {

        val canvas = holder.lockCanvas()
        val bgPaint = Paint().apply {
            alpha = 140
        }
        canvas.drawPaint(bgPaint)
        val rectPaint = Paint()
        rectPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        rectPaint.style = Paint.Style.FILL
        rectPaint.color = Color.WHITE
        val outlinePaint = Paint()
        outlinePaint.style = Paint.Style.STROKE
        outlinePaint.color = Color.WHITE
        outlinePaint.strokeWidth = 4f
        val surfaceWidth = holder.surfaceFrame.width()
        val surfaceHeight = holder.surfaceFrame.height()

        val cornerRadius = 25f
        // Set rect centered in frame
        val rectTop = surfaceHeight * heightTopCropped  / 100f
        val rectLeft = surfaceWidth * widthCropped / 2 / 100f
        val rectRight = surfaceWidth * (1 - widthCropped / 2 / 100f)
        val rectBottom = surfaceHeight * (1 - heightBottomCropped / 100f)
        /*   Log.d("controlMio", "alto del surfaceview ....."+ surfaceHeight)
           Log.d("controlMio", "ancho del surfaceview ....."+ surfaceWidth)
           Log.d("controlMio", "top del rect  ....."+ rectTop)
           Log.d("controlMio", "left del rect ....."+ rectLeft)
           Log.d("controlMio", "right del rect ....."+ rectRight)
           Log.d("controlMio", "bottom del rect ....."+ rectBottom)
*/

        val rect = RectF(rectLeft, rectTop, rectRight, rectBottom)
        canvas.drawRoundRect(
                rect, cornerRadius, cornerRadius, rectPaint
        )
        canvas.drawRoundRect(
                rect, cornerRadius, cornerRadius, outlinePaint
        )
        val textPaint = Paint()
        textPaint.color = Color.WHITE
        textPaint.textSize = 50F

        val overlayText = "Apunte"//context.getString(R.string.text_overlay)
        val textBounds = Rect()
        textPaint.getTextBounds(overlayText, 0, overlayText.length, textBounds)
        val textX = (surfaceWidth - textBounds.width()) / 2f
        val textY = rectBottom + textBounds.height() + 15f // put text below rect and 15f padding
        canvas.drawText(overlayText, textX, textY, textPaint)
        holder.unlockCanvasAndPost(canvas)
    }
}

