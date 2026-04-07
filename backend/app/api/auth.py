from fastapi import APIRouter, HTTPException, Depends
from pydantic import BaseModel, EmailStr
from app.core.security import get_password_hash, verify_password, create_access_token
from app.services.trading_engine import trading_engine

router = APIRouter(prefix="/api/auth", tags=["auth"])

# In-memory user store (replace with DB in production)
users_db = {}


class RegisterRequest(BaseModel):
    name: str
    email: str
    password: str


class LoginRequest(BaseModel):
    email: str
    password: str


class AuthResponse(BaseModel):
    token: str
    account: dict


@router.post("/register", response_model=AuthResponse)
async def register(req: RegisterRequest):
    if req.email in users_db:
        raise HTTPException(status_code=400, detail="Email already registered")

    hashed = get_password_hash(req.password)
    user_id = req.email  # Use email as user ID for simplicity

    users_db[req.email] = {
        "id": user_id,
        "name": req.name,
        "email": req.email,
        "hashed_password": hashed
    }

    # Create trading account
    account = trading_engine.get_or_create_account(user_id)

    token = create_access_token({"sub": user_id, "email": req.email})

    return AuthResponse(
        token=token,
        account=trading_engine.get_account_summary(user_id)
    )


@router.post("/login", response_model=AuthResponse)
async def login(req: LoginRequest):
    user = users_db.get(req.email)
    if not user or not verify_password(req.password, user["hashed_password"]):
        raise HTTPException(status_code=401, detail="Invalid credentials")

    user_id = user["id"]

    # Ensure account exists
    trading_engine.get_or_create_account(user_id)

    token = create_access_token({"sub": user_id, "email": req.email})

    return AuthResponse(
        token=token,
        account=trading_engine.get_account_summary(user_id)
    )


def get_current_user(token: str) -> str:
    """Extract user_id from Bearer token."""
    from app.core.security import decode_token
    if token.startswith("Bearer "):
        token = token[7:]
    payload = decode_token(token)
    if not payload:
        raise HTTPException(status_code=401, detail="Invalid token")
    return payload.get("sub")
