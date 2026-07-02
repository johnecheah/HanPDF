package com.example.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.util.Log
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt

object DocumentProcessor {
    private const val TAG = "DocumentProcessor"

    /**
     * Detects the 4 corners of a document in a bitmap (ordered: Top-Left, Top-Right, Bottom-Right, Bottom-Left).
     * It downscales the bitmap for fast real-time performance and scans radially to detect the first prominent
     * contrast / luminance transition which represents the paper border.
     */
    fun detectCorners(bitmap: Bitmap): List<PointF> {
        val width = bitmap.width
        val height = bitmap.height
        
        // Use a downscaled version of the bitmap for real-time responsiveness
        val targetScaleW = 320
        val targetScaleH = (targetScaleW * (height.toFloat() / width.toFloat())).toInt()
        val scaled = Bitmap.createScaledBitmap(bitmap, targetScaleW, targetScaleH, false)
        
        val sw = scaled.width
        val sh = scaled.height
        
        val cx = sw / 2f
        val cy = sh / 2f
        
        // Average brightness of the center area (assumed to be the document page)
        var centerLuma = 0f
        var count = 0
        for (dx in -15..15) {
            for (dy in -15..15) {
                val px = (cx + dx).toInt().coerceIn(0, sw - 1)
                val py = (cy + dy).toInt().coerceIn(0, sh - 1)
                val color = scaled.getPixel(px, py)
                val r = Color.red(color)
                val g = Color.green(color)
                val b = Color.blue(color)
                centerLuma += 0.299f * r + 0.587f * g + 0.114f * b
                count++
            }
        }
        centerLuma /= count.coerceAtLeast(1)
        
        // Radial scanning directions from center to corners
        val corners = mutableListOf<PointF>()
        
        // Target corners of the viewport
        val targets = listOf(
            PointF(0f, 0f),                      // Top-Left
            PointF(sw.toFloat(), 0f),            // Top-Right
            PointF(sw.toFloat(), sh.toFloat()),  // Bottom-Right
            PointF(0f, sh.toFloat())             // Bottom-Left
        )
        
        for (i in 0..3) {
            val target = targets[i]
            
            // Scan from outer corner towards the center to find the first high-contrast paper edge
            val steps = 50
            var foundCorner: PointF? = null
            
            for (step in 0..steps) {
                val t = step.toFloat() / steps
                // Interpolate from outer boundary to the center
                val px = (target.x * (1f - t) + cx * t).toInt().coerceIn(0, sw - 1)
                val py = (target.y * (1f - t) + cy * t).toInt().coerceIn(0, sh - 1)
                
                val color = scaled.getPixel(px, py)
                val r = Color.red(color)
                val g = Color.green(color)
                val b = Color.blue(color)
                val currentLuma = 0.299f * r + 0.587f * g + 0.114f * b
                
                // Detect high luminance or sharp transition to paper background (paper is usually white/light)
                // We also check local gradient to identify the edge transitions
                val diff = abs(currentLuma - centerLuma)
                if (diff < 35f || (currentLuma > 110f && diff < 50f)) {
                    // Edge detected at (px, py)
                    foundCorner = PointF(
                        (px.toFloat() / sw) * width,
                        (py.toFloat() / sh) * height
                    )
                    break
                }
            }
            
            // Fallback to proportional safe margins (e.g. 10% inside) if no distinct edge was detected
            if (foundCorner == null) {
                val marginX = width * 0.12f
                val marginY = height * 0.12f
                val fallback = when (i) {
                    0 -> PointF(marginX, marginY)
                    1 -> PointF(width - marginX, marginY)
                    2 -> PointF(width - marginX, height - marginY)
                    else -> PointF(marginX, height - marginY)
                }
                corners.add(fallback)
            } else {
                corners.add(foundCorner)
            }
        }
        
        if (scaled != bitmap) {
            scaled.recycle()
        }
        
        return corners
    }

