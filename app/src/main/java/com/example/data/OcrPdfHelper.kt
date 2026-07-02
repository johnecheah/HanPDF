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
     */
    suspend fun extractPageWordsAsAnnotations(
        context: Context,
        backgroundScanPath: String
    ): List<TextAnnotationDef> = withContext(Dispatchers.IO) {
        val annotations = mutableListOf<TextAnnotationDef>()
        try {
            val file = File(backgroundScanPath)
            if (!file.exists()) {
                Log.e(TAG, "Scan background image does not exist: $backgroundScanPath")
                return@withContext emptyList()
            }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext emptyList()
            
            val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = com.google.android.gms.tasks.Tasks.await(textRecognizer.process(image))
            
            val imgWidth = bitmap.width.toFloat()
            val imgHeight = bitmap.height.toFloat()
            
            var idCounter = 0
            for (block in result.textBlocks) {
                for (line in block.lines) {
                    val text = line.text
                    if (text.isBlank()) continue
                    val box = line.boundingBox ?: continue
                    
                    // Convert to relative coordinates (0..1)
                    val rx = box.left.toFloat() / imgWidth
                    val ry = box.top.toFloat() / imgHeight
                    
                    // Approximate font size based on height
                    val heightPx = box.height().toFloat()
                    val fontSize = (heightPx / imgHeight) * 350f
                    
                    annotations.add(
                        TextAnnotationDef(
                            id = "ocr_det_${System.currentTimeMillis()}_${idCounter++}",
                            text = text,
                            x = rx.coerceIn(0f, 1f),
                            y = ry.coerceIn(0f, 1f),
                            fontSize = if (fontSize > 4f) fontSize.coerceIn(8f, 24f) else 12f,
                            colorHex = "#1E293B" // default dark slate color
                        )
                    )
                }
            }
            bitmap.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Error performing ML Kit OCR", e)
        }
        return@withContext annotations
    }

    /**
     * Performs OCR, extracts editable text boxes, AND removes the original text 
     * from the scanned background image using basic white fill/inpainting.
     * Returns a pair of (List of TextAnnotationDef, new background image path).
     */
    suspend fun ocrAndCleanPageImage(
        context: Context,
        backgroundScanPath: String
    ): Pair<List<TextAnnotationDef>, String?> = withContext(Dispatchers.IO) {
        val annotations = mutableListOf<TextAnnotationDef>()
        var cleanedPath: String? = null
        try {
            val file = File(backgroundScanPath)
            if (!file.exists()) {
                Log.e(TAG, "Scan background image does not exist: $backgroundScanPath")
                return@withContext Pair(emptyList(), null)
            }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext Pair(emptyList(), null)
            
            val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = com.google.android.gms.tasks.Tasks.await(textRecognizer.process(image))
            
            val imgWidth = bitmap.width.toFloat()
            val imgHeight = bitmap.height.toFloat()
            
            var idCounter = 0
            for (block in result.textBlocks) {
                for (line in block.lines) {
                    val text = line.text
                    if (text.isBlank()) continue
                    val box = line.boundingBox ?: continue
                    
                    val rx = box.left.toFloat() / imgWidth
                    val ry = box.top.toFloat() / imgHeight
                    val heightPx = box.height().toFloat()
                    val fontSize = (heightPx / imgHeight) * 350f
                    
                    annotations.add(
                        TextAnnotationDef(
                            id = "ocr_det_${System.currentTimeMillis()}_${idCounter++}",
                            text = text,
                            x = rx.coerceIn(0f, 1f),
                            y = ry.coerceIn(0f, 1f),
                            fontSize = if (fontSize > 4f) fontSize.coerceIn(8f, 24f) else 12f,
                            colorHex = "#1E293B"
                        )
                    )
                }
            }
            
            // Clean/Inpaint background image
            val cleanedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(cleanedBitmap)
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
            
            // Save cleaned image to files dir
            val cleanedFile = File(context.filesDir, "cleaned_ocr_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
            val fos = FileOutputStream(cleanedFile)
            cleanedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            fos.close()
            
            cleanedPath = cleanedFile.absolutePath
            
            bitmap.recycle()
            cleanedBitmap.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Error performing ML Kit OCR and image cleaning", e)
        }
        return@withContext Pair(annotations, cleanedPath)
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
