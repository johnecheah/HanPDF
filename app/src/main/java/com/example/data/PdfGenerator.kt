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
                val pageInfo = PdfDocument.PageInfo.Builder(widthPoints, heightPoints, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // 1. Draw base white A4 background
                val bgPaint = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }
                canvas.drawRect(0f, 0f, widthPoints.toFloat(), heightPoints.toFloat(), bgPaint)

                // 1b. Apply page rotation around center
                if (pageDef.rotationDegrees != 0) {
                    canvas.rotate(pageDef.rotationDegrees.toFloat(), widthPoints / 2f, heightPoints / 2f)
                }

                // 2. Draw standard templates if scan background is missing
                if (pageDef.backgroundScanPath != null) {
                    val file = File(pageDef.backgroundScanPath)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            val rect = RectF(0f, 0f, widthPoints.toFloat(), heightPoints.toFloat())
                            canvas.drawBitmap(bitmap, null, rect, Paint(Paint.FILTER_BITMAP_FLAG))
                        }
                    }
                } else {
                    // Draw paper template lines
                    when (pageDef.type.lowercase()) {
                        "lined" -> {
                            drawLinedPaperTemplate(canvas, widthPoints, heightPoints)
                        }
                        "cornell" -> {
                            drawCornellNotesTemplate(canvas, widthPoints, heightPoints)
                        }
                        "meeting" -> {
                            drawMeetingMinutesTemplate(canvas, widthPoints, heightPoints)
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
                        strokeWidth = draw.strokeWidth * (widthPoints / 400f) // scale stroke from UI relative size
                        
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

                    val path = Path()
                    val start = draw.points.first()
                    path.moveTo(start.x * widthPoints, start.y * heightPoints)
                    for (i in 1 until draw.points.size) {
                        val pt = draw.points[i]
                        path.lineTo(pt.x * widthPoints, pt.y * heightPoints)
                    }
                    canvas.drawPath(path, drawPaint)
                }

                // 4. Draw Text Layer Annotations
                for (textDef in pageDef.textAnnotations) {
                    val sizeRatio = if (textDef.isPowerOf) 0.72f else 1.0f
                    val textPaint = Paint().apply {
                        color = Color.parseColor(textDef.colorHex)
                        textSize = (textDef.fontSize * sizeRatio) * (widthPoints / 400f)
                        val fontStyle = if (textDef.isBold) {
                            if (textDef.isItalic || textDef.fontName.lowercase() == "cursive") Typeface.BOLD_ITALIC else Typeface.BOLD
                        } else {
                            if (textDef.isItalic || textDef.fontName.lowercase() == "cursive") Typeface.ITALIC else Typeface.NORMAL
                        }
                        val fontTypeface = when (textDef.fontName.lowercase()) {
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
                    
                    val textWidth = textPaint.measureText(textDef.text)
                    val baseRx = textDef.x * widthPoints
                    val rawY = textDef.y * heightPoints
                    val fontMetricsTemp = Paint().apply { textSize = textDef.fontSize * (widthPoints / 400f) }.fontMetrics
                    val powerShift = if (textDef.isPowerOf) (fontMetricsTemp.descent - fontMetricsTemp.ascent) * 0.45f else 0f
                    val ry = rawY - powerShift
                    val rxStart = when (textPaint.textAlign) {
                        Paint.Align.CENTER -> baseRx - textWidth / 2f
                        Paint.Align.RIGHT -> baseRx - textWidth
                        else -> baseRx
                    }
                    val rxEnd = rxStart + textWidth

                    // Render background bounding box if a background color is set
                    if (textDef.bgColorHex.isNotEmpty() && textDef.bgColorHex.lowercase() != "transparent") {
                        try {
                            val fontMetrics = textPaint.fontMetrics
                            val bgPaint = Paint().apply {
                                color = Color.parseColor(textDef.bgColorHex)
                                style = Paint.Style.FILL
                            }
                            canvas.drawRect(
                                rxStart - 6f,
                                ry + fontMetrics.top - 4f,
                                rxEnd + 6f,
                                ry + fontMetrics.bottom + 4f,
                                bgPaint
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    if (textDef.hasOutline) {
                        try {
                            val fontMetrics = textPaint.fontMetrics
                            val outlinePaint = Paint().apply {
                                color = Color.parseColor(textDef.outlineColorHex)
                                style = Paint.Style.STROKE
                                strokeWidth = 2f
                            }
                            canvas.drawRect(
                                rxStart - 6f,
                                ry + fontMetrics.top - 4f,
                                rxEnd + 6f,
                                ry + fontMetrics.bottom + 4f,
                                outlinePaint
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    if (textDef.hasUnderline) {
                        try {
                            val underlinePaint = Paint().apply {
                                color = Color.parseColor(textDef.colorHex)
                                style = Paint.Style.STROKE
                                strokeWidth = 1.5f
                            }
                            canvas.drawLine(
                                rxStart,
                                ry + 3f,
                                rxEnd,
                                ry + 3f,
                                underlinePaint
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    if (textDef.hasStrikeThrough) {
                        try {
                            val fontMetrics = textPaint.fontMetrics
                            val middleY = ry + (fontMetrics.ascent + fontMetrics.descent) / 2f
                            val strikePaint = Paint().apply {
                                color = Color.parseColor(textDef.colorHex)
                                style = Paint.Style.STROKE
                                strokeWidth = 1.5f
                            }
                            canvas.drawLine(
                                rxStart,
                                middleY,
                                rxEnd,
                                middleY,
                                strikePaint
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    // Adjust y so text draws appropriately relative to baseline
                    canvas.drawText(
                        textDef.text,
                        baseRx,
                        ry,
                        textPaint
                    )
                }

                // 5. Draw Signatures overlays
                for (sigDef in pageDef.signatures) {
                    val matchingSig = signatures.find { it.id == sigDef.signatureProfileId }
                    if (matchingSig != null) {
                        drawSignatureOverlay(
                            canvas,
                            matchingSig,
                            sigDef.x * widthPoints,
                            sigDef.y * heightPoints,
                            sigDef.width * (widthPoints / 400f),
                            sigDef.height * (heightPoints / 400f)
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
        val marginPaint = Paint().apply {
            color = Color.parseColor("#E09090") // Red/pink margin line
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#C5D3E8") // Light Blue Horizontal Lines
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        val leftMargin = w * 0.15f
        canvas.drawLine(leftMargin, 0f, leftMargin, h.toFloat(), marginPaint)

        val lineSpacing = 28f
        var currentY = h * 0.08f
        while (currentY < h * 0.95f) {
            canvas.drawLine(0f, currentY, w.toFloat(), currentY, linePaint)
            currentY += lineSpacing
        }
    }

    private fun drawCornellNotesTemplate(canvas: Canvas, w: Int, h: Int) {
        val boundaryPaint = Paint().apply {
            color = Color.parseColor("#80B3D6")
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        
        val titlePaint = Paint().apply {
            color = Color.parseColor("#333333")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.NORMAL)
        }

        // Draw top banner line
        val topBannerY = h * 0.08f
        canvas.drawLine(0f, topBannerY, w.toFloat(), topBannerY, boundaryPaint)
        canvas.drawText("CORNELL NOTES", 20f, topBannerY - 20f, titlePaint)

        // Draw cue column divider (30% width)
        val cueX = w * 0.3f
        val summaryY = h * 0.85f
        canvas.drawLine(cueX, topBannerY, cueX, summaryY, boundaryPaint)

        // Draw summary footer line
        canvas.drawLine(0f, summaryY, w.toFloat(), summaryY, boundaryPaint)

        // Draw sub-labels
        titlePaint.textSize = 10f
        canvas.drawText("CUES / QUESTIONS", 10f, topBannerY + 20f, titlePaint)
        canvas.drawText("NOTES & DETAILS", cueX + 15f, topBannerY + 20f, titlePaint)
        canvas.drawText("SUMMARY", 10f, summaryY + 20f, titlePaint)
    }

    private fun drawMeetingMinutesTemplate(canvas: Canvas, w: Int, h: Int) {
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#5A6B7C")
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        val textPaint = Paint().apply {
            color = Color.parseColor("#333333")
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.NORMAL)
        }

        // Header
        canvas.drawText("MEETING MINUTES SUMMARY", 30f, 50f, textPaint)
        canvas.drawLine(20f, 65f, w - 20f, 65f, dividerPaint)

        val detailPaint = Paint().apply {
            color = Color.parseColor("#666666")
            textSize = 10f
        }
        canvas.drawText("DATE: ____/____/2026", 30f, 85f, detailPaint)
        canvas.drawText("FACILITATOR: __________________", 240f, 85f, detailPaint)
        canvas.drawText("SUBJECT / PURPOSE: _________________________________", 30f, 110f, detailPaint)

        canvas.drawLine(20f, 130f, w - 20f, 130f, dividerPaint)

        // Sections
        textPaint.textSize = 11f
        canvas.drawText("1. DISCUSSION POINTS & DECISIONS", 30f, 155f, textPaint)
        canvas.drawText("2. ACTION ITEMS & WORKFLOWS", 30f, h * 0.55f, textPaint)
        canvas.drawLine(20f, h * 0.55f + 15f, w - 20f, h * 0.55f + 15f, dividerPaint)
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
