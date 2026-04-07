package com.mt5clone.ui.quotes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.mt5clone.data.model.Symbol
import com.mt5clone.ui.common.theme.MT5ThemeProvider

@Composable
fun QuotesScreen(
    navController: NavHostController,
    viewModel: QuotesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = MT5ThemeProvider.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundPrimary)
    ) {
        // Top bar
        QuotesTopBar(colors)

        // Category tabs
        CategoryTabs(
            categories = uiState.categories,
            selectedCategory = uiState.selectedCategory,
            onCategorySelected = { viewModel.selectCategory(it) },
            colors = colors
        )

        // Column headers
        QuotesHeader(colors)

        // Symbol list
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            val filteredSymbols = viewModel.getFilteredSymbols()
            items(filteredSymbols) { symbol ->
                SymbolRow(
                    symbol = symbol,
                    colors = colors,
                    onClick = {
                        navController.navigate("chart")
                    }
                )
                HorizontalDivider(
                    color = colors.divider,
                    thickness = 0.5.dp
                )
            }
        }
    }
}

@Composable
private fun QuotesTopBar(colors: com.mt5clone.ui.common.theme.MT5ColorScheme) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.backgroundSecondary)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Quotes",
            color = colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CategoryTabs(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    colors: com.mt5clone.ui.common.theme.MT5ColorScheme
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.backgroundSecondary)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(categories) { category ->
            val selected = category == selectedCategory
            FilterChip(
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = category,
                        fontSize = 11.sp,
                        color = if (selected) colors.tabSelected else colors.textSecondary
                    )
                },
                selected = selected,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = colors.backgroundSecondary,
                    selectedContainerColor = colors.backgroundTertiary
                ),
                border = null,
                modifier = Modifier.height(28.dp)
            )
        }
    }
}

@Composable
private fun QuotesHeader(colors: com.mt5clone.ui.common.theme.MT5ColorScheme) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.backgroundSecondary)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Symbol",
            color = colors.textTertiary,
            fontSize = 10.sp,
            modifier = Modifier.weight(1.2f)
        )
        Text(
            text = "Bid",
            color = colors.textTertiary,
            fontSize = 10.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Ask",
            color = colors.textTertiary,
            fontSize = 10.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Spread",
            color = colors.textTertiary,
            fontSize = 10.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.6f)
        )
    }
    HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
}

@Composable
private fun SymbolRow(
    symbol: Symbol,
    colors: com.mt5clone.ui.common.theme.MT5ColorScheme,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Symbol name + description
        Column(modifier = Modifier.weight(1.2f)) {
            Text(
                text = symbol.name,
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = symbol.description,
                color = colors.textTertiary,
                fontSize = 9.sp,
                maxLines = 1
            )
        }

        // Bid price
        Text(
            text = formatPrice(symbol.bid, symbol.digits),
            color = colors.sellRed,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )

        // Ask price
        Text(
            text = formatPrice(symbol.ask, symbol.digits),
            color = colors.buyGreen,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )

        // Spread
        Text(
            text = symbol.spreadDisplay,
            color = colors.textSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.6f)
        )
    }
}

private fun formatPrice(price: Double, digits: Int): String {
    return String.format("%.${digits}f", price)
}
