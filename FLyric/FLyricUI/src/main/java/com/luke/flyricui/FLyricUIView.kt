package com.luke.flyricui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View

class FLyricUIView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var staticLayout: StaticLayout? = null
    private var highlightedStaticLayout: StaticLayout? = null

    private var textPaint: TextPaint = TextPaint().apply {
        isAntiAlias = true
        color = Color.WHITE
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            16f,
            context.resources.displayMetrics
        )
        typeface = Typeface.DEFAULT
    }

    private var highlightedTextPaint: TextPaint = TextPaint(textPaint).apply {
        color = Color.RED
    }

    private var text: String? = null
    private var highlightEnd = 0f

    fun setText(text: String): Float {
        this.text = text
        val textWidth = textPaint.measureText(text)
        updateStaticLayout()
        return textWidth
    }

    fun setHighlightEnd(end: Float) {
        this.highlightEnd = end
        invalidate()
    }

    private fun updateStaticLayout() {
        text?.let {
            val width = width - paddingLeft - paddingRight
            staticLayout = StaticLayout.Builder.obtain(it, 0, it.length, textPaint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(1f, 1f)
                .setIncludePad(true)
                .build()
            highlightedStaticLayout =
                StaticLayout.Builder.obtain(it, 0, it.length, highlightedTextPaint, width)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(1f, 1f)
                    .setIncludePad(true)
                    .build()
            invalidate() // Redraw the view
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.clipRect(0f, 0f, highlightEnd, height.toFloat())
        highlightedStaticLayout?.draw(canvas)
        canvas.restore()

        canvas.save()
        canvas.clipRect(highlightEnd, 0f, width.toFloat(), height.toFloat())
        staticLayout?.draw(canvas)
        canvas.restore()
    }
}