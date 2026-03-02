package com.example.webrtcdemo

import android.util.Log
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.net.URI

class SignalingClient(
    private val serverUrl: String,
    private val callback: Callback
) {
    private var webSocket: WebSocketClient? = null

    interface Callback {
        fun onConnected()
        fun onOfferReceived(sdp: String)
        fun onAnswerReceived(sdp: String)
        fun onIceCandidateReceived(sdpMid: String, sdpMLineIndex: Int, candidate: String)
        fun onDisconnected()
        fun onError(message: String)
    }

    fun connect() {
        try {
            val uri = URI(serverUrl)
            webSocket = object : WebSocketClient(uri) {
                override fun onOpen(handshakedata: ServerHandshake?) {
                    Log.d("SignalingClient", "Connected to signaling server")
                    callback.onConnected()
                }

                override fun onMessage(message: String?) {
                    Log.d("SignalingClient", "Message received: $message")
                    message?.let { handleMessage(it) }
                }

                override fun onClose(code: Int, reason: String?, remote: Boolean) {
                    Log.d("SignalingClient", "Disconnected: $reason")
                    callback.onDisconnected()
                }

                override fun onError(ex: Exception?) {
                    Log.e("SignalingClient", "Error: ${ex?.message}")
                    callback.onError(ex?.message ?: "Unknown error")
                }
            }
            webSocket?.connect()
        } catch (e: Exception) {
            callback.onError(e.message ?: "Invalid URL")
        }
    }

    private fun handleMessage(message: String) {
        try {
            val json = JSONObject(message)
            val type = json.optString("type")
            when (type) {
                "offer" -> {
                    val sdp = json.optString("sdp")
                    callback.onOfferReceived(sdp)
                }
                "answer" -> {
                    val sdp = json.optString("sdp")
                    callback.onAnswerReceived(sdp)
                }
                "candidate" -> {
                    val candidate = json.optString("candidate")
                    val sdpMid = json.optString("sdpMid")
                    val sdpMLineIndex = json.optInt("sdpMLineIndex")
                    callback.onIceCandidateReceived(sdpMid, sdpMLineIndex, candidate)
                }
            }
        } catch (e: Exception) {
            Log.e("SignalingClient", "Failed to parse message: $message", e)
        }
    }

    fun sendOffer(sdp: String) {
        val json = JSONObject().apply {
            put("type", "offer")
            put("sdp", sdp)
        }
        sendMessage(json.toString())
    }

    fun sendAnswer(sdp: String) {
        val json = JSONObject().apply {
            put("type", "answer")
            put("sdp", sdp)
        }
        sendMessage(json.toString())
    }

    fun sendIceCandidate(sdpMid: String, sdpMLineIndex: Int, candidate: String) {
        val json = JSONObject().apply {
            put("type", "candidate")
            put("sdpMid", sdpMid)
            put("sdpMLineIndex", sdpMLineIndex)
            put("candidate", candidate)
        }
        sendMessage(json.toString())
    }

    private fun sendMessage(message: String) {
        if (webSocket?.isOpen == true) {
            webSocket?.send(message)
        } else {
            Log.w("SignalingClient", "WebSocket is not open. Cannot send message.")
        }
    }

    fun close() {
        webSocket?.close()
        webSocket = null
    }
}
