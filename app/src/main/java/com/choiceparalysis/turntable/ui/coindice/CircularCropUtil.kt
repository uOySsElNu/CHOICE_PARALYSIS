package com.choiceparalysis.turntable.ui.coindice

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object CircularCropUtil {

    fun loadBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 2048): Bitmap? {
        return try {
            // First decode bounds only
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            // Calculate inSampleSize
            val width = options.outWidth
            val height = options.outHeight
            var sampleSize = 1
            while (width / sampleSize > maxDimension || height / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            // Decode with sample size
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun cropToCircle(
        source: Bitmap,
        cropCenterX: Float,
        cropCenterY: Float,
        cropRadius: Float,
        outputSize: Int = 512
    ): Bitmap {
        val srcWidth = source.width.toFloat()
        val srcHeight = source.height.toFloat()
        val shorterSide = minOf(srcWidth, srcHeight)

        // Convert normalized coordinates to source pixel coordinates
        val centerX = cropCenterX * srcWidth
        val centerY = cropCenterY * srcHeight
        val radiusPx = cropRadius * shorterSide

        // Compute source crop rect
        val srcLeft = (centerX - radiusPx).coerceIn(0f, srcWidth)
        val srcTop = (centerY - radiusPx).coerceIn(0f, srcHeight)
        val srcRight = (centerX + radiusPx).coerceIn(0f, srcWidth)
        val srcBottom = (centerY + radiusPx).coerceIn(0f, srcHeight)

        val srcRect = Rect(srcLeft.toInt(), srcTop.toInt(), srcRight.toInt(), srcBottom.toInt())
        val dstRect = Rect(0, 0, outputSize, outputSize)

        val output = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // Draw circular clipped image
        val circleRadius = outputSize / 2f
        canvas.save()
        canvas.clipPath(android.graphics.Path().apply {
            addCircle(circleRadius, circleRadius, circleRadius, android.graphics.Path.Direction.CW)
        })
        canvas.drawBitmap(source, srcRect, dstRect, paint)
        canvas.restore()

        return output
    }

    fun saveToInternalStorage(context: Context, bitmap: Bitmap, filename: String): String {
        val dir = File(context.filesDir, "coin_images")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file.absolutePath
    }

    fun deleteOldImage(context: Context, path: String?) {
        path?.let {
            if (it.startsWith(context.filesDir.absolutePath)) {
                File(it).delete()
            }
        }
    }
}
