package com.macswipe.remote

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors

/** Single serial socket connection; all I/O happens away from Android's main thread. */
class RemoteSocket(private val onStatus: (Boolean, String) -> Unit) {
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var connected = false
    private var socket: Socket? = null
    private var writer: BufferedWriter? = null

    fun connect(host: String, token: String) {
        executor.execute {
            closeInternal()
            try {
                val s = Socket()
                socket = s
                s.connect(InetSocketAddress(host.trim(), 8765), 3500)
                s.tcpNoDelay = true
                s.soTimeout = 3500
                val output = BufferedWriter(OutputStreamWriter(s.getOutputStream(), Charsets.UTF_8))
                val input = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
                output.write(JSONObject().put("type", "hello").put("token", token.trim()).toString())
                output.newLine()
                output.flush()
                val response = input.readLine() ?: throw Exception("Mac disconnected")
                val packet = JSONObject(response)
                if (packet.optString("type") != "ready") {
                    throw Exception(packet.optString("message", "Authentication failed"))
                }
                s.soTimeout = 0
                socket = s
                writer = output
                connected = true
                notifyStatus(true, "Connected to Mac · $host")
            } catch (e: Exception) {
                closeInternal()
                notifyStatus(false, "Connection failed: ${e.localizedMessage ?: "Check IP and token"}")
            }
        }
    }

    fun send(type: String, dx: Double = 0.0, dy: Double = 0.0) {
        if (!connected) return
        val packet = JSONObject().put("type", type)
        if (type == "move" || type == "scroll") {
            packet.put("dx", dx).put("dy", dy)
        }
        val json = packet.toString()
        executor.execute {
            if (!connected) return@execute
            try {
                writer?.write(json)
                writer?.newLine()
                writer?.flush()
            } catch (e: Exception) {
                closeInternal()
                notifyStatus(false, "Mac connection lost")
            }
        }
    }

    fun disconnect() {
        connected = false
        executor.execute {
            closeInternal()
            notifyStatus(false, "Disconnected")
        }
    }

    fun shutdown() {
        connected = false
        try { socket?.close() } catch (_: Exception) {}
        executor.shutdownNow()
    }

    private fun closeInternal() {
        connected = false
        try { writer?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        writer = null
        socket = null
    }

    private fun notifyStatus(ready: Boolean, message: String) {
        main.post { onStatus(ready, message) }
    }
}
