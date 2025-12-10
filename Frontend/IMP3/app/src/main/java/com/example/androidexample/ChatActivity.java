package com.example.androidexample;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class ChatActivity extends AppCompatActivity implements WebSocketListener {
    private int userId;
    private RequestQueue requestQueue;
    private String serverUrl = "http://coms-3090-027.class.las.iastate.edu:8080/";

    private LinearLayout friendLayout;
    private JSONArray friendsList = new JSONArray();
    private ArrayList<Integer> friendIDList = new ArrayList<Integer>();
    private HashMap<Integer, Bitmap> profileCache = new HashMap<>();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity);

        userId = getIntent().getIntExtra("userId",-1);

        Button back = findViewById(R.id.chatActivity_back);
        back.setOnClickListener(v -> {
            Intent backIntent = new Intent();
            backIntent.putExtra("userId", userId);
            setResult(RESULT_OK, backIntent);
            finish();
        });

        friendLayout = findViewById(R.id.chatActivity_listLayout);
        requestQueue = QueueApplication.getQueue();

        getFriendsList();
    }

    private void getFriendsList() {
        String url = serverUrl + "users/" + userId + "/mutuals";
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    friendsList = response;
                    try {
                        updateChatList();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e("ChatActivity", "Error fetching friends list: " + error.toString())
        );

        requestQueue.add(jsonArrayRequest);
    }

    private void updateChatList() throws JSONException {
        friendLayout.removeAllViews();
        int margin = (int) (getResources().getDisplayMetrics().density * 8); // 8dp

        for (int i = 0; i < friendsList.length(); i++) {
            JSONObject friendObj = friendsList.getJSONObject(i);
            int friendId = friendObj.getInt("id");
            String friendNameStr = friendObj.getString("username");

            friendIDList.add(friendId);

            // --- Friend Box ---
            LinearLayout friendBox = new LinearLayout(this);
            LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            boxParams.setMargins(margin, margin, margin, margin);
            friendBox.setLayoutParams(boxParams);
            friendBox.setOrientation(LinearLayout.HORIZONTAL);
            friendBox.setPadding(margin * 2, margin * 2, margin * 2, margin * 2);
            friendBox.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
            friendBox.setBackgroundColor(getResources().getColor(android.R.color.white));
            friendBox.setElevation(6);

            // --- Profile Image ---
            ImageView profileImageView = new ImageView(this);
            int imageSize = (int) (48 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(imageSize, imageSize);
            imageParams.setMargins(0, 0, margin * 2, 0);
            profileImageView.setLayoutParams(imageParams);
            profileImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            profileImageView.setBackgroundResource(R.drawable.circle);
            profileImageView.setClipToOutline(true);

            profileImageView.setImageResource(R.drawable.imp3);

            friendBox.addView(profileImageView);

            loadFriendProfileImage(friendId, profileImageView);

            // --- Friend Name ---
            TextView friendName = new TextView(this);
            friendName.setText(friendNameStr);
            friendName.setTextSize(18);
            friendName.setTextColor(getResources().getColor(android.R.color.black));
            friendName.setTypeface(friendName.getTypeface(), android.graphics.Typeface.BOLD);
            friendName.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
            ));
            friendBox.addView(friendName);

            // --- Chat Button ---
            Button chatButton = new Button(this);
            chatButton.setText("Chat");
            chatButton.setAllCaps(false);
            chatButton.setBackgroundResource(android.R.drawable.btn_default);
            chatButton.setOnClickListener(v -> {
                Intent chatPage = new Intent(ChatActivity.this, ChatPage.class);
                chatPage.putExtra("userId", userId);
                chatPage.putExtra("friendId", friendId);
                chatPage.putExtra("friendName", friendNameStr);
                startActivity(chatPage);
            });
            friendBox.addView(chatButton);

            // --- Add to Layout ---
            friendLayout.addView(friendBox);
        }
    }

    private void loadFriendProfileImage(int friendId, ImageView profileImageView) {
        if (profileCache.containsKey(friendId)) {
            Bitmap bmp = profileCache.get(friendId);
            if (bmp != null) profileImageView.setImageBitmap(bmp);
            return;
        }

        String url = serverUrl + "users/" + friendId;
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    String base64 = response.optString("picture", "");
                    Bitmap bmp = null;
                    if (!base64.isEmpty()) {
                        byte[] decoded = Base64.decode(base64, Base64.NO_WRAP);
                        bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                    }
                    profileCache.put(friendId, bmp);
                    if (bmp != null) {
                        profileImageView.setImageBitmap(bmp);
                    }
                },
                error -> {
                    profileImageView.setImageResource(R.drawable.imp3);
                    profileCache.put(friendId, null);
                }
        );
        requestQueue.add(request);
    }

    @Override
    public void onWebSocketMessage(String message) {
        Log.i("ChatActivity Websocket Message", "Websocket Message received");
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
        Log.i("ChatActivity WebsocketClosed", "Websocket Closed");
        String closedBy = remote ? "server" : "local";
        runOnUiThread(() -> {
        });
    }
    @Override
    public void onWebSocketOpen(ServerHandshake handshakedata) {
        Log.i("ChatActivity WebsocketOpen", "Websocket Opened");
    }
    @Override
    public void onWebSocketError(Exception ex) {Log.e("ChatActivity Websocket Error: ", ex.toString());}
}
