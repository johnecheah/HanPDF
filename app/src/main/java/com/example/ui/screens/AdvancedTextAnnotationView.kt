package com.example.ui.screens

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import com.example.data.TextAnnotationDef
import com.example.data.TextAnnotationRenderer
import kotlin.math.abs
import kotlin.math.max

class AdvancedTextAnnotationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // Reference frame — must match what PdfGenerator uses (curW / curH there).
    var refWidthPx: Float = 400f
    var refHeightPx: Float = 600f
    var zoomScale: Float = 1f

    private var def: TextAnnotationDef = TextAnnotationDef(id = "", text = "Tap to edit", x = 0f, y = 0f)
    private var lastBounds = RectF(0f, 0f, 120f, 60f)
    private val isMainContent get() = def.id == "word_main_content"

    private val editText: EditText

    var isSelectedState: Boolean = false
        set(value) {
            field = value
            if (!value) finishEditing()
            invalidate()
        }

    var isOutlineVisible: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    var isEditing: Boolean = false
        private set

    private val borderPaint = Paint().apply {
        color = 0xFF4F46E5.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }
    private val handlePaint = Paint().apply { color = 0xFF4F46E5.toInt(); style = Paint.Style.FILL }
    private val handleBorderPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 4f }
    private val deleteHandlePaint = Paint().apply { color = 0xFFEF4444.toInt(); style = Paint.Style.FILL }
    private val deleteTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 24f
        textAlign = Paint.Align.CENTER
        style = Paint.Style.FILL
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private var lastX = 0f
    private var lastY = 0f
    private var initialRawX = 0f
    private var initialFontSize = 16f
    private var isDragging = false
    private var isResizing = false

    private val handleRadius = 20f
    private val handleOuterTouchRadius = 48f

    private var lastTapTime = 0L
    private val doubleTapThreshold = 300L

    var onDefChanged: ((TextAnnotationDef) -> Unit)? = null
    var onSelected: (() -> Unit)? = null
    var onDeleteRequested: (() -> Unit)? = null

    var onDragStarted: (() -> Unit)? = null
    var onDragging: ((view: View, tx: Float, ty: Float, width: Float, height: Float, rotation: Float) -> Unit)? = null
    var onDragEnded: (() -> Unit)? = null

    init {
        setWillNotDraw(false)
        editText = EditText(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            background = null
            gravity = Gravity.TOP or Gravity.START
            setPadding(0, 0, 0, 0)
            visibility = View.GONE
            inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
            imeOptions = EditorInfo.IME_ACTION_DONE
        }
        addView(editText)

        editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { finishEditing(); true } else false
        }
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                def = def.copy(text = s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        editText.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus && isEditing) finishEditing() }
    }

    /** Call this whenever the annotation's persisted data (or the page's reference size) changes. */
    fun bind(newDef: TextAnnotationDef, refW: Float, refH: Float) {
        def = newDef
        refWidthPx = refW
        refHeightPx = refH
        relayout()
    }

    private fun relayout() {
        lastBounds = TextAnnotationRenderer.computeBounds(def, 0f, 0f, refWidthPx, isMainContent)
        val w = max(lastBounds.width().toInt(), 40)
        val h = max(lastBounds.height().toInt(), 30)
        translationX = def.x * refWidthPx + lastBounds.left
        translationY = def.y * refHeightPx + lastBounds.top
        rotation = def.rotation
        layoutParams?.let { lp ->
            if (lp.width != w || lp.height != h) { lp.width = w; lp.height = h; layoutParams = lp }
        }
        if (!isEditing) invalidate()
    }

    private fun pushChange() {
        val newX = (translationX - lastBounds.left) / refWidthPx
        val newY = (translationY - lastBounds.top) / refHeightPx
        def = def.copy(x = newX.coerceIn(0f, 0.99f), y = newY.coerceIn(0f, 0.99f))
        onDefChanged?.invoke(def)
    }

    fun startEditing() {
        if (isEditing) return
        isEditing = true
        editText.setText(def.text)
        editText.setTextColor(try { Color.parseColor(def.colorHex) } catch (e: Exception) { Color.BLACK })
        editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, TextAnnotationRenderer.buildPaint(def, refWidthPx).textSize)
        editText.visibility = View.VISIBLE
        editText.requestFocus()
        editText.setSelection(editText.text.length)
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        invalidate()
    }

    fun finishEditing() {
        if (!isEditing) return
        isEditing = false
        editText.clearFocus()
        editText.visibility = View.GONE
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(windowToken, 0)
        def = def.copy(text = editText.text.toString())
        relayout()
        onDefChanged?.invoke(def)
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val rawX = event.rawX
        val rawY = event.rawY
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isOutlineVisible = true
                onSelected?.invoke()
                isSelectedState = true

                if (isSelectedState && isNearDeleteHandle(x, y)) {
                    onDeleteRequested?.invoke()
                    return true
                }

                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTapTime < doubleTapThreshold) {
                    startEditing()
                    return true
                }
                lastTapTime = currentTime

                if (isSelectedState && isNearResizeCorner(x, y)) {
                    isResizing = true
                    isDragging = false
                } else {
                    isDragging = true
                    isResizing = false
                    onDragStarted?.invoke()
                }

                lastX = rawX
                lastY = rawY
                initialRawX = rawX
                initialFontSize = def.fontSize
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = (rawX - lastX) / zoomScale.coerceAtLeast(0.01f)
                val dy = (rawY - lastY) / zoomScale.coerceAtLeast(0.01f)

                if (isResizing) {
                    val totalDeltaX = (rawX - initialRawX) / zoomScale.coerceAtLeast(0.01f)
                    val scaleFactor = (1f + totalDeltaX / max(lastBounds.width(), 40f)).coerceIn(0.2f, 6f)
                    def = def.copy(fontSize = (initialFontSize * scaleFactor).coerceIn(6f, 200f))
                    relayout()
                } else if (isDragging && !isEditing) {
                    translationX += dx
                    translationY += dy
                    onDragging?.invoke(this, translationX, translationY, width.toFloat(), height.toFloat(), rotation)
                }

                lastX = rawX
                lastY = rawY
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging || isResizing) {
                    val wasDragging = isDragging
                    isDragging = false
                    isResizing = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                    pushChange()
                    if (wasDragging) {
                        onDragEnded?.invoke()
                    }
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun isNearResizeCorner(x: Float, y: Float): Boolean {
        return abs(x - width) < handleOuterTouchRadius && abs(y - height) < handleOuterTouchRadius
    }

    private fun isNearDeleteHandle(x: Float, y: Float): Boolean {
        return abs(x - width) < handleOuterTouchRadius && abs(y - 0) < handleOuterTouchRadius
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!isEditing) {
            TextAnnotationRenderer.draw(canvas, def.copy(rotation = 0f), -lastBounds.left, -lastBounds.top, refWidthPx, isMainContent)
        }

        if (isSelectedState && isOutlineVisible) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), borderPaint)
            canvas.drawCircle(0f, 0f, handleRadius, handlePaint)
            canvas.drawCircle(0f, 0f, handleRadius, handleBorderPaint)
            canvas.drawCircle(0f, height.toFloat(), handleRadius, handlePaint)
            canvas.drawCircle(0f, height.toFloat(), handleRadius, handleBorderPaint)
            canvas.drawCircle(width.toFloat(), height.toFloat(), handleRadius, handlePaint)
            canvas.drawCircle(width.toFloat(), height.toFloat(), handleRadius, handleBorderPaint)
            canvas.drawCircle(width.toFloat(), 0f, handleRadius + 4f, deleteHandlePaint)
            canvas.drawCircle(width.toFloat(), 0f, handleRadius + 4f, handleBorderPaint)
            canvas.drawText("✕", width.toFloat(), 8f, deleteTextPaint)
        }
    }
}
