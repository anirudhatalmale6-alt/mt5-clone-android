from fastapi import APIRouter, Header
from app.api.auth import get_current_user
from app.services.trading_engine import trading_engine

router = APIRouter(prefix="/api/account", tags=["account"])


@router.get("")
async def get_account(authorization: str = Header("")):
    user_id = get_current_user(authorization)
    summary = trading_engine.get_account_summary(user_id)
    if not summary:
        return {"error": "Account not found"}
    return summary
