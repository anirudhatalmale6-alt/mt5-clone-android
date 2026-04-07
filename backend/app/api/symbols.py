from fastapi import APIRouter
from app.services.price_engine import price_engine

router = APIRouter(prefix="/api/symbols", tags=["symbols"])


@router.get("")
async def get_symbols():
    """Get all available trading symbols with current prices."""
    symbols = []
    for name, config in price_engine.symbols.items():
        tick = price_engine.current_prices.get(name)
        symbols.append({
            "name": config.name,
            "description": get_description(config.name),
            "category": get_category(config.name),
            "digits": config.digits,
            "contractSize": config.contract_size,
            "minLot": 0.01,
            "maxLot": 100.0,
            "lotStep": 0.01,
            "spread": config.spread_points,
            "bid": tick.bid if tick else config.base_price,
            "ask": tick.ask if tick else config.base_price,
            "high": 0.0,
            "low": 0.0,
            "time": tick.time if tick else 0
        })
    return symbols


@router.get("/{name}")
async def get_symbol(name: str):
    """Get a specific symbol."""
    config = price_engine.symbols.get(name.upper())
    if not config:
        return {"error": "Symbol not found"}

    tick = price_engine.current_prices.get(name.upper())
    return {
        "name": config.name,
        "description": get_description(config.name),
        "category": get_category(config.name),
        "digits": config.digits,
        "contractSize": config.contract_size,
        "bid": tick.bid if tick else config.base_price,
        "ask": tick.ask if tick else config.base_price,
        "spread": config.spread_points
    }


@router.get("/{name}/candles")
async def get_candles(name: str, timeframe: str = "H1", count: int = 500):
    """Get historical candles (generated for demo)."""
    import random
    import time as time_module
    from datetime import datetime, timedelta

    config = price_engine.symbols.get(name.upper())
    if not config:
        return []

    tf_seconds = {
        "M1": 60, "M5": 300, "M15": 900, "M30": 1800,
        "H1": 3600, "H4": 14400, "D1": 86400, "W1": 604800, "MN": 2592000
    }
    interval = tf_seconds.get(timeframe, 3600)

    candles = []
    price = config.base_price
    now = int(time_module.time() * 1000)

    for i in range(count, 0, -1):
        t = now - (i * interval * 1000)
        change = random.gauss(0, config.volatility)
        o = price
        c = price + change
        h = max(o, c) + abs(random.gauss(0, config.volatility * 0.5))
        l = min(o, c) - abs(random.gauss(0, config.volatility * 0.5))
        vol = random.randint(100, 600)

        candles.append({
            "time": t,
            "open": round(o, config.digits),
            "high": round(h, config.digits),
            "low": round(l, config.digits),
            "close": round(c, config.digits),
            "volume": vol,
            "tickVolume": vol
        })
        price = c

    return candles


def get_description(name: str) -> str:
    descriptions = {
        "EURUSD": "Euro vs US Dollar",
        "GBPUSD": "Great Britain Pound vs US Dollar",
        "USDJPY": "US Dollar vs Japanese Yen",
        "USDCHF": "US Dollar vs Swiss Franc",
        "AUDUSD": "Australian Dollar vs US Dollar",
        "USDCAD": "US Dollar vs Canadian Dollar",
        "NZDUSD": "New Zealand Dollar vs US Dollar",
        "EURGBP": "Euro vs Great Britain Pound",
        "EURJPY": "Euro vs Japanese Yen",
        "GBPJPY": "Great Britain Pound vs Japanese Yen",
        "XAUUSD": "Gold vs US Dollar",
        "XAGUSD": "Silver vs US Dollar",
        "US500": "S&P 500 Index",
        "US30": "Dow Jones 30 Index",
        "NAS100": "Nasdaq 100 Index",
        "GER40": "Germany 40 Index",
        "BTCUSD": "Bitcoin vs US Dollar",
        "ETHUSD": "Ethereum vs US Dollar",
    }
    return descriptions.get(name, name)


def get_category(name: str) -> str:
    if name.startswith("XAU") or name.startswith("XAG"):
        return "Metals"
    if name in ("US500", "US30", "NAS100", "GER40"):
        return "Indices"
    if name in ("BTCUSD", "ETHUSD"):
        return "Crypto"
    return "Forex"
