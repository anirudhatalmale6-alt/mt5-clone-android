import uuid
from datetime import datetime
from sqlalchemy import Column, String, Float, Integer, Boolean, DateTime, ForeignKey, Text, Enum as SQLEnum
from sqlalchemy.orm import relationship
from app.core.database import Base
import enum


class OrderType(str, enum.Enum):
    BUY = "BUY"
    SELL = "SELL"
    BUY_LIMIT = "BUY_LIMIT"
    SELL_LIMIT = "SELL_LIMIT"
    BUY_STOP = "BUY_STOP"
    SELL_STOP = "SELL_STOP"


class OrderStatus(str, enum.Enum):
    OPEN = "OPEN"
    CLOSED = "CLOSED"
    PENDING = "PENDING"
    CANCELLED = "CANCELLED"


class User(Base):
    __tablename__ = "users"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    email = Column(String, unique=True, index=True, nullable=False)
    name = Column(String, nullable=False)
    hashed_password = Column(String, nullable=False)
    is_active = Column(Boolean, default=True)
    is_admin = Column(Boolean, default=False)
    created_at = Column(DateTime, default=datetime.utcnow)

    account = relationship("TradingAccount", back_populates="user", uselist=False)
    orders = relationship("Order", back_populates="user")


class TradingAccount(Base):
    __tablename__ = "trading_accounts"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String, ForeignKey("users.id"), unique=True)
    name = Column(String, default="Demo Account")
    server = Column(String, default="MT5Clone-Demo")
    currency = Column(String, default="USD")
    balance = Column(Float, default=10000.0)
    leverage = Column(Integer, default=100)
    margin_call_level = Column(Float, default=100.0)
    stop_out_level = Column(Float, default=50.0)
    created_at = Column(DateTime, default=datetime.utcnow)

    user = relationship("User", back_populates="account")


class Symbol(Base):
    __tablename__ = "symbols"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    name = Column(String, unique=True, index=True, nullable=False)
    description = Column(String)
    category = Column(String, default="Forex")
    digits = Column(Integer, default=5)
    contract_size = Column(Float, default=100000.0)
    min_lot = Column(Float, default=0.01)
    max_lot = Column(Float, default=100.0)
    lot_step = Column(Float, default=0.01)
    base_spread = Column(Integer, default=10)
    is_active = Column(Boolean, default=True)
    base_price = Column(Float, default=0.0)  # Reference price for simulation


class Order(Base):
    __tablename__ = "orders"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String, ForeignKey("users.id"))
    symbol = Column(String, nullable=False)
    type = Column(String, nullable=False)
    status = Column(String, default=OrderStatus.OPEN.value)
    lots = Column(Float, nullable=False)
    open_price = Column(Float, nullable=False)
    close_price = Column(Float, default=0.0)
    current_price = Column(Float, default=0.0)
    stop_loss = Column(Float, default=0.0)
    take_profit = Column(Float, default=0.0)
    profit = Column(Float, default=0.0)
    commission = Column(Float, default=0.0)
    swap = Column(Float, default=0.0)
    margin_used = Column(Float, default=0.0)
    comment = Column(Text, default="")
    open_time = Column(DateTime, default=datetime.utcnow)
    close_time = Column(DateTime, nullable=True)

    user = relationship("User", back_populates="orders")


class PriceHistory(Base):
    __tablename__ = "price_history"

    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    symbol = Column(String, index=True, nullable=False)
    timeframe = Column(String, nullable=False)
    time = Column(DateTime, nullable=False)
    open = Column(Float, nullable=False)
    high = Column(Float, nullable=False)
    low = Column(Float, nullable=False)
    close = Column(Float, nullable=False)
    volume = Column(Integer, default=0)
    tick_volume = Column(Integer, default=0)
