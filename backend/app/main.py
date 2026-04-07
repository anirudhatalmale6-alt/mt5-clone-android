"""
MT5 Clone Backend - FastAPI Application
Trading engine with real-time WebSocket price streaming.
"""

import asyncio
import json
from contextlib import asynccontextmanager
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from typing import List, Set

from app.api import auth, symbols, orders, account
from app.services.price_engine import price_engine, PriceTick
from app.services.trading_engine import trading_engine


# WebSocket connection manager
class ConnectionManager:
    def __init__(self):
        self.active_connections: List[WebSocket] = []
        self.subscriptions: dict = {}  # websocket -> set of symbols

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)
        # Subscribe to all symbols by default
        self.subscriptions[websocket] = set(price_engine.symbols.keys())

    def disconnect(self, websocket: WebSocket):
        self.active_connections.remove(websocket)
        self.subscriptions.pop(websocket, None)

    async def broadcast_tick(self, tick: PriceTick):
        disconnected = []
        for ws in self.active_connections:
            try:
                subs = self.subscriptions.get(ws, set())
                if tick.symbol in subs or not subs:
                    await ws.send_json({
                        "symbol": tick.symbol,
                        "bid": tick.bid,
                        "ask": tick.ask,
                        "time": tick.time,
                        "volume": tick.volume
                    })
            except Exception:
                disconnected.append(ws)

        for ws in disconnected:
            self.disconnect(ws)


manager = ConnectionManager()


async def on_price_tick(tick: PriceTick):
    """Callback for price engine ticks."""
    # Update trading engine (check SL/TP)
    trading_engine.update_orders(tick)
    # Broadcast to WebSocket clients
    await manager.broadcast_tick(tick)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    price_engine.subscribe(on_price_tick)
    price_task = asyncio.create_task(price_engine.run())

    # Create default demo account
    trading_engine.get_or_create_account("demo", balance=10000.0, leverage=100)

    yield

    # Shutdown
    price_engine.stop()
    price_task.cancel()


app = FastAPI(
    title="MT5 Clone Backend",
    description="Trading backend with real-time WebSocket price streaming",
    version="1.0.0",
    lifespan=lifespan
)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(auth.router)
app.include_router(symbols.router)
app.include_router(orders.router)
app.include_router(account.router)


@app.get("/")
async def root():
    return {
        "name": "MT5 Clone Backend",
        "version": "1.0.0",
        "status": "running",
        "symbols": len(price_engine.symbols),
        "connections": len(manager.active_connections)
    }


@app.get("/api/health")
async def health():
    return {"status": "ok"}


@app.websocket("/ws/prices")
async def websocket_prices(websocket: WebSocket):
    await manager.connect(websocket)
    try:
        while True:
            data = await websocket.receive_text()
            try:
                msg = json.loads(data)
                action = msg.get("action")

                if action == "subscribe":
                    symbols_list = msg.get("symbols", [])
                    manager.subscriptions[websocket] = set(s.upper() for s in symbols_list)

                elif action == "unsubscribe":
                    symbols_list = msg.get("symbols", [])
                    subs = manager.subscriptions.get(websocket, set())
                    for s in symbols_list:
                        subs.discard(s.upper())

            except json.JSONDecodeError:
                pass
    except WebSocketDisconnect:
        manager.disconnect(websocket)


# Admin endpoints
@app.get("/api/admin/users")
async def admin_get_users():
    """Get all registered users and their accounts."""
    users = []
    for email, user in auth.users_db.items():
        account_summary = trading_engine.get_account_summary(user["id"])
        users.append({
            "id": user["id"],
            "name": user["name"],
            "email": user["email"],
            "account": account_summary
        })
    return users


@app.get("/api/admin/stats")
async def admin_stats():
    """Get system statistics."""
    total_orders = sum(
        len(acc.open_orders) for acc in trading_engine.accounts.values()
    )
    total_closed = sum(
        len(orders) for orders in trading_engine.closed_orders.values()
    )
    return {
        "total_users": len(auth.users_db),
        "active_accounts": len(trading_engine.accounts),
        "open_orders": total_orders,
        "closed_orders": total_closed,
        "active_connections": len(manager.active_connections),
        "symbols": len(price_engine.symbols)
    }


@app.post("/api/admin/symbols/{name}/spread")
async def admin_set_spread(name: str, spread: int):
    """Update spread for a symbol."""
    config = price_engine.symbols.get(name.upper())
    if not config:
        return {"error": "Symbol not found"}
    config.spread_points = spread
    return {"symbol": name, "newSpread": spread}


@app.post("/api/admin/accounts/{user_id}/balance")
async def admin_adjust_balance(user_id: str, amount: float):
    """Adjust account balance (deposit/withdraw)."""
    account = trading_engine.accounts.get(user_id)
    if not account:
        return {"error": "Account not found"}
    account.balance += amount
    return trading_engine.get_account_summary(user_id)


@app.post("/api/admin/accounts/{user_id}/leverage")
async def admin_set_leverage(user_id: str, leverage: int):
    """Set account leverage."""
    account = trading_engine.accounts.get(user_id)
    if not account:
        return {"error": "Account not found"}
    account.leverage = leverage
    return {"user_id": user_id, "leverage": leverage}
