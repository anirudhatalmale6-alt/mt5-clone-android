package com.mt5clone.data.websocket

import com.google.gson.Gson
import com.mt5clone.data.model.PriceTick
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class PriceWebSocketClient(
    private val serverUri: String = "ws://10.0.2.2:8000/ws/prices"
) {
    private var client: WebSocketClient? = null
    private val gson = Gson()
    private val _tickChannel = Channel<PriceTick>(Channel.BUFFERED)
    val tickFlow: Flow<PriceTick> = _tickChannel.receiveAsFlow()

    private var _isConnected = false
    val isConnected: Boolean get() = _isConnected

    fun connect(symbols: List<String> = emptyList()) {
        try {
            client = object : WebSocketClient(URI(serverUri)) {
                override fun onOpen(handshakedata: ServerHandshake?) {
                    _isConnected = true
                    // Subscribe to symbols
                    if (symbols.isNotEmpty()) {
                        val subscribeMsg = gson.toJson(
                            mapOf("action" to "subscribe", "symbols" to symbols)
                        )
                        send(subscribeMsg)
                    }
                }

                override fun onMessage(message: String?) {
                    message?.let {
                        try {
                            val tick = gson.fromJson(it, PriceTick::class.java)
                            _tickChannel.trySend(tick)
                        } catch (e: Exception) {
                            // Handle parse error
                        }
                    }
                }

                override fun onClose(code: Int, reason: String?, remote: Boolean) {
                    _isConnected = false
                }

                override fun onError(ex: Exception?) {
                    _isConnected = false
                }
            }
            client?.connect()
        } catch (e: Exception) {
            _isConnected = false
        }
    }

    fun subscribe(symbols: List<String>) {
        if (_isConnected) {
            val msg = gson.toJson(mapOf("action" to "subscribe", "symbols" to symbols))
            client?.send(msg)
        }
    }

    fun disconnect() {
        client?.close()
        _isConnected = false
    }
}
