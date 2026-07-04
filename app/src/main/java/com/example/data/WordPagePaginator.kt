package com.example.data

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints

/**
 * Splits a block of text into page-sized chunks so each chunk's rendered
 * height fits inside a single A4 content box, given a fixed width/style.
 */
@OptIn(ExperimentalTextApi::class)
object WordPagePaginator {

    fun paginate(
        measurer: TextMeasurer,
        fullText: String,
        style: TextStyle,
        contentWidthPx: Int,
        contentHeightPx: Int
    ): List<String> {
        if (fullText.isEmpty()) return listOf("")
        if (contentWidthPx <= 0 || contentHeightPx <= 0) return listOf(fullText)

        val layout = measurer.measure(
            text = fullText,
            style = style,
            constraints = Constraints(maxWidth = contentWidthPx)
        )

        if (layout.lineCount == 0) return listOf(fullText)

        val lineHeightPx = layout.getLineBottom(0) - layout.getLineTop(0)
        val linesPerPage = maxOf(1, (contentHeightPx / lineHeightPx).toInt())

        if (layout.lineCount <= linesPerPage) return listOf(fullText)

        val chunks = mutableListOf<String>()
        var start = 0
        var lineIndex = 0
        while (lineIndex < layout.lineCount) {
            val endLine = minOf(lineIndex + linesPerPage, layout.lineCount) - 1
            val end = layout.getLineEnd(endLine, visibleEnd = false)
            chunks.add(fullText.substring(start, end))
            start = end
            lineIndex += linesPerPage
        }
        if (start < fullText.length) chunks.add(fullText.substring(start))
        return chunks
    }
}
