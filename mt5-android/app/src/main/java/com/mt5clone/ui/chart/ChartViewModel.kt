package com.mt5clone.ui.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mt5clone.data.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class ChartUiState(
    val symbol: Symbol = defaultChartSymbol(),
    val timeframe: Timeframe = Timeframe.H1,
    val candles: List<Candle> = emptyList(),
    val visibleCandles: List<Candle> = emptyList(),
    val chartTool: ChartTool = ChartTool.NONE,
    val crosshairX: Float = -1f,
    val crosshairY: Float = -1f,
    val crosshairCandle: Candle? = null,
    val crosshairPrice: Double = 0.0,
    val showCrosshair: Boolean = false,
    val scrollOffset: Int = 0,
    val candleWidth: Float = 8f,
    val visibleCount: Int = 60,
    val priceHigh: Double = 0.0,
    val priceLow: Double = 0.0,
    val isLoading: Boolean = false,
    val currentBid: Double = 0.0,
    val currentAsk: Double = 0.0
)

fun defaultChartSymbol() = Symbol(
    "EURUSD", "Euro vs US Dollar", "Forex", 5, 100000.0,
    spread = 12, bid = 1.08450, ask = 1.08462
)

@HiltViewModel
class ChartViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ChartUiState())
    val uiState: StateFlow<ChartUiState> = _uiState.asStateFlow()

    private val allCandles = mutableListOf<Candle>()

    init {
        generateHistoricalCandles()
    }

    fun setSymbol(symbol: Symbol) {
        _uiState.value = _uiState.value.copy(symbol = symbol)
        generateHistoricalCandles()
    }

    fun setTimeframe(timeframe: Timeframe) {
        _uiState.value = _uiState.value.copy(timeframe = timeframe)
        generateHistoricalCandles()
    }

    fun setChartTool(tool: ChartTool) {
        _uiState.value = _uiState.value.copy(chartTool = tool)
    }

    fun onScroll(delta: Int) {
        val state = _uiState.value
        val maxOffset = max(0, allCandles.size - state.visibleCount)
        val newOffset = (state.scrollOffset + delta).coerceIn(0, maxOffset)
        _uiState.value = state.copy(scrollOffset = newOffset)
        updateVisibleCandles()
    }

    fun onZoom(scaleFactor: Float) {
        val state = _uiState.value
        val newWidth = (state.candleWidth * scaleFactor).coerceIn(3f, 30f)
        val newVisibleCount = when {
            newWidth < 5f -> 120
            newWidth < 8f -> 80
            newWidth < 12f -> 60
            newWidth < 18f -> 40
            else -> 25
        }
        _uiState.value = state.copy(candleWidth = newWidth, visibleCount = newVisibleCount)
        updateVisibleCandles()
    }

    fun onCrosshair(x: Float, y: Float, chartWidth: Float, chartHeight: Float) {
        val state = _uiState.value
        if (state.visibleCandles.isEmpty()) return

        val candleIndex = ((x / chartWidth) * state.visibleCandles.size).toInt()
            .coerceIn(0, state.visibleCandles.size - 1)
        val candle = state.visibleCandles[candleIndex]

        val priceRange = state.priceHigh - state.priceLow
        val price = state.priceHigh - (y / chartHeight) * priceRange

        _uiState.value = state.copy(
            showCrosshair = true,
            crosshairX = x,
            crosshairY = y,
            crosshairCandle = candle,
            crosshairPrice = price
        )
    }

    fun hideCrosshair() {
        _uiState.value = _uiState.value.copy(showCrosshair = false)
    }

    fun addNewCandle(candle: Candle) {
        allCandles.add(candle)
        // Auto-scroll to latest
        val state = _uiState.value
        val maxOffset = max(0, allCandles.size - state.visibleCount)
        _uiState.value = state.copy(scrollOffset = maxOffset)
        updateVisibleCandles()
    }

    fun updateLastCandle(tick: PriceTick) {
        if (allCandles.isEmpty()) return
        val last = allCandles.last()
        val updatedCandle = last.copy(
            close = tick.bid,
            high = max(last.high, tick.bid),
            low = min(last.low, tick.bid),
            tickVolume = last.tickVolume + 1
        )
        allCandles[allCandles.size - 1] = updatedCandle
        _uiState.value = _uiState.value.copy(
            currentBid = tick.bid,
            currentAsk = tick.ask
        )
        updateVisibleCandles()
    }

    private fun updateVisibleCandles() {
        val state = _uiState.value
        val start = state.scrollOffset.coerceIn(0, max(0, allCandles.size - 1))
        val end = min(start + state.visibleCount, allCandles.size)
        val visible = if (start < end) allCandles.subList(start, end) else emptyList()

        if (visible.isEmpty()) return

        val high = visible.maxOf { it.high }
        val low = visible.minOf { it.low }
        val padding = (high - low) * 0.05

        _uiState.value = state.copy(
            visibleCandles = visible.toList(),
            priceHigh = high + padding,
            priceLow = low - padding,
            candles = allCandles.toList()
        )
    }

    private fun generateHistoricalCandles() {
        allCandles.clear()
        val state = _uiState.value
        val symbol = state.symbol
        val tf = state.timeframe

        var price = symbol.bid
        if (price == 0.0) price = 1.08450

        val now = System.currentTimeMillis()
        val count = 500 // Generate 500 historical candles

        val volatility = when {
            symbol.name.contains("JPY") -> 0.15
            symbol.name.contains("XAU") -> 5.0
            symbol.name.contains("BTC") -> 200.0
            symbol.name.contains("US500") || symbol.name.contains("NAS") || symbol.name.contains("US30") -> 15.0
            else -> 0.0008
        }

        for (i in count downTo 0) {
            val time = now - (i.toLong() * tf.seconds * 1000L)
            val change = (Random.nextGaussian() * volatility)
            val open = price
            val close = price + change
            val highWick = abs(Random.nextGaussian() * volatility * 0.5)
            val lowWick = abs(Random.nextGaussian() * volatility * 0.5)
            val high = max(open, close) + highWick
            val low = min(open, close) - lowWick
            val volume = (Random.nextInt(500) + 100).toLong()

            allCandles.add(
                Candle(
                    time = time,
                    open = open,
                    high = high,
                    low = low,
                    close = close,
                    volume = volume,
                    tickVolume = volume
                )
            )
            price = close
        }

        // Scroll to end
        val maxOffset = max(0, allCandles.size - state.visibleCount)
        _uiState.value = state.copy(
            scrollOffset = maxOffset,
            currentBid = price,
            currentAsk = price + symbol.spread * symbol.pointSize
        )
        updateVisibleCandles()
    }
}
