from fastapi import APIRouter, Header, HTTPException
from pydantic import BaseModel
from typing import Optional
from app.api.auth import get_current_user
from app.services.trading_engine import trading_engine

router = APIRouter(prefix="/api/orders", tags=["orders"])


class PlaceOrderRequest(BaseModel):
    symbol: str
    type: str  # BUY, SELL, BUY_LIMIT, SELL_LIMIT, BUY_STOP, SELL_STOP
    lots: float
    price: Optional[float] = 0.0
    stopLoss: Optional[float] = 0.0
    takeProfit: Optional[float] = 0.0
    comment: Optional[str] = ""


class ModifyOrderRequest(BaseModel):
    stopLoss: Optional[float] = None
    takeProfit: Optional[float] = None


@router.post("")
async def place_order(req: PlaceOrderRequest, authorization: str = Header("")):
    user_id = get_current_user(authorization)

    order, message = trading_engine.place_order(
        user_id=user_id,
        symbol=req.symbol.upper(),
        order_type=req.type.upper(),
        lots=req.lots,
        stop_loss=req.stopLoss or 0.0,
        take_profit=req.takeProfit or 0.0,
        comment=req.comment or ""
    )

    if not order:
        raise HTTPException(status_code=400, detail=message)

    return {
        "id": order.id,
        "symbol": order.symbol,
        "type": order.order_type,
        "lots": order.lots,
        "openPrice": order.open_price,
        "currentPrice": order.current_price,
        "stopLoss": order.stop_loss,
        "takeProfit": order.take_profit,
        "profit": order.profit,
        "marginUsed": order.margin_used,
        "openTime": order.open_time.isoformat(),
        "status": order.status,
        "message": message
    }


@router.post("/{order_id}/close")
async def close_order(order_id: str, authorization: str = Header("")):
    user_id = get_current_user(authorization)

    order, message = trading_engine.close_order(user_id, order_id)

    if not order:
        raise HTTPException(status_code=400, detail=message)

    return {
        "id": order.id,
        "symbol": order.symbol,
        "type": order.order_type,
        "lots": order.lots,
        "openPrice": order.open_price,
        "closePrice": order.close_price,
        "profit": order.profit,
        "closeTime": order.close_time.isoformat() if order.close_time else None,
        "status": order.status,
        "message": message
    }


@router.put("/{order_id}")
async def modify_order(order_id: str, req: ModifyOrderRequest, authorization: str = Header("")):
    user_id = get_current_user(authorization)

    order, message = trading_engine.modify_order(
        user_id, order_id,
        stop_loss=req.stopLoss,
        take_profit=req.takeProfit
    )

    if not order:
        raise HTTPException(status_code=400, detail=message)

    return {
        "id": order.id,
        "symbol": order.symbol,
        "stopLoss": order.stop_loss,
        "takeProfit": order.take_profit,
        "message": message
    }


@router.get("/open")
async def get_open_orders(authorization: str = Header("")):
    user_id = get_current_user(authorization)
    account = trading_engine.accounts.get(user_id)
    if not account:
        return []

    return [
        {
            "id": o.id,
            "symbol": o.symbol,
            "type": o.order_type,
            "lots": o.lots,
            "openPrice": o.open_price,
            "currentPrice": o.current_price,
            "stopLoss": o.stop_loss,
            "takeProfit": o.take_profit,
            "profit": o.profit,
            "commission": o.commission,
            "swap": o.swap,
            "marginUsed": o.margin_used,
            "openTime": o.open_time.isoformat(),
            "status": o.status
        }
        for o in account.open_orders.values()
    ]


@router.get("/history")
async def get_order_history(authorization: str = Header("")):
    user_id = get_current_user(authorization)
    closed = trading_engine.closed_orders.get(user_id, [])

    return [
        {
            "id": o.id,
            "symbol": o.symbol,
            "type": o.order_type,
            "lots": o.lots,
            "openPrice": o.open_price,
            "closePrice": o.close_price,
            "profit": o.profit,
            "commission": o.commission,
            "swap": o.swap,
            "openTime": o.open_time.isoformat(),
            "closeTime": o.close_time.isoformat() if o.close_time else None,
            "status": o.status
        }
        for o in reversed(closed)
    ]
