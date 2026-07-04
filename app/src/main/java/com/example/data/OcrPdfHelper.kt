package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.pdmodel.graphics.state.RenderingMode
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object OcrPdfHelper {
    private const val TAG = "OcrPdfHelper"

    /**
     * Convert Canvas/View coordinates to PDF coordinates
     */
    fun convertToPdfCoordinates(
        viewX: Float,
        viewY: Float,
        viewWidth: Float,
        viewHeight: Float,
        pdfPageWidth: Float,      // PDPage mediaBox width
        pdfPageHeight: Float,     // PDPage mediaBox height
        scale: Float              // Current zoom scale
    ): Pair<Float, Float> {
        // Adjust for scale
        val scaledX = viewX / scale
        val scaledY = viewY / scale

        // Convert Y axis (PDF origin is bottom-left)
        val pdfX = scaledX
        val pdfY = pdfPageHeight - scaledY   // <-- This is the most important line

        return Pair(pdfX, pdfY)
    }

    /**
     * Extracts text from a background page image using local ML Kit,
     * returning a list of editable TextAnnotationDef items mapped to positions.
     *
     * Uses WORD-level (ML Kit "elements") granularity rather than line-level.
     * Line-level grouping merges adjacent table columns into one garbled text
     * run on tabular documents (receipts, invoices) — word-level keeps every
     * label/number in its own correctly positioned, correctly sized box.
     */
    suspend fun extractPageWordsAsAnnotations(
        context: Context,
        backgroundScanPath: String
    ): List<TextAnnotationDef> = withContext(Dispatchers.IO) {
        val file = File(backgroundScanPath)
        if (!file.exists()) return@withContext emptyList()
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext emptyList()
        val result = runOcr(bitmap)
        val annotations = buildWordAnnotations(result, bitmap.width.toFloat(), bitmap.height.toFloat())
        bitmap.recycle()
        annotations
    }

    /**
     * Performs OCR, extracts editable WORD-level text boxes, AND removes the
     * original text from the scanned background image (line-level white fill,
     * so there's no gap of original ink left between words). Returns
     * (editable TextAnnotationDef list, path to the cleaned background image).
     */
    suspend fun ocrAndCleanPageImage(
        context: Context,
        backgroundScanPath: String
    ): Pair<List<TextAnnotationDef>, String?> = withContext(Dispatchers.IO) {
        try {
            val file = File(backgroundScanPath)
            if (!file.exists()) {
                Log.e(TAG, "Scan background image does not exist: $backgroundScanPath")
                return@withContext Pair(emptyList(), null)
            }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                ?: return@withContext Pair(emptyList(), null)

            val result = runOcr(bitmap)
            val annotations = buildWordAnnotations(result, bitmap.width.toFloat(), bitmap.height.toFloat())

            // Clean/inpaint background: white-fill each LINE's box (not just each
            // word) so no thin sliver of original ink is left between words.
            val cleanedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(cleanedBitmap)
            val paint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
            for (block in result.textBlocks) {
                for (line in block.lines) {
                    val box = line.boundingBox ?: continue
                    // Wider horizontal padding (covers edge slivers left by
                    // the recognizer's box) but tighter vertical padding
                    // (avoids eating into a ruled line sitting close above
                    // or below the text).
                    val hExpand = (box.height() * 0.22f).coerceIn(2f, 8f)
                    val vExpand = (box.height() * 0.10f).coerceIn(1f, 4f)
                    canvas.drawRect(
                        (box.left - hExpand),
                        (box.top - vExpand),
                        (box.right + hExpand),
                        (box.bottom + vExpand),
                        paint
                    )
                }
            }

            val cleanedFile = File(context.filesDir, "cleaned_ocr_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
            FileOutputStream(cleanedFile).use { fos ->
                cleanedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
            }

            bitmap.recycle()
            cleanedBitmap.recycle()
            Pair(annotations, cleanedFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Error performing ML Kit OCR and image cleaning", e)
            Pair(emptyList(), null)
        }
    }

    private suspend fun runOcr(bitmap: Bitmap): com.google.mlkit.vision.text.Text = withContext(Dispatchers.IO) {
        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)
        com.google.android.gms.tasks.Tasks.await(textRecognizer.process(image))
    }

    /**
     * ML Kit occasionally reports the same text twice as two heavily
     * overlapping boxes (common on small/compact labels). Without this,
     * you get doubled/ghosted text like "UUSERGAN ENG JOO" instead of
     * "USER GAN ENG JOO".
     */
    private fun dedupeOverlapping(
        words: List<Pair<String, android.graphics.Rect>>
    ): List<Pair<String, android.graphics.Rect>> {
        val sorted = words.sortedByDescending { it.second.width().toLong() * it.second.height().toLong() }
        val kept = mutableListOf<Pair<String, android.graphics.Rect>>()
        for (candidate in sorted) {
            val box = candidate.second
            val boxArea = (box.width() * box.height()).toFloat()
            val isDup = kept.any { existing ->
                val e = existing.second
                val left = maxOf(box.left, e.left)
                val top = maxOf(box.top, e.top)
                val right = minOf(box.right, e.right)
                val bottom = minOf(box.bottom, e.bottom)
                if (right <= left || bottom <= top) return@any false
                val overlapArea = ((right - left) * (bottom - top)).toFloat()
                val minArea = minOf(boxArea, (e.width() * e.height()).toFloat())
                minArea > 0f && (overlapArea / minArea) > 0.5f
            }
            if (!isDup) kept.add(candidate)
        }
        return kept
    }

    /**
     * Converts ML Kit word-level results into TextAnnotationDef objects.
     * IMPORTANT: fontSize here is expressed in the SAME "400px reference width"
     * unit that TextAnnotationRenderer / PdfGenerator use everywhere else in
     * the app. Get this unit wrong and every OCR'd word renders at the wrong
     * (usually doubled) size once it goes through the normal text pipeline.
     */
    private fun buildWordAnnotations(
        result: com.google.mlkit.vision.text.Text,
        imgWidth: Float,
        imgHeight: Float
    ): List<TextAnnotationDef> {
        val rawWords = mutableListOf<Pair<String, android.graphics.Rect>>()
        for (block in result.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    val text = element.text
                    if (text.isBlank()) continue
                    val box = element.boundingBox ?: continue
                    rawWords.add(text to box)
                }
            }
        }

        val dedupedWords = dedupeOverlapping(rawWords)
        val annotations = mutableListOf<TextAnnotationDef>()
        var idCounter = 0

        for ((text, box) in dedupedWords) {
            val rx = box.left.toFloat() / imgWidth
            val ry = box.top.toFloat() / imgHeight
            val boxWidthPx = box.width().toFloat()

            var fontSize = (box.height().toFloat() * (400f / imgWidth)).coerceIn(5f, 80f)

            // Auto-fit: target slightly NARROWER than the detected box (94%)
            // rather than letting it fill the full box — guarantees a real
            // gap survives between two tightly-kerned words (dates, times,
            // numbers) even when the substitute font is a bit wider.
            val probeDef = TextAnnotationDef(id = "", text = text, x = 0f, y = 0f, fontSize = fontSize)
            val probePaint = TextAnnotationRenderer.buildPaint(probeDef, imgWidth)
            val measuredWidth = probePaint.measureText(text)
            val targetWidth = boxWidthPx * 0.94f
            if (measuredWidth > targetWidth && measuredWidth > 0f) {
                fontSize = (fontSize * (targetWidth / measuredWidth)).coerceAtLeast(4f)
            }

            annotations.add(
                TextAnnotationDef(
                    id = "ocr_det_${System.currentTimeMillis()}_${idCounter++}",
                    text = text,
                    x = rx.coerceIn(0f, 1f),
                    y = ry.coerceIn(0f, 1f),
                    fontSize = fontSize,
                    colorHex = "#1E293B"
                )
            )
        }
        return annotations
    }

    /**
     * Processes an existing PDF, performs OCR on each page, 
     * and appends an invisible text layer to create a searchable PDF.
     */
    suspend fun makePdfEditable(
        context: Context,
        inputPdfPath: String,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            PDFBoxResourceLoader.init(context)
            val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val inputFile = File(inputPdfPath)
            if (!inputFile.exists()) {
                Log.e(TAG, "Input PDF file does not exist: $inputPdfPath")
                return@withContext false
            }
            
            val pfd = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(pfd)
            val outputDocument = PDDocument.load(inputFile)
            
            val pageCount = pdfRenderer.pageCount
            for (pageIndex in 0 until pageCount) {
                val page = pdfRenderer.openPage(pageIndex)
                val width = page.width
                val height = page.height
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                
                val image = InputImage.fromBitmap(bitmap, 0)
                val result = com.google.android.gms.tasks.Tasks.await(textRecognizer.process(image))
                
                val pdPage: PDPage = outputDocument.getPage(pageIndex)
                val contentStream = PDPageContentStream(
                    outputDocument, 
                    pdPage, 
                    PDPageContentStream.AppendMode.APPEND, 
                    true, 
                    true
                )
                
                val pageWidth = pdPage.mediaBox.width
                val pageHeight = pdPage.mediaBox.height
                val scaleX = pageWidth / bitmap.width.toFloat()
                val scaleY = pageHeight / bitmap.height.toFloat()
                
                contentStream.setRenderingMode(RenderingMode.NEITHER)
                
                for (block in result.textBlocks) {
                    for (line in block.lines) {
                        for (element in line.elements) {
                            val text = element.text
                            if (text.isBlank()) continue
                            val boundingBox = element.boundingBox ?: continue
                            
                            val x = boundingBox.left * scaleX
                            val y = pageHeight - (boundingBox.bottom * scaleY)
                            val elementHeight = boundingBox.height().toFloat() * scaleY
                            val fontSize = if (elementHeight > 1f) elementHeight else 10f
                            
                            contentStream.beginText()
                            contentStream.setFont(PDType1Font.HELVETICA, fontSize)
                            contentStream.newLineAtOffset(x, y)
                            contentStream.showText(text)
                            contentStream.endText()
                        }
                    }
                }
                contentStream.close()
                bitmap.recycle()
            }
            
            outputDocument.save(FileOutputStream(outputFile))
            outputDocument.close()
            pdfRenderer.close()
            pfd.close()
            
            Log.d(TAG, "Searchable PDF successfully generated at: ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error in makePdfEditable", e)
            false
        }
    }

    /**
     * Processes an existing PDF, performs OCR on each page, removes original rasterized text
     * by inpainting/filling detected areas with white, and overlays actual editable text on top.
     */
    suspend fun makePdfFullyEditable(
        context: Context,
        inputPdfPath: String,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            PDFBoxResourceLoader.init(context)
            val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val inputFile = File(inputPdfPath)
            if (!inputFile.exists()) {
                Log.e(TAG, "Input PDF file does not exist: $inputPdfPath")
                return@withContext false
            }
            
            val pfd = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(pfd)
            val outputDocument = PDDocument()
            
            for (pageIndex in 0 until pdfRenderer.pageCount) {
                val page = pdfRenderer.openPage(pageIndex)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                
                val image = InputImage.fromBitmap(bitmap, 0)
                val result = com.google.android.gms.tasks.Tasks.await(textRecognizer.process(image))
                
                // Clean the text from the bitmap
                val cleanedBitmap = cleanTextFromBitmap(bitmap, result)
                
                val pdPage = PDPage(PDRectangle(cleanedBitmap.width.toFloat(), cleanedBitmap.height.toFloat()))
                outputDocument.addPage(pdPage)
                
                val contentStream = PDPageContentStream(outputDocument, pdPage)
                val pdImage = LosslessFactory.createFromImage(outputDocument, cleanedBitmap)
                contentStream.drawImage(pdImage, 0f, 0f, pdPage.mediaBox.width, pdPage.mediaBox.height)
                
                val scaleX = pdPage.mediaBox.width / cleanedBitmap.width.toFloat()
                val scaleY = pdPage.mediaBox.height / cleanedBitmap.height.toFloat()
                
                for (block in result.textBlocks) {
                    for (line in block.lines) {
                        val text = line.text.trim()
                        if (text.isEmpty()) continue
                        
                        val bbox = line.boundingBox ?: continue
                        val x = bbox.left * scaleX
                        val y = pdPage.mediaBox.height - (bbox.bottom * scaleY)
                        
                        contentStream.beginText()
                        contentStream.setFont(PDType1Font.HELVETICA, 11f)
                        contentStream.newLineAtOffset(x, y)
                        contentStream.showText(text)
                        contentStream.endText()
                    }
                }
                
                contentStream.close()
                bitmap.recycle()
                cleanedBitmap.recycle()
            }
            
            outputDocument.save(FileOutputStream(outputFile))
            outputDocument.close()
            pdfRenderer.close()
            pfd.close()
            
            Log.d(TAG, "Fully editable PDF successfully generated at: ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error in makePdfFullyEditable", e)
            false
        }
    }

    private fun cleanTextFromBitmap(original: Bitmap, result: com.google.mlkit.vision.text.Text): Bitmap {
        val cleaned = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(cleaned)
        val paint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        
        for (block in result.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox ?: continue
                val expand = 4
                canvas.drawRect(
                    (box.left - expand).toFloat(),
                    (box.top - expand).toFloat(),
                    (box.right + expand).toFloat(),
                    (box.bottom + expand).toFloat(),
                    paint
                )
            }
        }
        return cleaned
    }

    /**
     * Checks if a PDF already contains a searchable/editable text layer.
     */
    private fun hasSearchableText(document: PDDocument): Boolean {
        return try {
            val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
            stripper.getText(document).trim().length > 50
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Extract text with position, font size, etc. from existing PDF text layer.
     */
    fun extractTextPositions(document: PDDocument, pageIndex: Int): List<com.tom_roush.pdfbox.text.TextPosition> {
        val positions = ArrayList<com.tom_roush.pdfbox.text.TextPosition>()
        try {
            val stripper = object : com.tom_roush.pdfbox.text.PDFTextStripper() {
                override fun writeString(text: String?, textPositions: MutableList<com.tom_roush.pdfbox.text.TextPosition>?) {
                    if (textPositions != null) {
                        positions.addAll(textPositions)
                    }
                    super.writeString(text, textPositions)
                }
            }
            stripper.startPage = pageIndex + 1
            stripper.endPage = pageIndex + 1
            stripper.getText(document)  // trigger processing
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text positions", e)
        }
        return positions
    }

    /**
     * Add extracted text positions as invisible/searchable layer.
     */
    private fun addTextPositionsToPage(
        contentStream: PDPageContentStream,
        textPositions: List<com.tom_roush.pdfbox.text.TextPosition>,
        pdPage: PDPage
    ) {
        val pageHeight = pdPage.mediaBox.height
        try {
            contentStream.setRenderingMode(RenderingMode.NEITHER)
        } catch (e: Exception) {}

        for (pos in textPositions) {
            try {
                contentStream.beginText()
                
                // === Better Font Size Scaling ===
                var fontSize = pos.fontSizeInPt
                fontSize = kotlin.math.max(6f, kotlin.math.min(48f, fontSize))           // Clamp size
                fontSize = (fontSize * 0.95f).coerceAtLeast(7f)  // Slight adjustment for better visual match
                contentStream.setFont(PDType1Font.HELVETICA, fontSize)

                // Convert coordinates (PDF is bottom-up)
                val x = pos.x
                val y = pageHeight - pos.y  // Important adjustment

                contentStream.newLineAtOffset(x, y)
                contentStream.showText(pos.unicode)
                contentStream.endText()
            } catch (e: Exception) {
                // Skip problematic characters
            }
        }
    }

    /**
     * Render PDF page to Bitmap for OCR.
     */
    private fun renderPageToBitmap(renderer: PdfRenderer, pageIndex: Int): Bitmap {
        val page = renderer.openPage(pageIndex)
        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        return bitmap
    }

    /**
     * Run ML Kit OCR on Bitmap.
     */
    private suspend fun runOcrOnBitmap(
        recognizer: com.google.mlkit.vision.text.TextRecognizer,
        bitmap: Bitmap
    ): com.google.mlkit.vision.text.Text = withContext(Dispatchers.IO) {
        val image = InputImage.fromBitmap(bitmap, 0)
        com.google.android.gms.tasks.Tasks.await(recognizer.process(image))
    }

    /**
     * Add OCR result to PDF as invisible text layer.
     */
    private fun addOcrResultToPage(
        contentStream: PDPageContentStream,
        result: com.google.mlkit.vision.text.Text,
        pdPage: PDPage,
        bitmap: Bitmap
    ) {
        val pageWidth = pdPage.mediaBox.width
        val pageHeight = pdPage.mediaBox.height

        val scaleX = pageWidth / bitmap.width.toFloat()
        val scaleY = pageHeight / bitmap.height.toFloat()

        try {
            contentStream.setRenderingMode(RenderingMode.NEITHER)
        } catch (e: Exception) {}

        for (block in result.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    val text = element.text.trim()
                    if (text.isEmpty()) continue

                    val box = element.boundingBox ?: continue

                    try {
                        contentStream.beginText()
                        
                        // === Better Font Size Scaling for OCR ===
                        val boxHeight = box.height().toFloat()
                        var estimatedFontSize = boxHeight * scaleY * 0.85f   // 0.85 is a good empirical factor
                        estimatedFontSize = kotlin.math.max(7f, kotlin.math.min(36f, estimatedFontSize)) // Clamp
                        contentStream.setFont(PDType1Font.HELVETICA, estimatedFontSize)

                        // Convert to PDF coordinates
                        val x = box.left * scaleX
                        val y = pageHeight - (box.bottom * scaleY)

                        contentStream.newLineAtOffset(x, y)
                        contentStream.showText(text)
                        contentStream.endText()
                    } catch (e: Exception) {}
                }
            }
        }
    }

    /**
     * Smart conversion: Checks if the PDF already has a text layer. If yes, saves as-is.
     * If no (scanned PDF), runs full editable scanned page reconstruction with inpainting or OCR overlay.
     */
    suspend fun makePdfEditableSmart(
        context: Context,
        inputFile: File,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        var pdfRenderer: PdfRenderer? = null
        var pfd: ParcelFileDescriptor? = null

        try {
            PDFBoxResourceLoader.init(context)
            document = PDDocument.load(inputFile)
            pfd = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(pfd)

            val hasSearchableText = hasSearchableText(document)

            if (hasSearchableText) {
                // Already has text layer -> just save as-is
                document.save(FileOutputStream(outputFile))
                return@withContext true
            }

            // Scanned PDF - we need to add text layer
            val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            for (pageIndex in 0 until document.numberOfPages) {
                val pdPage: PDPage = document.getPage(pageIndex)

                val contentStream = PDPageContentStream(
                    document,
                    pdPage,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
                )

                // 1. Try to extract existing text positions
                val textPositions = extractTextPositions(document, pageIndex)

                if (textPositions.isNotEmpty()) {
                    addTextPositionsToPage(contentStream, textPositions, pdPage)
                } else {
                    // 2. OCR Fallback
                    val bitmap = renderPageToBitmap(pdfRenderer, pageIndex)
                    val ocrResult = runOcrOnBitmap(textRecognizer, bitmap)
                    addOcrResultToPage(contentStream, ocrResult, pdPage, bitmap)
                    bitmap.recycle()
                }

                contentStream.close()
            }

            document.save(FileOutputStream(outputFile))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error in makePdfEditableSmart", e)
            false
        } finally {
            try {
                document?.close()
            } catch (e: Exception) {}
            try {
                pdfRenderer?.close()
            } catch (e: Exception) {}
            try {
                pfd?.close()
            } catch (e: Exception) {}
        }
    }
}
