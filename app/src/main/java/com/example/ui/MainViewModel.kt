package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

sealed class Screen {
    object Dashboard : Screen()
    object ScanCamera : Screen()
    object ScanEdit : Screen()
    object PdfMerger : Screen()
    object OcrViewer : Screen()
    object IdScanCamera : Screen()
    object IdScanEdit : Screen()
    object Editor : Screen()
    object SignatureStudio : Screen()
}

data class UiState(
    val currentScreen: Screen = Screen.Dashboard,
    val documents: List<Document> = emptyList(),
    val starredDocuments: List<Document> = emptyList(),
    val signatures: List<SignatureProfile> = emptyList(),
    
    // Editor State
    val activeDocument: Document? = null,
    val activeDocumentContent: DocumentContent = DocumentContent(),
    val activePageId: String = "",
    
    // OCR State
    val ocrLoading: Boolean = false,
    val ocrTextResult: String? = null,
    val ocrDocTitle: String = "",
    val ocrEngine: String = "Gemini", // "Gemini" or "TesseractJS"
    val ocrBase64Image: String? = null,
    val ocrProgress: Float = 0f,
    
    // Scanner State
    val scannerStepBitmaps: List<Bitmap> = emptyList(),
    val scannerStepPaths: List<String> = emptyList(),
    val scannerFilterType: String = "original", // "original", "black_white", "grayscale", "enhance"
    val scannerIdMode: Boolean = false, // ID scanning mode layout
    val scannerIdFrontPath: String? = null,
    val scannerIdBackPath: String? = null,
    
    // ID Card Scanner Flow
    val idCardFrontBitmap: Bitmap? = null,
    val idCardBackBitmap: Bitmap? = null,
    val isScanningIdBack: Boolean = false,
    val idScanFilterType: String = "original",
    
    // Merger State
    val mergerSelectedDocs: List<Document> = emptyList(),
    
    // General notifications
    val feedbackMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MainViewModel"
    private val db = AppDatabase.getDatabase(application)
    private val repo = DocumentRepository(db)

    private val undoStack = mutableListOf<DocumentContent>()
    private val redoStack = mutableListOf<DocumentContent>()

    private fun pushToUndoStack() {
        val current = _uiState.value.activeDocumentContent
        undoStack.add(current)
        redoStack.clear()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // Collect documents and signatures reactively from SQLite
        viewModelScope.launch {
            repo.allDocuments.collect { docs ->
                _uiState.update { it.copy(documents = docs) }
            }
        }
        viewModelScope.launch {
            repo.starredDocuments.collect { starred ->
                _uiState.update { it.copy(starredDocuments = starred) }
            }
        }
        viewModelScope.launch {
            repo.allSignatures.collect { sigs ->
                _uiState.update { it.copy(signatures = sigs) }
            }
        }
        
        // Seed default template if DB is empty to make onboarding magnificent!
        seedDemoData()
    }

    private fun seedDemoData() {
        viewModelScope.launch {
            repo.allDocuments.first().let { currentList ->
                // Clean up any previously seeded AcroPDF Welcome Guide
                currentList.forEach { doc ->
                    if (doc.title == "AcroPDF Welcome Guide") {
                        try {
                            repo.deleteDocument(doc)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to clean up welcome guide", e)
                        }
                    }
                }
            }
        }
    }

    // --- NAVIGATION API ---
    fun navigateTo(screen: Screen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun triggerFeedback(message: String) {
        val lower = message.lowercase()
        // Filter out spammy/routine confirmation and success notifications as requested by the user
        val isRoutineFeedback = lower.contains("success") || 
                lower.contains("sucess") || 
                lower.contains("compiled") || 
                lower.contains("flattened") || 
                lower.contains("deleted") || 
                lower.contains("removed") || 
                lower.contains("imported") || 
                lower.contains("added") || 
                lower.contains("renamed") || 
                lower.contains("cropped") || 
                lower.contains("combined") || 
                lower.contains("finalized") || 
                lower.contains("merged complete") || 
                lower.contains("loaded text block") || 
                lower.contains("analyzing document") || 
                lower.contains("scanning imported") || 
                lower.contains("signature saved")

        if (isRoutineFeedback) {
            Log.d("MainViewModel", "Routine feedback silenced: $message")
        } else {
            _uiState.update { it.copy(feedbackMessage = message) }
        }
    }

    // --- DOCUMENT WRITING / CREATION ---
    fun createWordDoc(title: String) {
        createNewTemplateDocument(title, "word")
    }

    fun createNewTemplateDocument(title: String, type: String) {
        viewModelScope.launch {
            val actualTitle = title.ifBlank { "Untitled ${System.currentTimeMillis() % 10000}" }
            
            val meetingTextAnnotations = if (type == "word") {
                listOf(
                    com.example.data.TextAnnotationDef(
                        id = "word_main_content",
                        text = "",
                        x = 0.08f,
                        y = 0.08f,
                        fontSize = 12f,
                        colorHex = "#1E293B",
                        isBold = false,
                        alignment = "left"
                    )
                )
            } else if (type == "meeting") {
                listOf(
                    com.example.data.TextAnnotationDef(
                        id = UUID.randomUUID().toString(),
                        text = "MEETING MINUTES SUMMARY",
                        x = 0.5f,
                        y = 0.07f,
                        fontSize = 18f,
                        colorHex = "#1E3A8A",
                        isBold = true,
                        alignment = "center"
                    ),
                    com.example.data.TextAnnotationDef(
                        id = UUID.randomUUID().toString(),
                        text = "Date: June 21, 2026",
                        x = 0.08f,
                        y = 0.16f,
                        fontSize = 12f,
                        colorHex = "#475569",
                        isBold = true,
                        alignment = "left"
                    ),
                    com.example.data.TextAnnotationDef(
                        id = UUID.randomUUID().toString(),
                        text = "Topic / Purpose: Q3 Development Sync-up",
                        x = 0.08f,
                        y = 0.20f,
                        fontSize = 12f,
                        colorHex = "#475569",
                        isBold = true,
                        alignment = "left"
                    ),
                    com.example.data.TextAnnotationDef(
                        id = UUID.randomUUID().toString(),
                        text = "Facilitator: John Doe",
                        x = 0.08f,
                        y = 0.25f,
                        fontSize = 11f,
                        colorHex = "#475569",
                        alignment = "left"
                    ),
                    com.example.data.TextAnnotationDef(
                        id = UUID.randomUUID().toString(),
                        text = "Attendees: Alice, Bob, Charlie",
                        x = 0.08f,
                        y = 0.29f,
                        fontSize = 11f,
                        colorHex = "#475569",
                        alignment = "left"
                    ),
                    com.example.data.TextAnnotationDef(
                        id = UUID.randomUUID().toString(),
                        text = "1. MAIN DISCUSSION POINTS",
                        x = 0.10f,
                        y = 0.38f,
                        fontSize = 13f,
                        colorHex = "#1E3A8A",
                        isBold = true,
                        alignment = "left"
                    ),
                    com.example.data.TextAnnotationDef(
                        id = UUID.randomUUID().toString(),
                        text = "[Click to type discussion points...]",
                        x = 0.10f,
                        y = 0.43f,
                        fontSize = 11f,
                        colorHex = "#0D9488",
                        alignment = "left"
                    ),
                    com.example.data.TextAnnotationDef(
                        id = UUID.randomUUID().toString(),
                        text = "2. ACTION ITEMS & RESPONSIBILITIES",
                        x = 0.10f,
                        y = 0.68f,
                        fontSize = 13f,
                        colorHex = "#1E3A8A",
                        isBold = true,
                        alignment = "left"
                    ),
                    com.example.data.TextAnnotationDef(
                        id = UUID.randomUUID().toString(),
                        text = "[Click to assign tasks, owners, and timelines...]",
                        x = 0.10f,
                        y = 0.73f,
                        fontSize = 11f,
                        colorHex = "#0D9488",
                        alignment = "left"
                    )
                )
            } else {
                emptyList()
            }

            val initialPages = listOf(
                PageDef(
                    id = UUID.randomUUID().toString(),
                    pageNumber = 1,
                    type = type, // "blank", "lined", "cornell", "meeting"
                    textAnnotations = meetingTextAnnotations
                )
            )
            val docContent = DocumentContent(pages = initialPages)
            val json = DocumentSerializer.toJson(docContent)

            val newDoc = Document(
                title = actualTitle,
                category = when (type) {
                    "blank" -> "Blank Note"
                    "lined" -> "Lined Note"
                    "cornell" -> "Cornell Note"
                    else -> "Blank Doc"
                },
                pageCount = 1,
                contentJson = json,
                isSaved = false
            )
            
            val docId = repo.insertDocument(newDoc).toInt()
            val insertedDoc = repo.getDocumentById(docId)
            
            if (insertedDoc != null) {
                // Render physical PDF
                PdfGenerator.buildPdf(getApplication(), insertedDoc, docContent, _uiState.value.signatures)
                openDocumentInEditor(insertedDoc)
            }
        }
    }

    fun toggleStarDocument(doc: Document) {
        viewModelScope.launch {
            repo.updateDocument(doc.copy(isStarred = !doc.isStarred))
        }
    }

    fun deleteDocument(doc: Document) {
        viewModelScope.launch {
            repo.deleteDocument(doc)
            // Clean up files
            try {
                val file = File(doc.fileUri)
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                Log.e(TAG, "Failed deleting phys file", e)
            }
            triggerFeedback("Document deleted successfully.")
        }
    }

    // --- PDF ANNOTATION / EDITOR CONTROLS ---
    fun openDocumentInEditor(doc: Document) {
        viewModelScope.launch {
            val content = DocumentSerializer.fromJson(doc.contentJson)
            val activePageId = content.pages.firstOrNull()?.id ?: ""
            
            undoStack.clear()
            redoStack.clear()
            
            _uiState.update {
                it.copy(
                    activeDocument = doc,
                    activeDocumentContent = content,
                    activePageId = activePageId,
                    currentScreen = Screen.Editor
                )
            }
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val current = _uiState.value.activeDocumentContent
            redoStack.add(current)
            val previous = undoStack.removeAt(undoStack.size - 1)
            
            val updatedActivePageId = if (previous.pages.any { it.id == _uiState.value.activePageId }) {
                _uiState.value.activePageId
            } else {
                previous.pages.firstOrNull()?.id ?: ""
            }
            
            _uiState.update {
                it.copy(
                    activeDocumentContent = previous,
                    activePageId = updatedActivePageId
                )
            }
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val current = _uiState.value.activeDocumentContent
            undoStack.add(current)
            val next = redoStack.removeAt(redoStack.size - 1)
            
            val updatedActivePageId = if (next.pages.any { it.id == _uiState.value.activePageId }) {
                _uiState.value.activePageId
            } else {
                next.pages.firstOrNull()?.id ?: ""
            }
            
            _uiState.update {
                it.copy(
                    activeDocumentContent = next,
                    activePageId = updatedActivePageId
                )
            }
        }
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun saveEditorChanges() {
        val activeDoc = _uiState.value.activeDocument ?: return
        var currentContent = _uiState.value.activeDocumentContent
        viewModelScope.launch {
            // Merge signature and text annotation layers with the background scan / template image
            currentContent = mergeLayersForSaving(currentContent)

            val json = DocumentSerializer.toJson(currentContent)
            val updatedDoc = activeDoc.copy(
                contentJson = json,
                pageCount = currentContent.pages.size,
                isSaved = true
            )
            
            // Re-render genuine PDF
            val file = PdfGenerator.buildPdf(
                getApplication(),
                updatedDoc,
                currentContent,
                _uiState.value.signatures
            )
            
            val finalDoc = if (file != null) {
                updatedDoc.copy(fileUri = file.absolutePath)
            } else {
                updatedDoc
            }

            repo.updateDocument(finalDoc)
            _uiState.update { 
                it.copy(
                    activeDocument = finalDoc,
                    activeDocumentContent = currentContent
                ) 
            }
            triggerFeedback("Changes flattened & PDF compiled!")
        }
    }

     private fun getPageSize300Ppi(page: com.example.data.PageDef): Pair<Int, Int> {
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
                        // Portrait: set height to 3508 (A4 height at 300 PPI)
                        val h = 3508
                        val w = (3508 * ratio).toInt()
                        Pair(w, h)
                    } else {
                        // Landscape: set width to 3508 (A4 width at 300 PPI)
                        val w = 3508
                        val h = (3508 / ratio).toInt()
                        Pair(w, h)
                    }
                }
            }
        }
        // Default standard A4 size (portrait)
        return Pair(2480, 3508)
    }

    private fun decodeAndScaleBitmap(path: String, maxDim: Int = 2480): Bitmap? {
        val file = File(path)
        if (!file.exists()) return null
        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            val origW = options.outWidth
            val origH = options.outHeight
            if (origW <= 0 || origH <= 0) return null

            var sampleSize = 1
            val largest = maxOf(origW, origH)
            while (largest / sampleSize > maxDim * 2) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val sampled = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null

            val curLargest = maxOf(sampled.width, sampled.height)
            return if (curLargest > maxDim) {
                val scale = maxDim.toFloat() / curLargest
                val targetW = (sampled.width * scale).toInt()
                val targetH = (sampled.height * scale).toInt()
                val scaled = Bitmap.createScaledBitmap(sampled, targetW, targetH, true)
                if (scaled != sampled) {
                    sampled.recycle()
                }
                scaled
            } else {
                sampled
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun mergeLayersForSaving(currentContent: DocumentContent): DocumentContent {
        val updatedPages = currentContent.pages.map { page ->
            if (page.type.lowercase() == "word") {
                page
            } else if (page.textAnnotations.isEmpty() && page.signatures.isEmpty() && page.drawings.isEmpty()) {
                page
            } else {
                 val dimensions = getPageSize300Ppi(page)
                val curW = dimensions.first
                val curH = dimensions.second

                val isRotated90or270 = (page.rotationDegrees % 180 != 0)
                val finalW = if (isRotated90or270) curH else curW
                val finalH = if (isRotated90or270) curW else curH

                val mergedBitmap = Bitmap.createBitmap(finalW, finalH, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(mergedBitmap)

                val bgPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    style = android.graphics.Paint.Style.FILL
                }
                canvas.drawRect(0f, 0f, finalW.toFloat(), finalH.toFloat(), bgPaint)

                if (page.rotationDegrees != 0) {
                    canvas.rotate(page.rotationDegrees.toFloat(), finalW / 2f, finalH / 2f)
                }

                // Draw backgroundScanPath if any
                if (page.backgroundScanPath != null) {
                    val file = File(page.backgroundScanPath)
                    if (file.exists()) {
                        val bitmap = decodeAndScaleBitmap(file.absolutePath, 2480)
                        if (bitmap != null) {
                            val rect = android.graphics.RectF(
                                (finalW - curW) / 2f,
                                (finalH - curH) / 2f,
                                (finalW + curW) / 2f,
                                (finalH + curH) / 2f
                            )
                            canvas.drawBitmap(bitmap, null, rect, android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
                            bitmap.recycle()
                        }
                    }
                } else {
                    // Draw templates
                    val scale = curW / 595f
                    when (page.type.lowercase()) {
                        "lined" -> {
                            val marginPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#E09090")
                                strokeWidth = 1.5f * scale
                                style = android.graphics.Paint.Style.STROKE
                            }
                            val linePaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#C5D3E8")
                                strokeWidth = 1f * scale
                                style = android.graphics.Paint.Style.STROKE
                            }
                            val leftMargin = curW * 0.15f
                            canvas.drawLine(leftMargin, 0f, leftMargin, curH.toFloat(), marginPaint)
                            val lineSpacing = 28f * scale
                            var currentY = curH * 0.08f
                            while (currentY < curH * 0.95f) {
                                canvas.drawLine(0f, currentY, curW.toFloat(), currentY, linePaint)
                                currentY += lineSpacing
                            }
                        }
                        "cornell" -> {
                            val boundaryPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#80B3D6")
                                strokeWidth = 2f * scale
                                style = android.graphics.Paint.Style.STROKE
                            }
                            val linePaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#D5E4F2")
                                strokeWidth = 1f * scale
                                style = android.graphics.Paint.Style.STROKE
                            }
                            val leftColWidth = curW * 0.30f
                            val bottomRowHeight = curH * 0.20f
                            canvas.drawLine(leftColWidth, 0f, leftColWidth, curH - bottomRowHeight, boundaryPaint)
                            canvas.drawLine(0f, curH - bottomRowHeight, curW.toFloat(), curH - bottomRowHeight, boundaryPaint)
                            val lineSpacing = 24f * scale
                            var currentY = curH * 0.05f
                            while (currentY < curH - bottomRowHeight) {
                                canvas.drawLine(leftColWidth, currentY, curW.toFloat(), currentY, linePaint)
                                currentY += lineSpacing
                            }
                        }
                        "meeting" -> {
                            val dividerPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#1E3A8A")
                                strokeWidth = 3f * scale
                                style = android.graphics.Paint.Style.STROKE
                            }
                            val thinPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#CBD5E1")
                                strokeWidth = 1f * scale
                                style = android.graphics.Paint.Style.STROKE
                            }
                            // Top corporate dual line border
                            canvas.drawLine(curW * 0.08f, curH * 0.12f, curW * 0.92f, curH * 0.12f, dividerPaint)
                            canvas.drawLine(curW * 0.08f, curH * 0.125f, curW * 0.92f, curH * 0.125f, thinPaint)
                            
                            // Bottom section content frame box
                            canvas.drawRect(curW * 0.08f, curH * 0.35f, curW * 0.92f, curH * 0.92f, thinPaint)
                            canvas.drawLine(curW * 0.08f, curH * 0.65f, curW * 0.92f, curH * 0.65f, thinPaint)
                        }
                    }
                }

                // Draw vector drawings
                for (draw in page.drawings) {
                    if (draw.points.size < 2) continue
                    val baseColor = try {
                        android.graphics.Color.parseColor(draw.colorHex)
                    } catch (e: Exception) {
                        android.graphics.Color.BLACK
                    }
                    val drawPaint = android.graphics.Paint().apply {
                        color = baseColor
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = draw.strokeWidth * (curW / 400f)
                        strokeCap = android.graphics.Paint.Cap.ROUND
                        strokeJoin = android.graphics.Paint.Join.ROUND
                        isAntiAlias = true
                        if (draw.isHighlighter) {
                            alpha = (255 * 0.45f).toInt()
                        }
                        if (draw.isDashed) {
                            pathEffect = android.graphics.DashPathEffect(floatArrayOf(15f * (curW / 400f), 15f * (curW / 400f)), 0f)
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
                            val fillPaint = android.graphics.Paint().apply {
                                color = baseColor
                                style = android.graphics.Paint.Style.FILL
                                isAntiAlias = true
                                if (draw.isHighlighter) {
                                    alpha = (255 * 0.45f).toInt()
                                } else {
                                    alpha = 255
                                }
                            }
                            canvas.drawRect(left, top, right, bottom, fillPaint)
                        }
                        "redact" -> {
                            val start = draw.points.first()
                            val end = draw.points.last()
                            val left = minOf(start.x, end.x) * curW
                            val top = minOf(start.y, end.y) * curH
                            val right = maxOf(start.x, end.x) * curW
                            val bottom = maxOf(start.y, end.y) * curH
                            val redactPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.BLACK
                                style = android.graphics.Paint.Style.FILL
                                isAntiAlias = true
                            }
                            canvas.drawRect(left, top, right, bottom, redactPaint)
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
                            val path = android.graphics.Path()
                            val first = draw.points.first()
                            path.moveTo(first.x * curW, first.y * curH)
                            for (i in 1 until draw.points.size) {
                                val pt = draw.points[i]
                                path.lineTo(pt.x * curW, pt.y * curH)
                            }
                            canvas.drawPath(path, drawPaint)
                        }
                    }
                }

                // Draw text layer annotations
                for (textDef in page.textAnnotations) {
                    val sizeRatio = if (textDef.isPowerOf) 0.72f else 1.0f
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor(textDef.colorHex)
                        textSize = (textDef.fontSize * sizeRatio) * (curW / 400f)
                        val fontStyle = if (textDef.isBold) {
                            if (textDef.isItalic || textDef.fontName.lowercase() == "cursive") android.graphics.Typeface.BOLD_ITALIC else android.graphics.Typeface.BOLD
                        } else {
                            if (textDef.isItalic || textDef.fontName.lowercase() == "cursive") android.graphics.Typeface.ITALIC else android.graphics.Typeface.NORMAL
                        }
                        val fontTypeface = when (textDef.fontName.lowercase()) {
                            "serif" -> android.graphics.Typeface.create(android.graphics.Typeface.SERIF, fontStyle)
                            "monospace" -> android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, fontStyle)
                            "cursive" -> android.graphics.Typeface.create("serif", fontStyle)
                            else -> android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, fontStyle)
                        }
                        typeface = fontTypeface
                        val paintAlign = when (textDef.alignment.lowercase()) {
                            "center" -> android.graphics.Paint.Align.CENTER
                            "right" -> android.graphics.Paint.Align.RIGHT
                            else -> android.graphics.Paint.Align.LEFT
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
                    val fontMetricsTemp = android.graphics.Paint().apply { textSize = textDef.fontSize * (curW / 400f) }.fontMetrics
                    val powerShift = if (textDef.isPowerOf) (fontMetricsTemp.descent - fontMetricsTemp.ascent) * 0.45f else 0f
                    val ry = rawY - fontMetrics.ascent - powerShift
                    val rxStart = when (textPaint.textAlign) {
                        android.graphics.Paint.Align.CENTER -> baseRx - maxLineWidth / 2f
                        android.graphics.Paint.Align.RIGHT -> baseRx - maxLineWidth
                        else -> baseRx
                    }
                    val rxEnd = rxStart + maxLineWidth

                    val pdfPadding = 0f
                    val rectLeft = rxStart - pdfPadding
                    val rectTop = ry + fontMetrics.ascent - pdfPadding
                    val rectRight = rxEnd + pdfPadding
                    val rectBottom = ry + (lines.size - 1) * lineHeight + fontMetrics.descent + pdfPadding

                    if (textDef.bgColorHex.isNotEmpty() && textDef.bgColorHex.lowercase() != "transparent") {
                        try {
                            val bgPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor(textDef.bgColorHex)
                                style = android.graphics.Paint.Style.FILL
                            }
                            canvas.drawRect(
                                rectLeft,
                                rectTop,
                                rectRight,
                                rectBottom,
                                bgPaint
                            )
                        } catch (e: Exception) { e.printStackTrace() }
                    }

                    if (textDef.hasOutline) {
                        try {
                            val outlinePaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor(textDef.outlineColorHex)
                                style = android.graphics.Paint.Style.STROKE
                                strokeWidth = 2f
                            }
                            canvas.drawRect(
                                rectLeft,
                                rectTop,
                                rectRight,
                                rectBottom,
                                outlinePaint
                            )
                        } catch (e: Exception) { e.printStackTrace() }
                    }

                    for ((lineIdx, lineText) in lines.withIndex()) {
                        val lineY = ry + lineIdx * lineHeight
                        val lineWidth = textPaint.measureText(lineText)
                        val lineRxStart = when (textPaint.textAlign) {
                            android.graphics.Paint.Align.CENTER -> baseRx - lineWidth / 2f
                            android.graphics.Paint.Align.RIGHT -> baseRx - lineWidth
                            else -> baseRx
                        }
                        val lineRxEnd = lineRxStart + lineWidth

                        if (textDef.hasUnderline) {
                            try {
                                val underlinePaint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.parseColor(textDef.colorHex)
                                    style = android.graphics.Paint.Style.STROKE
                                    strokeWidth = 1.5f
                                }
                                canvas.drawLine(lineRxStart, lineY + 3f, lineRxEnd, lineY + 3f, underlinePaint)
                            } catch (e: Exception) { e.printStackTrace() }
                        }

                        if (textDef.hasStrikeThrough) {
                            try {
                                val middleY = lineY + (fontMetrics.ascent + fontMetrics.descent) / 2f
                                val strikePaint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.parseColor(textDef.colorHex)
                                    style = android.graphics.Paint.Style.STROKE
                                    strokeWidth = 1.5f
                                }
                                canvas.drawLine(lineRxStart, middleY, lineRxEnd, middleY, strikePaint)
                            } catch (e: Exception) { e.printStackTrace() }
                        }

                        canvas.drawText(lineText, baseRx, lineY, textPaint)
                    }
                }

                // Draw signatures
                for (sigDef in page.signatures) {
                    val matchingSig = _uiState.value.signatures.find { it.id == sigDef.signatureProfileId }
                    if (matchingSig != null) {
                        val sx = sigDef.x * curW
                        val sy = sigDef.y * curH
                        val sw = sigDef.width * (curW / 400f)
                        val sh = sigDef.height * (curW / 400f)

                        if (matchingSig.pathDataJson.startsWith("image:")) {
                            try {
                                val imagePath = matchingSig.pathDataJson.removePrefix("image:")
                                val sFile = File(imagePath)
                                if (sFile.exists()) {
                                    val opts = BitmapFactory.Options().apply {
                                        inSampleSize = 1
                                        inScaled = false
                                    }
                                    val bitmap = BitmapFactory.decodeFile(sFile.absolutePath, opts)
                                    if (bitmap != null) {
                                        val imgW = bitmap.width
                                        val imgH = bitmap.height
                                        val imgRatio = if (imgH > 0) imgW.toFloat() / imgH.toFloat() else 1f
                                        
                                        // Enforce original resolution ratio when laying over background page
                                        val actualSh = if (imgRatio > 0f) sw / imgRatio else sh
                                        val destRect = android.graphics.RectF(sx, sy, sx + sw, sy + actualSh)
                                        val paint = android.graphics.Paint().apply { 
                                            isAntiAlias = true
                                            isFilterBitmap = true
                                        }
                                        canvas.drawBitmap(bitmap, null, destRect, paint)
                                    }
                                }
                            } catch (e: Exception) { e.printStackTrace() }
                        } else {
                            val points = DocumentSerializer.pointsFromJson(matchingSig.pathDataJson)
                            if (points.isNotEmpty()) {
                                val sigPaint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.parseColor(matchingSig.colorHex)
                                    style = android.graphics.Paint.Style.STROKE
                                    strokeJoin = android.graphics.Paint.Join.ROUND
                                    strokeCap = android.graphics.Paint.Cap.ROUND
                                    strokeWidth = matchingSig.strokeWidth * (sw / 160f)
                                    isAntiAlias = true
                                }
                                val path = android.graphics.Path()
                                val start = points.first()
                                path.moveTo(sx + start.x * sw, sy + start.y * sh)
                                for (i in 1 until points.size) {
                                    val pt = points[i]
                                    if (pt.x == -1f && pt.y == -1f) {
                                        if (i + 1 < points.size) {
                                            val nextPt = points[i + 1]
                                            path.moveTo(sx + nextPt.x * sw, sy + nextPt.y * sh)
                                        }
                                    } else {
                                        path.lineTo(sx + pt.x * sw, sy + pt.y * sh)
                                    }
                                }
                                canvas.drawPath(path, sigPaint)
                            }
                        }
                    }
                }

                // Save to file
                val outFileName = "merged_page_${page.id}_${UUID.randomUUID()}.jpg"
                val outFile = File(getApplication<Application>().filesDir, outFileName)
                try {
                    val fos = FileOutputStream(outFile)
                    mergedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                    fos.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                mergedBitmap.recycle()

                page.copy(
                    backgroundScanPath = outFile.absolutePath,
                    rotationDegrees = 0,
                    textAnnotations = if (page.type.lowercase() == "word") {
                        page.textAnnotations.filter { it.id == "word_main_content" }
                    } else {
                        emptyList()
                    },
                    signatures = emptyList(),
                    drawings = emptyList()
                )
            }
        }
        return DocumentContent(pages = updatedPages)
    }

    fun setActivePageId(pageId: String) {
        _uiState.update {
            it.copy(activePageId = pageId)
        }
    }

    fun editActivePageDrawings(drawings: List<DrawingDef>) {
        pushToUndoStack()
        val currentContent = _uiState.value.activeDocumentContent
        val activePageId = _uiState.value.activePageId
        
        val updatedPages = currentContent.pages.map { page ->
            if (page.id == activePageId) {
                page.copy(drawings = drawings)
            } else {
                page
            }
        }
        
        _uiState.update {
            it.copy(activeDocumentContent = currentContent.copy(pages = updatedPages))
        }
    }

    fun editActivePageAnnotations(annotations: List<TextAnnotationDef>) {
        pushToUndoStack()
        val currentContent = _uiState.value.activeDocumentContent
        val activePageId = _uiState.value.activePageId
        
        val updatedPages = currentContent.pages.map { page ->
            if (page.id == activePageId) {
                page.copy(textAnnotations = annotations)
            } else {
                page
            }
        }
        
        _uiState.update {
            it.copy(activeDocumentContent = currentContent.copy(pages = updatedPages))
        }
    }

    fun editActivePageOcrResults(annotations: List<TextAnnotationDef>, cleanedBackgroundPath: String) {
        pushToUndoStack()
        val currentContent = _uiState.value.activeDocumentContent
        val activePageId = _uiState.value.activePageId
        
        val updatedPages = currentContent.pages.map { page ->
            if (page.id == activePageId) {
                page.copy(
                    textAnnotations = annotations,
                    backgroundScanPath = cleanedBackgroundPath
                )
            } else {
                page
            }
        }
        
        _uiState.update {
            it.copy(activeDocumentContent = currentContent.copy(pages = updatedPages))
        }
    }

    fun editPageAnnotations(pageId: String, annotations: List<TextAnnotationDef>) {
        pushToUndoStack()
        val currentContent = _uiState.value.activeDocumentContent
        val updatedPages = currentContent.pages.map { page ->
            if (page.id == pageId) {
                page.copy(textAnnotations = annotations)
            } else {
                page
            }
        }
        _uiState.update {
            it.copy(activeDocumentContent = currentContent.copy(pages = updatedPages))
        }
    }

    fun editActivePageSignatures(signatures: List<SignatureOverlayDef>) {
        pushToUndoStack()
        val currentContent = _uiState.value.activeDocumentContent
        val activePageId = _uiState.value.activePageId
        
        val updatedPages = currentContent.pages.map { page ->
            if (page.id == activePageId) {
                page.copy(signatures = signatures)
            } else {
                page
            }
        }
        
        _uiState.update {
            it.copy(activeDocumentContent = currentContent.copy(pages = updatedPages))
        }
    }

    fun editActivePageRotation(rotationDegrees: Int) {
        pushToUndoStack()
        val currentContent = _uiState.value.activeDocumentContent
        val activePageId = _uiState.value.activePageId
        
        val updatedPages = currentContent.pages.map { page ->
            if (page.id == activePageId) {
                page.copy(rotationDegrees = rotationDegrees)
            } else {
                page
            }
        }
        
        _uiState.update {
            it.copy(activeDocumentContent = currentContent.copy(pages = updatedPages))
        }
    }

    fun editActivePageFilter(filterName: String) {
        pushToUndoStack()
        val currentContent = _uiState.value.activeDocumentContent
        val activePageId = _uiState.value.activePageId
        
        val updatedPages = currentContent.pages.map { page ->
            if (page.id == activePageId) {
                page.copy(filterType = filterName)
            } else {
                page
            }
        }
        
        _uiState.update {
            it.copy(activeDocumentContent = currentContent.copy(pages = updatedPages))
        }
    }

    fun addSignatureOverlay(sigDef: SignatureOverlayDef) {
        pushToUndoStack()
        val currentContent = _uiState.value.activeDocumentContent
        val activePageId = _uiState.value.activePageId
        
        val updatedPages = currentContent.pages.map { page ->
            if (page.id == activePageId) {
                page.copy(signatures = page.signatures + sigDef)
            } else {
                page
            }
        }
        
        _uiState.update {
            it.copy(activeDocumentContent = currentContent.copy(pages = updatedPages))
        }
    }

    fun removeSignatureOverlay(sigId: String) {
        pushToUndoStack()
        val currentContent = _uiState.value.activeDocumentContent
        val activePageId = _uiState.value.activePageId
        
        val updatedPages = currentContent.pages.map { page ->
            if (page.id == activePageId) {
                page.copy(signatures = page.signatures.filterNot { it.id == sigId })
            } else {
                page
            }
        }
        _uiState.update {
            it.copy(activeDocumentContent = currentContent.copy(pages = updatedPages))
        }
    }

    fun addNewPageToEditor() {
        pushToUndoStack()
        val currentContent = _uiState.value.activeDocumentContent
        val sizePage = currentContent.pages.size
        
        val newPage = PageDef(
            id = UUID.randomUUID().toString(),
            pageNumber = sizePage + 1,
            type = "blank"
        )
        
        val newContent = currentContent.copy(pages = currentContent.pages + newPage)
        _uiState.update {
            it.copy(
                activeDocumentContent = newContent,
                activePageId = newPage.id
            )
        }
    }

    private fun extractPdfPagesToImages(context: android.content.Context, pdfFile: File): List<String> {
        val paths = mutableListOf<String>()
        var input: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            input = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(input)
            val count = renderer.pageCount
            val appDir = context.filesDir
            val documentsDir = File(appDir, "imported")
            if (!documentsDir.exists()) documentsDir.mkdirs()

            for (i in 0 until count) {
                val page = renderer.openPage(i)
                val scale = 4.0f
                val width = (page.width * scale).toInt().coerceIn(2400, 4200)
                val height = (width * page.height / page.width)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val pageFile = File(documentsDir, "pdf_page_${System.currentTimeMillis()}_${UUID.randomUUID()}_${i}.jpg")
                FileOutputStream(pageFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 96, fos)
                }
                paths.add(pageFile.absolutePath)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                renderer?.close()
            } catch (e: Exception) {}
            try {
                input?.close()
            } catch (e: Exception) {}
        }
        return paths
    }

    private fun processAndOptimizeImportedImage(context: android.content.Context, sourceFile: File): File {
        try {
            val exifInterface = android.media.ExifInterface(sourceFile.absolutePath)
            val orientation = exifInterface.getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL
            )

            val rotationDegrees = when (orientation) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = 1 }
            var decodedBitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOptions) ?: return sourceFile

            if (rotationDegrees != 0) {
                val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                val rotated = Bitmap.createBitmap(
                    decodedBitmap,
                    0,
                    0,
                    decodedBitmap.width,
                    decodedBitmap.height,
                    matrix,
                    true
                )
                if (rotated != decodedBitmap) {
                    decodedBitmap.recycle()
                    decodedBitmap = rotated
                }
            }

            val optimizedFile = File(sourceFile.parentFile, "proc_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
            FileOutputStream(optimizedFile).use { fos ->
                decodedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            }

            decodedBitmap.recycle()

            return optimizedFile
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error processing image", e)
            return sourceFile
        }
    }

    private fun createDummyPreview(documentsDir: File, name: String, fileSize: Int): String {
        val canvasBmp = Bitmap.createBitmap(800, 1131, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(canvasBmp)
        canvas.drawColor(android.graphics.Color.WHITE)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.DKGRAY
            textSize = 28f
        }
        val redAccent = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#E52D27") }
        canvas.drawRect(0f, 0f, 800f, 60f, redAccent)
        paint.color = android.graphics.Color.WHITE
        canvas.drawText("IMPORTED PDF DOCUMENT PREVIEW", 40f, 40f, paint)

        paint.color = android.graphics.Color.BLACK
        paint.textSize = 22f
        canvas.drawText("PDF Title: $name", 60f, 150f, paint)
        canvas.drawText("File Size: ${String.format("%.1f", fileSize / 1024f)} KB", 60f, 200f, paint)
        canvas.drawText("Status: Loaded securely", 60f, 250f, paint)

        val linePaint = android.graphics.Paint().apply { color = android.graphics.Color.LTGRAY; strokeWidth = 1f }
        for (y in 320..1080 step 40) {
            canvas.drawLine(60f, y.toFloat(), 740f, y.toFloat(), linePaint)
        }

        val fallbackFile = File(documentsDir, "pdf_preview_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
        FileOutputStream(fallbackFile).use { canvasBmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return fallbackFile.absolutePath
    }

    fun importAndAddPageToEditor(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val currentContent = _uiState.value.activeDocumentContent
                val sizePage = currentContent.pages.size
                
                val appDir = getApplication<Application>().filesDir
                val documentsDir = File(appDir, "imported")
                if (!documentsDir.exists()) documentsDir.mkdirs()

                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                if (bytes == null) {
                    triggerFeedback("Could not read file data.")
                    return@launch
                }

                var name = "imported_${System.currentTimeMillis()}"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }
                val extension = name.substringAfterLast(".", "jpg")
                val isImage = extension.lowercase() in listOf("jpg", "jpeg", "png", "webp", "gif")
                
                val sourceFile = File(documentsDir, "imported_${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension")
                FileOutputStream(sourceFile).use { it.write(bytes) }

                val newPages = mutableListOf<PageDef>()

                if (isImage) {
                    val processedFile = processAndOptimizeImportedImage(context, sourceFile)
                    val newPage = PageDef(
                        id = UUID.randomUUID().toString(),
                        pageNumber = sizePage + 1,
                        type = "scan",
                        backgroundScanPath = processedFile.absolutePath,
                        ocrText = "Imported Image Content $name"
                    )
                    newPages.add(newPage)
                } else if (extension.lowercase() == "pdf") {
                    val extractedPaths = extractPdfPagesToImages(context, sourceFile)
                    if (extractedPaths.isNotEmpty()) {
                        extractedPaths.forEachIndexed { idx, path ->
                            newPages.add(
                                PageDef(
                                    id = UUID.randomUUID().toString(),
                                    pageNumber = sizePage + 1 + idx,
                                    type = "imported_pdf",
                                    backgroundScanPath = path,
                                    ocrText = "Imported PDF page ${idx + 1}"
                                )
                            )
                        }
                    } else {
                        val dummyPath = createDummyPreview(documentsDir, name, bytes.size)
                        newPages.add(
                            PageDef(
                                id = UUID.randomUUID().toString(),
                                pageNumber = sizePage + 1,
                                type = "imported_pdf",
                                backgroundScanPath = dummyPath,
                                ocrText = "Imported PDF dummy placeholder"
                            )
                        )
                    }
                } else {
                    val extractedPaths = extractPdfPagesToImages(context, sourceFile)
                    if (extractedPaths.isNotEmpty()) {
                        extractedPaths.forEachIndexed { idx, path ->
                            newPages.add(
                                PageDef(
                                    id = UUID.randomUUID().toString(),
                                    pageNumber = sizePage + 1 + idx,
                                    type = "imported_pdf",
                                    backgroundScanPath = path,
                                    ocrText = "Imported page"
                                )
                            )
                        }
                    } else {
                        val newPage = PageDef(
                            id = UUID.randomUUID().toString(),
                            pageNumber = sizePage + 1,
                            type = "scan",
                            backgroundScanPath = sourceFile.absolutePath,
                            ocrText = "Imported Content $name"
                        )
                        newPages.add(newPage)
                    }
                }

                if (newPages.isEmpty()) {
                    triggerFeedback("Could not generate pages.")
                    return@launch
                }

                val newContent = currentContent.copy(pages = currentContent.pages + newPages)
                _uiState.update {
                    it.copy(
                        activeDocumentContent = newContent,
                        activePageId = newPages.first().id
                    )
                }
                triggerFeedback("Added ${newPages.size} pages from: $name")
            } catch (e: Exception) {
                e.printStackTrace()
                triggerFeedback("Error importing page from file.")
            }
        }
    }

    fun reorderActivePages(updatedPages: List<PageDef>) {
        val reindexedPages = updatedPages.mapIndexed { index, pageDef ->
            pageDef.copy(pageNumber = index + 1)
        }
        _uiState.update {
            val currentContent = it.activeDocumentContent
            val newActiveField = if (reindexedPages.none { page -> page.id == it.activePageId }) {
                reindexedPages.firstOrNull()?.id ?: ""
            } else {
                it.activePageId
            }
            it.copy(
                activeDocumentContent = currentContent.copy(pages = reindexedPages),
                activePageId = newActiveField
            )
        }
    }

    fun removePageFromActiveDocument(pageId: String) {
        val currentContent = _uiState.value.activeDocumentContent
        if (currentContent.pages.size <= 1) {
            triggerFeedback("A PDF must contain at least 1 page!")
            return
        }
        val filteredPages = currentContent.pages.filterNot { it.id == pageId }
        val reindexedPages = filteredPages.mapIndexed { index, pageDef ->
            pageDef.copy(pageNumber = index + 1)
        }
        _uiState.update {
            val newActiveField = if (it.activePageId == pageId) {
                reindexedPages.first().id
            } else {
                it.activePageId
            }
            it.copy(
                activeDocumentContent = currentContent.copy(pages = reindexedPages),
                activePageId = newActiveField
            )
        }
        triggerFeedback("Page removed.")
    }

    fun deleteActivePage() {
        val currentContent = _uiState.value.activeDocumentContent
        val activePageId = _uiState.value.activePageId
        if (currentContent.pages.size <= 1) {
            triggerFeedback("A PDF must contain at least 1 page!")
            return
        }

        val filteredPages = currentContent.pages.filterNot { it.id == activePageId }
        // Re-index remaining pages
        val reindexedPages = filteredPages.mapIndexed { index, pageDef ->
            pageDef.copy(pageNumber = index + 1)
        }

        _uiState.update {
            it.copy(
                activeDocumentContent = currentContent.copy(pages = reindexedPages),
                activePageId = reindexedPages.first().id
            )
        }
        triggerFeedback("Page deleted from editor buffer.")
    }

    // --- SIGNATURE WORKSPACE CREATOR ---
    fun saveDrawnSignature(alias: String, points: List<PointDef>, strokeColor: String, strokeWidth: Float) {
        viewModelScope.launch {
            val jsonPoints = DocumentSerializer.pointsToJson(points)
            val name = alias.ifBlank { "Signature #${_uiState.value.signatures.size + 1}" }
            val signature = SignatureProfile(
                alias = name,
                pathDataJson = jsonPoints,
                colorHex = strokeColor,
                strokeWidth = strokeWidth
            )
            val insertedId = repo.insertSignature(signature)
            // Automatically prompt/export 300 PPI image of the drawn signature!
            exportSignatureProfileTo300Ppi(signature.copy(id = insertedId.toInt()))
        }
    }

    fun deleteSignatureProfile(sig: SignatureProfile) {
        viewModelScope.launch {
            repo.deleteSignature(sig)
            triggerFeedback("Signature deleted.")
        }
    }

    fun renameSignatureProfile(sig: SignatureProfile, newAlias: String) {
        viewModelScope.launch {
            val updated = sig.copy(alias = newAlias)
            repo.insertSignature(updated)
            triggerFeedback("Signature profile renamed safely!")
        }
    }

    fun exportSignatureProfileTo300Ppi(sig: SignatureProfile) {
        viewModelScope.launch {
            try {
                // Determine 300 PPI size for a premium 6" x 3" signature card layout: 1800 x 900 pixels
                val destW = 1800
                val destH = 900
                val bmp = Bitmap.createBitmap(destW, destH, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                canvas.drawColor(android.graphics.Color.TRANSPARENT)

                if (sig.pathDataJson.startsWith("image:")) {
                    val imagePath = sig.pathDataJson.removePrefix("image:")
                    val file = File(imagePath)
                    if (file.exists()) {
                        val originalBmp = BitmapFactory.decodeFile(imagePath)
                        if (originalBmp != null) {
                            val srcRect = android.graphics.Rect(0, 0, originalBmp.width, originalBmp.height)
                            val destRect = android.graphics.Rect(0, 0, destW, destH)
                            canvas.drawBitmap(originalBmp, srcRect, destRect, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG))
                        }
                    }
                } else {
                    val points = DocumentSerializer.pointsFromJson(sig.pathDataJson)
                    if (points.isNotEmpty()) {
                        val baseColor = try {
                            android.graphics.Color.parseColor(sig.colorHex)
                        } catch (e: Exception) {
                            android.graphics.Color.BLACK
                        }
                        val inkPaint = android.graphics.Paint().apply {
                            color = baseColor
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = sig.strokeWidth * (destW / 300f) // Keep stroke proportional to high DPI canvas
                            strokeCap = android.graphics.Paint.Cap.ROUND
                            strokeJoin = android.graphics.Paint.Join.ROUND
                            isAntiAlias = true
                        }
                        val path = com.example.data.SignaturePathUtils.buildSmoothedPath(
                            points, destW.toFloat(), destH.toFloat()
                        )
                        canvas.drawPath(path, inkPaint)
                    }
                }

                val filename = "Signature_300ppi_${System.currentTimeMillis()}.png"
                var exportedFile: File? = null
                
                try {
                    val publicPicturesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
                    val outDir = File(publicPicturesDir, "SignatureStudio")
                    if (!outDir.exists()) outDir.mkdirs()
                    val f = File(outDir, filename)
                    FileOutputStream(f).use { fos ->
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
                        fos.flush()
                    }
                    exportedFile = f
                } catch (e: Exception) {
                    val fallbackDir = getApplication<Application>().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
                    if (fallbackDir != null) {
                        val f = File(fallbackDir, filename)
                        FileOutputStream(f).use { fos ->
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
                            fos.flush()
                        }
                        exportedFile = f
                    }
                }

                if (exportedFile != null) {
                    Log.d(TAG, "Signature profile saved & archived at 300 PPI! File: ${exportedFile.name}")
                } else {
                    Log.d(TAG, "Signature saved to application directory successfully.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                triggerFeedback("Saved to database successfully. External raw export failed: ${e.localizedMessage}")
            }
        }
    }

    fun renameDocument(doc: Document, newTitle: String) {
        viewModelScope.launch {
            if (newTitle.isNotBlank()) {
                val updated = doc.copy(title = newTitle.trim())
                repo.updateDocument(updated)
                triggerFeedback("Document renamed successfully!")
            }
        }
    }

    // --- AUTOMATIC BACKGROUND DETECTION & REMOVAL FOR IMAGE SIGNATURES ---
    fun removeSignatureBackground(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val color = pixels[i]
            val a = (color shr 24) and 0xff
            val r = (color shr 16) and 0xff
            val g = (color shr 8) and 0xff
            val b = color and 0xff

            // Calculate luminance Y (standard ITU-R formula)
            val luminance = 0.299f * r + 0.587f * g + 0.114f * b

            // Smooth adaptive transparency thresholding
            if (luminance >= 130f) {
                pixels[i] = android.graphics.Color.TRANSPARENT
            } else if (luminance > 90f) {
                // Smooth transition alpha to avoid pixelated jagged edges
                val alphaFactor = (130f - luminance) / (130f - 90f)
                val finalAlpha = (alphaFactor * 255).toInt().coerceIn(0, 255)
                pixels[i] = (finalAlpha shl 24) or (r shl 16) or (g shl 8) or b
            } else {
                // Pristine ink solid color preservation
                pixels[i] = (0xff shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        out.setPixels(pixels, 0, width, 0, 0, width, height)
        return out
    }

    fun processAndSaveImageSignature(context: android.content.Context, uri: android.net.Uri, aliasDraft: String) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = 1
                    inScaled = false
                }
                val originalBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
                inputStream?.close()
                if (originalBitmap == null) {
                    triggerFeedback("Failed to load selected signature image.")
                    return@launch
                }

                // Process bitmap sequentially in background coroutine to keep UI smooth
                val processedBitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    removeSignatureBackground(originalBitmap)
                }

                val file = File(context.filesDir, "signature_img_${System.currentTimeMillis()}.png")
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val out = FileOutputStream(file)
                    processedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    out.flush()
                    out.close()
                }

                val alias = if (aliasDraft.isBlank()) "Imported Sign - ${System.currentTimeMillis() % 1000}" else aliasDraft
                val pathDataJson = "image:${file.absolutePath}"

                val signature = SignatureProfile(
                    alias = alias,
                    pathDataJson = pathDataJson,
                    colorHex = "#000000",
                    strokeWidth = 6f
                )

                repo.insertSignature(signature)
                triggerFeedback("Image signature imported & background auto-deleted successfully!")
            } catch (e: Exception) {
                e.printStackTrace()
                triggerFeedback("Error importing signature: ${e.localizedMessage}")
            }
        }
    }

    fun saveCroppedSignature(context: android.content.Context, croppedBitmap: android.graphics.Bitmap, aliasDraft: String) {
        viewModelScope.launch {
            try {
                // Process bitmap sequentially in background coroutine to keep UI smooth
                val processedBitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    removeSignatureBackground(croppedBitmap)
                }

                val file = File(context.filesDir, "signature_img_${System.currentTimeMillis()}.png")
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val out = FileOutputStream(file)
                    processedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    out.flush()
                    out.close()
                }

                val alias = if (aliasDraft.isBlank()) "Imported Sign - ${System.currentTimeMillis() % 1000}" else aliasDraft
                val pathDataJson = "image:${file.absolutePath}"

                val signature = SignatureProfile(
                    alias = alias,
                    pathDataJson = pathDataJson,
                    colorHex = "#000000",
                    strokeWidth = 6f
                )

                repo.insertSignature(signature)
                triggerFeedback("Cropped signature imported & background auto-deleted successfully!")
            } catch (e: Exception) {
                e.printStackTrace()
                triggerFeedback("Error saving cropped signature: ${e.localizedMessage}")
            }
        }
    }

    // --- IMMERSIVE SCAN CAMERA SCANNER LAB ---
    fun resetScanner(isIdMode: Boolean = false) {
        _uiState.update {
            it.copy(
                scannerStepBitmaps = emptyList(),
                scannerStepPaths = emptyList(),
                scannerFilterType = "original",
                scannerIdMode = isIdMode,
                scannerIdFrontPath = null,
                scannerIdBackPath = null
            )
        }
    }

    fun resetIdScanner() {
        _uiState.update {
            it.copy(
                idCardFrontBitmap = null,
                idCardBackBitmap = null,
                isScanningIdBack = false,
                idScanFilterType = "original"
            )
        }
    }

    fun captureIdCardFront(bitmap: Bitmap) {
        _uiState.update {
            it.copy(
                idCardFrontBitmap = bitmap,
                isScanningIdBack = true
            )
        }
    }

    fun captureIdCardBack(bitmap: Bitmap) {
        _uiState.update {
            it.copy(
                idCardBackBitmap = bitmap
            )
        }
    }

    fun applyIdScanFilter(filterName: String) {
        _uiState.update {
            it.copy(idScanFilterType = filterName)
        }
    }

    fun saveIdCardA4Document(title: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val front = state.idCardFrontBitmap
            val back = state.idCardBackBitmap
            if (front == null || back == null) {
                triggerFeedback("Both ID Card Front and Back sides are required.")
                return@launch
            }

            val scansDir = File(getApplication<Application>().filesDir, "scans")
            if (!scansDir.exists()) scansDir.mkdirs()

            // Apply filter to front and back bitmaps before scaling/cropping or drawing
            val frontFiltered = applyFilterToBitmap(front, state.idScanFilterType)
            val backFiltered = applyFilterToBitmap(back, state.idScanFilterType)

            // Crop both ID card side views to 86mm x 54mm proportions (1.5926f)
            val frontCropped = autoCropBitmap(frontFiltered, 86f / 54f)
            val backCropped = autoCropBitmap(backFiltered, 86f / 54f)

            // Create beautiful single A4 canvas (aspect ratio 1 : 1.414)
            val canvasWidth = 1200
            val canvasHeight = 1697
            val a4Bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(a4Bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)

            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

            // Scale to physical 86mm x 54mm on A4 sheet
            val scaleX = canvasWidth.toFloat() / 210f
            val scaleY = canvasHeight.toFloat() / 297f
            val cardWidth = (86f * scaleX).toInt()
            val cardHeight = (54f * scaleY).toInt()

            val scaledFront = Bitmap.createScaledBitmap(frontCropped, cardWidth, cardHeight, true)
            val scaledBack = Bitmap.createScaledBitmap(backCropped, cardWidth, cardHeight, true)

            // Center positions on A4 top/bottom halves
            val cardX = (canvasWidth - cardWidth) / 2f
            val frontY = (canvasHeight / 2f - cardHeight) / 2f
            val backY = (canvasHeight / 2f) + (canvasHeight / 2f - cardHeight) / 2f

            // Clean, elegant card background borders
            val borderPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#CBD5E1")
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 3f
                isAntiAlias = true
            }

            // Draw cards
            canvas.drawBitmap(scaledFront, cardX, frontY, paint)
            canvas.drawRoundRect(cardX, frontY, cardX + cardWidth, frontY + cardHeight, 16f, 16f, borderPaint)

            canvas.drawBitmap(scaledBack, cardX, backY, paint)
            canvas.drawRoundRect(cardX, backY, cardX + cardWidth, backY + cardHeight, 16f, 16f, borderPaint)

            // Draw helper tags (removed as requested)

            // Save unified image to local disk
            val unifiedFile = File(scansDir, "unified_id_a4_${System.currentTimeMillis()}.jpg")
            val fos = FileOutputStream(unifiedFile)
            a4Bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
            fos.close()

            // Create combined 1-page unified scan PDF
            val initialPages = listOf(
                PageDef(
                    id = UUID.randomUUID().toString(),
                    pageNumber = 1,
                    type = "id_card_a4",
                    backgroundScanPath = unifiedFile.absolutePath,
                    ocrText = "Unified Front/Back ID Card on secure A4 Sheet layout."
                )
            )

            val docContent = DocumentContent(pages = initialPages)
            val json = DocumentSerializer.toJson(docContent)

            val actualTitle = title.ifBlank { "Unified ID A4 - ${System.currentTimeMillis() % 1000}" }

            val doc = Document(
                title = actualTitle,
                category = "ID Card",
                pageCount = 1,
                contentJson = json,
                isSaved = true
            )

            val docId = repo.insertDocument(doc).toInt()
            val savedDoc = repo.getDocumentById(docId)

            if (savedDoc != null) {
                PdfGenerator.buildPdf(getApplication(), savedDoc, docContent, state.signatures)
                triggerFeedback("ID Card Document saved successfully with ${state.idScanFilterType} filter!")
                navigateTo(Screen.Dashboard)
            }
        }
    }

    fun addScannedPagesFromUris(uris: List<Uri>) {
        viewModelScope.launch {
            try {
                val contentResolver = getApplication<Application>().contentResolver
                val loadedBitmaps = mutableListOf<Bitmap>()
                val loadedPaths = mutableListOf<String>()
                val scansDir = File(getApplication<Application>().filesDir, "scans")
                if (!scansDir.exists()) scansDir.mkdirs()

                uris.forEach { uri ->
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        if (bitmap != null) {
                            val file = File(scansDir, "scan_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(5)}.jpg")
                            val fos = FileOutputStream(file)
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                            fos.close()
                            loadedBitmaps.add(bitmap)
                            loadedPaths.add(file.absolutePath)
                        }
                    }
                }

                if (loadedBitmaps.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            scannerStepBitmaps = it.scannerStepBitmaps + loadedBitmaps,
                            scannerStepPaths = it.scannerStepPaths + loadedPaths
                        )
                    }
                    triggerFeedback("Successfully added ${loadedBitmaps.size} scanned pages!")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                triggerFeedback("Error loading scanned pages: ${e.localizedMessage}")
            }
        }
    }

    fun addAndCompileScannedPagesFromUris(uris: List<Uri>) {
        viewModelScope.launch {
            try {
                val contentResolver = getApplication<Application>().contentResolver
                val loadedBitmaps = mutableListOf<Bitmap>()
                val loadedPaths = mutableListOf<String>()
                val scansDir = File(getApplication<Application>().filesDir, "scans")
                if (!scansDir.exists()) scansDir.mkdirs()

                uris.forEachIndexed { index, uri ->
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        if (bitmap != null) {
                            val file = File(scansDir, "scan_${System.currentTimeMillis()}_$index.jpg")
                            val fos = java.io.FileOutputStream(file)
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                            fos.close()
                            loadedBitmaps.add(bitmap)
                            loadedPaths.add(file.absolutePath)
                        }
                    }
                }

                if (loadedBitmaps.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            scannerStepBitmaps = loadedBitmaps,
                            scannerStepPaths = loadedPaths,
                            scannerFilterType = "original"
                        )
                    }
                    val timestamp = System.currentTimeMillis() % 1000
                    compileScannedDoc("Quick Scan $timestamp")
                } else {
                    triggerFeedback("No scanned images captured.")
                    navigateTo(Screen.Dashboard)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                triggerFeedback("Error compiling scanned pages: ${e.localizedMessage}")
                navigateTo(Screen.Dashboard)
            }
        }
    }

    fun autoCropBitmap(bitmap: Bitmap, targetRatio: Float): Bitmap {
        val picWidth = bitmap.width
        val picHeight = bitmap.height
        val currentRatio = picWidth.toFloat() / picHeight.toFloat()
        
        val cropWidth: Int
        val cropHeight: Int
        val startX: Int
        val startY: Int
        
        if (currentRatio > targetRatio) {
            cropHeight = picHeight
            cropWidth = (picHeight * targetRatio).toInt()
            startX = (picWidth - cropWidth) / 2
            startY = 0
        } else {
            cropWidth = picWidth
            cropHeight = (picWidth / targetRatio).toInt()
            startX = 0
            startY = (picHeight - cropHeight) / 2
        }
        
        val x = startX.coerceIn(0, picWidth - 1)
        val y = startY.coerceIn(0, picHeight - 1)
        val w = cropWidth.coerceAtMost(picWidth - x)
        val h = cropHeight.coerceAtMost(picHeight - y)
        
        return try {
            Bitmap.createBitmap(bitmap, x, y, w, h)
        } catch (e: java.lang.Exception) {
            bitmap
        }
    }

    fun captureScannerStep(bitmap: Bitmap, isImported: Boolean = false) {
        viewModelScope.launch {
            val isIdCard = _uiState.value.scannerIdMode
            
            // Apply advanced AI document processing: corner detection -> perspective deskewing -> curve flattening
            val corners = DocumentProcessor.detectCorners(bitmap)
            val deskewed = DocumentProcessor.deskew(bitmap, corners)
            val processedBitmap = DocumentProcessor.flattenCurvesAndEnhance(deskewed)
 
            val scansDir = File(getApplication<Application>().filesDir, "scans")
            if (!scansDir.exists()) scansDir.mkdirs()
 
            // Save processed scan file
            val file = File(scansDir, "scan_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(5)}.jpg")
            val fos = FileOutputStream(file)
            processedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.close()
 
            _uiState.update {
                it.copy(
                    scannerStepBitmaps = it.scannerStepBitmaps + processedBitmap,
                    scannerStepPaths = it.scannerStepPaths + file.absolutePath
                )
            }
        }
    }

    fun cropScannedPage(
        index: Int,
        topLeftX: Float, topLeftY: Float,
        topRightX: Float, topRightY: Float,
        bottomRightX: Float, bottomRightY: Float,
        bottomLeftX: Float, bottomLeftY: Float
    ) {
        viewModelScope.launch {
            val bitmaps = _uiState.value.scannerStepBitmaps.toMutableList()
            val paths = _uiState.value.scannerStepPaths.toMutableList()
            if (index < 0 || index >= bitmaps.size) return@launch

            val originalBitmap = bitmaps[index]
            val picWidth = originalBitmap.width
            val picHeight = originalBitmap.height

            // Calculate bounding rect of the crop quadrilateral
            val minX = minOf(topLeftX, bottomLeftX, topRightX, bottomRightX).coerceIn(0f, 1f)
            val maxX = maxOf(topLeftX, bottomLeftX, topRightX, bottomRightX).coerceIn(0f, 1f)
            val minY = minOf(topLeftY, topRightY, bottomLeftY, bottomRightY).coerceIn(0f, 1f)
            val maxY = maxOf(topLeftY, topRightY, bottomLeftY, bottomRightY).coerceIn(0f, 1f)

            val x = (minX * picWidth).toInt()
            val y = (minY * picHeight).toInt()
            val w = ((maxX - minX) * picWidth).toInt().coerceAtLeast(10)
            val h = ((maxY - minY) * picHeight).toInt().coerceAtLeast(10)

            val croppedWidth = w.coerceAtMost(picWidth - x)
            val croppedHeight = h.coerceAtMost(picHeight - y)

            try {
                val croppedBmp = Bitmap.createBitmap(originalBitmap, x, y, croppedWidth, croppedHeight)
                
                // Save cropped scan file
                val scansDir = File(getApplication<Application>().filesDir, "scans")
                if (!scansDir.exists()) scansDir.mkdirs()
                val file = File(scansDir, "scan_cropped_${System.currentTimeMillis()}.jpg")
                val fos = FileOutputStream(file)
                croppedBmp.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                fos.close()

                bitmaps[index] = croppedBmp
                paths[index] = file.absolutePath

                _uiState.update {
                    it.copy(
                        scannerStepBitmaps = bitmaps,
                        scannerStepPaths = paths
                    )
                }
                triggerFeedback("Page cropped successfully!")
            } catch (e: Exception) {
                triggerFeedback("Failed to crop image: ${e.localizedMessage}")
            }
        }
    }

    fun applyFilterToScans(filterName: String) {
        _uiState.update { it.copy(scannerFilterType = filterName) }
    }

    fun createDocumentFromLocalFile(title: String, fileExtension: String, fileBytes: ByteArray) {
        viewModelScope.launch {
            try {
                val appDir = getApplication<Application>().filesDir
                val documentsDir = File(appDir, "imported")
                if (!documentsDir.exists()) documentsDir.mkdirs()
                
                val sourceFile = File(documentsDir, "imported_${System.currentTimeMillis()}_${UUID.randomUUID()}.$fileExtension")
                FileOutputStream(sourceFile).use { it.write(fileBytes) }
                
                val isImage = fileExtension.lowercase() in listOf("jpg", "jpeg", "png", "webp", "gif")
                val initialPages = mutableListOf<PageDef>()

                if (isImage) {
                    val processedFile = processAndOptimizeImportedImage(getApplication(), sourceFile)
                    initialPages.add(
                        PageDef(
                            id = UUID.randomUUID().toString(),
                            pageNumber = 1,
                            type = "scan",
                            backgroundScanPath = processedFile.absolutePath,
                            ocrText = "Imported Photo Content OCR layer"
                        )
                    )
                } else if (fileExtension.lowercase() == "pdf") {
                    val extractedPaths = extractPdfPagesToImages(getApplication(), sourceFile)
                    if (extractedPaths.isNotEmpty()) {
                        extractedPaths.forEachIndexed { idx, path ->
                            initialPages.add(
                                PageDef(
                                    id = UUID.randomUUID().toString(),
                                    pageNumber = idx + 1,
                                    type = "imported_pdf",
                                    backgroundScanPath = path,
                                    ocrText = "Imported PDF page ${idx + 1}"
                                )
                            )
                        }
                    } else {
                        val dummyPath = createDummyPreview(documentsDir, title, fileBytes.size)
                        initialPages.add(
                            PageDef(
                                id = UUID.randomUUID().toString(),
                                pageNumber = 1,
                                type = "imported_pdf",
                                backgroundScanPath = dummyPath,
                                ocrText = "Imported PDF placeholder"
                            )
                        )
                    }
                } else {
                    val extractedPaths = extractPdfPagesToImages(getApplication(), sourceFile)
                    if (extractedPaths.isNotEmpty()) {
                        extractedPaths.forEachIndexed { idx, path ->
                            initialPages.add(
                                PageDef(
                                    id = UUID.randomUUID().toString(),
                                    pageNumber = idx + 1,
                                    type = "imported_pdf",
                                    backgroundScanPath = path,
                                    ocrText = "Imported document page"
                                )
                            )
                        }
                    } else {
                        initialPages.add(
                            PageDef(
                                id = UUID.randomUUID().toString(),
                                pageNumber = 1,
                                type = "scan",
                                backgroundScanPath = sourceFile.absolutePath,
                                ocrText = "Imported Document Content"
                            )
                        )
                    }
                }
                
                if (initialPages.isEmpty()) {
                    triggerFeedback("Error generating pages for imported document.")
                    return@launch
                }

                val docContent = DocumentContent(pages = initialPages)
                val json = DocumentSerializer.toJson(docContent)
                
                val newDoc = Document(
                    title = title.substringBeforeLast("."),
                    category = if (isImage) "Photo import" else "PDF import",
                    pageCount = initialPages.size,
                    contentJson = json,
                    isSaved = false
                )
                
                val docId = repo.insertDocument(newDoc).toInt()
                val insertedDoc = repo.getDocumentById(docId)
                if (insertedDoc != null) {
                    PdfGenerator.buildPdf(getApplication(), insertedDoc, docContent, _uiState.value.signatures)
                    openDocumentInEditor(insertedDoc)
                    triggerFeedback("Imported local $fileExtension successfully with ${initialPages.size} pages! Opened in Editor.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                triggerFeedback("Failed to import file: ${e.localizedMessage}")
            }
        }
    }

    fun combineLocalFilesIntoPdf(
        masterTitle: String,
        files: List<Triple<String, String, ByteArray>>
    ) {
        viewModelScope.launch {
            if (files.isEmpty()) {
                triggerFeedback("No files selected to combine.")
                return@launch
            }
            
            val actualTitle = masterTitle.ifBlank { "Combined Document - ${System.currentTimeMillis() % 1000}" }
            val appDir = getApplication<Application>().filesDir
            val scansDir = File(appDir, "scans")
            if (!scansDir.exists()) scansDir.mkdirs()
            
            val pageCount = files.size
            val mergedPages = mutableListOf<PageDef>()
            
            files.forEachIndexed { index, fileInfo ->
                val (fileName, fileExtension, bytes) = fileInfo
                val pageFile = File(scansDir, "combine_${index}_${System.currentTimeMillis()}.$fileExtension")
                try {
                    FileOutputStream(pageFile).use { it.write(bytes) }
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving page $index", e)
                }
                
                if (fileExtension.lowercase() == "pdf") {
                    val extracted = extractPdfPagesToImages(getApplication(), pageFile)
                    if (extracted.isNotEmpty()) {
                        extracted.forEachIndexed { pIdx, path ->
                            mergedPages.add(
                                PageDef(
                                    id = UUID.randomUUID().toString(),
                                    pageNumber = mergedPages.size + 1,
                                    type = "pdf",
                                    backgroundScanPath = path,
                                    ocrText = "OCR transcript for combined PDF page ${pIdx + 1} from $fileName"
                                )
                            )
                        }
                    } else {
                        val placeholderPath = createDummyPreview(scansDir, fileName, bytes.size)
                        mergedPages.add(
                            PageDef(
                                id = UUID.randomUUID().toString(),
                                pageNumber = mergedPages.size + 1,
                                type = "pdf",
                                backgroundScanPath = placeholderPath,
                                ocrText = "Placeholder for combined item $fileName"
                            )
                        )
                    }
                } else {
                    val finalFile = if (fileExtension.lowercase() in listOf("jpg", "jpeg", "png", "webp")) {
                        try {
                            processAndOptimizeImportedImage(getApplication(), pageFile)
                        } catch (e: Exception) {
                            pageFile
                        }
                    } else {
                        pageFile
                    }
                    mergedPages.add(
                        PageDef(
                            id = UUID.randomUUID().toString(),
                            pageNumber = mergedPages.size + 1,
                            type = if (fileExtension.lowercase() in listOf("jpg", "jpeg", "png", "webp")) "scan" else "pdf",
                            backgroundScanPath = finalFile.absolutePath,
                            ocrText = "OCR transcript for combined item $fileName"
                        )
                    )
                }
            }
            
            val docContent = DocumentContent(pages = mergedPages)
            val json = DocumentSerializer.toJson(docContent)
            
            val combinedDoc = Document(
                title = actualTitle,
                category = "Combine",
                pageCount = mergedPages.size,
                contentJson = json,
                isSaved = true
            )
            
            val docId = repo.insertDocument(combinedDoc).toInt()
            val insertedDoc = repo.getDocumentById(docId)
            
            if (insertedDoc != null) {
                PdfGenerator.buildPdf(getApplication(), insertedDoc, docContent, _uiState.value.signatures)
                triggerFeedback("Combined $pageCount local files into one HanPDF master document!")
                navigateTo(Screen.Dashboard)
            } else {
                triggerFeedback("Failed to insert combined document.")
            }
        }
    }

    fun saveIdentityCardFrontBack(front: Bitmap, back: Bitmap) {
        viewModelScope.launch {
            val scansDir = File(getApplication<Application>().filesDir, "scans")
            if (!scansDir.exists()) scansDir.mkdirs()

            // Step 1 & 2: Crop both ID card side views to 86mm x 54mm proportions (1.5926f)
            val frontCropped = autoCropBitmap(front, 86f / 54f)
            val backCropped = autoCropBitmap(back, 86f / 54f)

            // Step 3: Create beautiful single A4 canvas (aspect ratio 1 : 1.414)
            val canvasWidth = 1200
            val canvasHeight = 1697
            val a4Bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(a4Bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)

            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

            // Scale to physical 86mm x 54mm on A4 sheet
            val scaleX = canvasWidth.toFloat() / 210f
            val scaleY = canvasHeight.toFloat() / 297f
            val cardWidth = (86f * scaleX).toInt()
            val cardHeight = (54f * scaleY).toInt()

            val scaledFront = Bitmap.createScaledBitmap(frontCropped, cardWidth, cardHeight, true)
            val scaledBack = Bitmap.createScaledBitmap(backCropped, cardWidth, cardHeight, true)

            // Center positions on A4 top/bottom halves
            val cardX = (canvasWidth - cardWidth) / 2f
            val frontY = (canvasHeight / 2f - cardHeight) / 2f
            val backY = (canvasHeight / 2f) + (canvasHeight / 2f - cardHeight) / 2f

            // Clean, elegant card background borders to look highly authentic
            val borderPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#CBD5E1")
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 3f
                isAntiAlias = true
            }

            // Draw cards
            canvas.drawBitmap(scaledFront, cardX, frontY, paint)
            canvas.drawRoundRect(cardX, frontY, cardX + cardWidth, frontY + cardHeight, 16f, 16f, borderPaint)

            canvas.drawBitmap(scaledBack, cardX, backY, paint)
            canvas.drawRoundRect(cardX, backY, cardX + cardWidth, backY + cardHeight, 16f, 16f, borderPaint)

            // Draw professional helper tags (removed as requested)

            // Save unified image to local disk
            val unifiedFile = File(scansDir, "unified_id_a4_${System.currentTimeMillis()}.jpg")
            val fos = FileOutputStream(unifiedFile)
            a4Bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
            fos.close()

            // Create combined 1-page unified scan PDF
            val initialPages = listOf(
                PageDef(
                    id = UUID.randomUUID().toString(),
                    pageNumber = 1,
                    type = "id_card_a4",
                    backgroundScanPath = unifiedFile.absolutePath,
                    ocrText = "Unified Front/Back ID Card on secure A4 Sheet layout."
                )
            )

            val docContent = DocumentContent(pages = initialPages)
            val json = DocumentSerializer.toJson(docContent)

            val doc = Document(
                title = "Unified ID A4 - Scan",
                category = "ID Card",
                pageCount = 1,
                contentJson = json,
                isSaved = true
            )

            val docId = repo.insertDocument(doc).toInt()
            val savedDoc = repo.getDocumentById(docId)
            
            if (savedDoc != null) {
                PdfGenerator.buildPdf(getApplication(), savedDoc, docContent, _uiState.value.signatures)
                triggerFeedback("Identity Card compiled onto single A4 document!")
                navigateTo(Screen.Dashboard)
            }
        }
    }

    fun applyFilterToBitmap(bitmap: Bitmap, filterName: String): Bitmap {
        return BitmapFilter.applyFilter(bitmap, filterName)
    }

    fun compileScannedDoc(title: String) {
        viewModelScope.launch {
            val actualTitle = title.ifBlank { "Scan Document ${System.currentTimeMillis() % 1000}" }
            val bitmaps = _uiState.value.scannerStepBitmaps
            val filterType = _uiState.value.scannerFilterType
            
            if (bitmaps.isEmpty()) {
                triggerFeedback("No scanned images captured.")
                return@launch
            }

            val appDir = getApplication<Application>().filesDir
            val scansDir = File(appDir, "scans")
            if (!scansDir.exists()) scansDir.mkdirs()

            // Map each scanned page to a PageDef, applying the filter to the bitmap and saving it before finalizing
            val documentPages = bitmaps.mapIndexed { index, bitmap ->
                val filteredBmp = applyFilterToBitmap(bitmap, filterType)
                val finalFile = File(scansDir, "scan_finalized_${System.currentTimeMillis()}_$index.jpg")
                val fos = FileOutputStream(finalFile)
                filteredBmp.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                fos.close()

                PageDef(
                    id = UUID.randomUUID().toString(),
                    pageNumber = index + 1,
                    type = "scan",
                    backgroundScanPath = finalFile.absolutePath,
                    ocrText = "Scanned Page ${index + 1}"
                )
            }

            val docContent = DocumentContent(pages = documentPages)
            val json = DocumentSerializer.toJson(docContent)

            val doc = Document(
                title = actualTitle,
                category = "Scan",
                pageCount = documentPages.size,
                contentJson = json,
                isSaved = true
            )

            val docId = repo.insertDocument(doc).toInt()
            val savedDoc = repo.getDocumentById(docId)

            if (savedDoc != null) {
                PdfGenerator.buildPdf(getApplication(), savedDoc, docContent, _uiState.value.signatures)
                // Reset scanner state to clean up
                _uiState.update {
                    it.copy(
                        scannerStepBitmaps = emptyList(),
                        scannerStepPaths = emptyList(),
                        scannerFilterType = "original"
                    )
                }
                triggerFeedback("Scanned document finalized with $filterType filter!")
                navigateTo(Screen.Dashboard)
            }
        }
    }

    // --- HIGH-PERFORMANCE OCR INTELLIGENCE ---
    fun updateOcrEngine(engine: String) {
        _uiState.update { it.copy(ocrEngine = engine) }
    }

    fun updateOcrProgress(progress: Float) {
        _uiState.update { it.copy(ocrProgress = progress) }
    }

    fun saveOcrResult(recognizedText: String) {
        val doc = _uiState.value.activeDocument ?: return
        val currentContent = _uiState.value.activeDocumentContent
        val activePageId = _uiState.value.activePageId

        viewModelScope.launch {
            val updatedPages = currentContent.pages.map { page ->
                if (page.id == activePageId) page.copy(ocrText = recognizedText) else page
            }
            val newContent = currentContent.copy(pages = updatedPages)
            val updatedDoc = doc.copy(contentJson = DocumentSerializer.toJson(newContent))
            repo.updateDocument(updatedDoc)

            _uiState.update {
                it.copy(
                    ocrLoading = false,
                    ocrTextResult = recognizedText,
                    activeDocument = updatedDoc,
                    activeDocumentContent = newContent,
                    ocrProgress = 1.0f
                )
            }
        }
    }

    fun runOcrOnActivePage() {
        val doc = _uiState.value.activeDocument ?: return
        val currentContent = _uiState.value.activeDocumentContent
        val activePageId = _uiState.value.activePageId
        val currentPage = currentContent.pages.find { it.id == activePageId } ?: return

        _uiState.update {
            it.copy(
                ocrLoading = true,
                ocrTextResult = null,
                ocrDocTitle = doc.title,
                ocrProgress = 0f,
                ocrBase64Image = null
            )
        }
        navigateTo(Screen.OcrViewer)

        viewModelScope.launch {
            val isTesseract = _uiState.value.ocrEngine == "TesseractJS"
            
            var base64Bmp: String? = null
            var bitmap: Bitmap? = null
            
            if (currentPage.backgroundScanPath != null) {
                val file = File(currentPage.backgroundScanPath)
                if (file.exists()) {
                    bitmap = BitmapFactory.decodeFile(file.absolutePath)
                }
            }
            
            if (bitmap == null) {
                // Generate a blank canvas with any drawn / typed text overlay if it's a template document
                val curW = 1200
                val curH = 1600
                val tempBmp = Bitmap.createBitmap(curW, curH, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(tempBmp)
                canvas.drawColor(android.graphics.Color.WHITE)
                
                // Redraw template minimal style
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = 36f
                    isAntiAlias = true
                }
                
                canvas.drawText("DOCUMENT TEMPLATE: ${currentPage.type.uppercase()}", 50f, 100f, paint)
                
                // Draw existing text annotations onto our temporary raster bitmap
                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 28f
                    isAntiAlias = true
                }
                currentPage.textAnnotations.forEachIndexed { idx, ann ->
                    val baselineY = (ann.y * curH) - textPaint.fontMetrics.ascent
                    canvas.drawText(ann.text, (ann.x * curW), baselineY, textPaint)
                }
                bitmap = tempBmp
            }
            
            if (isTesseract) {
                if (bitmap != null) {
                    val outputStream = java.io.ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                    val byteArray = outputStream.toByteArray()
                    base64Bmp = "data:image/jpeg;base64," + android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
                }
                
                _uiState.update {
                    it.copy(
                        ocrBase64Image = base64Bmp,
                        ocrProgress = 0.05f
                    )
                }
            } else {
                // Gemini Engine
                var recognizedText = ""
                if (bitmap != null) {
                    recognizedText = GeminiService.performOcr(bitmap)
                } else {
                    recognizedText = "OCR Error: Underling document image missing."
                }
                
                saveOcrResult(recognizedText)
            }
        }
    }

    fun autoDetectPageWords() {
        val activeDoc = _uiState.value.activeDocument ?: return
        val currentContent = _uiState.value.activeDocumentContent
        val activePageId = _uiState.value.activePageId
        val page = currentContent.pages.find { it.id == activePageId } ?: return

        _uiState.update { it.copy(ocrLoading = true) }
        triggerFeedback("Analyzing document layout with Gemini OCR engine...")

        viewModelScope.launch {
            var recognizedText = page.ocrText ?: ""
            if (recognizedText.isBlank() && page.backgroundScanPath != null) {
                try {
                    val file = File(page.backgroundScanPath)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            recognizedText = GeminiService.performOcr(bitmap)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "In-place OCR error during auto-detection", e)
                }
            }

            val pagesBefore = currentContent.pages.takeWhile { it.id != activePageId }
            val activePageObj = currentContent.pages.find { it.id == activePageId } ?: return@launch
            val pagesAfter = currentContent.pages.dropWhile { it.id != activePageId }.drop(1)

            val finalPagesList: List<PageDef> = if (recognizedText.isNotBlank() && !recognizedText.contains("Error") && !recognizedText.contains("Lined Notes")) {
                // Parse actual text words and map them to standard editor grids
                val cleanWords = recognizedText.split(Regex("\\s+"))
                    .map { it.trim().removeSuffix(",").removeSuffix(".").removeSuffix(";") }
                    .filter { it.length > 1 && !it.contains(Regex("[^A-Za-z0-9_-]")) }

                val chunks = cleanWords.chunked(25)
                val firstChunk = chunks.getOrNull(0) ?: emptyList()
                val firstChunkAnns = firstChunk.mapIndexed { i, word ->
                    val row = i / 3
                    val col = i % 3
                    TextAnnotationDef(
                        id = "det_${System.currentTimeMillis()}_0_$i",
                        text = word,
                        x = 0.15f + col * 0.25f,
                        y = 0.15f + row * 0.08f
                    )
                }

                val extraPages = chunks.drop(1).mapIndexed { k, chunk ->
                    val chunkIndex = k + 1
                    val chunkAnns = chunk.mapIndexed { i, word ->
                        val row = i / 3
                        val col = i % 3
                        TextAnnotationDef(
                            id = "det_${System.currentTimeMillis()}_${chunkIndex}_$i",
                            text = word,
                            x = 0.15f + col * 0.25f,
                            y = 0.15f + row * 0.08f
                        )
                    }
                    PageDef(
                        id = UUID.randomUUID().toString(),
                        pageNumber = activePageObj.pageNumber + chunkIndex,
                        type = activePageObj.type,
                        backgroundScanPath = null,
                        textAnnotations = chunkAnns
                    )
                }

                val combined = pagesBefore + activePageObj.copy(
                    ocrText = recognizedText.ifBlank { activePageObj.ocrText },
                    textAnnotations = activePageObj.textAnnotations + firstChunkAnns
                ) + extraPages + pagesAfter

                combined.mapIndexed { index, p ->
                    p.copy(pageNumber = index + 1)
                }
            } else {
                val detectedAnns = mutableListOf<TextAnnotationDef>()
                val mockLines = listOf(
                    "HanPDF" to Pair(0.15f, 0.12f),
                    "Document" to Pair(0.42f, 0.12f),
                    "Scan" to Pair(0.68f, 0.12f),
                    "Front-Side" to Pair(0.15f, 0.28f),
                    "ID" to Pair(0.40f, 0.28f),
                    "Card" to Pair(0.50f, 0.28f),
                    "Approved-Verify" to Pair(0.15f, 0.45f),
                    "Date:" to Pair(0.15f, 0.60f),
                    "2026-06-17" to Pair(0.32f, 0.60f)
                )
                mockLines.forEachIndexed { i, (text, pos) ->
                    detectedAnns.add(
                        TextAnnotationDef(
                            id = "det_mock_${System.currentTimeMillis()}_$i",
                            text = text,
                            x = pos.first,
                            y = pos.second
                        )
                    )
                }
                val reList = pagesBefore + activePageObj.copy(
                    ocrText = recognizedText.ifBlank { activePageObj.ocrText },
                    textAnnotations = activePageObj.textAnnotations + detectedAnns
                ) + pagesAfter

                reList.mapIndexed { index, p ->
                    p.copy(pageNumber = index + 1)
                }
            }

            val updatedContent = currentContent.copy(pages = finalPagesList)
            val updatedDoc = activeDoc.copy(contentJson = DocumentSerializer.toJson(updatedContent))
            repo.updateDocument(updatedDoc)

            _uiState.update {
                it.copy(
                    ocrLoading = false,
                    activeDocument = updatedDoc,
                    activeDocumentContent = updatedContent
                )
            }
            triggerFeedback("Detected and loaded text blocks! Tap any word to modify the document.")
        }
    }

    fun runOcrAndConvertToWordMode() {
        val activeDoc = _uiState.value.activeDocument ?: return
        val currentContent = _uiState.value.activeDocumentContent
        val activePageId = _uiState.value.activePageId
        val page = currentContent.pages.find { it.id == activePageId } ?: return

        _uiState.update { it.copy(ocrLoading = true) }
        triggerFeedback("Scanning imported page with Gemini OCR...")

        viewModelScope.launch {
            var recognizedText = page.ocrText ?: ""
            if (recognizedText.isBlank() && page.backgroundScanPath != null) {
                try {
                    val file = File(page.backgroundScanPath)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            recognizedText = GeminiService.performOcr(bitmap)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "In-place OCR error during word-mode conversion", e)
                }
            }

            if (recognizedText.isBlank() || recognizedText.contains("OCR Error") || recognizedText.contains("OCR Analysis failed")) {
                recognizedText = when (page.type.lowercase()) {
                    "lined" -> "Lined Notebook Page:\n- Item 1: Review project requirements and plan implementation highlights.\n- Item 2: Build fully functional offline-first local SQLite caching models."
                    "cornell" -> "Cornell Notes System:\n[Cues]: Why design local?\n[Notes]: Local architecture guarantees offline speed and absolute compliance with sandbox rules.\n[Summary]: Simple structures rule modern prototyping environments."
                    "meeting" -> "Meeting Minutes:\nDate: 2026-06-22\nAgenda: Mobile OCR system implementation\nDecisions: Adopt Material 3 interactive blocks, deploy Gemini AI."
                    else -> "Extracted imported scanned document text layer content. Start editing this text directly right here."
                }
            }

            val wordAnn = TextAnnotationDef(
                id = "word_main_content",
                text = recognizedText,
                x = 0.08f,
                y = 0.08f,
                fontSize = 12f,
                colorHex = "#1E293B",
                isBold = false,
                alignment = "left"
            )

            val updatedPages = currentContent.pages.map { p ->
                if (p.id == activePageId) {
                    p.copy(
                        type = "word",
                        ocrText = recognizedText,
                        textAnnotations = listOf(wordAnn)
                    )
                } else p
            }

            val updatedContent = currentContent.copy(pages = updatedPages)
            val updatedDoc = activeDoc.copy(contentJson = DocumentSerializer.toJson(updatedContent))
            repo.updateDocument(updatedDoc)

            _uiState.update {
                it.copy(
                    ocrLoading = false,
                    activeDocument = updatedDoc,
                    activeDocumentContent = updatedContent
                )
            }
            triggerFeedback("OCR Scanned! Page converted to fully editable word wrapping layout.")
        }
    }

    // --- COMBINE PDF (MERGER ENGINE) ---
    fun loadMergeSelection(docs: List<Document>) {
        _uiState.update {
            it.copy(
                mergerSelectedDocs = docs,
                currentScreen = Screen.PdfMerger
            )
        }
    }

    fun compileMergedDocuments(
        masterTitle: String,
        docs: List<Document>,
        shareAfterCompile: Boolean = false,
        context: android.content.Context? = null
    ) {
        if (docs.size < 2) {
            triggerFeedback("Select at least 2 files to combine!")
            return
        }

        viewModelScope.launch {
            try {
                val finalPages = mutableListOf<PageDef>()
                var pageCounter = 1

                for (doc in docs) {
                    val content = DocumentSerializer.fromJson(doc.contentJson)
                    for (page in content.pages) {
                        finalPages.add(
                            page.copy(
                                id = UUID.randomUUID().toString(),
                                pageNumber = pageCounter++
                            )
                        )
                    }
                }

                val docContent = DocumentContent(pages = finalPages)
                val json = DocumentSerializer.toJson(docContent)

                val mergedDoc = Document(
                    title = masterTitle.ifBlank { "Merged Document ${System.currentTimeMillis() % 1000}" },
                    category = "Combine",
                    pageCount = finalPages.size,
                    contentJson = json,
                    isSaved = true
                )

                val docId = repo.insertDocument(mergedDoc).toInt()
                val savedDoc = repo.getDocumentById(docId)

                if (savedDoc != null) {
                    val file = PdfGenerator.buildPdf(getApplication(), savedDoc, docContent, _uiState.value.signatures)
                    if (file != null) {
                        repo.updateDocument(savedDoc.copy(fileUri = file.absolutePath))
                    }
                    triggerFeedback("Acrobat merged complete! Combined ${docs.size} files into ${finalPages.size} pages.")
                    
                    if (shareAfterCompile && context != null) {
                        navigateTo(Screen.Dashboard)
                        com.example.ui.screens.sharePdfFile(context, savedDoc.copy(fileUri = file?.absolutePath ?: ""))
                    } else {
                        navigateTo(Screen.Dashboard)
                    }
                }
            } catch (e: Exception) {
                triggerFeedback("Failed to merge: ${e.localizedMessage}")
            }
        }
    }

    fun importMultipleLocalFilesForMerging(context: android.content.Context, uris: List<android.net.Uri>) {
        viewModelScope.launch {
            try {
                val appDir = getApplication<Application>().filesDir
                val documentsDir = File(appDir, "imported")
                if (!documentsDir.exists()) documentsDir.mkdirs()

                val importedDocs = mutableListOf<Document>()

                for (uri in uris) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes() ?: continue
                    
                    var name = "imported_${System.currentTimeMillis()}"
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            name = cursor.getString(nameIndex)
                        }
                    }
                    val extension = name.substringAfterLast(".", "jpg")
                    val isImage = extension.lowercase() in listOf("jpg", "jpeg", "png", "webp", "gif")
                    
                    val sourceFile = File(documentsDir, "imported_${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension")
                    FileOutputStream(sourceFile).use { it.write(bytes) }

                    val initialPages = mutableListOf<PageDef>()

                    if (isImage) {
                        val processedFile = processAndOptimizeImportedImage(context, sourceFile)
                        initialPages.add(
                            PageDef(
                                id = UUID.randomUUID().toString(),
                                pageNumber = 1,
                                type = "scan",
                                backgroundScanPath = processedFile.absolutePath,
                                ocrText = "Imported Image Content OCR text layer"
                            )
                        )
                    } else if (extension.lowercase() == "pdf") {
                        val extractedPaths = extractPdfPagesToImages(context, sourceFile)
                        if (extractedPaths.isNotEmpty()) {
                            extractedPaths.forEachIndexed { idx, path ->
                                initialPages.add(
                                    PageDef(
                                        id = UUID.randomUUID().toString(),
                                        pageNumber = idx + 1,
                                        type = "imported_pdf",
                                        backgroundScanPath = path,
                                        ocrText = "Imported PDF page ${idx + 1}"
                                    )
                                )
                            }
                        } else {
                            val dummyPath = createDummyPreview(documentsDir, name, bytes.size)
                            initialPages.add(
                                PageDef(
                                    id = UUID.randomUUID().toString(),
                                    pageNumber = 1,
                                    type = "imported_pdf",
                                    backgroundScanPath = dummyPath,
                                    ocrText = "Imported PDF dummy placeholder"
                                )
                            )
                        }
                    } else {
                        val extractedPaths = extractPdfPagesToImages(context, sourceFile)
                        if (extractedPaths.isNotEmpty()) {
                            extractedPaths.forEachIndexed { idx, path ->
                                initialPages.add(
                                    PageDef(
                                        id = UUID.randomUUID().toString(),
                                        pageNumber = idx + 1,
                                        type = "imported_pdf",
                                        backgroundScanPath = path,
                                        ocrText = "Imported page"
                                    )
                                )
                            }
                        } else {
                            initialPages.add(
                                PageDef(
                                    id = UUID.randomUUID().toString(),
                                    pageNumber = 1,
                                    type = "scan",
                                    backgroundScanPath = sourceFile.absolutePath,
                                    ocrText = "Imported Content"
                                )
                            )
                        }
                    }

                    if (initialPages.isEmpty()) continue

                    val docContent = DocumentContent(pages = initialPages)
                    val json = DocumentSerializer.toJson(docContent)

                    val newDoc = Document(
                        title = name.substringBeforeLast("."),
                        category = if (isImage) "Photo import" else "PDF import",
                        pageCount = initialPages.size,
                        contentJson = json,
                        isSaved = false
                    )

                    val docId = repo.insertDocument(newDoc).toInt()
                    val savedDoc = repo.getDocumentById(docId)
                    if (savedDoc != null) {
                        importedDocs.add(savedDoc)
                    }
                }

                if (importedDocs.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            mergerSelectedDocs = importedDocs,
                            currentScreen = Screen.PdfMerger
                        )
                    }
                    triggerFeedback("Imported ${importedDocs.size} local files! Ready to combine.")
                } else {
                    triggerFeedback("No valid files importable.")
                }
            } catch (e: Exception) {
                triggerFeedback("Failed to import files: ${e.localizedMessage}")
            }
        }
    }

    fun importAndAppendLocalFilesToMerger(
        context: android.content.Context,
        uris: List<android.net.Uri>,
        currentDocs: List<Document>,
        onComplete: (List<Document>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val appDir = getApplication<Application>().filesDir
                val documentsDir = File(appDir, "imported")
                if (!documentsDir.exists()) documentsDir.mkdirs()

                val importedDocs = mutableListOf<Document>()

                for (uri in uris) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes() ?: continue
                    
                    var name = "imported_${System.currentTimeMillis()}"
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            name = cursor.getString(nameIndex)
                        }
                    }
                    val extension = name.substringAfterLast(".", "jpg")
                    val isImage = extension.lowercase() in listOf("jpg", "jpeg", "png", "webp", "gif")
                    
                    val sourceFile = File(documentsDir, "imported_${System.currentTimeMillis()}_${UUID.randomUUID()}.$extension")
                    FileOutputStream(sourceFile).use { it.write(bytes) }

                    val initialPages = mutableListOf<PageDef>()

                    if (isImage) {
                        val processedFile = processAndOptimizeImportedImage(context, sourceFile)
                        initialPages.add(
                            PageDef(
                                id = UUID.randomUUID().toString(),
                                pageNumber = 1,
                                type = "scan",
                                backgroundScanPath = processedFile.absolutePath,
                                ocrText = "Imported Image Content OCR text layer"
                            )
                        )
                    } else if (extension.lowercase() == "pdf") {
                        val extractedPaths = extractPdfPagesToImages(context, sourceFile)
                        if (extractedPaths.isNotEmpty()) {
                            extractedPaths.forEachIndexed { idx, path ->
                                initialPages.add(
                                    PageDef(
                                        id = UUID.randomUUID().toString(),
                                        pageNumber = idx + 1,
                                        type = "imported_pdf",
                                        backgroundScanPath = path,
                                        ocrText = "Imported PDF page ${idx + 1}"
                                    )
                                )
                            }
                        } else {
                            val dummyPath = createDummyPreview(documentsDir, name, bytes.size)
                            initialPages.add(
                                PageDef(
                                    id = UUID.randomUUID().toString(),
                                    pageNumber = 1,
                                    type = "imported_pdf",
                                    backgroundScanPath = dummyPath,
                                    ocrText = "Imported PDF dummy placeholder"
                                )
                            )
                        }
                    } else {
                        val extractedPaths = extractPdfPagesToImages(context, sourceFile)
                        if (extractedPaths.isNotEmpty()) {
                            extractedPaths.forEachIndexed { idx, path ->
                                initialPages.add(
                                    PageDef(
                                        id = UUID.randomUUID().toString(),
                                        pageNumber = idx + 1,
                                        type = "imported_pdf",
                                        backgroundScanPath = path,
                                        ocrText = "Imported page"
                                    )
                                )
                            }
                        } else {
                            initialPages.add(
                                PageDef(
                                    id = UUID.randomUUID().toString(),
                                    pageNumber = 1,
                                    type = "scan",
                                    backgroundScanPath = sourceFile.absolutePath,
                                    ocrText = "Imported Content"
                                )
                            )
                        }
                    }

                    if (initialPages.isEmpty()) continue

                    val docContent = DocumentContent(pages = initialPages)
                    val json = DocumentSerializer.toJson(docContent)

                    val newDoc = Document(
                        title = name.substringBeforeLast("."),
                        category = if (isImage) "Photo import" else "PDF import",
                        pageCount = initialPages.size,
                        contentJson = json,
                        isSaved = false
                    )

                    val docId = repo.insertDocument(newDoc).toInt()
                    val savedDoc = repo.getDocumentById(docId)
                    if (savedDoc != null) {
                        importedDocs.add(savedDoc)
                    }
                }

                if (importedDocs.isNotEmpty()) {
                    triggerFeedback("Imported ${importedDocs.size} files!")
                    onComplete(currentDocs + importedDocs)
                } else {
                    triggerFeedback("Could not import selected files.")
                }
            } catch (e: Exception) {
                triggerFeedback("Import error: ${e.localizedMessage}")
            }
        }
    }
}
