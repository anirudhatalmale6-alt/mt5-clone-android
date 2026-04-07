"""
Price Simulation Engine

When no order is open: streams real API prices directly.
When an order is open: applies geometric Brownian motion with directional drift.
On order close: smoothly interpolates back to real market price.
"""

import asyncio
import math
import random
import time
from typing import Dict, List, Optional, Callable
from dataclasses import dataclass, field
from app.core.config import settings


@dataclass
class PriceTick:
    symbol: str
    bid: float
    ask: float
    time: int  # unix ms
    volume: int = 0


@dataclass
class SymbolConfig:
    name: str
    digits: int
    spread_points: int
    contract_size: float
    base_price: float
    volatility: float  # per-tick volatility
    min_lot: float = 0.01
    max_lot: float = 100.0
    lot_step: float = 0.01


@dataclass
class ActiveDrift:
    """Tracks drift state when an order is open."""
    direction: int  # +1 for buy, -1 for sell
    start_price: float
    current_simulated_price: float
    drift_rate: float
    volatility: float
    ticks_elapsed: int = 0


@dataclass
class PriceReconnection:
    """Tracks smooth reconnection to real price after order close."""
    start_simulated_price: float
    target_real_price: float
    total_steps: int = 30  # number of ticks to converge
    current_step: int = 0


