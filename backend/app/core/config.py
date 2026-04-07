from pydantic_settings import BaseSettings
from typing import Optional


class Settings(BaseSettings):
    # App
    APP_NAME: str = "MT5 Clone Backend"
    DEBUG: bool = True
    VERSION: str = "1.0.0"

    # Database
    DATABASE_URL: str = "postgresql+asyncpg://mt5clone:mt5clone@localhost:5432/mt5clone"
    DATABASE_URL_SYNC: str = "postgresql://mt5clone:mt5clone@localhost:5432/mt5clone"

    # JWT
    SECRET_KEY: str = "mt5clone-secret-key-change-in-production"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 1440  # 24 hours

    # Redis
    REDIS_URL: str = "redis://localhost:6379/0"

    # Trading defaults
    DEFAULT_LEVERAGE: int = 100
    DEFAULT_BALANCE: float = 10000.0
    MARGIN_CALL_LEVEL: float = 100.0  # percentage
    STOP_OUT_LEVEL: float = 50.0  # percentage

    # Price simulation
    PRICE_UPDATE_INTERVAL: float = 1.0  # seconds
    DRIFT_MULTIPLIER: float = 0.00002  # directional drift strength
    VOLATILITY_BASE: float = 0.0001  # base volatility for forex

    class Config:
        env_file = ".env"
        case_sensitive = True


settings = Settings()
