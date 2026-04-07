package com.mt5clone.ui.history

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mt5clone.data.model.Order
import com.mt5clone.ui.common.theme.MT5ThemeProvider
import java.text.SimpleDateFormat
import java.util.*

data class HistoryUiState(
    val closedOrders: List<Order> = emptyList(),
    val selectedTab: Int = 0, // 0 = Deals, 1 = Orders, 2 = Balance operations
    val totalProfit: Double = 0.0,
    val totalDeposit: Double = 10000.0,
    val period: String = "Last Month"
)

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
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
                text = "History",
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Tab row
        TabRow(
            selectedTabIndex = uiState.selectedTab,
            containerColor = colors.backgroundSecondary,
            contentColor = colors.tabSelected,
            divider = { HorizontalDivider(color = colors.divider, thickness = 0.5.dp) }
        ) {
            listOf("Deals", "Orders", "Balance").forEachIndexed { index, title ->
                Tab(
                    selected = uiState.selectedTab == index,
                    onClick = { viewModel.selectTab(index) },
                    text = {
                        Text(
                            title,
                            fontSize = 12.sp,
                            color = if (uiState.selectedTab == index) colors.tabSelected else colors.textSecondary
                        )
                    }
                )
            }
        }

        // Period selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = uiState.period,
                color = colors.textSecondary,
                fontSize = 11.sp
            )
            val profitColor = when {
                uiState.totalProfit > 0 -> colors.buyGreen
                uiState.totalProfit < 0 -> colors.sellRed
                else -> colors.textSecondary
            }
            Text(
                text = "Total: ${String.format("%.2f", uiState.totalProfit)} USD",
                color = profitColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        HorizontalDivider(color = colors.divider, thickness = 0.5.dp)

        // History list
        if (uiState.closedOrders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No history available",
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.closedOrders) { order ->
                    HistoryOrderRow(order, colors)
                    HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun HistoryOrderRow(
    order: Order,
    colors: com.mt5clone.ui.common.theme.MT5ColorScheme
) {
    val typeColor = if (order.isBuy) colors.buyGreen else colors.sellRed
    val profitColor = when {
        order.profit > 0 -> colors.buyGreen
        order.profit < 0 -> colors.sellRed
        else -> colors.textSecondary
    }
    val sdf = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())

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
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${String.format("%.5f", order.openPrice)} -> ${String.format("%.5f", order.closePrice)}",
                color = colors.textSecondary,
                fontSize = 10.sp
            )
            if (order.closeTime > 0) {
                Text(
                    text = sdf.format(Date(order.closeTime)),
                    color = colors.textTertiary,
                    fontSize = 9.sp
                )
            }
        }
    }
}
