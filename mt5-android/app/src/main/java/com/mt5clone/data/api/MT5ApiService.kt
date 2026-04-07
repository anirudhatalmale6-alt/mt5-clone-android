package com.mt5clone.data.api

import com.mt5clone.data.model.Candle
import com.mt5clone.data.model.Order
import com.mt5clone.data.model.Symbol
import com.mt5clone.data.model.TradingAccount
import retrofit2.Response
import retrofit2.http.*

interface MT5ApiService {

    // Auth
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>

    // Symbols
    @GET("api/symbols")
    suspend fun getSymbols(): Response<List<Symbol>>

    @GET("api/symbols/{name}/candles")
    suspend fun getCandles(
        @Path("name") symbol: String,
        @Query("timeframe") timeframe: String,
        @Query("count") count: Int = 500
    ): Response<List<Candle>>

    // Account
    @GET("api/account")
    suspend fun getAccount(@Header("Authorization") token: String): Response<TradingAccount>

    // Orders
    @POST("api/orders")
    suspend fun placeOrder(
        @Header("Authorization") token: String,
        @Body request: PlaceOrderRequest
    ): Response<Order>

    @POST("api/orders/{id}/close")
    suspend fun closeOrder(
        @Header("Authorization") token: String,
        @Path("id") orderId: String
    ): Response<Order>

    @GET("api/orders/open")
    suspend fun getOpenOrders(@Header("Authorization") token: String): Response<List<Order>>

    @GET("api/orders/history")
    suspend fun getOrderHistory(@Header("Authorization") token: String): Response<List<Order>>

    @PUT("api/orders/{id}")
    suspend fun modifyOrder(
        @Header("Authorization") token: String,
        @Path("id") orderId: String,
        @Body request: ModifyOrderRequest
    ): Response<Order>
}

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val name: String, val email: String, val password: String)
data class LoginResponse(val token: String, val account: TradingAccount)
data class PlaceOrderRequest(
    val symbol: String,
    val type: String,
    val lots: Double,
    val price: Double,
    val stopLoss: Double = 0.0,
    val takeProfit: Double = 0.0,
    val comment: String = ""
)
data class ModifyOrderRequest(
    val stopLoss: Double = 0.0,
    val takeProfit: Double = 0.0
)
