package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.example.data.DocumentSerializer
import com.example.data.SignatureProfile
import kotlin.math.abs

class ResizableDraggableSignatureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var lastX = 0f
    private var lastY = 0f
    private var initialRawX = 0f
    private var initialRawY = 0f
    private var isDragging = false
    private var isResizing = false
    private val handleSize = 30f // px
    private var handlePaint = Paint().apply {
        color = 0xFF2196F3.toInt()
        style = Paint.Style.FILL
    }
    private var borderPaint = Paint().apply {
        color = 0xFF2196F3.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private var deletePaint = Paint().apply {
        color = 0xFFEF4444.toInt()
        style = Paint.Style.FILL
    }
    private var deleteTextPaint = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 22f
        textAlign = Paint.Align.CENTER
        style = Paint.Style.FILL
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private var initialWidth = 0
    private var initialHeight = 0
    private val resizeHandleRect = RectF()

    // Loaded image cache
    private var imagePath: String? = null
    private var imageBitmap: Bitmap? = null

    // Vector drawing points cache
    private var vectorPoints: List<com.example.data.PointDef> = emptyList()
    private var baseStrokeWidth = 4f
    private var vectorPenType: String = "pen"
    private var vectorPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    var onSignatureChanged: ((Float, Float, Float, Float) -> Unit)? = null // (translationX, translationY, newWidth, newHeight)
    var onSelected: (() -> Unit)? = null
    var onDeleteRequested: (() -> Unit)? = null

    var onDragStarted: (() -> Unit)? = null
    var onDragging: ((view: View, tx: Float, ty: Float, width: Float, height: Float, rotation: Float) -> Unit)? = null
    var onDragEnded: (() -> Unit)? = null

    var isSelectedState: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    init {
        isFocusable = true
        isClickable = true
    }

    fun setSignatureProfile(profile: SignatureProfile?, overrideColorHex: String) {
        if (profile == null) {
            imagePath = null
            imageBitmap = null
            vectorPoints = emptyList()
            invalidate()
            return
        }

        if (profile.pathDataJson.startsWith("image:")) {
            val path = profile.pathDataJson.removePrefix("image:")
            if (path != imagePath) {
                imagePath = path
                imageBitmap = try {
                    BitmapFactory.decodeFile(path)
                } catch (e: Exception) {
                    null
                }
            }
            vectorPoints = emptyList()
        } else {
            imagePath = null
            imageBitmap = null
            vectorPoints = DocumentSerializer.pointsFromJson(profile.pathDataJson)
            val parsedColor = try {
                android.graphics.Color.parseColor(overrideColorHex.ifBlank { profile.colorHex })
            } catch (e: Exception) {
                android.graphics.Color.BLACK
            }
            vectorPaint.color = parsedColor
            baseStrokeWidth = profile.strokeWidth
            vectorPenType = profile.penType
            applyVectorPaintStyle()
        }
        invalidate()
    }

    private fun applyVectorPaintStyle() {
        val base = (baseStrokeWidth * (width.toFloat() / com.example.data.SignaturePathUtils.BASE_SCALE_WIDTH)).coerceAtLeast(4f)
        vectorPaint.strokeWidth = base * com.example.data.SignaturePathUtils.thicknessMultiplier(vectorPenType)
        vectorPaint.alpha = com.example.data.SignaturePathUtils.alphaForPenType(vectorPenType)
        val dash = com.example.data.SignaturePathUtils.dashIntervals(vectorPenType)
        vectorPaint.pathEffect = if (dash != null) android.graphics.DashPathEffect(dash, 0f) else null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        val rawX = event.rawX
        val rawY = event.rawY

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                onSelected?.invoke()

                if (isSelectedState && isNearDeleteHandle(x, y)) {
                    onDeleteRequested?.invoke()
                    return true
                }

                lastX = rawX
                lastY = rawY
                initialRawX = rawX
                initialRawY = rawY
                initialWidth = width
                initialHeight = height

                if (isNearResizeHandle(x, y)) {
                    isResizing = true
                    isDragging = false
                } else {
                    isDragging = true
                    isResizing = false
                    onDragStarted?.invoke()
                }
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = rawX - lastX
                val dy = rawY - lastY

                if (isResizing) {
                    val totalDeltaX = rawX - initialRawX
                    val ratio = if (initialHeight > 0) initialWidth.toFloat() / initialHeight.toFloat() else 1f
                    val newWidth = (initialWidth + totalDeltaX).coerceAtLeast(60f).toInt()
                    val newHeight = (newWidth / ratio).toInt().coerceAtLeast(30)

                    layoutParams?.let { lp ->
                        lp.width = newWidth
                        lp.height = newHeight
                        layoutParams = lp
                    }
                    requestLayout()
                } else if (isDragging) {
                    translationX += dx
                    translationY += dy
                    onDragging?.invoke(this, translationX, translationY, width.toFloat(), height.toFloat(), 0f)
                }

                lastX = rawX
                lastY = rawY
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val wasDragging = isDragging
                isDragging = false
                isResizing = false
                parent?.requestDisallowInterceptTouchEvent(false)
                onSignatureChanged?.invoke(translationX, translationY, width.toFloat(), height.toFloat())
                if (wasDragging) {
                    onDragEnded?.invoke()
                }
            }
        }
        return true
    }

    private fun isNearResizeHandle(x: Float, y: Float): Boolean {
        if (!isSelectedState) return false
        val threshold = 50f
        return abs(x - width) < threshold && abs(y - height) < threshold
    }

    private fun isNearDeleteHandle(x: Float, y: Float): Boolean {
        if (!isSelectedState) return false
        val threshold = 50f
        return abs(x - width) < threshold && abs(y - 0) < threshold
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw image bitmap or vector paths
        val bmp = imageBitmap
        if (bmp != null) {
            val destRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawBitmap(bmp, null, destRect, null)
        } else if (vectorPoints.isNotEmpty()) {
            applyVectorPaintStyle()
            val path = com.example.data.SignaturePathUtils.buildSmoothedPath(
                vectorPoints, width.toFloat(), height.toFloat()
            )
            canvas.drawPath(path, vectorPaint)
            if (vectorPenType == "calligraphy") {
                val offsetPath = android.graphics.Path(path).apply { offset(1.5f, 1.5f) }
                val offsetPaint = Paint(vectorPaint).apply {
                    alpha = (vectorPaint.alpha * 0.7f).toInt()
                    strokeWidth = vectorPaint.strokeWidth * 0.7f
                }
                canvas.drawPath(offsetPath, offsetPaint)
            }
        }

        // Selected bounding borders and control handles
        if (isSelectedState) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), borderPaint)
            
            // Draw resize handle (bottom right)
            resizeHandleRect.set(width - handleSize, height - handleSize, width.toFloat(), height.toFloat())
            canvas.drawRect(resizeHandleRect, handlePaint)

            // Draw delete handle (top right)
            canvas.drawCircle(width.toFloat(), 0f, 15f, deletePaint)
            canvas.drawText("✕", width.toFloat(), 7f, deleteTextPaint)
        }
    }

    fun setAnnotationPosition(pdfX: Float, pdfY: Float) {
        translationX = pdfX
        translationY = pdfY
    }
}
