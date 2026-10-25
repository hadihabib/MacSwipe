package com.macswipe.remote;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** A token-authenticated, LAN-only command socket; never runs network I/O on the UI thread. */
final class RemoteSocket {
    interface StatusListener { void onStatus(boolean ready, String message); }

    private final StatusListener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private volatile boolean connected;
    private Socket socket;
    private BufferedWriter writer;

    RemoteSocket(StatusListener listener) { this.listener = listener; }

    void connect(String host, String token) {
        worker.execute(() -> {
            closeInternal();
            try {
                Socket s = new Socket();
                socket = s;
                s.connect(new InetSocketAddress(host.trim(), 8765), 3500);
                s.setTcpNoDelay(true);
                s.setSoTimeout(3500);
                BufferedWriter output = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8));
                BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                JSONObject hello = new JSONObject();
                hello.put("type", "hello");
                hello.put("token", token.trim());
                output.write(hello.toString());
                output.newLine();
                output.flush();
                String response = input.readLine();
                if (response == null) throw new IllegalStateException("Mac disconnected");
                JSONObject packet = new JSONObject(response);
                if (!"ready".equals(packet.optString("type"))) {
                    throw new IllegalStateException(packet.optString("message", "Authentication failed"));
                }
                s.setSoTimeout(0);
                writer = output;
                connected = true;
                notifyStatus(true, "Connected to Mac · " + host);
            } catch (Exception ex) {
                closeInternal();
                notifyStatus(false, "Connection failed: " + (ex.getLocalizedMessage() == null ? "Check IP and token" : ex.getLocalizedMessage()));
            }
        });
    }

    void send(String type) { send(type, 0, 0); }
    void send(String type, double dx, double dy) {
        if (!connected) return;
        worker.execute(() -> {
            if (!connected || writer == null) return;
            try {
                JSONObject packet = new JSONObject();
                packet.put("type", type);
                if ("move".equals(type) || "scroll".equals(type)) {
                    packet.put("dx", dx);
                    packet.put("dy", dy);
                }
                writer.write(packet.toString());
                writer.newLine();
                writer.flush();
            } catch (Exception ex) {
                closeInternal();
                notifyStatus(false, "Mac connection lost");
            }
        });
    }

    void disconnect() {
        connected = false;
        worker.execute(() -> { closeInternal(); notifyStatus(false, "Disconnected"); });
    }

    void shutdown() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (Exception ignored) { }
        worker.shutdownNow();
    }

    private void closeInternal() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (Exception ignored) { }
        try { if (writer != null) writer.close(); } catch (Exception ignored) { }
        writer = null;
        socket = null;
    }

    private void notifyStatus(boolean ready, String text) {
        main.post(() -> listener.onStatus(ready, text));
    }
}
