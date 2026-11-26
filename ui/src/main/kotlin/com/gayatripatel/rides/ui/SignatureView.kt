package com.gayatripatel.rides.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.sin

/**
 * Custom-drawn signature pad for parcel proof of delivery.
 *
 * Touch points are smoothed with quadratic Bézier segments through the midpoints of historical samples,
 * so fast strokes stay smooth without allocating per event. Exported as a compressed PNG for upload.
 */
class SignatureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2.5f, resources.displayMetrics)
        color = Color.parseColor("#1A1C1E")
    }
    private val path = Path()
    private var lastX = 0f
    private var lastY = 0f
    private var demoPending = false

    var onSignedChange: ((Boolean) -> Unit)? = null
    var isSigned: Boolean = false
        private set(value) {
            if (field != value) {
                field = value
                onSignedChange?.invoke(value)
            }
        }

    var inkColor: Int
        get() = paint.color
        set(value) {
            paint.color = value
            invalidate()
        }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                path.moveTo(event.x, event.y)
                lastX = event.x
                lastY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.historySize) smoothTo(event.getHistoricalX(i), event.getHistoricalY(i))
                smoothTo(event.x, event.y)
            }
            MotionEvent.ACTION_UP -> {
                path.lineTo(event.x, event.y)
                isSigned = true
            }
            else -> return false
        }
        invalidate()
        return true
    }

    private fun smoothTo(x: Float, y: Float) {
        path.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2)
        lastX = x
        lastY = y
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(path, paint)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (demoPending && w > 0) drawDemoSignature()
    }

    fun clear() {
        path.reset()
        isSigned = false
        invalidate()
    }

    /** Draws a sample signature, used by screenshot tests and the demo build. */
    fun showDemoSignature() {
        if (width > 0) drawDemoSignature() else demoPending = true
    }

    private fun drawDemoSignature() {
        demoPending = false
        path.reset()
        val w = width.toFloat()
        val h = height.toFloat()
        val steps = 120
        for (i in 0..steps) {
            val t = i / steps.toFloat()
            val x = w * (0.12f + 0.62f * t)
            val y = h * (0.55f + 0.18f * sin(t * 5 * PI).toFloat() - 0.12f * t)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.moveTo(w * 0.70f, h * 0.30f)
        path.quadTo(w * 0.80f, h * 0.75f, w * 0.88f, h * 0.40f)
        path.moveTo(w * 0.15f, h * 0.80f)
        path.lineTo(w * 0.86f, h * 0.76f)
        isSigned = true
        invalidate()
    }

    fun toPng(): ByteArray {
        val bitmap = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.WHITE)
            drawPath(path, paint)
        }
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }
}

@Stable
class SignaturePadState {
    internal var view: SignatureView? = null
    var isSigned by mutableStateOf(false)
        internal set

    fun clear() {
        view?.clear()
    }

    fun exportPng(): ByteArray? = view?.takeIf { it.isSigned }?.toPng()
}

@Composable
fun rememberSignaturePadState(): SignaturePadState = remember { SignaturePadState() }

@Composable
fun SignaturePad(state: SignaturePadState, inkColor: Int, modifier: Modifier = Modifier, demo: Boolean = false) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            SignatureView(ctx).apply {
                onSignedChange = { state.isSigned = it }
                state.view = this
                if (demo) showDemoSignature()
            }
        },
        update = { it.inkColor = inkColor },
        onRelease = { state.view = null },
    )
}
