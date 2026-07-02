package com.example.ui.screens

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.text.Editable
import android.text.Layout
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.StrikethroughSpan
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.max

class AdvancedTextAnnotationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // Properties Data Model for Easy Save & Load
    data class AnnotationProperties(
        var id: String = "",
        var text: String = "Tap to edit",
        var x: Float = 0f,
        var y: Float = 0f,
        var width: Int = LayoutParams.WRAP_CONTENT,
        var height: Int = LayoutParams.WRAP_CONTENT,
        var fontSize: Float = 16f, // sp
        var textColorHex: String = "#000000",
        var bgColorHex: String = "transparent",
        var fontName: String = "sans-serif",
        var isBold: Boolean = false,
        var isItalic: Boolean = false,
        var hasUnderline: Boolean = false,
        var hasStrikeThrough: Boolean = false,
        var hasOutline: Boolean = false,
        var outlineColorHex: String = "#FFFFFF",
        var outlineThickness: Float = 4f, // px
        var alignment: String = "left", // "left", "center", "right", "justified"
        var rotationAngle: Float = 0f
    )

    private var currentProps = AnnotationProperties()

    // Child Views
    private val textView: CustomStrokeTextView
    private val editText: EditText

    // Selection and State Flags
    var isSelectedState: Boolean = false
        set(value) {
            field = value
            if (!value) {
                // Hide edit text if we lose selection
                finishEditing()
            }
            invalidate()
        }

    var isEditing: Boolean = false
        private set

    // Drawing Paints
    private val borderPaint = Paint().apply {
        color = 0xFF4F46E5.toInt() // Indigo/Modern Accent
        style = Paint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }

    private val handlePaint = Paint().apply {
        color = 0xFF4F46E5.toInt()
        style = Paint.Style.FILL
    }

    private val handleBorderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val deleteHandlePaint = Paint().apply {
        color = 0xFFEF4444.toInt() // Red for delete
        style = Paint.Style.FILL
    }

    private val deleteTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 24f
        textAlign = Paint.Align.CENTER
        style = Paint.Style.FILL
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    // Touch Handling Variables (Smooth & Jump-free)
    private var lastX = 0f
    private var lastY = 0f
    private var initialRawX = 0f
    private var initialRawY = 0f
    private var initialWidth = 0
    private var initialHeight = 0
    private var initialFontSize = 16f
    private var isDragging = false
    private var isResizing = false
    private var activeResizeCorner = -1 // 0: Top-Left, 1: Top-Right, 2: Bottom-Right, 3: Bottom-Left

    // Handles sizing
    private val handleRadius = 20f // px
    private val handleOuterTouchRadius = 48f // px accessibility

    // Double Tap Detection
    private var lastTapTime = 0L
    private val doubleTapThreshold = 300L

    // Event Callbacks
    var onAnnotationChanged: ((Float, Float, Float) -> Unit)? = null // (translationX, translationY, newFontSize)
    var onPropertiesChanged: ((AnnotationProperties) -> Unit)? = null
    var onSelected: (() -> Unit)? = null
    var onDeleteRequested: (() -> Unit)? = null

    init {
        // Allow FrameLayout to draw its border/handles
        setWillNotDraw(false)

        // Initialize styled TextView
        textView = CustomStrokeTextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
            setPadding(28, 28, 28, 28)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, currentProps.fontSize)
        }

        // Initialize transparent EditText for inline editing
        editText = EditText(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            background = null
            gravity = textView.gravity
            setPadding(28, 28, 28, 28)
            visibility = View.GONE
            inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
            imeOptions = EditorInfo.IME_ACTION_DONE
        }

        addView(textView)
        addView(editText)

        setupEditListeners()
    }

    private fun setupEditListeners() {
        editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                finishEditing()
                true
            } else false
        }

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val newText = s?.toString() ?: ""
                currentProps.text = newText
                applyTextProperties()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Also finish editing if focus is lost
        editText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && isEditing) {
                finishEditing()
            }
        }
    }

    // Public API to set/get properties easily
    fun setProperties(props: AnnotationProperties) {
        currentProps = props
        
        // Apply position & size (compensating for padding so the text itself starts at props.x, props.y)
        translationX = props.x - textView.paddingLeft.toFloat()
        translationY = props.y - textView.paddingTop.toFloat()
        rotation = props.rotationAngle

        layoutParams?.let { lp ->
            if (lp.width != props.width || lp.height != props.height) {
                lp.width = props.width
                lp.height = props.height
                layoutParams = lp
            }
        }

        applyTextProperties()
        invalidate()
    }

    fun getProperties(): AnnotationProperties {
        // Compensating for padding when returning coordinates so that the saved position is the text's actual starting position
        currentProps.x = translationX + textView.paddingLeft.toFloat()
        currentProps.y = translationY + textView.paddingTop.toFloat()
        currentProps.rotationAngle = rotation
        layoutParams?.let { lp ->
            currentProps.width = lp.width
            currentProps.height = lp.height
        }
        return currentProps
    }

    // Helper to format/apply styles to the TextView and EditText
    private fun applyTextProperties() {
        // Set text
        val textStr = currentProps.text
        val spannable = SpannableString(textStr)
        if (currentProps.hasUnderline) {
            spannable.setSpan(UnderlineSpan(), 0, textStr.length, 0)
        }
        if (currentProps.hasStrikeThrough) {
            spannable.setSpan(StrikethroughSpan(), 0, textStr.length, 0)
        }

        textView.text = spannable
        if (editText.text.toString() != textStr) {
            editText.setText(textStr)
        }

        // Colors
        val textColor = try { Color.parseColor(currentProps.textColorHex) } catch (e: Exception) { Color.BLACK }
        textView.setTextColor(textColor)
        editText.setTextColor(textColor)

        val isBgTransparent = currentProps.bgColorHex.lowercase() == "transparent" || currentProps.bgColorHex.isBlank()
        val bgColor = if (isBgTransparent) Color.TRANSPARENT else {
            try { Color.parseColor(currentProps.bgColorHex) } catch (e: Exception) { Color.TRANSPARENT }
        }
        setBackgroundColor(bgColor)

        // Font Family
        val fontStyle = when {
            currentProps.isBold && currentProps.isItalic -> Typeface.BOLD_ITALIC
            currentProps.isBold -> Typeface.BOLD
            currentProps.isItalic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }

        val baseTypeface = when (currentProps.fontName.lowercase()) {
            "arial", "sans-serif" -> Typeface.SANS_SERIF
            "calibri" -> Typeface.create("sans-serif-light", Typeface.NORMAL)
            "tahoma" -> Typeface.create("sans-serif-condensed", Typeface.NORMAL)
            "times new roman", "serif" -> Typeface.SERIF
            "monospace" -> Typeface.MONOSPACE
            else -> Typeface.DEFAULT
        }
        val styledTypeface = Typeface.create(baseTypeface, fontStyle)
        textView.typeface = styledTypeface
        editText.typeface = styledTypeface

        // Alignment
        val textGravity = when (currentProps.alignment.lowercase()) {
            "center" -> Gravity.CENTER
            "right" -> Gravity.RIGHT
            "justified" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    textView.justificationMode = Layout.JUSTIFICATION_MODE_INTER_WORD
                }
                Gravity.START
            }
            else -> Gravity.START
        }
        textView.gravity = textGravity or Gravity.CENTER_VERTICAL
        editText.gravity = textGravity or Gravity.CENTER_VERTICAL

        // Font Size
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentProps.fontSize)
        editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentProps.fontSize)

        // Outline / Stroke
        textView.hasOutline = currentProps.hasOutline
        textView.outlineColor = try { Color.parseColor(currentProps.outlineColorHex) } catch (e: Exception) { Color.WHITE }
        textView.strokeWidthPx = currentProps.outlineThickness
    }

    // Inline Editing Controls
    fun startEditing() {
        if (isEditing) return
        isEditing = true
        textView.visibility = View.GONE
        editText.visibility = View.VISIBLE
        editText.requestFocus()
        editText.setSelection(editText.text.length)

        // Show Keyboard
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        invalidate()
    }

    fun finishEditing() {
        if (!isEditing) return
        isEditing = false
        editText.clearFocus()
        textView.visibility = View.VISIBLE
        editText.visibility = View.GONE

        // Hide Keyboard
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(windowToken, 0)

        currentProps.text = editText.text.toString()
        applyTextProperties()
        
        onPropertiesChanged?.invoke(getProperties())
        onAnnotationChanged?.invoke(translationX, translationY, textView.paint.textSize)
        invalidate()
    }

    // Touch Event Handling
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Rotated touches mapping can be adjusted if rotation is enabled, using simple raw touch is robust here.
        val rawX = event.rawX
        val rawY = event.rawY
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                onSelected?.invoke()
                isSelectedState = true

                // Check for Delete click (Top-Right custom circle overlay)
                if (isSelectedState && isNearDeleteHandle(x, y)) {
                    onDeleteRequested?.invoke()
                    return true
                }

                // Check double-tap to enter editing mode
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTapTime < doubleTapThreshold) {
                    startEditing()
                    return true
                }
                lastTapTime = currentTime

                // Check for resize handles
                val touchedCorner = getTouchedResizeCorner(x, y)
                if (isSelectedState && touchedCorner != -1) {
                    isResizing = true
                    activeResizeCorner = touchedCorner
                    isDragging = false
                } else {
                    isDragging = true
                    isResizing = false
                }

                lastX = rawX
                lastY = rawY
                initialRawX = rawX
                initialRawY = rawY
                initialWidth = width
                initialHeight = height
                initialFontSize = currentProps.fontSize

                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = rawX - lastX
                val dy = rawY - lastY

                if (isResizing) {
                    val totalDeltaX = rawX - initialRawX
                    val ratio = if (initialHeight > 0) initialWidth.toFloat() / initialHeight.toFloat() else 1f
                    var newWidth = (initialWidth + totalDeltaX).toInt()

                    // Enforce reasonable bounds while maintaining aspect ratio
                    newWidth = max(newWidth, 120)
                    var newHeight = (newWidth / ratio).toInt()
                    if (newHeight < 100) {
                        newHeight = 100
                        newWidth = (100 * ratio).toInt()
                    }

                    // Auto-scale the font size proportionally with width change to keep visual hierarchy!
                    val scaleFactor = newWidth.toFloat() / initialWidth.toFloat()
                    val newFontSize = (initialFontSize * scaleFactor).coerceIn(8f, 120f)
                    currentProps.fontSize = newFontSize

                    layoutParams?.let { lp ->
                        lp.width = newWidth
                        lp.height = newHeight
                        layoutParams = lp
                    }
                    applyTextProperties()
                    requestLayout()
                } else if (isDragging && !isEditing) {
                    translationX += dx
                    translationY += dy
                }

                lastX = rawX
                lastY = rawY
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging || isResizing) {
                    isDragging = false
                    isResizing = false
                    activeResizeCorner = -1
                    parent?.requestDisallowInterceptTouchEvent(false)
                    onPropertiesChanged?.invoke(getProperties())
                    onAnnotationChanged?.invoke(translationX, translationY, textView.paint.textSize)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun getTouchedResizeCorner(x: Float, y: Float): Int {
        // We evaluate corners: Top-Left (0), Top-Right (1), Bottom-Right (2), Bottom-Left (3)
        // Check Bottom-Right first as it is standard
        if (abs(x - width) < handleOuterTouchRadius && abs(y - height) < handleOuterTouchRadius) return 2
        if (abs(x - 0) < handleOuterTouchRadius && abs(y - height) < handleOuterTouchRadius) return 3
        if (abs(x - 0) < handleOuterTouchRadius && abs(y - 0) < handleOuterTouchRadius) return 0
        return -1
    }

    private fun isNearDeleteHandle(x: Float, y: Float): Boolean {
        // Delete handle placed at Top-Right
        return abs(x - width) < handleOuterTouchRadius && abs(y - 0) < handleOuterTouchRadius
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isSelectedState) {
            // Draw Beautiful dashed selection border
            canvas.drawRect(
                0f, 
                0f, 
                width.toFloat(), 
                height.toFloat(), 
                borderPaint
            )

            // Draw Corner resizing handles (Bottom-Right, Bottom-Left, Top-Left)
            // Top-Left
            canvas.drawCircle(0f, 0f, handleRadius, handlePaint)
            canvas.drawCircle(0f, 0f, handleRadius, handleBorderPaint)

            // Bottom-Left
            canvas.drawCircle(0f, height.toFloat(), handleRadius, handlePaint)
            canvas.drawCircle(0f, height.toFloat(), handleRadius, handleBorderPaint)

            // Bottom-Right
            canvas.drawCircle(width.toFloat(), height.toFloat(), handleRadius, handlePaint)
            canvas.drawCircle(width.toFloat(), height.toFloat(), handleRadius, handleBorderPaint)

            // Draw Delete circle handle in Top-Right corner
            canvas.drawCircle(width.toFloat(), 0f, handleRadius + 4f, deleteHandlePaint)
            canvas.drawCircle(width.toFloat(), 0f, handleRadius + 4f, handleBorderPaint)
            canvas.drawText("✕", width.toFloat(), 8f, deleteTextPaint)
        }
    }

    // Custom internal class that supports stroke/outline rendering
    private class CustomStrokeTextView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : androidx.appcompat.widget.AppCompatTextView(context, attrs, defStyleAttr) {

        var hasOutline: Boolean = false
        var outlineColor: Int = Color.BLACK
        var strokeWidthPx: Float = 4f

        override fun onDraw(canvas: Canvas) {
            if (hasOutline && strokeWidthPx > 0) {
                val originalColors = textColors
                
                // Pass 1: Draw the outline stroke
                paint.style = Paint.Style.STROKE
                paint.strokeJoin = Paint.Join.ROUND
                paint.strokeMiter = 10f
                paint.strokeWidth = strokeWidthPx
                setTextColor(outlineColor)
                super.onDraw(canvas)

                // Pass 2: Draw fill on top
                paint.style = Paint.Style.FILL
                setTextColor(originalColors)
                super.onDraw(canvas)
            } else {
                super.onDraw(canvas)
            }
        }
    }
}
