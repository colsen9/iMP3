package com.example.androidexample;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FollowerView extends AppCompatActivity {

    private int userId;
    private RequestQueue requestQueue;
    private String serverUrl = "http://coms-3090-027.class.las.iastate.edu:8080";
    private Set<Integer> displayedUsers = new HashSet<>();
    // Section layouts
    private LinearLayout allUsersLayout, followersLayout, followingLayout, mutualsLayout;

    // Cache for profile images
    private Map<Integer, Bitmap> profileCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.follower_view);

        userId = getIntent().getIntExtra("userId", -1);

        Button backBtn = findViewById(R.id.follower_profile_btn);
        backBtn.setOnClickListener(v -> {
            Intent back = new Intent();
            back.putExtra("userId", userId);
            setResult(RESULT_OK, back);
            finish();
        });

        allUsersLayout = findViewById(R.id.section_all_users);
        followersLayout = findViewById(R.id.section_followers);
        followingLayout = findViewById(R.id.section_following);
        mutualsLayout = findViewById(R.id.section_mutuals);

        requestQueue = QueueApplication.getQueue();

        loadAllSections();
    }

    private void loadAllSections() {
        displayedUsers.clear();
        loadMutuals();
    }

    private void loadMutuals() {
        loadSectionRequest(
                "/users/" + userId + "/mutuals",
                mutualsLayout,
                SectionType.MUTUALS,
                this::loadFollowing
        );
    }

    private void loadFollowing() {
        loadSectionRequest(
                "/users/" + userId + "/following",
                followingLayout,
                SectionType.FOLLOWING,
                this::loadFollowers
        );
    }

    private void loadFollowers() {
        loadSectionRequest(
                "/users/" + userId + "/followers",
                followersLayout,
                SectionType.FOLLOWERS,
                this::loadAllUsers
        );
    }

    private void loadAllUsers() {
        loadSectionRequest(
                "/users/all",
                allUsersLayout,
                SectionType.ALL_USERS,
                null
        );
    }

    private void loadSectionRequest(
            String endpoint,
            LinearLayout container,
            SectionType type,
            Runnable next
    ) {
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET,
                serverUrl + endpoint,
                null,
                response -> {
                    populateSection(container, response, type, displayedUsers);
                    if (next != null) next.run();
                },
                error -> {
                    Log.e("FollowerView", "Failed to load " + type + ": " + error);
                    if (next != null) next.run();
                }
        );

        requestQueue.add(req);
    }

    private enum SectionType { ALL_USERS, FOLLOWERS, FOLLOWING, MUTUALS }

    /*
    private void loadSection(String endpoint, LinearLayout container, SectionType type, Set<Integer> displayedUsers) {
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET,
                serverUrl + endpoint,
                null,
                response -> populateSection(container, response, type, displayedUsers),
                error -> Log.e("FollowerView", "Failed to load " + type + ": " + error)
        );
        requestQueue.add(req);
    }
    */

    private void populateSection(LinearLayout container, JSONArray users, SectionType type, Set<Integer> displayedUsers) {
        container.removeAllViews();
        int margin = (int) (8 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < users.length(); i++) {
            try {
                JSONObject user = users.getJSONObject(i);
                Log.i("friend user JSON", user.toString());
                int friendId = user.getInt("id");

                // Skip users already displayed or self
                if (displayedUsers.contains(friendId) || friendId == userId) continue;
                displayedUsers.add(friendId);

                String username = user.getString("username");

                // Row container
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(margin, margin, margin, margin);
                row.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));

                // Profile image
                ImageButton profileImage = new ImageButton(this);
                int size = (int) (48 * getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(size, size);
                imageParams.setMargins(0, 0, margin * 2, 0);
                profileImage.setLayoutParams(imageParams);

                // Circular background using circle.xml
                profileImage.setBackgroundResource(R.drawable.circle);
                profileImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                profileImage.setClipToOutline(true); // makes it follow the circle shape
                profileImage.setPadding(0, 0, 0, 0);

                // Default fallback image
                profileImage.setImageResource(R.drawable.imp3);

                // Click opens new activity and sends friendId
                profileImage.setOnClickListener(v -> {
                    Intent intent = new Intent(FollowerView.this, OtherUserProfile.class);
                    intent.putExtra("userId", userId);
                    intent.putExtra("otherUserId", friendId);
                    startActivity(intent);
                });

                row.addView(profileImage);

                // Load actual profile picture
                loadFriendProfileImage(friendId, profileImage);

                // Username
                TextView tvName = new TextView(this);
                tvName.setText(username);
                tvName.setTextSize(16);
                tvName.setLayoutParams(new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                ));
                row.addView(tvName);

                // Action button
                Button actionBtn = new Button(this);
                actionBtn.setAllCaps(false);
                actionBtn.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));

                switch (type) {
                    case ALL_USERS:
                    case FOLLOWERS:
                        actionBtn.setText("Follow");
                        actionBtn.setOnClickListener(v -> followUser(friendId));
                        break;
                    case FOLLOWING:
                    case MUTUALS:
                        actionBtn.setText("Unfollow");
                        actionBtn.setOnClickListener(v -> unfollowUser(friendId));
                        break;
                }
                row.addView(actionBtn);

                container.addView(row);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadFriendProfileImage(int friendId, ImageView profileImageView) {
        if (profileCache.containsKey(friendId)) {
            Bitmap bmp = profileCache.get(friendId);
            if (bmp != null) profileImageView.setImageBitmap(bmp);
            return;
        }

        String url = serverUrl + "/users/" + friendId;
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

                        // Make bitmap fill ImageView properly
                        profileImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        profileImageView.setImageBitmap(bmp);
                    }
                    profileCache.put(friendId, bmp);
                },
                error -> {
                    profileImageView.setImageResource(R.drawable.ic_settings);
                    profileCache.put(friendId, null);
                }
        );
        requestQueue.add(request);
    }

    private void followUser(int friendId) {
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                serverUrl + "/users/" + friendId + "/follow",
                null,
                response -> {
                    Log.i("FollowerView", "Followed user " + friendId);
                    sendFollowNotification(friendId, userId);
                    loadAllSections();
                },
                error -> Log.e("FollowerView", "Failed to follow user " + friendId)
        );
        requestQueue.add(req);
    }

    private void unfollowUser(int friendId) {
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.DELETE,
                serverUrl + "/users/" + friendId + "/follow",
                null,
                response -> {
                    Log.i("FollowerView", "Unfollowed user " + friendId);
                    loadAllSections();
                },
                error -> Log.e("FollowerView", "Failed to unfollow user " + friendId + ": " + error)
        );
        requestQueue.add(req);
    }

    private void sendFollowNotification(int recipientId, int followerId) {
        String url = "http://coms-3090-027.class.las.iastate.edu:8080/notif";

        JSONObject notifBody = new JSONObject();
        try {
            notifBody.put("recipientId", recipientId);
            notifBody.put("message", "User " + followerId + " followed you!");
            notifBody.put("type", "FOLLOW");
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        JsonObjectRequest notifRequest = new JsonObjectRequest(
                Request.Method.POST,
                url,
                notifBody,
                response -> {
                    Log.i("FollowerView", "Follow notification sent to user " + recipientId);
                },
                error -> Log.e("FollowerView", "Failed to send follow notification: " + error)
        ) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        if (requestQueue == null) {
            requestQueue = QueueApplication.getQueue();
        }
        requestQueue.add(notifRequest);
    }
}
