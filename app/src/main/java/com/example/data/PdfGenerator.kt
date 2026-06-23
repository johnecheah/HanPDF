package com.example.data

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {
    private const val TAG = "PdfGenerator"

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
        val widthPoints = 595 // A4 Width
        val heightPoints = 842 // A4 Height

        try {
            // Ensure folder exists
            val docsDir = File(context.filesDir, "documents")
            if (!docsDir.exists()) docsDir.mkdirs()
            val outputFile = File(docsDir, "${document.title.replace(" ", "_")}_${document.id}.pdf")

            val pages = content.pages.ifEmpty { listOf(PageDef("1", 1)) }

            for ((index, pageDef) in pages.withIndex()) {
                // Enforce A4 size at 300 PPI: 2480 x 3508 pixels
                val curW = 2480
                val curH = 3508

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
                    val file = File(pageDef.backgroundScanPath)
                    if (file.exists()) {
                        val originalBmp = BitmapFactory.decodeFile(file.absolutePath)
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
                    val sizeRatio = if (textDef.isPowerOf) 0.72f else 1.0f
                    val textPaint = Paint().apply {
                        color = Color.parseColor(textDef.colorHex)
                        textSize = (textDef.fontSize * sizeRatio) * (curW / 400f)
                        val fontStyle = if (textDef.isBold) {
                            if (textDef.isItalic || textDef.fontName.lowercase() == "cursive") Typeface.BOLD_ITALIC else Typeface.BOLD
                        } else {
                            if (textDef.isItalic || textDef.fontName.lowercase() == "cursive") Typeface.ITALIC else Typeface.NORMAL
                        }
                        val fontTypeface = when (textDef.fontName.lowercase()) {
                            "times new roman" -> Typeface.create(Typeface.SERIF, fontStyle)
                            "tahoma" -> Typeface.create("sans-serif-condensed", fontStyle)
                            "calibri" -> Typeface.create("sans-serif-light", fontStyle)
                            "arial" -> Typeface.create("sans-serif", fontStyle)
                            "serif" -> Typeface.create(Typeface.SERIF, fontStyle)
                            "monospace" -> Typeface.create(Typeface.MONOSPACE, fontStyle)
                            "cursive" -> Typeface.create("serif", fontStyle)
                            else -> Typeface.create(Typeface.DEFAULT, fontStyle)
                        }
                        typeface = fontTypeface
                        val paintAlign = when (textDef.alignment.lowercase()) {
                            "center" -> Paint.Align.CENTER
                            "right" -> Paint.Align.RIGHT
                            else -> Paint.Align.LEFT
                        }
                        textAlign = paintAlign
                        isAntiAlias = true
                    }
                    
                    val lines = textDef.text.split("\n")
                    val fontMetrics = textPaint.fontMetrics
                    val lineHeight = fontMetrics.descent - fontMetrics.ascent
                    val maxLineWidth = lines.map { textPaint.measureText(it) }.maxOrNull() ?: 0f

                    val baseRx = textDef.x * curW
                    val rawY = textDef.y * curH
                    val fontMetricsTemp = Paint().apply { textSize = textDef.fontSize * (curW / 400f) }.fontMetrics
                    val powerShift = if (textDef.isPowerOf) (fontMetricsTemp.descent - fontMetricsTemp.ascent) * 0.45f else 0f
                    val ry = rawY - powerShift

                    val rxStart = when (textPaint.textAlign) {
                        Paint.Align.CENTER -> baseRx - maxLineWidth / 2f
                        Paint.Align.RIGHT -> baseRx - maxLineWidth
                        else -> baseRx
                    }
                    val rxEnd = rxStart + maxLineWidth

                    // Render background bounding box if a background color is set around the entire multiline block
                    if (textDef.bgColorHex.isNotEmpty() && textDef.bgColorHex.lowercase() != "transparent") {
                        try {
                            val bgPaint = Paint().apply {
                                color = Color.parseColor(textDef.bgColorHex)
                                style = Paint.Style.FILL
                            }
                            canvas.drawRect(
                                rxStart - 6f,
                                ry + fontMetrics.top - 4f,
                                rxEnd + 6f,
                                ry + (lines.size - 1) * lineHeight + fontMetrics.bottom + 4f,
                                bgPaint
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    if (textDef.hasOutline) {
                        try {
                            val outlinePaint = Paint().apply {
                                color = Color.parseColor(textDef.outlineColorHex)
                                style = Paint.Style.STROKE
                                strokeWidth = 2f
                            }
                            canvas.drawRect(
                                rxStart - 6f,
                                ry + fontMetrics.top - 4f,
                                rxEnd + 6f,
                                ry + (lines.size - 1) * lineHeight + fontMetrics.bottom + 4f,
                                outlinePaint
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    for ((lineIdx, lineText) in lines.withIndex()) {
                        val lineY = ry + lineIdx * lineHeight
                        val lineWidth = textPaint.measureText(lineText)
                        val lineRxStart = when (textPaint.textAlign) {
                            Paint.Align.CENTER -> baseRx - lineWidth / 2f
                            Paint.Align.RIGHT -> baseRx - lineWidth
                            else -> baseRx
                        }
                        val lineRxEnd = lineRxStart + lineWidth

                        if (textDef.hasUnderline) {
                            try {
                                val underlinePaint = Paint().apply {
                                    color = Color.parseColor(textDef.colorHex)
                                    style = Paint.Style.STROKE
                                    strokeWidth = 1.5f
                                }
                                canvas.drawLine(
                                    lineRxStart,
                                    lineY + 3f,
                                    lineRxEnd,
                                    lineY + 3f,
                                    underlinePaint
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        if (textDef.hasStrikeThrough) {
                            try {
                                val middleY = lineY + (fontMetrics.ascent + fontMetrics.descent) / 2f
                                val strikePaint = Paint().apply {
                                    color = Color.parseColor(textDef.colorHex)
                                    style = Paint.Style.STROKE
                                    strokeWidth = 1.5f
                                }
                                canvas.drawLine(
                                    lineRxStart,
                                    middleY,
                                    lineRxEnd,
                                    middleY,
                                    strikePaint
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        // Draw line text correctly centered, right, or left of the baseline coordinate!
                        canvas.drawText(lineText, baseRx, lineY, textPaint)
                    }
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

        val sigPaint = Paint().apply {
            color = Color.parseColor(sig.colorHex)
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = sig.strokeWidth * (w / 160f) // relative width scaling
            isAntiAlias = true
        }

        // Signature vector coordinates are formatted as normalized coords inside signature builder workspace (0..1)
        // Draw the path translated to signature bounds (x, y, w, h)
        val path = Path()
        val start = points.first()
        path.moveTo(x + start.x * w, y + start.y * h)
        for (i in 1 until points.size) {
            val pt = points[i]
            // Draw path lines
            if (pt.x == -1f && pt.y == -1f) {
                // Liftoff/New path start (we represent lift with a placeholder coordinate pair (-1, -1) to split strokes nicely)
                if (i + 1 < points.size) {
                    val nextPt = points[i + 1]
                    path.moveTo(x + nextPt.x * w, y + nextPt.y * h)
                }
            } else {
                path.lineTo(x + pt.x * w, y + pt.y * h)
            }
        }
        canvas.drawPath(path, sigPaint)
    }
}
