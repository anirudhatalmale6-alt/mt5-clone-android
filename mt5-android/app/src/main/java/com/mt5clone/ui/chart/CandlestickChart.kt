package com.mt5clone.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mt5clone.data.model.Candle
import com.mt5clone.ui.common.theme.MT5Colors
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun CandlestickChart(
    candles: List<Candle>,
    priceHigh: Double,
    priceLow: Double,
    currentBid: Double,
    currentAsk: Double,
    showCrosshair: Boolean,
    crosshairX: Float,
    crosshairY: Float,
    crosshairCandle: Candle?,
    crosshairPrice: Double,
    digits: Int,
    onScroll: (Int) -> Unit,
    onZoom: (Float) -> Unit,
    onCrosshairUpdate: (Float, Float, Float, Float) -> Unit,
    onCrosshairHide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val priceScaleWidth = 70.dp
    val timeScaleHeight = 24.dp
    val priceScaleWidthPx = with(density) { priceScaleWidth.toPx() }
    val timeScaleHeightPx = with(density) { timeScaleHeight.toPx() }

    val greenColor = MT5Colors.CandleGreen
    val redColor = MT5Colors.CandleRed
    val gridColor = MT5Colors.ChartGrid
    val textColor = MT5Colors.TextSecondary
    val crosshairColor = MT5Colors.ChartCrosshair
    val bgColor = MT5Colors.BackgroundPrimary
    val bidLineColor = MT5Colors.TextSecondary

    Box(modifier = modifier.background(bgColor)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (abs(zoom - 1f) > 0.01f) {
                            onZoom(zoom)
                        } else {
                            val scrollDelta = (-pan.x / 10f).roundToInt()
                            if (scrollDelta != 0) onScroll(scrollDelta)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { offset ->
                            onCrosshairUpdate(
                                offset.x,
                                offset.y,
                                size.width - priceScaleWidthPx,
                                size.height - timeScaleHeightPx
                            )
                        },
                        onTap = {
                            onCrosshairHide()
                        }
                    )
                }
                .pointerInput(showCrosshair) {
                    if (showCrosshair) {
                        detectDragGestures(
                            onDragEnd = { /* keep crosshair visible */ },
                            onDrag = { change, _ ->
                                change.consume()
                                onCrosshairUpdate(
                                    change.position.x,
                                    change.position.y,
                                    size.width - priceScaleWidthPx,
                                    size.height - timeScaleHeightPx
                                )
                            }
                        )
                    }
                }
        ) {
            if (candles.isEmpty() || priceHigh <= priceLow) return@Canvas

            val chartWidth = size.width - priceScaleWidthPx
            val chartHeight = size.height - timeScaleHeightPx
            val priceRange = priceHigh - priceLow

            // Draw grid
            drawGrid(chartWidth, chartHeight, priceHigh, priceLow, digits, gridColor, textColor, priceScaleWidthPx)

            // Draw candles
            val candleSpacing = 2f
            val totalCandleWidth = chartWidth / candles.size
            val bodyWidth = max(totalCandleWidth - candleSpacing, 1f)
            val wickWidth = max(bodyWidth * 0.15f, 1f)

            candles.forEachIndexed { index, candle ->
                val x = index * totalCandleWidth + totalCandleWidth / 2f
                val isBullish = candle.close >= candle.open
                val color = if (isBullish) greenColor else redColor

                val highY = ((priceHigh - candle.high) / priceRange * chartHeight).toFloat()
                val lowY = ((priceHigh - candle.low) / priceRange * chartHeight).toFloat()
                val openY = ((priceHigh - candle.open) / priceRange * chartHeight).toFloat()
                val closeY = ((priceHigh - candle.close) / priceRange * chartHeight).toFloat()

                val bodyTop = if (isBullish) closeY else openY
                val bodyBottom = if (isBullish) openY else closeY
                val bodyHeight = max(bodyBottom - bodyTop, 1f)

                // Wick
                drawLine(
                    color = color,
                    start = Offset(x, highY),
                    end = Offset(x, lowY),
                    strokeWidth = wickWidth
                )

                // Body
                drawRect(
                    color = color,
                    topLeft = Offset(x - bodyWidth / 2f, bodyTop),
                    size = Size(bodyWidth, bodyHeight)
                )

                // For bearish candles, fill solid; for bullish, outline style (MT5 default)
                if (isBullish && bodyHeight > 2f) {
                    drawRect(
                        color = bgColor,
                        topLeft = Offset(x - bodyWidth / 2f + 1f, bodyTop + 1f),
                        size = Size(max(bodyWidth - 2f, 0f), max(bodyHeight - 2f, 0f))
                    )
                }
            }

            // Current bid price line (dashed)
            if (currentBid > 0.0) {
                val bidY = ((priceHigh - currentBid) / priceRange * chartHeight).toFloat()
                if (bidY in 0f..chartHeight) {
                    drawLine(
                        color = bidLineColor,
                        start = Offset(0f, bidY),
                        end = Offset(chartWidth, bidY),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                    )
                    // Bid price label
                    drawRect(
                        color = Color(0xFF787B86),
                        topLeft = Offset(chartWidth, bidY - 10f),
                        size = Size(priceScaleWidthPx, 20f)
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        String.format("%.${digits}f", currentBid),
                        chartWidth + 4f,
                        bidY + 4f,
                        android.graphics.Paint().apply {
                            this.color = android.graphics.Color.WHITE
                            textSize = 22f
                            isAntiAlias = true
                        }
                    )
                }
            }

            // Draw price scale (right side)
            drawPriceScale(chartWidth, chartHeight, priceHigh, priceLow, digits, priceScaleWidthPx, textColor)

            // Draw time scale (bottom)
            drawTimeScale(candles, chartWidth, chartHeight, timeScaleHeightPx, totalCandleWidth, textColor)

            // Draw crosshair
            if (showCrosshair && crosshairX >= 0 && crosshairY >= 0) {
                drawCrosshair(
                    crosshairX, crosshairY, chartWidth, chartHeight,
                    crosshairColor, crosshairCandle, crosshairPrice, digits,
                    priceScaleWidthPx, timeScaleHeightPx
                )
            }
        }
    }
}

