package com.mt5clone.ui.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mt5clone.data.model.ChartTool
import com.mt5clone.data.model.Timeframe
import com.mt5clone.ui.common.theme.MT5Colors
import com.mt5clone.ui.common.theme.MT5ThemeProvider

@Composable
fun ChartScreen(
    viewModel: ChartViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = MT5ThemeProvider.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundPrimary)
    ) {
        // Chart toolbar - symbol name + price info
        ChartToolbar(uiState, colors)

        // Timeframe selector bar
        TimeframeBar(
            currentTimeframe = uiState.timeframe,
            onTimeframeSelected = { viewModel.setTimeframe(it) },
            colors = colors
        )

        // Main chart area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            CandlestickChart(
                candles = uiState.visibleCandles,
                priceHigh = uiState.priceHigh,
                priceLow = uiState.priceLow,
                currentBid = uiState.currentBid,
                currentAsk = uiState.currentAsk,
                showCrosshair = uiState.showCrosshair,
                crosshairX = uiState.crosshairX,
                crosshairY = uiState.crosshairY,
                crosshairCandle = uiState.crosshairCandle,
                crosshairPrice = uiState.crosshairPrice,
                digits = uiState.symbol.digits,
                onScroll = { viewModel.onScroll(it) },
                onZoom = { viewModel.onZoom(it) },
                onCrosshairUpdate = { x, y, w, h -> viewModel.onCrosshair(x, y, w, h) },
                onCrosshairHide = { viewModel.hideCrosshair() },
                modifier = Modifier.fillMaxSize()
            )

            // Crosshair OHLC info overlay
            if (uiState.showCrosshair && uiState.crosshairCandle != null) {
                CandleInfoOverlay(uiState.crosshairCandle!!, uiState.symbol.digits, colors)
            }
        }

        // Chart tools bar
        ChartToolsBar(
            currentTool = uiState.chartTool,
            onToolSelected = { viewModel.setChartTool(it) },
            colors = colors
        )

        // Quick trade buttons at bottom
        QuickTradeBar(uiState, colors)
    }
}

@Composable
private fun ChartToolbar(
    state: ChartUiState,
    colors: com.mt5clone.ui.common.theme.MT5ColorScheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.backgroundSecondary)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.symbol.name,
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = state.symbol.description,
                color = colors.textSecondary,
                fontSize = 10.sp
            )
        }

        // Current price display
        Column(horizontalAlignment = Alignment.End) {
            Row {
                Text(
                    text = "B: ",
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = String.format("%.${state.symbol.digits}f", state.currentBid),
                    color = colors.sellRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Row {
                Text(
                    text = "A: ",
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = String.format("%.${state.symbol.digits}f", state.currentAsk),
                    color = colors.buyGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun TimeframeBar(
    currentTimeframe: Timeframe,
    onTimeframeSelected: (Timeframe) -> Unit,
    colors: com.mt5clone.ui.common.theme.MT5ColorScheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.backgroundSecondary)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Timeframe.entries.forEach { tf ->
            val selected = tf == currentTimeframe
            Box(
                modifier = Modifier
                    .clickable { onTimeframeSelected(tf) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tf.label,
                    color = if (selected) colors.tabSelected else colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
    HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
}

@Composable
private fun CandleInfoOverlay(
    candle: com.mt5clone.data.model.Candle,
    digits: Int,
    colors: com.mt5clone.ui.common.theme.MT5ColorScheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xAA131722))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val isBullish = candle.close >= candle.open
        val changeColor = if (isBullish) colors.buyGreen else colors.sellRed

        OhlcItem("O", candle.open, digits, changeColor)
        OhlcItem("H", candle.high, digits, changeColor)
        OhlcItem("L", candle.low, digits, changeColor)
        OhlcItem("C", candle.close, digits, changeColor)
        Text(
            text = "Vol: ${candle.volume}",
            color = colors.textSecondary,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun OhlcItem(label: String, value: Double, digits: Int, color: Color) {
    Row {
        Text(
            text = "$label: ",
            color = MT5Colors.TextSecondary,
            fontSize = 10.sp
        )
        Text(
            text = String.format("%.${digits}f", value),
            color = color,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
    }
}

@Composable
private fun ChartToolsBar(
    currentTool: ChartTool,
    onToolSelected: (ChartTool) -> Unit,
    colors: com.mt5clone.ui.common.theme.MT5ColorScheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.backgroundSecondary)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChartToolButton(Icons.Default.Add, "Crosshair", currentTool == ChartTool.CROSSHAIR, colors) {
            onToolSelected(if (currentTool == ChartTool.CROSSHAIR) ChartTool.NONE else ChartTool.CROSSHAIR)
        }
        ChartToolButton(Icons.Default.Remove, "H-Line", currentTool == ChartTool.HORIZONTAL_LINE, colors) {
            onToolSelected(ChartTool.HORIZONTAL_LINE)
        }
        ChartToolButton(Icons.Default.ArrowUpward, "Trend", currentTool == ChartTool.TREND_LINE, colors) {
            onToolSelected(ChartTool.TREND_LINE)
        }
        ChartToolButton(Icons.Default.Menu, "Indicators", false, colors) { }
        ChartToolButton(Icons.Default.Settings, "Objects", false, colors) { }
    }
}

@Composable
private fun ChartToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    colors: com.mt5clone.ui.common.theme.MT5ColorScheme,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) colors.tabSelected else colors.textSecondary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            color = if (selected) colors.tabSelected else colors.textSecondary,
            fontSize = 8.sp
        )
    }
}

@Composable
private fun QuickTradeBar(
    state: ChartUiState,
    colors: com.mt5clone.ui.common.theme.MT5ColorScheme
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.backgroundSecondary)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Sell button
        Button(
            onClick = { /* TODO: open sell dialog */ },
            colors = ButtonDefaults.buttonColors(containerColor = colors.sellRed),
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            shape = MaterialTheme.shapes.small,
            contentPadding = PaddingValues(4.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Sell", fontSize = 10.sp, color = Color.White)
                Text(
                    text = String.format("%.${state.symbol.digits}f", state.currentBid),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Spread display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                text = "${state.symbol.spread}",
                color = colors.textSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "spread",
                color = colors.textTertiary,
                fontSize = 8.sp,
                textAlign = TextAlign.Center
            )
        }

        // Buy button
        Button(
            onClick = { /* TODO: open buy dialog */ },
            colors = ButtonDefaults.buttonColors(containerColor = colors.buyGreen),
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            shape = MaterialTheme.shapes.small,
            contentPadding = PaddingValues(4.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Buy", fontSize = 10.sp, color = Color.White)
                Text(
                    text = String.format("%.${state.symbol.digits}f", state.currentAsk),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
