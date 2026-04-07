package com.mt5clone.ui.trade

import androidx.lifecycle.ViewModel
import com.mt5clone.data.model.Order
import com.mt5clone.data.model.TradingAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class TradeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(TradeUiState())
    val uiState: StateFlow<TradeUiState> = _uiState.asStateFlow()

    fun selectTab(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun updateAccount(account: TradingAccount) {
        _uiState.value = _uiState.value.copy(account = account)
    }

    fun updateOrders(orders: List<Order>) {
        val state = _uiState.value
        val filtered = if (state.selectedTab == 0) {
            orders.filter { it.isOpen && !it.isPending }
        } else {
            orders.filter { it.isOpen && it.isPending }
        }
        _uiState.value = state.copy(openOrders = filtered)
    }
}