private fun DrawScope.drawGrid(
    chartWidth: Float,
    chartHeight: Float,
    priceHigh: Double,
    priceLow: Double,
    digits: Int,
    gridColor: Color,
    textColor: Color,
    priceScaleWidth: Float
) {
    val gridLines = 6
    val priceRange = priceHigh - priceLow
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)

    for (i in 0..gridLines) {
        val y = chartHeight * i / gridLines
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(chartWidth, y),
            strokeWidth = 0.5f,
            pathEffect = dashEffect
        )
    }

    // Vertical grid lines
    val vGridLines = 5
    for (i in 0..vGridLines) {
        val x = chartWidth * i / vGridLines
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, chartHeight),
            strokeWidth = 0.5f,
            pathEffect = dashEffect
        )
    }
}

private fun DrawScope.drawPriceScale(
    chartWidth: Float,
    chartHeight: Float,
    priceHigh: Double,
    priceLow: Double,
    digits: Int,
    scaleWidth: Float,
    textColor: Color
) {
    val gridLines = 6
    val priceRange = priceHigh - priceLow
    val paint = android.graphics.Paint().apply {
        color = textColor.hashCode()
        textSize = 20f
        isAntiAlias = true
    }

    for (i in 0..gridLines) {
        val y = chartHeight * i / gridLines
        val price = priceHigh - (priceRange * i / gridLines)
        drawContext.canvas.nativeCanvas.drawText(
            String.format("%.${digits}f", price),
            chartWidth + 4f,
            y + 5f,
            paint
        )
    }
}

private fun DrawScope.drawTimeScale(
    candles: List<Candle>,
    chartWidth: Float,
    chartHeight: Float,
    timeScaleHeight: Float,
    candleWidth: Float,
    textColor: Color
) {
    if (candles.isEmpty()) return

    val paint = android.graphics.Paint().apply {
        color = textColor.hashCode()
        textSize = 18f
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
    }

    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val sdfDate = SimpleDateFormat("MMM dd", Locale.getDefault())
    val labelInterval = max(candles.size / 5, 1)

    for (i in candles.indices step labelInterval) {
        val x = i * candleWidth + candleWidth / 2f
        val time = Date(candles[i].time)
        val label = if (labelInterval > 20) sdfDate.format(time) else sdf.format(time)

        drawContext.canvas.nativeCanvas.drawText(
            label,
            x,
            chartHeight + timeScaleHeight - 4f,
            paint
        )
    }
}

private fun DrawScope.drawCrosshair(
    x: Float, y: Float,
    chartWidth: Float, chartHeight: Float,
    color: Color,
    candle: Candle?,
    price: Double,
    digits: Int,
    priceScaleWidth: Float,
    timeScaleHeight: Float
) {
    val clampedX = x.coerceIn(0f, chartWidth)
    val clampedY = y.coerceIn(0f, chartHeight)

    // Horizontal line
    drawLine(
        color = color,
        start = Offset(0f, clampedY),
        end = Offset(chartWidth, clampedY),
        strokeWidth = 0.8f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f), 0f)
    )

    // Vertical line
    drawLine(
        color = color,
        start = Offset(clampedX, 0f),
        end = Offset(clampedX, chartHeight),
        strokeWidth = 0.8f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f), 0f)
    )

    // Price label on right
    drawRect(
        color = Color(0xFF2196F3),
        topLeft = Offset(chartWidth, clampedY - 10f),
        size = Size(priceScaleWidth, 20f)
    )
    drawContext.canvas.nativeCanvas.drawText(
        String.format("%.${digits}f", price),
        chartWidth + 4f,
        clampedY + 4f,
        android.graphics.Paint().apply {
            this.color = android.graphics.Color.WHITE
            textSize = 22f
            isAntiAlias = true
        }
    )

    // Time label on bottom
    if (candle != null) {
        val sdf = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
        val timeLabel = sdf.format(Date(candle.time))
        val labelWidth = 140f
        drawRect(
            color = Color(0xFF2196F3),
            topLeft = Offset(clampedX - labelWidth / 2f, chartHeight),
            size = Size(labelWidth, timeScaleHeight)
        )
        drawContext.canvas.nativeCanvas.drawText(
            timeLabel,
            clampedX,
            chartHeight + timeScaleHeight - 6f,
            android.graphics.Paint().apply {
                this.color = android.graphics.Color.WHITE
                textSize = 18f
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
            }
        )
    }
}
