/** @author Cayden Olsen **/

package com.example.androidexample;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.java_websocket.handshake.ServerHandshake;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Notifications page that displays a list of user notifications,
 * supports marking as read, deleting, and real-time updates via WebSocket.
 */
public class NotificationsPage extends AppCompatActivity {
    private static final String TAG = "NotificationsPage";
    private final String serverBase = "http://coms-3090-027.class.las.iastate.edu:8080";
    private final String websocketBase = "ws://coms-3090-027.class.las.iastate.edu:8080";
    private RequestQueue requestQueue;
    private RecyclerView rv;
    private NotificationsAdapter adapter;
    private List<NotificationModel> notifications = new ArrayList<>();
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notifications_page);

        Log.d(TAG, "onCreate called");

        Intent intent = getIntent();
        userId = intent.getIntExtra("userId", -1);
        Log.d(TAG, "Received userId: " + userId);

        requestQueue = QueueApplication.getQueue();
        Log.d(TAG, "RequestQueue initialized");

        // Use global QueueApplication request queue
        requestQueue = QueueApplication.getQueue();

        // Confirm cookies exist
        CookieManager cookieManager = (CookieManager) CookieHandler.getDefault();
        if (cookieManager != null && cookieManager.getCookieStore() != null) {
            List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
            if (cookies.isEmpty()) {
                Log.w(TAG, "No cookies found in CookieManager");
            } else {
                for (HttpCookie cookie : cookies) {
                    Log.d(TAG, "Cookie: " + cookie.toString());
                }
            }
        } else {
            Log.e(TAG, "CookieManager or CookieStore is null!");
        }

        rv = findViewById(R.id.rvNotifications);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationsAdapter(notifications);
        rv.setAdapter(adapter);
        Log.d(TAG, "RecyclerView + Adapter initialized");

        Button btnReturn = findViewById(R.id.btnReturn);
        btnReturn.setOnClickListener(v -> {
            Intent back = new Intent();
            back.putExtra("userId", userId);
            setResult(RESULT_OK, back);
            finish();
        });

        Button btnReadAll = findViewById(R.id.btnReadAll);
        btnReadAll.setOnClickListener(v -> readAllNotifications());

        fetchNotifications();
        startWebSocket();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy - disconnecting WebSocket");
        WebSocketManager.getInstance().disconnectWebSocket();
    }

    /** ---------------- REST CALLS ---------------- **/

    private void fetchNotifications() {
        String url = serverBase + "/notif";
        Log.d(TAG, "fetchNotifications called: " + url);

        JsonArrayRequest req = new JsonArrayRequest(
                com.android.volley.Request.Method.GET,
                url,
                null,
                response -> {
                    Log.d(TAG, "Fetched " + response.length() + " notifications from server");
                    notifications.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject o = response.getJSONObject(i);
                            notifications.add(NotificationModel.fromJson(o));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    adapter.notifyDataSetChanged();
                },
                error -> Log.e(TAG, "fetchNotifications error: " + error.toString())
        );
        requestQueue.add(req);
    }

    private void markAsRead(NotificationModel n, int position) {
        String url = serverBase + "/notif/" + n.notifId + "/read";
        Log.d(TAG, "markAsRead: " + url);

        JsonObjectRequest req = new JsonObjectRequest(
                com.android.volley.Request.Method.PUT,
                url,
                null,
                response -> {
                    n.readAt = System.currentTimeMillis();
                    adapter.notifyItemChanged(position);
                    Log.d(TAG, "Notification " + n.notifId + " marked as read");
                },
                error -> Log.e(TAG, "markAsRead error: " + error.toString())
        );

        requestQueue.add(req);
    }

    private void deleteNotification(NotificationModel n, int position) {
        String url = serverBase + "/notif/" + n.notifId;
        Log.d(TAG, "DELETE URL: " + url);

        StringRequest req = new StringRequest(
            com.android.volley.Request.Method.DELETE,
            url,
            response -> {
                // Success: remove notification locally
                notifications.remove(position);
                adapter.notifyItemRemoved(position);
                Log.d(TAG, "Deleted notification " + n.notifId + " successfully");
            },
            error -> {
                if (error.networkResponse != null) {
                    int statusCode = error.networkResponse.statusCode;
                    String respData = new String(error.networkResponse.data);
                    Log.e(TAG, "DELETE failed. Status: " + statusCode + ", Response: " + respData);
                } else {
                    Log.e(TAG, "DELETE error: " + error.toString());
                }
            }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();

                // Grab cookies from CookieManager
                CookieManager cookieManager = (CookieManager) CookieHandler.getDefault();
                if (cookieManager != null) {
                    List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
                    if (!cookies.isEmpty()) {
                        StringBuilder cookieHeader = new StringBuilder();
                        for (HttpCookie cookie : cookies) {
                            cookieHeader.append(cookie.getName()).append("=").append(cookie.getValue()).append("; ");
                        }
                        headers.put("Cookie", cookieHeader.toString());
                        Log.d(TAG, "Sending cookies: " + cookieHeader.toString());
                    }
                }
                return headers;
            }
        };
        requestQueue.add(req);
    }

    private void readAllNotifications() {
        String url = serverBase + "/notif/read";
        Log.d(TAG, "readAllNotifications called: " + url);

        JsonArrayRequest req = new JsonArrayRequest(
                com.android.volley.Request.Method.PUT,
                url,
                null,
                response -> {
                    Log.d(TAG, "readAllNotifications success");
                    for (NotificationModel n : notifications) {
                        n.readAt = System.currentTimeMillis();
                    }
                    adapter.notifyDataSetChanged();
                },
                error -> Log.e(TAG, "readAllNotifications error: " + error.toString())
        );
        requestQueue.add(req);
    }

    /** ---------------- WEBSOCKET ---------------- **/

    private void startWebSocket() {
        String wsUrl = websocketBase + "/ws/notif/" + userId;
        Log.d(TAG, "startWebSocket called with " + wsUrl);
        WebSocketManager wsManager = WebSocketManager.getInstance();

        wsManager.setWebSocketListener(new com.example.androidexample.WebSocketListener() {
            @Override
            public void onWebSocketOpen(ServerHandshake handshakedata) {
                Log.i(TAG, "WebSocket connected");
            }

            @Override
            public void onWebSocketMessage(String message) {
                Log.i(TAG, "WebSocket message received: " + message);
                runOnUiThread(() -> handleIncomingWebsocketMessage(message));
            }

            @Override
            public void onWebSocketClose(int code, String reason, boolean remote) {
                Log.i(TAG, "WebSocket closed: " + reason);
            }

            @Override
            public void onWebSocketError(Exception ex) {
                Log.e(TAG, "WebSocket error: ", ex);
            }
        });
        wsManager.connectWebSocket(wsUrl);
    }

    private void handleIncomingWebsocketMessage(String jsonText) {
        Log.d(TAG, "handleIncomingWebsocketMessage thread: " + Thread.currentThread().getName());

        try {
            JSONObject o = new JSONObject(jsonText);
            String event = o.optString("event", "");
            int notifId = o.optInt("notifId", -1);

            Log.d(TAG, "Received WebSocket event=" + event + ", notifId=" + notifId);

            if ("NOTIFICATION".equals(event) && notifId != -1) {
                fetchNotificationById(notifId);
            } else {
                Log.w(TAG, "Unknown event type or invalid notifId in WS message: " + jsonText);
            }

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing WebSocket message", e);
        }
    }

    private void fetchNotificationById(int notifId) {
        String url = serverBase + "/notif/" + notifId;
        Log.d(TAG, "Fetching notification by ID: " + url);

        JsonObjectRequest req = new JsonObjectRequest(
                com.android.volley.Request.Method.GET,
                url,
                null,
                response -> {
                    Log.d(TAG, "Fetched single notification: " + response.toString());
                    try {
                        NotificationModel n = NotificationModel.fromJson(response);

                        notifications.add(0, n);

                        runOnUiThread(() -> {
                            adapter.notifyItemInserted(0);
                            rv.scrollToPosition(0);
                            Log.d(TAG, "UI updated with new notification");
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing fetched notification JSON", e);
                    }
                },
                error -> {
                    Log.e(TAG, "Error fetching notification by ID: " + error.toString());
                    if (error.networkResponse != null) {
                        Log.e(TAG, "Status code: " + error.networkResponse.statusCode);
                    }
                }
        );

        requestQueue.add(req);
    }

    /** ---------------- MODEL ---------------- **/

    static class NotificationModel {
        int notifId;
        String message;
        String type;
        long createdAt;
        Long readAt;
        JSONObject actor;
static NotificationModel fromJson(JSONObject o) {
    NotificationModel m = new NotificationModel();
    m.notifId = o.optInt("notifId");
    m.message = o.optString("message");
    m.type = o.optString("type");
    m.actor = o.optJSONObject("actor");

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
    sdf.setTimeZone(TimeZone.getDefault());

    String created = o.optString("createdAt", null);
    if (created != null && !created.isEmpty()) {
        try {
            if (created.contains(".")) {
                created = created.substring(0, created.indexOf("."));
            }
            m.createdAt = sdf.parse(created).getTime();
        } catch (Exception e) {
            m.createdAt = System.currentTimeMillis();
        }
    } else {
        m.createdAt = System.currentTimeMillis();
    }

    String read = o.optString("readAt", null);
    if (read != null && !read.equals("null") && !read.isEmpty()) {
        try {
            if (read.contains(".")) {
                read = read.substring(0, read.indexOf("."));
            }
            m.readAt = sdf.parse(read).getTime();
        } catch (Exception e) {
            m.readAt = null;
        }
    } else {
        m.readAt = null;
    }
    return m;
}
    }

    /** ---------------- ADAPTER ---------------- **/

    private class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.VH> {
        private final List<NotificationModel> items;

        NotificationsAdapter(List<NotificationModel> items) { this.items = items; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            NotificationModel m = items.get(pos);
            Log.d(TAG, "Binding item at pos " + pos + ": " + m.message);
            h.tvTitle.setText(m.type);
            h.tvMessage.setText(m.message);

            CharSequence relTime = DateUtils.getRelativeTimeSpanString(
                    m.createdAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
            h.tvTime.setText(relTime);

            if (m.readAt == null) {
                h.ivStatus.setImageResource(R.drawable.unread_dot);
                h.itemView.setAlpha(1f);
            } else {
                h.ivStatus.setImageResource(R.drawable.read_dot);
                h.itemView.setAlpha(0.85f);
            }

            h.itemView.setOnClickListener(v -> {
                Log.d(TAG, "Clicked notification " + m.notifId);
                if (m.readAt == null) markAsRead(m, pos);
                Toast.makeText(NotificationsPage.this, m.message, Toast.LENGTH_SHORT).show();
            });

            h.btnDelete.setOnClickListener(v -> {
                Log.d(TAG, "Delete clicked for " + m.notifId);
                deleteNotification(m, pos);
            });
        }

        @Override
        public int getItemCount() {
            Log.d(TAG, "getItemCount(): " + items.size());
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            ImageView ivStatus;
            TextView tvTitle, tvMessage, tvTime;
            ImageButton btnDelete;
            VH(@NonNull View itemView) {
                super(itemView);
                ivStatus = itemView.findViewById(R.id.ivStatus);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvMessage = itemView.findViewById(R.id.tvMessage);
                tvTime = itemView.findViewById(R.id.tvTime);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }
}






