package com.example.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

object BitmapFilter {
    fun applyFilter(bitmap: Bitmap, filterName: String): Bitmap {
        return when (filterName) {
            "black_white" -> {
                val dest = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
                val canvas = Canvas(dest)
                val paint = Paint()
                val cm = ColorMatrix()
                cm.setSaturation(0f)
                val scale = 2.0f
                val translate = -128f * scale + 128f
                val contrast = ColorMatrix(floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(contrast)
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                dest
            }
            "grayscale" -> {
                val dest = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
                val canvas = Canvas(dest)
                val paint = Paint()
                val cm = ColorMatrix()
                cm.setSaturation(0f)
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                dest
            }
            "enhance" -> {
                val dest = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
                val canvas = Canvas(dest)
                val paint = Paint()
                val cm = ColorMatrix()
                cm.setSaturation(1.4f)
                val scale = 1.2f
                val translate = -128f * scale + 128f + 10f
                val contrast = ColorMatrix(floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(contrast)
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                dest
            }
            else -> bitmap
        }
    }
}
