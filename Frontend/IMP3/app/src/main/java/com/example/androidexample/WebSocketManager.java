package com.example.androidexample;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton WebSocketManager instance used for managing WebSocket connections
 * in the Android application.
 *
 * This instance ensures that there is only one WebSocketManager throughout
 * the application's lifecycle, allowing for centralized WebSocket handling.
 */
public class WebSocketManager {

    private static WebSocketManager instance;
    private MyWebSocketClient webSocketClient;
    private WebSocketListener webSocketListener;

    private WebSocketManager() {}

    /**
     * Retrieves a synchronized instance of the WebSocketManager, ensuring that
     * only one instance of the WebSocketManager exists throughout the application.
     * Synchronization ensures thread safety when accessing or creating the instance.
     *
     * @return A synchronized instance of WebSocketManager.
     */
    public static synchronized WebSocketManager getInstance() {
        if (instance == null) {
            Log.d("WebSocket", "Creating new WebSocketManager instance");
            instance = new WebSocketManager();
        } else {
            Log.d("WebSocket", "Reusing existing WebSocketManager instance: " + instance);
        }
        return instance;
    }

    /**
     * Sets the WebSocketListener for this WebSocketManager instance. The WebSocketListener
     * is responsible for handling WebSocket events, such as received messages and errors.
     *
     * @param listener The WebSocketListener to be set for this WebSocketManager.
     */
    public void setWebSocketListener(WebSocketListener listener) {
        this.webSocketListener = listener;
        Log.d("WebSocket", "Listener set: " + listener);
    }

    /**
     * Removes the currently set WebSocketListener from this WebSocketManager instance.
     * This action effectively disconnects the listener from handling WebSocket events.
     */
    public void removeWebSocketListener() {
        Log.d("WebSocket", "Listener removed");
        this.webSocketListener = null;
    }

    /**
     * Initiates a WebSocket connection to the specified server URL. This method
     * establishes a connection with the WebSocket server located at the given URL.
     *
     * @param serverUrl The URL of the WebSocket server to connect to.
     */
    public void connectWebSocket(String serverUrl) {
        try {
            Log.d("WebSocket", "connectWebSocket() called with URL: " + serverUrl);
            Log.d("WebSocket", "Current listener: " + webSocketListener);
            Log.d("WebSocket", "Existing client: " + webSocketClient);

            URI serverUri = URI.create(serverUrl);
            Map<String, String> headers = new HashMap<>();

            CookieManager cookieManager = (CookieManager) CookieHandler.getDefault();
            if (cookieManager != null) {
                List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
                if (!cookies.isEmpty()) {
                    StringBuilder cookieHeader = new StringBuilder();
                    for (HttpCookie cookie : cookies) {
                        cookieHeader.append(cookie.getName()).append("=").append(cookie.getValue()).append("; ");
                    }
                    headers.put("Cookie", cookieHeader.toString());
                    Log.d("WebSocket", "Sending cookies in handshake: " + cookieHeader);
                } else {
                    Log.w("WebSocket", "No cookies found in CookieManager");
                }
            }

            webSocketClient = new MyWebSocketClient(serverUri, headers);
            Log.d("WebSocket", "Connecting WebSocket client: " + webSocketClient);
            webSocketClient.connect();
        } catch (Exception e) {
            Log.e("WebSocket", "Error connecting WebSocket", e);
        }
    }

    /**
     * Sends a WebSocket message to the connected WebSocket server. This method allows
     * the application to send a message to the server through the established WebSocket
     * connection.
     *
     * @param message The message to be sent to the WebSocket server.
     */
    public void sendMessage(String message) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            Log.d("WebSocket", "Sending message: " + message);
            webSocketClient.send(message);
        } else {
            Log.w("WebSocket", "Cannot send message. Client not open.");
        }
    }

    /**
     * Disconnects the WebSocket connection, terminating the communication with the
     * WebSocket server.
     */
    public void disconnectWebSocket() {
        if (webSocketClient != null) {
            Log.d("WebSocket", "Disconnecting WebSocket");
            webSocketClient.close();
        }
    }

    public boolean isConnected() {
        return webSocketClient != null && webSocketClient.isOpen();
    }

    /**
     * A private inner class that extends WebSocketClient and represents a WebSocket
     * client instance tailored for specific functionalities within the WebSocketManager.
     * This class encapsulates the WebSocketClient and provides custom behavior or
     * handling for WebSocket communication as needed by the application.
     */
    private class MyWebSocketClient extends WebSocketClient {

        private MyWebSocketClient(URI serverUri, Map<String, String> headers) {
            super(serverUri, headers);
            Log.d("WebSocket", "MyWebSocketClient created with URI: " + serverUri);
        }

        /**
         * Called when the WebSocket connection is successfully opened and a handshake
         * with the server has been completed. This method is invoked to handle the event
         * when the WebSocket connection becomes ready for sending and receiving messages.
         *
         * @param handshakedata The ServerHandshake object containing details about the
         *                      handshake with the server.
         */
        @Override
        public void onOpen(ServerHandshake handshakedata) {
            Log.d("WebSocket", "Connected");
            if (webSocketListener != null) {
                webSocketListener.onWebSocketOpen(handshakedata);
            } else {
                Log.w("WebSocket", "Listener null in onOpen()");
            }
        }

        /**
         * Called when a WebSocket message is received from the server. This method is
         * invoked to handle incoming WebSocket messages and allows the application to
         * process and respond to messages as needed.
         *
         * @param message The WebSocket message received from the server as a string.
         */
        @Override
        public void onMessage(String message) {
            Log.d("WebSocket", "Received message: " + message + " | listener=" + webSocketListener);
            if (webSocketListener != null) {
                webSocketListener.onWebSocketMessage(message);
            } else {
                Log.w("WebSocket", "Listener is null — message dropped!");
            }
        }

        /**
         * Called when the WebSocket connection is closed, either due to a client request
         * or a server-initiated close. This method is invoked to handle the WebSocket
         * connection closure event and provides details about the closure, such as the
         * closing code, reason, and whether it was initiated remotely.
         *
         * @param code   The WebSocket closing code indicating the reason for closure.
         * @param reason A human-readable explanation for the WebSocket connection closure.
         * @param remote A boolean flag indicating whether the closure was initiated remotely.
         *               'true' if initiated remotely, 'false' if initiated by the client.
         */
        @Override
        public void onClose(int code, String reason, boolean remote) {
            Log.d("WebSocket", "Closed (code=" + code + ", reason=" + reason + ", remote=" + remote + ")");
            if (webSocketListener != null) {
                webSocketListener.onWebSocketClose(code, reason, remote);
            } else {
                Log.w("WebSocket", "Listener null in onClose()");
            }
        }

        /**
         * Called when an error occurs during WebSocket communication. This method is
         * invoked to handle WebSocket-related errors and allows the application to
         * respond to and log error details.
         *
         * @param ex The Exception representing the WebSocket communication error.
         */
        @Override
        public void onError(Exception ex) {
            Log.e("WebSocket", "Error", ex);
            if (webSocketListener != null) {
                webSocketListener.onWebSocketError(ex);
            } else {
                Log.w("WebSocket", "Listener null in onError()");
            }
        }
    }
}
