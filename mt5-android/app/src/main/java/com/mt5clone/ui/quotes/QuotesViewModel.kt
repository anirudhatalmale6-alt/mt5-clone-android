package com.mt5clone.ui.quotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mt5clone.data.model.PriceTick
import com.mt5clone.data.model.Symbol
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuotesUiState(
    val symbols: List<Symbol> = defaultSymbols(),
    val selectedCategory: String = "All",
    val categories: List<String> = listOf("All", "Forex", "Metals", "Indices", "Crypto"),
    val isConnected: Boolean = false,
    val searchQuery: String = ""
)

fun defaultSymbols(): List<Symbol> = listOf(
    // Forex Major
    Symbol("EURUSD", "Euro vs US Dollar", "Forex", 5, 100000.0, spread = 12, bid = 1.08450, ask = 1.08462),
    Symbol("GBPUSD", "Great Britain Pound vs US Dollar", "Forex", 5, 100000.0, spread = 15, bid = 1.26320, ask = 1.26335),
    Symbol("USDJPY", "US Dollar vs Japanese Yen", "Forex", 3, 100000.0, spread = 13, bid = 151.235, ask = 151.248),
    Symbol("USDCHF", "US Dollar vs Swiss Franc", "Forex", 5, 100000.0, spread = 16, bid = 0.88340, ask = 0.88356),
    Symbol("AUDUSD", "Australian Dollar vs US Dollar", "Forex", 5, 100000.0, spread = 14, bid = 0.65780, ask = 0.65794),
    Symbol("USDCAD", "US Dollar vs Canadian Dollar", "Forex", 5, 100000.0, spread = 18, bid = 1.35620, ask = 1.35638),
    Symbol("NZDUSD", "New Zealand Dollar vs US Dollar", "Forex", 5, 100000.0, spread = 20, bid = 0.60120, ask = 0.60140),
    // Forex Cross
    Symbol("EURGBP", "Euro vs Great Britain Pound", "Forex", 5, 100000.0, spread = 18, bid = 0.85830, ask = 0.85848),
    Symbol("EURJPY", "Euro vs Japanese Yen", "Forex", 3, 100000.0, spread = 20, bid = 163.890, ask = 163.910),
    Symbol("GBPJPY", "Great Britain Pound vs Japanese Yen", "Forex", 3, 100000.0, spread = 30, bid = 191.050, ask = 191.080),
    // Metals
    Symbol("XAUUSD", "Gold vs US Dollar", "Metals", 2, 100.0, spread = 30, bid = 2345.50, ask = 2345.80),
    Symbol("XAGUSD", "Silver vs US Dollar", "Metals", 3, 5000.0, spread = 25, bid = 27.850, ask = 27.875),
    // Indices
    Symbol("US500", "S&P 500", "Indices", 2, 50.0, spread = 50, bid = 5234.50, ask = 5235.00),
    Symbol("US30", "Dow Jones 30", "Indices", 2, 10.0, spread = 30, bid = 39150.00, ask = 39153.00),
    Symbol("NAS100", "Nasdaq 100", "Indices", 2, 20.0, spread = 40, bid = 18320.00, ask = 18324.00),
    Symbol("GER40", "Germany 40", "Indices", 2, 25.0, spread = 20, bid = 18450.00, ask = 18452.00),
    // Crypto
    Symbol("BTCUSD", "Bitcoin vs US Dollar", "Crypto", 2, 1.0, spread = 5000, bid = 69500.00, ask = 69550.00),
    Symbol("ETHUSD", "Ethereum vs US Dollar", "Crypto", 2, 1.0, spread = 300, bid = 3520.00, ask = 3523.00)
)

@HiltViewModel
class QuotesViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(QuotesUiState())
    val uiState: StateFlow<QuotesUiState> = _uiState.asStateFlow()

    fun selectCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun updateSearch(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun getFilteredSymbols(): List<Symbol> {
        val state = _uiState.value
        return state.symbols.filter { symbol ->
            val matchesCategory = state.selectedCategory == "All" || symbol.category == state.selectedCategory
            val matchesSearch = state.searchQuery.isEmpty() ||
                symbol.name.contains(state.searchQuery, ignoreCase = true) ||
                symbol.description.contains(state.searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    fun onTickReceived(tick: PriceTick) {
        val updatedSymbols = _uiState.value.symbols.map { symbol ->
            if (symbol.name == tick.symbol) {
                symbol.copy(
                    bid = tick.bid,
                    ask = tick.ask,
                    time = tick.time,
                    spread = ((tick.ask - tick.bid) / symbol.pointSize).toInt()
                )
            } else symbol
        }
        _uiState.value = _uiState.value.copy(symbols = updatedSymbols)
    }
}
