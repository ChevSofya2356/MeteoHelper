package com.example.meteohelper.ui.charts

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class SimpleLineChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var data: List<Float> = emptyList()
        set(value) { field = value; invalidate() }

    var labels: List<String> = emptyList()
        set(value) { field = value; invalidate() }

    var lineColor: Int = Color.parseColor("#FF387C")
    var minY: Float? = null
    var maxY: Float? = null

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    // Сетка
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EEEEEE")
        strokeWidth = 1f
    }
    // Вертикальная разметка (чуть ярче)
    private val timeGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0")
        strokeWidth = 2f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#666666")
        textSize = 20f
        textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        val padding = 50f
        val bottomPadding = 70f

        val chartWidth = width - padding * 2
        val chartHeight = height - padding - bottomPadding

        val minVal = minY ?: (data.minOrNull() ?: 0f)
        val maxVal = maxY ?: (data.maxOrNull() ?: 1f)
        val range = max(maxVal - minVal, 1f)

        linePaint.color = lineColor
        fillPaint.color = Color.argb(40, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor))
        pointPaint.color = lineColor

        // 1. Горизонтальная сетка и подписи Y
        val stepY = chartHeight / 3
        for (i in 0..3) {
            val y = padding + i * stepY
            canvas.drawLine(padding, y, width - padding, y, gridPaint)
            val value = maxVal - (range * i / 3)
            canvas.drawText(String.format("%.1f", value), padding - 15f, y + 8f, textPaint)
        }

        val stepX = if (data.size > 1) chartWidth / (data.size - 1) else chartWidth

        // 2. Путь графика
        val path = Path()
        data.forEachIndexed { i, value ->
            val x = padding + i * stepX
            val normalizedY = (value - minVal) / range
            val y = padding + chartHeight - (normalizedY * chartHeight)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        // 3. Заливка
        val fillPath = Path(path)
        fillPath.lineTo(padding + (data.size - 1) * stepX, padding + chartHeight)
        fillPath.lineTo(padding, padding + chartHeight)
        fillPath.close()
        canvas.drawPath(fillPath, fillPaint)

        // 4. Линия и точки
        canvas.drawPath(path, linePaint)

        // 5. 🔥 РАЗМЕТКА ВРЕМЕНИ (вертикальные линии каждые 3 часа)
        data.forEachIndexed { i, value ->
            val x = padding + i * stepX
            val normalizedY = (value - minVal) / range
            val y = padding + chartHeight - (normalizedY * chartHeight)

            // Рисуем точку
            canvas.drawCircle(x, y, 5f, pointPaint)

            // 🔥 Рисуем вертикальную черту каждые 3 часа
            if (i % 3 == 0) {
                canvas.drawLine(x, padding, x, padding + chartHeight, timeGridPaint)
            }
        }

        // 6. Подписи (попробуем нарисовать, может сработает)
        labels.forEachIndexed { i, label ->
            if (i % 3 == 0) {
                val x = padding + i * stepX
                canvas.drawText(label, x, height - 20f, textPaint)
            }
        }
    }
}