    /**
     * Deskews (straightens) a quadrilateral document image into a perfect rectangle using
     * Android's built-in perspective transformation matrix (setPolyToPoly).
     */
    fun deskew(bitmap: Bitmap, corners: List<PointF>): Bitmap {
        if (corners.size < 4) return bitmap
        
        val tl = corners[0]
        val tr = corners[1]
        val br = corners[2]
        val bl = corners[3]
        
        // Calculate the ideal straight dimensions based on average edge lengths
        val topWidth = hypot((tr.x - tl.x).toDouble(), (tr.y - tl.y).toDouble())
        val bottomWidth = hypot((br.x - bl.x).toDouble(), (br.y - bl.y).toDouble())
        val targetWidth = maxOf(topWidth, bottomWidth).toInt().coerceIn(100, 4000)
        
        val leftHeight = hypot((bl.x - tl.x).toDouble(), (bl.y - tl.y).toDouble())
        val rightHeight = hypot((br.x - tr.x).toDouble(), (br.y - tr.y).toDouble())
        val targetHeight = maxOf(leftHeight, rightHeight).toInt().coerceIn(100, 4000)
        
        val deskewed = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(deskewed)
        
        val srcPoints = floatArrayOf(
            tl.x, tl.y,
            tr.x, tr.y,
            br.x, br.y,
            bl.x, bl.y
        )
        
        val dstPoints = floatArrayOf(
            0f, 0f,
            targetWidth.toFloat(), 0f,
            targetWidth.toFloat(), targetHeight.toFloat(),
            0f, targetHeight.toFloat()
        )
        
        val matrix = Matrix()
        val success = matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)
        
