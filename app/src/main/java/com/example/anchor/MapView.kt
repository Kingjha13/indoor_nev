package com.example.anchor


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class MapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // Floor map image
    private var floorMapBitmap: Bitmap? = null

    // Paints
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    // User position
    private var pointX = -1f
    private var pointY = -1f
    private var dragging = false
    var isStartPointSelectionEnabled = false
        private set

    // Path points
    private val pathPoints = mutableListOf<FloatArray>()

    fun setFloorMapBitmap(bitmap: Bitmap) {
        floorMapBitmap = bitmap
        invalidate()
    }

    fun enableStartPointSelection() {
        isStartPointSelectionEnabled = true
        Toast.makeText(
            context,
            "Tap on the map to set the start point.",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val bitmap = floorMapBitmap
        if (bitmap == null) {
            paint.color = Color.BLACK
            paint.textSize = 50f
            paint.textAlign = Paint.Align.CENTER
            paint.style = Paint.Style.FILL
            canvas.drawText(
                "Upload map here",
                width / 2f,
                height / 2f,
                paint
            )
            return
        }

        // Scale bitmap to fit view
        val scaleX = width.toFloat() / bitmap.width
        val scaleY = height.toFloat() / bitmap.height
        val scale = min(scaleX, scaleY)

        val offsetX = (width - bitmap.width * scale) / 2f
        val offsetY = (height - bitmap.height * scale) / 2f

        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(offsetX, offsetY)
        }

        canvas.drawBitmap(bitmap, matrix, paint)

        // Draw path
        if (pathPoints.isNotEmpty()) {
            val path = Path()
            val start = pathPoints.first()
            path.moveTo(start[0], start[1])

            for (i in 1 until pathPoints.size) {
                val p = pathPoints[i]
                path.lineTo(p[0], p[1])
            }
            canvas.drawPath(path, pathPaint)
        }

        // Draw user point
        if (pointX >= 0 && pointY >= 0) {
            paint.color = Color.RED
            paint.style = Paint.Style.FILL
            canvas.drawCircle(pointX, pointY, 10f, paint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {

            MotionEvent.ACTION_DOWN -> {
                if (pointX < 0 && pointY < 0) {
                    pointX = x
                    pointY = y
                    pathPoints.add(floatArrayOf(x, y))
                    invalidate()
                } else {
                    val distance = sqrt(
                        (x - pointX).pow(2) + (y - pointY).pow(2)
                    )
                    if (distance <= 20f) {
                        dragging = true
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (dragging) {
                    pointX = x
                    pointY = y
                    pathPoints.add(floatArrayOf(x, y))
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP -> {
                dragging = false
            }
        }
        return true
    }

    /**
     * Move user position programmatically (for steps / fake movement)
     */
    fun updatePosition(deltaX: Float, deltaY: Float) {
        if (pointX < 0 || pointY < 0) return
        pointX += deltaX
        pointY += deltaY
        pathPoints.add(floatArrayOf(pointX, pointY))
        invalidate()
    }
}
