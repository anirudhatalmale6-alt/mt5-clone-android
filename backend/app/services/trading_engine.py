"""
Trading Engine - Handles order execution, margin calculations, P/L tracking.
Mirrors MT5 calculation logic.
"""

import uuid
from datetime import datetime
from typing import Dict, List, Optional, Tuple
from dataclasses import dataclass
from app.services.price_engine import price_engine, PriceTick


@dataclass
class AccountState:
    user_id: str
    balance: float
    leverage: int
    currency: str = "USD"

    @property
    def equity(self) -> float:
        return self.balance + self.floating_pl

    @property
    def used_margin(self) -> float:
        return sum(o.margin_used for o in self.open_orders.values())

    @property
    def free_margin(self) -> float:
        return self.equity - self.used_margin

    @property
    def margin_level(self) -> float:
        if self.used_margin <= 0:
            return 0.0
        return (self.equity / self.used_margin) * 100.0

    @property
    def floating_pl(self) -> float:
        return sum(o.profit for o in self.open_orders.values())

    open_orders: Dict[str, "TradeOrder"] = None

    def __post_init__(self):
        if self.open_orders is None:
            self.open_orders = {}


@dataclass
class TradeOrder:
    id: str
    user_id: str
    symbol: str
    order_type: str  # BUY, SELL, BUY_LIMIT, etc.
    lots: float
    open_price: float
    current_price: float = 0.0
    stop_loss: float = 0.0
    take_profit: float = 0.0
    profit: float = 0.0
    commission: float = 0.0
    swap: float = 0.0
    margin_used: float = 0.0
    open_time: datetime = None
    close_time: datetime = None
    close_price: float = 0.0
    status: str = "OPEN"
    comment: str = ""

    def __post_init__(self):
        if self.open_time is None:
            self.open_time = datetime.utcnow()


