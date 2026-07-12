package com.example.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

object BitmapFilter {
    fun applyFilter(
        bitmap: Bitmap,
        filterName: String,
        brightness: Float = 0f,
        contrast: Float = 1f,
        saturation: Float = 1f,
        shade: Float = 0f
    ): Bitmap {
        val baseFiltered = when (filterName) {
            "black_white" -> {
                val dest = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
                val canvas = Canvas(dest)
                val paint = Paint()
                val cm = ColorMatrix()
                cm.setSaturation(0f)
                val scale = 2.0f
                val translate = -128f * scale + 128f
                val contrastMat = ColorMatrix(floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(contrastMat)
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
                val contrastMat = ColorMatrix(floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(contrastMat)
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                dest
            }
            else -> bitmap
        }

        if (brightness != 0f || contrast != 1f || saturation != 1f || shade != 0f) {
            return applyAdjustments(baseFiltered, brightness, contrast, saturation, shade)
        }
        return baseFiltered
    }

    fun applyAdjustments(bitmap: Bitmap, brightness: Float, contrast: Float, saturation: Float, shade: Float = 0f): Bitmap {
        val dest = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint()

        val cm = ColorMatrix()
        cm.setSaturation(saturation)

        val scale = contrast
        val translate = -128f * scale + 128f + brightness
        val contrastMatrix = ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(contrastMatrix)

        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        if (shade != 0f) {
            val width = dest.width
            val height = dest.height
            val pixels = IntArray(width * height)
            dest.getPixels(pixels, 0, width, 0, 0, width, height)

            val shadeFactor = shade / 100f

            for (i in pixels.indices) {
                val color = pixels[i]
                val a = (color shr 24) and 0xFF
                if (a == 0) continue
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF

                // Quick average luminance
                val lum = (r + g + b) / 3f
                val shadowWeight = 1f - (lum / 255f)

                if (shadowWeight > 0f) {
                    val factor = shadeFactor * shadowWeight
                    val nr = if (shadeFactor >= 0f) {
                        (r + (255 - r) * factor).toInt().coerceIn(0, 255)
                    } else {
                        (r + r * factor).toInt().coerceIn(0, 255)
                    }
                    val ng = if (shadeFactor >= 0f) {
                        (g + (255 - g) * factor).toInt().coerceIn(0, 255)
                    } else {
                        (g + g * factor).toInt().coerceIn(0, 255)
                    }
                    val nb = if (shadeFactor >= 0f) {
                        (b + (255 - b) * factor).toInt().coerceIn(0, 255)
                    } else {
                        (b + b * factor).toInt().coerceIn(0, 255)
                    }
                    pixels[i] = (a shl 24) or (nr shl 16) or (ng shl 8) or nb
                }
            }
            dest.setPixels(pixels, 0, width, 0, 0, width, height)
        }

        return dest
    }
}
