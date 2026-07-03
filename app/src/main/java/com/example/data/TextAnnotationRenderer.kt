package com.example.data

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface

/**
 * SINGLE SOURCE OF TRUTH for drawing a TextAnnotationDef.
 * Used by BOTH the live editor (AdvancedTextAnnotationView) and the
 * final PDF export (PdfGenerator), so what you see in the app is
 * always exactly what gets saved.
 */
object TextAnnotationRenderer {

    data class Measured(
        val lines: List<String>,
        val textPaint: Paint,
        val lineHeight: Float,
        val maxLineWidth: Float,
        val ascent: Float,
        val descent: Float,
        val powerShift: Float
    )

    fun buildPaint(textDef: TextAnnotationDef, refWidthPx: Float): Paint {
        val sizeRatio = if (textDef.isPowerOf) 0.72f else 1.0f
        return Paint().apply {
            color = try { Color.parseColor(textDef.colorHex) } catch (e: Exception) { Color.BLACK }
            textSize = (textDef.fontSize * sizeRatio) * (refWidthPx / 400f)
            val fontStyle = if (textDef.isBold) {
                if (textDef.isItalic || textDef.fontName.lowercase() == "cursive") Typeface.BOLD_ITALIC else Typeface.BOLD
            } else {
                if (textDef.isItalic || textDef.fontName.lowercase() == "cursive") Typeface.ITALIC else Typeface.NORMAL
            }
            typeface = when (textDef.fontName.lowercase()) {
                "times new roman" -> Typeface.create(Typeface.SERIF, fontStyle)
                "tahoma" -> Typeface.create("sans-serif-condensed", fontStyle)
                "calibri" -> Typeface.create("sans-serif-light", fontStyle)
                "arial" -> Typeface.create("sans-serif", fontStyle)
                "serif" -> Typeface.create(Typeface.SERIF, fontStyle)
                "monospace" -> Typeface.create(Typeface.MONOSPACE, fontStyle)
                "cursive" -> Typeface.create("serif", fontStyle)
                else -> Typeface.create(Typeface.DEFAULT, fontStyle)
            }
            textAlign = when (textDef.alignment.lowercase()) {
                "center" -> Paint.Align.CENTER
                "right" -> Paint.Align.RIGHT
                else -> Paint.Align.LEFT
            }
            isAntiAlias = true
        }
    }

    fun measure(textDef: TextAnnotationDef, refWidthPx: Float, isMainContent: Boolean): Measured {
        val textPaint = buildPaint(textDef, refWidthPx)
        val rawLines = textDef.text.split("\n")

        val lines = if (isMainContent) {
            val wrapList = mutableListOf<String>()
            val maxW = refWidthPx * 0.84f
            for (rawLine in rawLines) {
                if (rawLine.trim().isEmpty()) { wrapList.add(""); continue }
                val words = rawLine.split(Regex("\\s+"))
                var currentLine = StringBuilder()
                for (word in words) {
                    if (currentLine.isEmpty()) {
                        currentLine.append(word)
                    } else {
                        val testLine = currentLine.toString() + " " + word
                        if (textPaint.measureText(testLine) <= maxW) {
                            currentLine.append(" ").append(word)
                        } else {
                            wrapList.add(currentLine.toString())
                            currentLine = StringBuilder(word)
                        }
                    }
                }
                if (currentLine.isNotEmpty()) wrapList.add(currentLine.toString())
            }
            wrapList
        } else rawLines

        val fm = textPaint.fontMetrics
        val lineHeight = fm.descent - fm.ascent
        val maxLineWidth = lines.map { textPaint.measureText(it) }.maxOrNull() ?: 0f
        val fmTemp = Paint().apply { textSize = textDef.fontSize * (refWidthPx / 400f) }.fontMetrics
        val powerShift = if (textDef.isPowerOf) (fmTemp.descent - fmTemp.ascent) * 0.45f else 0f

        return Measured(lines, textPaint, lineHeight, maxLineWidth, fm.ascent, fm.descent, powerShift)
    }

    /** Computes the visible bounding box for a given anchor, WITHOUT drawing. */
    fun computeBounds(textDef: TextAnnotationDef, originX: Float, originY: Float, refWidthPx: Float, isMainContent: Boolean): RectF {
        val m = measure(textDef, refWidthPx, isMainContent)
        val baseRx = originX
        val ry = originY - m.ascent - m.powerShift
        val rxStart = when (m.textPaint.textAlign) {
            Paint.Align.CENTER -> baseRx - m.maxLineWidth / 2f
            Paint.Align.RIGHT -> baseRx - m.maxLineWidth
            else -> baseRx
        }
        val rxEnd = rxStart + m.maxLineWidth
        val pdfPadding = 28f * (refWidthPx / 400f)
        return RectF(
            rxStart - pdfPadding,
            ry + m.ascent - pdfPadding,
            rxEnd + pdfPadding,
            ry + (m.lines.size - 1) * m.lineHeight + m.descent + pdfPadding
        )
    }

    /** Draws the annotation anchored at (originX, originY) — same meaning as textDef.x/y. Returns its bounding box. */
    fun draw(
        canvas: Canvas,
        textDef: TextAnnotationDef,
        originX: Float,
        originY: Float,
        refWidthPx: Float,
        isMainContent: Boolean
    ): RectF {
        val m = measure(textDef, refWidthPx, isMainContent)
        val textPaint = m.textPaint
        val bounds = computeBounds(textDef, originX, originY, refWidthPx, isMainContent)
        val baseRx = originX
        val ry = originY - m.ascent - m.powerShift

        if (textDef.bgColorHex.isNotEmpty() && textDef.bgColorHex.lowercase() != "transparent") {
            try {
                val bgPaint = Paint().apply { color = Color.parseColor(textDef.bgColorHex); style = Paint.Style.FILL }
                canvas.drawRect(bounds, bgPaint)
            } catch (e: Exception) { }
        }

        if (textDef.hasOutline) {
            try {
                val outlinePaint = Paint().apply {
                    color = Color.parseColor(textDef.outlineColorHex)
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                canvas.drawRect(bounds, outlinePaint)
            } catch (e: Exception) { }
        }

        for ((lineIdx, lineText) in m.lines.withIndex()) {
            val lineY = ry + lineIdx * m.lineHeight
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
                        color = Color.parseColor(textDef.colorHex); style = Paint.Style.STROKE; strokeWidth = 1.5f
                    }
                    canvas.drawLine(lineRxStart, lineY + 3f, lineRxEnd, lineY + 3f, underlinePaint)
                } catch (e: Exception) { }
            }

            if (textDef.hasStrikeThrough) {
                try {
                    val middleY = lineY + (m.ascent + m.descent) / 2f
                    val strikePaint = Paint().apply {
                        color = Color.parseColor(textDef.colorHex); style = Paint.Style.STROKE; strokeWidth = 1.5f
                    }
                    canvas.drawLine(lineRxStart, middleY, lineRxEnd, middleY, strikePaint)
                } catch (e: Exception) { }
            }

            canvas.drawText(lineText, baseRx, lineY, textPaint)
        }

        return bounds
    }
}