class TradingEngine:
    def __init__(self):
        self.accounts: Dict[str, AccountState] = {}
        self.closed_orders: Dict[str, List[TradeOrder]] = {}  # user_id -> orders

    def get_or_create_account(self, user_id: str, balance: float = 10000.0, leverage: int = 100) -> AccountState:
        if user_id not in self.accounts:
            self.accounts[user_id] = AccountState(
                user_id=user_id,
                balance=balance,
                leverage=leverage
            )
            self.closed_orders[user_id] = []
        return self.accounts[user_id]

    def calculate_margin(self, symbol: str, lots: float, price: float, leverage: int) -> float:
        """Calculate required margin for a trade. MT5 formula:
        Margin = (Lots * ContractSize * Price) / Leverage
        """
        config = price_engine.symbols.get(symbol)
        if not config:
            return 0.0

        contract_size = config.contract_size

        # For JPY pairs and metals, adjust calculation
        if symbol.endswith("JPY"):
            # USDJPY etc - quote currency is JPY
            margin = (lots * contract_size * price) / leverage
            # Convert to USD approximately
            margin = margin / price if price > 0 else margin
        elif symbol.startswith("XAU") or symbol.startswith("XAG"):
            margin = (lots * contract_size * price) / leverage
        elif symbol in ("US500", "US30", "NAS100", "GER40"):
            margin = (lots * contract_size * price) / leverage
        elif symbol in ("BTCUSD", "ETHUSD"):
            margin = (lots * contract_size * price) / leverage
        else:
            # Standard forex - base currency is first
            margin = (lots * contract_size) / leverage
            # If base is not USD, multiply by price
            if not symbol.startswith("USD"):
                margin = (lots * contract_size * price) / leverage
            else:
                margin = (lots * contract_size) / leverage

        return round(margin, 2)

    def calculate_profit(self, symbol: str, order_type: str, lots: float,
                         open_price: float, current_price: float) -> float:
        """Calculate floating P/L for a position. MT5 formula:
        For Buy: Profit = (CurrentPrice - OpenPrice) * Lots * ContractSize
        For Sell: Profit = (OpenPrice - CurrentPrice) * Lots * ContractSize
        """
        config = price_engine.symbols.get(symbol)
        if not config:
            return 0.0

        contract_size = config.contract_size
        is_buy = order_type in ("BUY", "BUY_LIMIT", "BUY_STOP")

        if is_buy:
            profit = (current_price - open_price) * lots * contract_size
        else:
            profit = (open_price - current_price) * lots * contract_size

        # Convert profit to USD if needed
        if symbol.endswith("JPY"):
            # Profit is in JPY, convert to USD
            usdjpy_price = price_engine.real_prices.get("USDJPY", 151.0)
            profit = profit / usdjpy_price
        elif symbol.endswith("CHF"):
            usdchf_price = price_engine.real_prices.get("USDCHF", 0.88)
            profit = profit / usdchf_price
        elif symbol.endswith("CAD"):
            usdcad_price = price_engine.real_prices.get("USDCAD", 1.36)
            profit = profit / usdcad_price

        return round(profit, 2)

    def place_order(self, user_id: str, symbol: str, order_type: str,
                    lots: float, stop_loss: float = 0.0, take_profit: float = 0.0,
                    comment: str = "") -> Tuple[Optional[TradeOrder], str]:
        """Place a new order."""
        account = self.accounts.get(user_id)
        if not account:
            return None, "Account not found"

        # Get current price
        tick = price_engine.get_current_price(symbol)
        if not tick:
            return None, f"Symbol {symbol} not found"

        is_buy = order_type in ("BUY", "BUY_LIMIT", "BUY_STOP")
        price = tick.ask if is_buy else tick.bid

        # Validate lot size
        config = price_engine.symbols.get(symbol)
        if not config:
            return None, "Invalid symbol"
        if lots < config.min_lot or lots > config.max_lot:
            return None, f"Lot size must be between {config.min_lot} and {config.max_lot}"

        # Calculate and check margin
        required_margin = self.calculate_margin(symbol, lots, price, account.leverage)
        if required_margin > account.free_margin:
            return None, "Not enough free margin"

        # Create order
        order = TradeOrder(
            id=str(uuid.uuid4()),
            user_id=user_id,
            symbol=symbol,
            order_type=order_type,
            lots=lots,
            open_price=price,
            current_price=price,
            stop_loss=stop_loss,
            take_profit=take_profit,
            margin_used=required_margin,
            comment=comment
        )

        account.open_orders[order.id] = order

        # Start price drift in order direction
        direction = 1 if is_buy else -1
        price_engine.start_drift(symbol, direction)

        return order, "Order placed successfully"

    def close_order(self, user_id: str, order_id: str) -> Tuple[Optional[TradeOrder], str]:
        """Close an open order."""
        account = self.accounts.get(user_id)
        if not account:
            return None, "Account not found"

        order = account.open_orders.get(order_id)
        if not order:
            return None, "Order not found"

        # Get closing price
        tick = price_engine.get_current_price(order.symbol)
        if not tick:
            return None, "Cannot get current price"

        is_buy = order.order_type in ("BUY", "BUY_LIMIT", "BUY_STOP")
        close_price = tick.bid if is_buy else tick.ask  # Close at opposite side

        # Calculate final profit
        final_profit = self.calculate_profit(
            order.symbol, order.order_type, order.lots,
            order.open_price, close_price
        )

        # Update order
        order.close_price = close_price
        order.close_time = datetime.utcnow()
        order.profit = final_profit
        order.status = "CLOSED"

        # Update balance
        account.balance += final_profit

        # Move to closed orders
        del account.open_orders[order_id]
        if user_id not in self.closed_orders:
            self.closed_orders[user_id] = []
        self.closed_orders[user_id].append(order)

        # Stop drift and start reconnection to real price
        price_engine.stop_drift(order.symbol)

        return order, f"Order closed. Profit: {final_profit}"

    def modify_order(self, user_id: str, order_id: str,
                     stop_loss: float = None, take_profit: float = None) -> Tuple[Optional[TradeOrder], str]:
        """Modify SL/TP of an open order."""
        account = self.accounts.get(user_id)
        if not account:
            return None, "Account not found"

        order = account.open_orders.get(order_id)
        if not order:
            return None, "Order not found"

        if stop_loss is not None:
            order.stop_loss = stop_loss
        if take_profit is not None:
            order.take_profit = take_profit

        return order, "Order modified"

    def update_orders(self, tick: PriceTick):
        """Update all open orders with new price tick."""
        for user_id, account in self.accounts.items():
            for order_id, order in list(account.open_orders.items()):
                if order.symbol != tick.symbol:
                    continue

                is_buy = order.order_type in ("BUY", "BUY_LIMIT", "BUY_STOP")
                order.current_price = tick.bid if is_buy else tick.ask

                # Update floating P/L
                order.profit = self.calculate_profit(
                    order.symbol, order.order_type, order.lots,
                    order.open_price, order.current_price
                )

                # Check SL/TP
                if order.stop_loss > 0:
                    if (is_buy and tick.bid <= order.stop_loss) or \
                       (not is_buy and tick.ask >= order.stop_loss):
                        self.close_order(user_id, order_id)
                        continue

                if order.take_profit > 0:
                    if (is_buy and tick.bid >= order.take_profit) or \
                       (not is_buy and tick.ask <= order.take_profit):
                        self.close_order(user_id, order_id)
                        continue

    def get_account_summary(self, user_id: str) -> dict:
        """Get full account summary."""
        account = self.accounts.get(user_id)
        if not account:
            return {}

        return {
            "balance": round(account.balance, 2),
            "equity": round(account.equity, 2),
            "margin": round(account.used_margin, 2),
            "free_margin": round(account.free_margin, 2),
            "margin_level": round(account.margin_level, 2),
            "profit": round(account.floating_pl, 2),
            "leverage": account.leverage,
            "currency": account.currency,
            "open_positions": len(account.open_orders)
        }


# Global instance
trading_engine = TradingEngine()
