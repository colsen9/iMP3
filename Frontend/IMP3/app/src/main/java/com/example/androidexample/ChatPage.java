package com.example.androidexample;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatPage extends AppCompatActivity implements WebSocketListener {
    private final String serverBase = "http://coms-3090-027.class.las.iastate.edu:8080";
    private final String websocketBase = "ws://coms-3090-027.class.las.iastate.edu:8080";
    private RequestQueue requestQueue;
    private RecyclerView rv;
    private ChatAdapter adapter;
    private List<MessageModel> messages = new ArrayList<>();
    private final String PAGE_TAG = "ChatPage";
    private EditText etMessage;
    private Button btnSend, btnBack;
    private int userId;
    private int friendId;
    private String friendName = "Username Not Found";
    private Map<Integer, Bitmap> userProfileMap = new HashMap<>();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_page);
        Intent intent = getIntent();

        userId = intent.getIntExtra("userId", -1);
        friendId = intent.getIntExtra("friendId", -1);
        friendName = intent.getStringExtra("friendName");

        TextView chatName = findViewById(R.id.chatPage_label);
        chatName.setText("Chat with " + friendName);
        requestQueue = QueueApplication.getQueue();

        rv = findViewById(R.id.rvChat);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatAdapter(messages, userId);
        rv.setAdapter(adapter);

        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.chatPage_back);

        loadUserProfileImage(userId, null);

        btnBack.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent back = new Intent();
                back.putExtra("userId", userId);
                setResult(RESULT_OK, back);
                finish();
            }});

        btnSend.setOnClickListener(v -> sendMessage());

        // fetchChatHistory();
        startWebSocket();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WebSocketManager.getInstance().disconnectWebSocket();
    }

    /** ---------------- REST: Fetch previous messages ---------------- **/
    /* private void fetchChatHistory() {
        String url = serverBase + "/chat/" + friendId + "/messages";

        JsonArrayRequest req = new JsonArrayRequest(
                com.android.volley.Request.Method.GET,
                url,
                null,
                response -> {
                    messages.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject obj = response.getJSONObject(i);
                            messages.add(MessageModel.fromJson(obj));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    // Sort messages chronologically (oldest first)
                    messages.sort(Comparator.comparingLong(m -> m.timestamp));
                    adapter.notifyDataSetChanged();
                    rv.scrollToPosition(messages.size() - 1);
                },
                error -> Log.e(PAGE_TAG, "fetchChatHistory error: " + error.toString())
        );
        requestQueue.add(req);
    } */

    /** ---------------- WEBSOCKET ---------------- **/
    private void startWebSocket() {
        Log.i(PAGE_TAG,"Started websocket");
        String wsUrl = websocketBase + "/chat/" + userId;

        WebSocketManager wsManager = WebSocketManager.getInstance();

        wsManager.setWebSocketListener(new WebSocketListener() {
            @Override
            public void onWebSocketOpen(ServerHandshake handshakedata) {
                Log.i(PAGE_TAG, "WebSocket connected to chat group " + friendId);
            }

            @Override
            public void onWebSocketMessage(String message) {
                Log.i(PAGE_TAG,message);
                runOnUiThread(() -> handleIncomingWebsocketMessage(message));
            }

            @Override
            public void onWebSocketClose(int code, String reason, boolean remote) {
                Log.i(PAGE_TAG, "WebSocket closed: " + reason);
            }

            @Override
            public void onWebSocketError(Exception ex) {
                Log.e(PAGE_TAG, "WebSocket error: ", ex);
            }
        });

        wsManager.connectWebSocket(wsUrl);
    }

    private void loadUserProfileImage(int senderId, Runnable callback) {
        if (userProfileMap.containsKey(senderId)) {
            if (callback != null) callback.run();
            return;
        }

        String url = serverBase + "/users/" + senderId;
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    String base64 = response.optString("picture", "");
                    Bitmap bmp = null;
                    if (!base64.isEmpty()) {
                        byte[] decoded = Base64.decode(base64, Base64.NO_WRAP);
                        bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                    }
                    userProfileMap.put(senderId, bmp);
                    if (callback != null) callback.run();
                },
                error -> {
                    userProfileMap.put(senderId, null);
                    if (callback != null) callback.run();
                }
        );
        requestQueue.add(request);
    }

    private void handleIncomingWebsocketMessage(String wsText) {
        try {
            // Assume message format: "senderId:messageContent"
            int colonIndex = wsText.indexOf(":");
            int senderId = -1;
            String message = wsText;

            if (colonIndex != -1) {
                try {
                    senderId = Integer.parseInt(wsText.substring(0, colonIndex).trim());
                    message = wsText.substring(colonIndex + 1).trim();
                } catch (NumberFormatException ignored) {}
            }

            if (senderId == userId) {
                return;
            }

            final int finalSenderId = senderId;
            final String finalMessage = message;

            // Load profile image before adding to chat
            loadUserProfileImage(finalSenderId, () -> {
                Bitmap profile = userProfileMap.get(finalSenderId);
                String nameToShow = friendName;
                MessageModel msg = MessageModel.fromString(finalMessage, finalSenderId, nameToShow, profile);

                messages.add(msg);
                messages.sort(Comparator.comparingLong(m -> m.timestamp));
                adapter.notifyDataSetChanged();
                rv.scrollToPosition(messages.size() - 1);
            });

        } catch (Exception e) {
            Log.e(PAGE_TAG, "Error parsing message: " + wsText, e);
        }
    }


    /** ---------------- SEND MESSAGE ---------------- **/
    private void sendMessage() {
        String content = etMessage.getText().toString().trim();
        if (content.isEmpty()) return;

        Bitmap myProfile = userProfileMap.get(userId);

        MessageModel msg = MessageModel.fromString(content, userId, "Me", myProfile); // "Me" for your own display
        messages.add(msg);
        messages.sort(Comparator.comparingLong(m -> m.timestamp));
        adapter.notifyDataSetChanged();
        rv.scrollToPosition(messages.size() - 1);

        String msgToSend = friendId + ", " + content;
        WebSocketManager.getInstance().sendMessage(msgToSend);

        etMessage.setText("");
    }

    /** ---------------- MODEL ---------------- **/
    static class MessageModel {
        int senderId;
        String senderName;
        String content;
        long timestamp;
        Bitmap senderProfile;

        static MessageModel fromString(String raw, int senderId, String senderName, Bitmap profile) {
            MessageModel m = new MessageModel();
            m.senderId = senderId;
            m.senderName = senderName;
            m.senderProfile = profile;
            m.content = raw;
            m.timestamp = System.currentTimeMillis();
            return m;
        }
    }

    /** ---------------- ADAPTER ---------------- **/
    private static class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<MessageModel> items;
        private final int currentUserId;
        private static final int VIEW_TYPE_SENT = 1;
        private static final int VIEW_TYPE_RECEIVED = 2;

        ChatAdapter(List<MessageModel> items, int userId) {
            this.items = items;
            this.currentUserId = userId;
        }

        @Override
        public int getItemViewType(int position) {
            MessageModel msg = items.get(position);
            return msg.senderId == currentUserId ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_SENT) {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_sent, parent, false);
                return new SentMessageVH(v);
            } else {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_received, parent, false);
                return new ReceivedMessageVH(v);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            MessageModel msg = items.get(position);
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    msg.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);

            Bitmap profileBitmap = msg.senderProfile;

            if (holder instanceof SentMessageVH) {
                ((SentMessageVH) holder).tvContent.setText(msg.content);
                ((SentMessageVH) holder).tvTime.setText(timeAgo);
                if (profileBitmap != null) ((SentMessageVH) holder).ivProfile.setImageBitmap(profileBitmap);
            } else if (holder instanceof ReceivedMessageVH) {
                ((ReceivedMessageVH) holder).tvSender.setText(msg.senderName);
                ((ReceivedMessageVH) holder).tvContent.setText(msg.content);
                ((ReceivedMessageVH) holder).tvTime.setText(timeAgo);
                if (profileBitmap != null) ((ReceivedMessageVH) holder).ivProfile.setImageBitmap(profileBitmap);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class SentMessageVH extends RecyclerView.ViewHolder {
            TextView tvContent, tvTime;
            ImageView ivProfile;

            SentMessageVH(@NonNull View itemView) {
                super(itemView);
                tvContent = itemView.findViewById(R.id.tvMessageContent);
                tvTime = itemView.findViewById(R.id.tvMessageTime);
                ivProfile = itemView.findViewById(R.id.ivSenderProfile);
                ivProfile.setBackgroundResource(R.drawable.circle);
                ivProfile.setClipToOutline(true);
                ivProfile.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
        }

        static class ReceivedMessageVH extends RecyclerView.ViewHolder {
            TextView tvSender, tvContent, tvTime;
            ImageView ivProfile;

            ReceivedMessageVH(@NonNull View itemView) {
                super(itemView);
                tvSender = itemView.findViewById(R.id.tvSenderName);
                tvContent = itemView.findViewById(R.id.tvMessageContent);
                tvTime = itemView.findViewById(R.id.tvMessageTime);
                ivProfile = itemView.findViewById(R.id.ivSenderProfile);
                ivProfile.setBackgroundResource(R.drawable.circle);
                ivProfile.setClipToOutline(true);
                ivProfile.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
        }
    }

    private void updateChatList(){}
    @Override
    public void onWebSocketMessage(String message) {
        Log.i("ChatApp Websocket Message", "Websocket Message received");
        /**
         * In Android, all UI-related operations must be performed on the main UI thread
         * to ensure smooth and responsive user interfaces. The 'runOnUiThread' method
         * is used to post a runnable to the UI thread's message queue, allowing UI updates
         * to occur safely from a background or non-UI thread.
         */
        runOnUiThread(() -> {

        });
    }
    @Override
    public void onWebSocketClose(int code, String reason, boolean remote) {
        Log.i("ChatPage WebsocketClose", "Websocket Closed");
        String closedBy = remote ? "server" : "local";
        runOnUiThread(() -> {
        });
    }
    @Override
    public void onWebSocketOpen(ServerHandshake handshakedata) {
        Log.i("ChatPage WebsocketOpen", "Websocket Opened");
    }
    @Override
    public void onWebSocketError(Exception ex) {Log.e("ChatPage Websocket Error: ", ex.toString());}


}
