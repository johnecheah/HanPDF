package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

// --- DATA CLASSES FOR INTERACTIVE DOCUMENT CONTENT ---

data class PointDef(
    val x: Float,
    val y: Float
)

data class DrawingDef(
    val points: List<PointDef>,
    val colorHex: String,
    val strokeWidth: Float,
    val isHighlighter: Boolean = false,
    val isDashed: Boolean = false,
    val id: String = "",
    val shapeType: String = "freehand"
)

data class TextAnnotationDef(
    val id: String,
    val text: String,
    val x: Float,
    val y: Float,
    val fontSize: Float = 14f,
    val colorHex: String = "#000000",
    val fontName: String = "sans-serif",
    val bgColorHex: String = "transparent",
    val hasOutline: Boolean = false,
    val hasUnderline: Boolean = false,
    val outlineColorHex: String = "#000000",
    val hasDoubleUnderline: Boolean = false,
    val isBold: Boolean = false,
    val alignment: String = "left", // "left", "center", "right"
    val isPowerOf: Boolean = false,
    val isItalic: Boolean = false,
    val hasStrikeThrough: Boolean = false
)

data class SignatureOverlayDef(
    val id: String,
    val x: Float,
    val y: Float,
    val width: Float = 110f,
    val height: Float = 55f,
    val signatureProfileId: Int, // Refers to the drawn signature entity
    val colorHex: String = "#000000"
)

data class PageDef(
    val id: String,
    val pageNumber: Int,
    val type: String = "blank", // "blank", "lined", "cornell", "meeting", "scan", "id_card"
    val backgroundScanPath: String? = null, // Set if page is scanned from camera
    val drawings: List<DrawingDef> = emptyList(),
    val textAnnotations: List<TextAnnotationDef> = emptyList(),
    val signatures: List<SignatureOverlayDef> = emptyList(),
    val ocrText: String? = null, // Extracted text content for OCR page index
    val rotationDegrees: Int = 0,
    val filterType: String = "original"
)

data class DocumentContent(
    val pages: List<PageDef> = emptyList()
)

// --- LIGHTWEIGHT SERIALIZATION HELPER ---

object DocumentSerializer {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(DocumentContent::class.java)

    fun toJson(content: DocumentContent): String {
        return try {
            adapter.toJson(content)
        } catch (e: Exception) {
            "{}"
        }
    }

    fun fromJson(json: String): DocumentContent {
        if (json.isBlank()) return DocumentContent(pages = listOf(PageDef("1", 1)))
        return try {
            adapter.fromJson(json) ?: DocumentContent()
        } catch (e: Exception) {
            DocumentContent()
        }
    }

    // List of point parser
    private val pointListAdapter = moshi.adapter<List<PointDef>>(
        Types.newParameterizedType(List::class.java, PointDef::class.java)
    )

    fun pointsToJson(points: List<PointDef>): String {
        return try {
            pointListAdapter.toJson(points)
        } catch (e: Exception) {
            "[]"
        }
    }

    fun pointsFromJson(json: String): List<PointDef> {
        if (json.isBlank()) return emptyList()
        return try {
            pointListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
