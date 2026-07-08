package com.example.data

import android.graphics.Path

/**
 * Rebuilds a smoothed signature path from normalized (0..1) points, using the
 * same quadratic-curve smoothing as the live Signature Studio canvas. Use this
 * everywhere a saved signature is redrawn so it always matches what was drawn.
 *
 * Points of exactly (-1, -1) mark a pen lift / new sub-stroke boundary.
 */
object SignaturePathUtils {
    const val STUDIO_CANVAS_ASPECT_RATIO = 2.1f

    /**
     * Logical canvas width the Signature Studio and every downstream renderer scale
     * stroke thickness against. Every place that draws a saved signature (the editor
     * overlay, the final PDF export) MUST use this same base so thickness stays
     * consistent from the moment it's drawn to the moment it's saved.
     */
    const val BASE_SCALE_WIDTH = 500f

    /** Extra thickness multiplier per pen type, matching Signature Studio's live canvas. */
    fun thicknessMultiplier(penType: String): Float = when (penType) {
        "highlighter" -> 4.5f
        "calligraphy" -> 1.3f
        else -> 1f
    }

    /** Stroke alpha (0..255) for a given pen type. */
    fun alphaForPenType(penType: String): Int = if (penType == "highlighter") 90 else 255

    /** Dash effect intervals, or null if the pen type isn't dashed. */
    fun dashIntervals(penType: String): FloatArray? =
        if (penType == "dashed") floatArrayOf(15f, 15f) else null

    fun buildSmoothedPath(points: List<PointDef>, width: Float, height: Float): Path {
        val path = Path()
        var subStroke = mutableListOf<PointDef>()

        fun flushSubStroke() {
            if (subStroke.isEmpty()) return
            val first = subStroke.first()
            path.moveTo(first.x * width, first.y * height)
            if (subStroke.size > 1) {
                for (i in 1 until subStroke.size) {
                    val p = subStroke[i]
                    val prev = subStroke[i - 1]
                    val midX = (prev.x + p.x) / 2f * width
                    val midY = (prev.y + p.y) / 2f * height
                    path.quadTo(prev.x * width, prev.y * height, midX, midY)
                }
                val last = subStroke.last()
                path.lineTo(last.x * width, last.y * height)
            }
            subStroke = mutableListOf()
        }

        for (pt in points) {
            if (pt.x == -1f && pt.y == -1f) {
                flushSubStroke()
            } else {
                subStroke.add(pt)
            }
        }
        flushSubStroke()
        return path
    }
}
