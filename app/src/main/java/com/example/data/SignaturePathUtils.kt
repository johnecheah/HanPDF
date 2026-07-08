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
