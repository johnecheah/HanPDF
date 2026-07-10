package com.example.data

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {
    private const val TAG = "PdfGenerator"

    private fun getPageSizePoints(page: PageDef): Pair<Int, Int> {
        if (page.backgroundScanPath != null) {
            val file = File(page.backgroundScanPath)
            if (file.exists()) {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, options)
                val origW = options.outWidth
                val origH = options.outHeight
                if (origW > 0 && origH > 0) {
                    val ratio = origW.toFloat() / origH.toFloat()
                    return if (origH >= origW) {
                        // Portrait: set height to 842 points (Standard A4 Height)
                        val h = 842
                        val w = (842 * ratio).toInt()
                        Pair(w, h)
                    } else {
                        // Landscape: set width to 842 points (Standard A4 Width)
                        val w = 842
                        val h = (842 / ratio).toInt()
                        Pair(w, h)
                    }
                }
            }
        }
        // Default standard A4 size (portrait)
        return Pair(595, 842)
    }

    private fun decodeAndScaleBackground(path: String, maxDim: Int = 2480): Bitmap? {
        val file = File(path)
        if (!file.exists()) return null
        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            val origW = options.outWidth
            val origH = options.outHeight
            if (origW <= 0 || origH <= 0) return null

            var sampleSize = 1
            val largestDim = maxOf(origW, origH)
            while (largestDim / sampleSize > maxDim * 2) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // Uses 50% less memory and is extremely fast to decode/draw
            }
            val sampledBmp = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null

            val currentW = sampledBmp.width
            val currentH = sampledBmp.height
            val currentLargest = maxOf(currentW, currentH)

            return if (currentLargest > maxDim) {
                val scale = maxDim.toFloat() / currentLargest
                val targetW = (currentW * scale).toInt()
                val targetH = (currentH * scale).toInt()
                val scaledBmp = Bitmap.createScaledBitmap(sampledBmp, targetW, targetH, true)
                if (scaledBmp != sampledBmp) {
                    sampledBmp.recycle()
                }
                scaledBmp
            } else {
                sampledBmp
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding background image", e)
            return null
        }
    }

    // Standard A4 dimensions in PostScript points (1/72 of an inch)
    /**
     * Renders an interactive Document object and outputs a genuine physical standard PDF file
     * with all scan backgrounds, vector drawing annotations, text layers, and embedded user signatures.
     */
    fun buildPdf(
        context: Context,
        document: Document,
        content: DocumentContent,
        signatures: List<SignatureProfile>
    ): File? {
        val pdfDocument = PdfDocument()

        try {
            // Ensure folder exists
            val docsDir = File(context.filesDir, "documents")
            if (!docsDir.exists()) docsDir.mkdirs()
            val outputFile = File(docsDir, "${document.title.replace(" ", "_")}_${document.id}.pdf")

            val pages = content.pages.ifEmpty { listOf(PageDef("1", 1)) }

            for ((index, pageDef) in pages.withIndex()) {
                val dimensions = getPageSizePoints(pageDef)
                val curW = dimensions.first
                val curH = dimensions.second

                val isRotated90or270 = (pageDef.rotationDegrees % 180 != 0)
                val finalW = if (isRotated90or270) curH else curW
                val finalH = if (isRotated90or270) curW else curH

                val pageInfo = PdfDocument.PageInfo.Builder(finalW, finalH, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // 1. Draw base white A4 background
                val bgPaint = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }
                canvas.drawRect(0f, 0f, finalW.toFloat(), finalH.toFloat(), bgPaint)

                // 1b. Apply page rotation around center
                if (pageDef.rotationDegrees != 0) {
                    canvas.rotate(pageDef.rotationDegrees.toFloat(), finalW / 2f, finalH / 2f)
                }

                // 2. Draw standard templates if scan background is missing
                if (pageDef.backgroundScanPath != null) {
                    val originalBmp = decodeAndScaleBackground(pageDef.backgroundScanPath, 2480)
                    if (originalBmp != null) {
                        val bitmap = if (pageDef.filterType != "original") {
                            val filtered = BitmapFilter.applyFilter(originalBmp, pageDef.filterType)
                            if (filtered != originalBmp) {
                                originalBmp.recycle()
                            }
                            filtered
                        } else {
                            originalBmp
                        }
                        val rect = RectF(
                            (finalW - curW) / 2f,
                            (finalH - curH) / 2f,
                            (finalW + curW) / 2f,
                            (finalH + curH) / 2f
                        )
                        canvas.drawBitmap(bitmap, null, rect, Paint(Paint.FILTER_BITMAP_FLAG))
                        bitmap.recycle()
                    }
                } else {
                    // Draw paper template lines
                    when (pageDef.type.lowercase()) {
                        "lined" -> {
                            drawLinedPaperTemplate(canvas, curW, curH)
                        }
                        "cornell" -> {
                            drawCornellNotesTemplate(canvas, curW, curH)
                        }
                        "meeting" -> {
                            drawMeetingMinutesTemplate(canvas, curW, curH)
                        }
                        "collage" -> {
                            drawCollageTemplate(canvas, curW, curH, pageDef)
                        }
                    }
                }

                // 3. Draw Vector Drawings (annotations, highlighter paths)
                for (draw in pageDef.drawings) {
                    if (draw.points.size < 2) continue

                    val drawPaint = Paint().apply {
                        color = Color.parseColor(draw.colorHex)
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        strokeWidth = draw.strokeWidth * (curW / 400f) // scale stroke from UI relative size
                        
                        if (draw.isHighlighter) {
                            alpha = 100 // Beautiful transparent highlight
                            xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
                        } else {
                            alpha = 255
                        }
                        
                        if (draw.isDashed) {
                            pathEffect = android.graphics.DashPathEffect(floatArrayOf(15f, 15f), 0f)
                        }
                    }

                    when (draw.shapeType.lowercase()) {
                        "line" -> {
                            val start = draw.points.first()
                            val end = draw.points.last()
                            canvas.drawLine(
                                start.x * curW, start.y * curH,
                                end.x * curW, end.y * curH,
                                drawPaint
                            )
                        }
                        "box" -> {
                            val start = draw.points.first()
                            val end = draw.points.last()
                            val left = minOf(start.x, end.x) * curW
                            val top = minOf(start.y, end.y) * curH
                            val right = maxOf(start.x, end.x) * curW
                            val bottom = maxOf(start.y, end.y) * curH
                            canvas.drawRect(left, top, right, bottom, drawPaint)
                        }
                        "filled_box" -> {
                            val start = draw.points.first()
                            val end = draw.points.last()
                            val left = minOf(start.x, end.x) * curW
                            val top = minOf(start.y, end.y) * curH
                            val right = maxOf(start.x, end.x) * curW
                            val bottom = maxOf(start.y, end.y) * curH
                            val fillPaint = Paint().apply {
                                color = Color.parseColor(draw.colorHex)
                                style = Paint.Style.FILL
                                if (draw.isHighlighter) {
                                    alpha = 100
                                    xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
                                } else {
                                    alpha = 255
                                }
                            }
                            canvas.drawRect(left, top, right, bottom, fillPaint)
                        }
                        "circle" -> {
                            val start = draw.points.first()
                            val end = draw.points.last()
                            val left = minOf(start.x, end.x) * curW
                            val top = minOf(start.y, end.y) * curH
                            val right = maxOf(start.x, end.x) * curW
                            val bottom = maxOf(start.y, end.y) * curH
                            canvas.drawOval(left, top, right, bottom, drawPaint)
                        }
                        else -> {
                            // freehand / brush
                            val path = Path()
                            val start = draw.points.first()
                            path.moveTo(start.x * curW, start.y * curH)
                            for (i in 1 until draw.points.size) {
                                val pt = draw.points[i]
                                path.lineTo(pt.x * curW, pt.y * curH)
                            }
                            canvas.drawPath(path, drawPaint)
                        }
                    }
                }

                // 4. Draw Text Layer Annotations
                for (textDef in pageDef.textAnnotations) {
                    if (textDef.id == "word_main_content" && pageDef.backgroundScanPath != null) {
                        continue
                    }
                    TextAnnotationRenderer.draw(
                        canvas = canvas,
                        textDef = textDef,
                        originX = textDef.x * curW,
                        originY = textDef.y * curH,
                        refWidthPx = curW.toFloat(),
                        isMainContent = textDef.id == "word_main_content"
                    )
                }

                // 5. Draw Signatures overlays
                for (sigDef in pageDef.signatures) {
                    val matchingSig = signatures.find { it.id == sigDef.signatureProfileId }
                    if (matchingSig != null) {
                        drawSignatureOverlay(
                            canvas,
                            matchingSig,
                            sigDef.x * curW,
                            sigDef.y * curH,
                            sigDef.width * (curW / 400f),
                            sigDef.height * (curW / 400f)
                        )
                    }
                }

                pdfDocument.finishPage(page)
            }

            val fos = FileOutputStream(outputFile)
            pdfDocument.writeTo(fos)
            fos.close()
            Log.d(TAG, "Successfully compiled real standard PDF at: ${outputFile.absolutePath}")
            return outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Error compiling physical PDF document", e)
            return null
        } finally {
            pdfDocument.close()
        }
    }

    private fun drawLinedPaperTemplate(canvas: Canvas, w: Int, h: Int) {
        val scale = w / 595f
        val marginPaint = Paint().apply {
            color = Color.parseColor("#E09090") // Red/pink margin line
            strokeWidth = 1.5f * scale
            style = Paint.Style.STROKE
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#C5D3E8") // Light Blue Horizontal Lines
            strokeWidth = 1f * scale
            style = Paint.Style.STROKE
        }

        val leftMargin = w * 0.15f
        canvas.drawLine(leftMargin, 0f, leftMargin, h.toFloat(), marginPaint)

        val lineSpacing = 28f * scale
        var currentY = h * 0.08f
        while (currentY < h * 0.95f) {
            canvas.drawLine(0f, currentY, w.toFloat(), currentY, linePaint)
            currentY += lineSpacing
        }
    }

    private fun drawCornellNotesTemplate(canvas: Canvas, w: Int, h: Int) {
        val scale = w / 595f
        val boundaryPaint = Paint().apply {
            color = Color.parseColor("#80B3D6")
            strokeWidth = 2f * scale
            style = Paint.Style.STROKE
        }
        
        val titlePaint = Paint().apply {
            color = Color.parseColor("#333333")
            textSize = 14f * scale
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.NORMAL)
        }

        // Draw top banner line
        val topBannerY = h * 0.08f
        canvas.drawLine(0f, topBannerY, w.toFloat(), topBannerY, boundaryPaint)
        canvas.drawText("CORNELL NOTES", 20f * scale, topBannerY - 20f * scale, titlePaint)

        // Draw cue column divider (30% width)
        val cueX = w * 0.3f
        val summaryY = h * 0.85f
        canvas.drawLine(cueX, topBannerY, cueX, summaryY, boundaryPaint)

        // Draw summary footer line
        canvas.drawLine(0f, summaryY, w.toFloat(), summaryY, boundaryPaint)

        // Draw sub-labels
        titlePaint.textSize = 10f * scale
        canvas.drawText("CUES / QUESTIONS", 10f * scale, topBannerY + 20f * scale, titlePaint)
        canvas.drawText("NOTES & DETAILS", cueX + 15f * scale, topBannerY + 20f * scale, titlePaint)
        canvas.drawText("SUMMARY", 10f * scale, summaryY + 20f * scale, titlePaint)
    }

    private fun drawMeetingMinutesTemplate(canvas: Canvas, w: Int, h: Int) {
        val scale = w / 595f
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#1E3A8A")
            strokeWidth = 3f * scale
            style = Paint.Style.STROKE
        }
        val thinPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1f * scale
            style = Paint.Style.STROKE
        }
        // Top corporate dual line border
        canvas.drawLine(w * 0.08f, h * 0.12f, w * 0.92f, h * 0.12f, dividerPaint)
        canvas.drawLine(w * 0.08f, h * 0.125f, w * 0.92f, h * 0.125f, thinPaint)
        
        // Bottom section content frame box
        canvas.drawRect(w * 0.08f, h * 0.35f, w * 0.92f, h * 0.92f, thinPaint)
        canvas.drawLine(w * 0.08f, h * 0.65f, w * 0.92f, h * 0.65f, thinPaint)
    }

    private fun drawSignatureOverlay(
        canvas: Canvas,
        sig: SignatureProfile,
        x: Float,
        y: Float,
        w: Float,
        h: Float
    ) {
        if (sig.pathDataJson.startsWith("image:")) {
            try {
                val imagePath = sig.pathDataJson.removePrefix("image:")
                val file = java.io.File(imagePath)
                if (file.exists()) {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        val destRect = android.graphics.RectF(x, y, x + w, y + h)
                        val paint = Paint().apply { isAntiAlias = true }
                        canvas.drawBitmap(bitmap, null, destRect, paint)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }

        // Draw matching drawn vector stroke paths inside bounds
        val points = DocumentSerializer.pointsFromJson(sig.pathDataJson)
        if (points.isEmpty()) return

        val penType = sig.penType
        val baseWidth = (sig.strokeWidth * (w / com.example.data.SignaturePathUtils.BASE_SCALE_WIDTH)).coerceAtLeast(4f)
        val sigPaint = Paint().apply {
            color = Color.parseColor(sig.colorHex)
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = baseWidth * com.example.data.SignaturePathUtils.thicknessMultiplier(penType)
            alpha = com.example.data.SignaturePathUtils.alphaForPenType(penType)
            val dash = com.example.data.SignaturePathUtils.dashIntervals(penType)
            if (dash != null) pathEffect = android.graphics.DashPathEffect(dash, 0f)
            isAntiAlias = true
        }

        val smoothed = DocumentSerializer.let {
            com.example.data.SignaturePathUtils.buildSmoothedPath(points, w, h)
        }
        smoothed.offset(x, y)
        canvas.drawPath(smoothed, sigPaint)

        if (penType == "calligraphy") {
            val offsetPath = Path(smoothed).apply { offset(1.5f, 1.5f) }
            val offsetPaint = Paint(sigPaint).apply {
                alpha = (sigPaint.alpha * 0.7f).toInt()
                strokeWidth = sigPaint.strokeWidth * 0.7f
            }
            canvas.drawPath(offsetPath, offsetPaint)
        }
    }

    private fun drawCollageTemplate(canvas: Canvas, w: Int, h: Int, pageDef: PageDef) {
        // Render top and bottom collage images if available
        pageDef.collageTop?.let { item ->
            drawCollageItem(canvas, item, isTop = true, curW = w, curH = h, filterType = pageDef.filterType)
        }
        pageDef.collageBottom?.let { item ->
            drawCollageItem(canvas, item, isTop = false, curW = w, curH = h, filterType = pageDef.filterType)
        }
    }

    private fun drawCollageItem(canvas: Canvas, item: CollageItemDef, isTop: Boolean, curW: Int, curH: Int, filterType: String) {
        val bmp = try {
            val originalBmp = BitmapFactory.decodeFile(item.imagePath)
            if (originalBmp != null && filterType != "original") {
                val filtered = BitmapFilter.applyFilter(originalBmp, filterType)
                if (filtered != originalBmp) {
                    originalBmp.recycle()
                }
                filtered
            } else {
                originalBmp
            }
        } catch (e: Exception) {
            null
        } ?: return
        
        canvas.save()
        
        val rectW = curW * 0.95f
        val rectH = curH * 0.475f
        
        val centerX = curW / 2f
        val halfH = curH / 2f
        val centerY = if (isTop) halfH / 2f else halfH + (halfH / 2f)
        
        val left = centerX - rectW / 2f
        val top = centerY - rectH / 2f
        val right = centerX + rectW / 2f
        val bottom = centerY + rectH / 2f
        
        canvas.clipRect(left, top, right, bottom)
        
        val imgW = bmp.width.toFloat()
        val imgH = bmp.height.toFloat()
        
        // Center-fit/fill the image inside the 19.8cm x 11.8cm bounding box
        val scaleToFit = minOf(rectW / imgW, rectH / imgH)
        
        val matrix = android.graphics.Matrix()
        // 1. Move origin to center of bitmap
        matrix.postTranslate(-imgW / 2f, -imgH / 2f)
        
        // 2. Apply flip
        val flipX = if (item.flipHorizontal) -1f else 1f
        val flipY = if (item.flipVertical) -1f else 1f
        matrix.postScale(flipX, flipY)
        
        // 3. Apply rotation
        matrix.postRotate(item.rotation)
        
        // 4. Apply scale (scaleToFit * zoom/scale)
        val totalScale = scaleToFit * item.scale
        matrix.postScale(totalScale, totalScale)
        
        // 5. Apply translation to the center of portion, plus normalized user offset panning
        val scaleFactor = curW.toFloat() / 400f
        val panX = item.offsetX * scaleFactor
        val panY = item.offsetY * scaleFactor
        matrix.postTranslate(centerX + panX, centerY + panY)
        
        canvas.drawBitmap(bmp, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
        
        canvas.restore()
        bmp.recycle()
    }
}
