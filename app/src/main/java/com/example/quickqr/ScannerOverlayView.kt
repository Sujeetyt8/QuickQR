package com.example.quickqr.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class ScannerOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val scrimPaint: Paint = Paint().apply {
        color = Color.parseColor("#99000000") // Semi-transparent black
    }

    private val eraserPaint: Paint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        isAntiAlias = true
    }

    private val boxRect = RectF()
    private val boxCornerRadius = 32f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val boxWidth = width * 0.7f // Make the box 70% of the screen width
        val boxHeight = boxWidth // Make it square
        val cx = width / 2f
        val cy = height / 2f
        boxRect.set(cx - boxWidth / 2, cy - boxHeight / 2, cx + boxWidth / 2, cy + boxHeight / 2)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)
        canvas.drawRoundRect(boxRect, boxCornerRadius, boxCornerRadius, eraserPaint)
    }
}