class PriceEngine:
    def __init__(self):
        self.symbols: Dict[str, SymbolConfig] = {}
        self.current_prices: Dict[str, PriceTick] = {}
        self.real_prices: Dict[str, float] = {}  # latest from API
        self.active_drifts: Dict[str, ActiveDrift] = {}  # symbol -> drift
        self.reconnections: Dict[str, PriceReconnection] = {}
        self.subscribers: List[Callable] = []
        self._running = False
        self._init_default_symbols()

    def _init_default_symbols(self):
        defaults = [
            ("EURUSD", 5, 12, 100000.0, 1.08450, 0.00008),
            ("GBPUSD", 5, 15, 100000.0, 1.26320, 0.00010),
            ("USDJPY", 3, 13, 100000.0, 151.235, 0.015),
            ("USDCHF", 5, 16, 100000.0, 0.88340, 0.00008),
            ("AUDUSD", 5, 14, 100000.0, 0.65780, 0.00007),
            ("USDCAD", 5, 18, 100000.0, 1.35620, 0.00008),
            ("NZDUSD", 5, 20, 100000.0, 0.60120, 0.00007),
            ("EURGBP", 5, 18, 100000.0, 0.85830, 0.00006),
            ("EURJPY", 3, 20, 100000.0, 163.890, 0.015),
            ("GBPJPY", 3, 30, 100000.0, 191.050, 0.020),
            ("XAUUSD", 2, 30, 100.0, 2345.50, 0.50),
            ("XAGUSD", 3, 25, 5000.0, 27.850, 0.010),
            ("US500", 2, 50, 50.0, 5234.50, 0.50),
            ("US30", 2, 30, 10.0, 39150.00, 5.0),
            ("NAS100", 2, 40, 20.0, 18320.00, 3.0),
            ("GER40", 2, 20, 25.0, 18450.00, 2.0),
            ("BTCUSD", 2, 5000, 1.0, 69500.00, 20.0),
            ("ETHUSD", 2, 300, 1.0, 3520.00, 3.0),
        ]
        for name, digits, spread, cs, price, vol in defaults:
            self.symbols[name] = SymbolConfig(name, digits, spread, cs, price, vol)
            point = 10 ** (-digits)
            self.current_prices[name] = PriceTick(
                symbol=name,
                bid=price,
                ask=price + spread * point,
                time=int(time.time() * 1000)
            )
            self.real_prices[name] = price

    def subscribe(self, callback: Callable):
        self.subscribers.append(callback)

    def unsubscribe(self, callback: Callable):
        self.subscribers = [s for s in self.subscribers if s != callback]

    def start_drift(self, symbol: str, direction: int):
        """Start directional price drift when order opens."""
        if symbol not in self.symbols:
            return
        config = self.symbols[symbol]
        current_price = self.real_prices.get(symbol, config.base_price)

        self.active_drifts[symbol] = ActiveDrift(
            direction=direction,
            start_price=current_price,
            current_simulated_price=current_price,
            drift_rate=config.volatility * 0.3 * direction,  # subtle directional bias
            volatility=config.volatility
        )
        # Remove any active reconnection
        self.reconnections.pop(symbol, None)

    def stop_drift(self, symbol: str):
        """Stop drift and start smooth reconnection to real price."""
        if symbol in self.active_drifts:
            drift = self.active_drifts.pop(symbol)
            real_price = self.real_prices.get(symbol, drift.start_price)
            self.reconnections[symbol] = PriceReconnection(
                start_simulated_price=drift.current_simulated_price,
                target_real_price=real_price,
                total_steps=30,
                current_step=0
            )

    def update_real_price(self, symbol: str, price: float):
        """Update the real market price from external API."""
        self.real_prices[symbol] = price
        # If reconnecting, update target
        if symbol in self.reconnections:
            self.reconnections[symbol].target_real_price = price

    def generate_tick(self, symbol: str) -> Optional[PriceTick]:
        """Generate the next price tick for a symbol."""
        if symbol not in self.symbols:
            return None

        config = self.symbols[symbol]
        point = 10 ** (-config.digits)
        now_ms = int(time.time() * 1000)

        # Case 1: Active drift (order is open)
        if symbol in self.active_drifts:
            drift = self.active_drifts[symbol]
            drift.ticks_elapsed += 1

            # Geometric Brownian Motion: dS = mu*S*dt + sigma*S*dW
            dt = 1.0
            dW = random.gauss(0, 1)
            price_change = (drift.drift_rate * dt + drift.volatility * dW)

            # Add mean reversion to prevent price running too far
            distance = drift.current_simulated_price - drift.start_price
            mean_reversion = -0.001 * distance

            new_price = drift.current_simulated_price + price_change + mean_reversion
            drift.current_simulated_price = new_price

            bid = round(new_price, config.digits)
            ask = round(new_price + config.spread_points * point, config.digits)

        # Case 2: Reconnecting to real price
        elif symbol in self.reconnections:
            recon = self.reconnections[symbol]
            recon.current_step += 1

            # Exponential interpolation
            progress = recon.current_step / recon.total_steps
            # Use ease-in-out curve for smooth transition
            ease = progress * progress * (3 - 2 * progress)

            price = recon.start_simulated_price + (recon.target_real_price - recon.start_simulated_price) * ease

            # Add small noise for realism
            noise = random.gauss(0, config.volatility * 0.3)
            price += noise

            bid = round(price, config.digits)
            ask = round(price + config.spread_points * point, config.digits)

            if recon.current_step >= recon.total_steps:
                self.reconnections.pop(symbol)

        # Case 3: Normal - use real price with small random walk
        else:
            real = self.real_prices.get(symbol, config.base_price)
            # Small random fluctuation around real price
            noise = random.gauss(0, config.volatility * 0.5)
            price = real + noise
            self.real_prices[symbol] = real  # keep real price stable

            bid = round(price, config.digits)
            ask = round(price + config.spread_points * point, config.digits)

        tick = PriceTick(
            symbol=symbol,
            bid=bid,
            ask=ask,
            time=now_ms,
            volume=random.randint(1, 50)
        )
        self.current_prices[symbol] = tick
        return tick

    async def run(self):
        """Main price generation loop."""
        self._running = True
        while self._running:
            for symbol in list(self.symbols.keys()):
                tick = self.generate_tick(symbol)
                if tick:
                    for callback in self.subscribers:
                        try:
                            await callback(tick)
                        except Exception:
                            pass
            await asyncio.sleep(settings.PRICE_UPDATE_INTERVAL)

    def stop(self):
        self._running = False

    def get_current_price(self, symbol: str) -> Optional[PriceTick]:
        return self.current_prices.get(symbol)


# Global instance
price_engine = PriceEngine()
