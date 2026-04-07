package com.mt5clone.data.model

import java.util.UUID

// Symbol/Instrument
data class Symbol(
    val name: String,           // e.g. "EURUSD"
    val description: String,    // e.g. "Euro vs US Dollar"
    val category: String,       // e.g. "Forex", "Metals", "Indices", "Crypto"
    val digits: Int,            // decimal places (5 for forex, 2 for indices)
    val contractSize: Double,   // 100000 for standard forex lot
    val minLot: Double = 0.01,
    val maxLot: Double = 100.0,
    val lotStep: Double = 0.01,
    val spread: Int = 0,        // in points
    val bid: Double = 0.0,
    val ask: Double = 0.0,
    val high: Double = 0.0,
    val low: Double = 0.0,
    val time: Long = 0L
) {
    val spreadDisplay: String
        get() = if (digits >= 3) {
            String.format("%.1f", spread.toDouble() / if (digits == 5 || digits == 3) 10.0 else 1.0)
        } else {
            spread.toString()
        }

    val pipSize: Double
        get() = if (digits == 5 || digits == 3) {
            Math.pow(10.0, -(digits - 1).toDouble())
        } else {
            Math.pow(10.0, -digits.toDouble())
        }

    val pointSize: Double
        get() = Math.pow(10.0, -digits.toDouble())
}

// OHLC Candle data
data class Candle(
    val time: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long = 0,
    val tickVolume: Long = 0
)

// Price tick
data class PriceTick(
    val symbol: String,
    val bid: Double,
    val ask: Double,
    val time: Long,
    val volume: Long = 0
)

// Timeframes
enum class Timeframe(val label: String, val seconds: Int) {
    M1("M1", 60),
    M5("M5", 300),
    M15("M15", 900),
    M30("M30", 1800),
    H1("H1", 3600),
    H4("H4", 14400),
    D1("D1", 86400),
    W1("W1", 604800),
    MN("MN", 2592000);

    companion object {
        fun fromLabel(label: String): Timeframe =
            entries.firstOrNull { it.label == label } ?: M1
    }
}

// Order types
enum class OrderType(val label: String) {
    BUY("Buy"),
    SELL("Sell"),
    BUY_LIMIT("Buy Limit"),
    SELL_LIMIT("Sell Limit"),
    BUY_STOP("Buy Stop"),
    SELL_STOP("Sell Stop")
}

// Order
data class Order(
    val id: String = UUID.randomUUID().toString(),
    val symbol: String,
    val type: OrderType,
    val lots: Double,
    val openPrice: Double,
    val currentPrice: Double = openPrice,
    val stopLoss: Double = 0.0,
    val takeProfit: Double = 0.0,
    val openTime: Long = System.currentTimeMillis(),
    val closeTime: Long = 0L,
    val closePrice: Double = 0.0,
    val profit: Double = 0.0,
    val commission: Double = 0.0,
    val swap: Double = 0.0,
    val comment: String = "",
    val isOpen: Boolean = true
) {
    val isBuy: Boolean get() = type == OrderType.BUY || type == OrderType.BUY_LIMIT || type == OrderType.BUY_STOP
    val isSell: Boolean get() = type == OrderType.SELL || type == OrderType.SELL_LIMIT || type == OrderType.SELL_STOP
    val isPending: Boolean get() = type != OrderType.BUY && type != OrderType.SELL
}

// Account
data class TradingAccount(
    val id: String = "default",
    val name: String = "Demo Account",
    val server: String = "MT5Clone-Demo",
    val currency: String = "USD",
    val balance: Double = 10000.0,
    val equity: Double = 10000.0,
    val margin: Double = 0.0,
    val freeMargin: Double = 10000.0,
    val marginLevel: Double = 0.0,
    val leverage: Int = 100,
    val profit: Double = 0.0
)

// Chart drawing tools
enum class ChartTool {
    NONE,
    CROSSHAIR,
    HORIZONTAL_LINE,
    VERTICAL_LINE,
    TREND_LINE,
    RECTANGLE,
    FIBONACCI
}

// Chart indicator types
enum class IndicatorType(val label: String) {
    MA("Moving Average"),
    EMA("Exponential MA"),
    BOLLINGER("Bollinger Bands"),
    RSI("RSI"),
    MACD("MACD"),
    STOCHASTIC("Stochastic"),
    VOLUME("Volume")
}
