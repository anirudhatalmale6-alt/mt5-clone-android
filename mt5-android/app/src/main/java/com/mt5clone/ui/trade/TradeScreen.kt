package com.mt5clone.ui.trade

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.mt5clone.data.model.Order
import com.mt5clone.data.model.TradingAccount
import com.mt5clone.ui.common.theme.MT5ThemeProvider

data class TradeUiState(
    val account: TradingAccount = TradingAccount(),
    val openOrders: List<Order> = emptyList(),
    val selectedTab: Int = 0 // 0 = Positions, 1 = Orders (pending)
)

@Composable
fun TradeScreen(
    viewModel: TradeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = MT5ThemeProvider.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundPrimary)
    ) {
        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.backgroundSecondary)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Trade",
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Tab row - Positions / Orders
        TabRow(
            selectedTabIndex = uiState.selectedTab,
            containerColor = colors.backgroundSecondary,
            contentColor = colors.tabSelected,
            divider = { HorizontalDivider(color = colors.divider, thickness = 0.5.dp) }
        ) {
            Tab(
                selected = uiState.selectedTab == 0,
                onClick = { viewModel.selectTab(0) },
                text = {
                    Text(
                        "Positions",
                        fontSize = 12.sp,
                        color = if (uiState.selectedTab == 0) colors.tabSelected else colors.textSecondary
                    )
                }
            )
            Tab(
                selected = uiState.selectedTab == 1,
                onClick = { viewModel.selectTab(1) },
                text = {
                    Text(
                        "Orders",
                        fontSize = 12.sp,
                        color = if (uiState.selectedTab == 1) colors.tabSelected else colors.textSecondary
                    )
                }
            )
        }

        // Account summary bar
        AccountSummaryBar(uiState.account, colors)

        // Orders list
        if (uiState.openOrders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (uiState.selectedTab == 0) "No open positions" else "No pending orders",
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiState.openOrders) { order ->
                    OrderRow(order, colors)
                    HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
                }
            }
        }

        // New order button
        Button(
            onClick = { /* TODO: open new order dialog */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .height(44.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accentBlue),
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = "New Order",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AccountSummaryBar(
    account: TradingAccount,
    colors: com.mt5clone.ui.common.theme.MT5ColorScheme
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AccountInfoItem("Balance", String.format("%.2f", account.balance), colors)
            AccountInfoItem("Equity", String.format("%.2f", account.equity), colors)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AccountInfoItem("Margin", String.format("%.2f", account.margin), colors)
            AccountInfoItem("Free Margin", String.format("%.2f", account.freeMargin), colors)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val profitColor = when {
                account.profit > 0 -> colors.buyGreen
                account.profit < 0 -> colors.sellRed
                else -> colors.textSecondary
            }
            AccountInfoItem("Profit", String.format("%.2f", account.profit), colors, profitColor)
            AccountInfoItem(
                "Margin Level",
                if (account.margin > 0) String.format("%.2f%%", account.marginLevel) else "---",
                colors
            )
        }
    }
    HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
}

@Composable
private fun AccountInfoItem(
    label: String,
    value: String,
    colors: com.mt5clone.ui.common.theme.MT5ColorScheme,
    valueColor: Color = colors.textPrimary
) {
    Column {
        Text(
            text = label,
            color = colors.textTertiary,
            fontSize = 9.sp
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun OrderRow(
    order: Order,
    colors: com.mt5clone.ui.common.theme.MT5ColorScheme
) {
    val typeColor = if (order.isBuy) colors.buyGreen else colors.sellRed
    val profitColor = when {
        order.profit > 0 -> colors.buyGreen
        order.profit < 0 -> colors.sellRed
        else -> colors.textSecondary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = order.type.label,
                    color = typeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = order.symbol,
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${order.lots}",
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
            }
            Text(
                text = String.format("%.2f", order.profit),
                color = profitColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Open: ${String.format("%.5f", order.openPrice)}",
                color = colors.textSecondary,
                fontSize = 10.sp
            )
            Text(
                text = "Current: ${String.format("%.5f", order.currentPrice)}",
                color = colors.textSecondary,
                fontSize = 10.sp
            )
        }

        if (order.stopLoss > 0 || order.takeProfit > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (order.stopLoss > 0) "SL: ${String.format("%.5f", order.stopLoss)}" else "SL: ---",
                    color = colors.sellRed.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
                Text(
                    text = if (order.takeProfit > 0) "TP: ${String.format("%.5f", order.takeProfit)}" else "TP: ---",
                    color = colors.buyGreen.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
    }
}
