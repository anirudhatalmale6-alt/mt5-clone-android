# MT5 Clone - Android Trading Application

Full MetaTrader 5 clone for Android with real-time price streaming, order execution, and trading engine.

## Project Structure

```
mt5-android/          - Android app (Kotlin + Jetpack Compose)
backend/              - Trading backend (Python FastAPI)
postman_collection.json - API testing collection
```

## Android App

### Tech Stack
- Kotlin + Jetpack Compose
- MVVM Architecture
- Hilt Dependency Injection
- Retrofit + OkHttp (REST API)
- Java-WebSocket (real-time prices)
- Room (local storage)
- Coroutines + Flow

### Build & Run
1. Open `mt5-android/` in Android Studio
2. Sync Gradle
3. Update `AppModule.kt` base URL to point to your backend
4. Build and run on device/emulator

### Features
- MT5-style dark theme UI
- Bottom navigation: Quotes, Chart, Trade, History, Messages
- Real-time candlestick chart with 9 timeframes (M1-MN)
- Chart interactions: zoom, scroll, crosshair with OHLC overlay
- Live bid/ask/spread quotes
- Buy/Sell with lot size, SL, TP
- Account summary (balance, equity, margin, free margin)
- Order history

## Backend

### Tech Stack
- Python FastAPI
- PostgreSQL (via Docker)
- Redis (price caching)
- WebSocket (real-time streaming)

### Quick Start (Docker)
```bash
cd backend
docker-compose up -d
```

### Quick Start (Local)
```bash
cd backend
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

### API Endpoints

**Auth:**
- POST /api/auth/register
- POST /api/auth/login

**Symbols:**
- GET /api/symbols
- GET /api/symbols/{name}
- GET /api/symbols/{name}/candles?timeframe=H1&count=500

**Orders:**
- POST /api/orders (place order)
- POST /api/orders/{id}/close
- PUT /api/orders/{id} (modify SL/TP)
- GET /api/orders/open
- GET /api/orders/history

**Account:**
- GET /api/account

**WebSocket:**
- ws://localhost:8000/ws/prices

**Admin:**
- GET /api/admin/users
- GET /api/admin/stats
- POST /api/admin/symbols/{name}/spread?spread=15
- POST /api/admin/accounts/{user_id}/balance?amount=5000
- POST /api/admin/accounts/{user_id}/leverage?leverage=200

## Price Simulation Engine

- **No order open**: Streams real market prices (or simulated random walk)
- **Order opened (Buy)**: Price drifts upward using geometric Brownian motion
- **Order opened (Sell)**: Price drifts downward
- **Order closed**: Smooth exponential interpolation back to real market price

## Trading Calculations (MT5 Logic)

- Margin = (Lots x ContractSize x Price) / Leverage
- P/L (Buy) = (CurrentPrice - OpenPrice) x Lots x ContractSize
- P/L (Sell) = (OpenPrice - CurrentPrice) x Lots x ContractSize
- Equity = Balance + FloatingP/L
- Free Margin = Equity - UsedMargin
- Margin Level = (Equity / UsedMargin) x 100%