        if (success) {
            val paint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
            }
            canvas.drawBitmap(bitmap, matrix, paint)
            return deskewed
        }
        
        return bitmap
    }

    /**
     * Curve Flattening & Shadows Neutralization:
     * Eliminates curvature folding lines, pages bend, and dark background shadows.
     * It uses flat-field local background normalization:
     * pixel_new = (pixel_original / local_background_luminance) * 255.
     */
    fun flattenCurvesAndEnhance(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        
        val dest = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        
        // Create a heavily blurred background profile to model curved shadows and folded lighting gradients
        val downScale = 0.25f
        val dw = (w * downScale).toInt().coerceAtLeast(10)
        val dh = (h * downScale).toInt().coerceAtLeast(10)
        val small = Bitmap.createScaledBitmap(bitmap, dw, dh, false)
        
        // Apply box blur to capture ambient illumination curvature
        val blurredSmall = fastBlur(small, 20)
        val background = Bitmap.createScaledBitmap(blurredSmall, w, h, true)
        
        // Process pixels to divide original by blurred illumination model to flatten folds
        val originalPixels = IntArray(w * h)
        val backgroundPixels = IntArray(w * h)
        val outputPixels = IntArray(w * h)
        
        bitmap.getPixels(originalPixels, 0, w, 0, 0, w, h)
        background.getPixels(backgroundPixels, 0, w, 0, 0, w, h)
        
        for (i in 0 until w * h) {
            val orig = originalPixels[i]
            val bg = backgroundPixels[i]
            
            val oR = (orig shr 16) and 0xFF
            val oG = (orig shr 8) and 0xFF
            val oB = orig and 0xFF
            
            val bR = ((bg shr 16) and 0xFF).coerceAtLeast(1)
            val bG = ((bg shr 8) and 0xFF).coerceAtLeast(1)
            val bB = (bg and 0xFF).coerceAtLeast(1)
            
            // Divide original by background intensity to cancel background gradients (curves, folds, shadows)
            // Scale up to normalize white balance
            val nR = ((oR.toFloat() / bR) * 235f).toInt().coerceIn(0, 255)
            val nG = ((oG.toFloat() / bG) * 235f).toInt().coerceIn(0, 255)
            val nB = ((oB.toFloat() / bB) * 235f).toInt().coerceIn(0, 255)
            
            // Add slight contrast enhancement to sharpen text
            val contrast = 1.15f
            val fR = (((nR - 128) * contrast) + 128).toInt().coerceIn(0, 255)
            val fG = (((nG - 128) * contrast) + 128).toInt().coerceIn(0, 255)
            val fB = (((nB - 128) * contrast) + 128).toInt().coerceIn(0, 255)
            
            outputPixels[i] = (0xFF shl 24) or (fR shl 16) or (fG shl 8) or fB
        }
        
        dest.setPixels(outputPixels, 0, w, 0, 0, w, h)
        
        small.recycle()
        blurredSmall.recycle()
        background.recycle()
        
        return dest
    }

    /**
     * Efficient box blur implementation for smoothing out the illumination model.
     */
    private fun fastBlur(sentBitmap: Bitmap, radius: Int): Bitmap {
        val bitmap = sentBitmap.copy(sentBitmap.config ?: Bitmap.Config.ARGB_8888, true)
        if (radius < 1) return sentBitmap
        
        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)
        
        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1
        
        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        
        val vmin = IntArray(maxOf(w, h))
        val dv = IntArray(256 * div)
        for (idx in 0 until 256 * div) {
            dv[idx] = idx / div
        }
        
        yw = 0
        yi = 0
        
        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int
        
        for (yIdx in 0 until h) {
            rinsum = 0
            ginsum = 0
            binsum = 0
            routsum = 0
            goutsum = 0
            boutsum = 0
            rsum = 0
            gsum = 0
            bsum = 0
            for (idx in -radius..radius) {
                p = pix[yi + minOf(wm, maxOf(idx, 0))]
                sir = stack[idx + radius]
                sir[0] = (p shr 16) and 0xff
                sir[1] = (p shr 8) and 0xff
                sir[2] = p and 0xff
                rbs = r1 - abs(idx)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (idx > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
            }
            stackpointer = radius
            
            for (xIdx in 0 until w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]
                
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                
                if (yIdx == 0) {
                    vmin[xIdx] = minOf(xIdx + radius + 1, wm)
                }
                p = pix[yw + vmin[xIdx]]
                
                sir[0] = (p shr 16) and 0xff
                sir[1] = (p shr 8) and 0xff
                sir[2] = p and 0xff
                
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                
                yi++
            }
            yw += w
        }
        
        for (xIdx in 0 until w) {
            rinsum = 0
            ginsum = 0
            binsum = 0
            routsum = 0
            goutsum = 0
            boutsum = 0
            rsum = 0
            gsum = 0
            bsum = 0
            yp = -radius * w
            for (idx in -radius..radius) {
                yi = maxOf(0, yp) + xIdx
                sir = stack[idx + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - abs(idx)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (idx > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                yp += w
            }
            yi = xIdx
            stackpointer = radius
            for (yIdx in 0 until h) {
                pix[yi] = (0xff000000.toInt() or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum])
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (xIdx == 0) {
                    vmin[yIdx] = minOf(yIdx + r1, hm) * w
                }
                p = xIdx + vmin[yIdx]
                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi += w
            }
        }
        
        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }

    /**
     * Calculates the focal point of an image (centroid of contrast/detail)
     * and returns the adjusted crop box coordinates (left, top, right, bottom)
     * such that the focal point aligns with one of the 4 Rule of Thirds intersection points,
     * while keeping the crop box inside the image boundaries and preserving the target aspect ratio.
     */
    fun calculateRuleOfThirdsCrop(
        bitmap: Bitmap,
        aspectRatio: Float // e.g. 1.0f (1:1), 1.333f (4:3), 1.777f (16:9)
    ): RectFNormalized {
        val w = bitmap.width
        val h = bitmap.height
        
        // Downscale for ultra-fast visual interest/saliency analysis
        val sw = 80
        val sh = ((sw.toFloat() * h) / w).toInt().coerceAtLeast(10)
        val scaled = Bitmap.createScaledBitmap(bitmap, sw, sh, false)
        
        var sumX = 0f
        var sumY = 0f
        var totalWeight = 0f
        
        // Horizontal and vertical gradient accumulation to find key features/texts
        for (y in 1 until sh - 1) {
            for (x in 1 until sw - 1) {
                val c00 = Color.red(scaled.getPixel(x - 1, y - 1))
                val c02 = Color.red(scaled.getPixel(x + 1, y - 1))
                val c20 = Color.red(scaled.getPixel(x - 1, y + 1))
                val c22 = Color.red(scaled.getPixel(x + 1, y + 1))
                val c10 = Color.red(scaled.getPixel(x - 1, y))
                val c12 = Color.red(scaled.getPixel(x + 1, y))
                val c01 = Color.red(scaled.getPixel(x, y - 1))
                val c21 = Color.red(scaled.getPixel(x, y + 1))
                
                val gx = (c02 + 2 * c12 + c22) - (c00 + 2 * c10 + c20)
                val gy = (c20 + 2 * c21 + c22) - (c00 + 2 * c01 + c02)
                val mag = kotlin.math.sqrt((gx * gx + gy * gy).toDouble()).toFloat()
                
                if (mag > 50f) {
                    sumX += x * mag
                    sumY += y * mag
                    totalWeight += mag
                }
            }
        }
        
        var fx = 0.5f
        var fy = 0.5f
        if (totalWeight > 100f) {
            fx = (sumX / totalWeight) / sw
            fy = (sumY / totalWeight) / sh
        } else {
            // Apply a slight aesthetic off-center dynamic point if interest detail is low
            fx = 0.38f
            fy = 0.38f
        }
        
        if (scaled != bitmap) {
            scaled.recycle()
        }
        
        val imageRatio = w.toFloat() / h.toFloat()
        
        // We want the crop box to cover about 70% of the image size
        val cropW: Float
        val cropH: Float
        
        if (imageRatio > aspectRatio) {
            // Image is wider than crop aspect ratio
            cropH = 0.75f
            cropW = (aspectRatio * cropH) / imageRatio
        } else {
            // Image is taller than crop aspect ratio
            cropW = 0.75f
            cropH = (cropW * imageRatio) / aspectRatio
        }
        
        // Test aligning (fx, fy) with all four Rule of Thirds power intersections:
        // intersections lie at:
        // ix = left + tx * cropW
        // iy = top + ty * cropH
        // where tx and ty are 1/3 and 2/3.
        val options = listOf(1f / 3f, 2f / 3f)
        var bestLeft = 0.1f
        var bestTop = 0.1f
        var minPenalty = Float.MAX_VALUE
        
        for (tx in options) {
            for (ty in options) {
                val testLeft = fx - tx * cropW
                val testTop = fy - ty * cropH
                
                // Calculate out of boundary overflow penalties
                val outLeft = if (testLeft < 0f) -testLeft else 0f
                val outRight = if (testLeft + cropW > 1f) (testLeft + cropW) - 1f else 0f
                val outTop = if (testTop < 0f) -testTop else 0f
                val outBottom = if (testTop + cropH > 1f) (testTop + cropH) - 1f else 0f
                
                val penalty = outLeft + outRight + outTop + outBottom
                
                if (penalty < minPenalty) {
                    minPenalty = penalty
                    bestLeft = testLeft
                    bestTop = testTop
                }
            }
        }
        
        val finalLeft = bestLeft.coerceIn(0.01f, 1f - cropW - 0.01f)
        val finalTop = bestTop.coerceIn(0.01f, 1f - cropH - 0.01f)
        
        return RectFNormalized(
            left = finalLeft,
            top = finalTop,
            right = finalLeft + cropW,
            bottom = finalTop + cropH,
            focalX = fx,
            focalY = fy
        )
    }

    data class RectFNormalized(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val focalX: Float,
        val focalY: Float
    )
}
