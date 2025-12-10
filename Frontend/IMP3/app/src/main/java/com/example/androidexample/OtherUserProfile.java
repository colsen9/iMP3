/** @Author Cayden Olsen **/

package com.example.androidexample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.flexbox.FlexboxLayout;
import org.json.JSONArray;

public class OtherUserProfile extends AppCompatActivity {
    private Button music, lists;
    private Button friends, back;
    private ImageView profilePicture;
    private TextView bioView, fullname, spotifyStatusText;
    private int userId = -1;
    private int otherUserId = -1;
    private RequestQueue requestQueue;
    private static final String BASE_URL = "http://coms-3090-027.class.las.iastate.edu:8080";
    private TagManager tagService;
    private UserTagUIManager userTagUIManager;
    private FlexboxLayout userTagContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.other_user_page);

        /* initialize UI elements */
        profilePicture = findViewById(R.id.profile_picture);
        friends = findViewById(R.id.friends_btn);
        bioView = findViewById(R.id.user_bio);
        music = findViewById(R.id.music_btn);
        lists = findViewById(R.id.lists_btn);
        fullname = findViewById(R.id.user_fullname);
        spotifyStatusText = findViewById(R.id.spotify_status_text);
        back = findViewById(R.id.backButton);
        userTagContainer = findViewById(R.id.userTagContainer);
        userTagUIManager = new UserTagUIManager(this, userTagContainer);
        tagService = new TagManager(this);


        /* initialize Volley */
        requestQueue = QueueApplication.getQueue();

        /* retrieve userId from previous activity */
        Intent intent = getIntent();
        userId = intent.getIntExtra("userId", -1);
        otherUserId = intent.getIntExtra("otherUserId", -1);
        Log.i("UserId : otherUserId", userId + " : " + otherUserId);

        loadUserTags();
        if (userId == -1 && savedInstanceState != null) {
            userId = savedInstanceState.getInt("userId", -1);
        }
        if (otherUserId == -1 && savedInstanceState != null) {
            otherUserId = savedInstanceState.getInt("otherUserId", -1);
        }

        if (otherUserId == -1) {
            Toast.makeText(this, "Error: No profile specified", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        /* set onClickListeners for music catalogue and pass the current user id */
        music.setOnClickListener(v -> {
            Intent tempMain = new Intent(OtherUserProfile.this, MusicCatalogue.class);
            tempMain.putExtra("userId", userId);
            startActivity(tempMain);
        });

        /* set onClickListeners for custom lists view and pass the current user id */
        lists.setOnClickListener(v -> {
            Intent tempMain = new Intent(OtherUserProfile.this, CustomListListPage.class);
            tempMain.putExtra("userId", otherUserId);
            startActivity(tempMain);
        });

        /* set onClickListeners for follower view and pass the current user id */
        friends.setOnClickListener(v -> {
            Intent tempMain = new Intent(OtherUserProfile.this, FollowerView.class);
            tempMain.putExtra("userId", otherUserId);
            startActivity(tempMain);
        });

        back.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("userId", userId);
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        if (otherUserId != -1) {
            loadUserBio(otherUserId);
            loadProfilePicture(otherUserId);
            loadSpotifyStatus(otherUserId);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userId != -1) {
            loadSpotifyStatus(userId);
        }
    }

    /* Load Spotify link status */
    private void loadSpotifyStatus(int otherUserId) {
        String url = BASE_URL + "/api/spotify/token?uid=" + otherUserId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    if(response.has("accessToken")) {
                        String spotifyId = response.optString("spotifyUserId", "");
                        showLinkedUI(spotifyId);
                    } else if (response.has("needsAuth") && response.optBoolean("needsAuth", false)) {
                        showNotLinkedUI();
                    } else {
                        showNotLinkedUI();
                    }
                },
                error -> {
                    Toast.makeText(OtherUserProfile.this, "Failed to load Spotify status", Toast.LENGTH_SHORT).show();
                    showNotLinkedUI();
                });

        requestQueue.add(request);
    }

    private void showNotLinkedUI() {
        spotifyStatusText.setText("Spotify account not linked");
    }

    private void showLinkedUI(String spotifyId) {
        spotifyStatusText.setText("Linked to Spotify \nID: " + spotifyId);
    }

    /* Fetch profile picture from backend */
    private void loadProfilePicture(int id) {
        String url = BASE_URL + "/users/" + id;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            String base64Image = response.optString("picture", "");
            if (!base64Image.isEmpty()) {
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                profilePicture.setImageBitmap(decodedBitmap);
            }
        }, error -> Toast.makeText(OtherUserProfile.this, "Failed to load profile picture", Toast.LENGTH_SHORT).show());
        requestQueue.add(request);
    }

    /* Fetch user bio from backend */
    private void loadUserBio(int id) {
        String url = BASE_URL + "/users/" + id;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            String bio = response.optString("bio", "").trim();
            if (!bio.isEmpty()) {
                bioView.setText(bio);
                bioView.setVisibility(TextView.VISIBLE);
            } else {
                bioView.setVisibility(TextView.GONE);
            }
            String first = response.optString("firstname", "");
            String last = response.optString("lastname", "");
            if (!first.isEmpty() || !last.isEmpty()) {
                fullname.setText(first + " " + last);
                fullname.setVisibility(TextView.VISIBLE);
            }
        }, error -> Toast.makeText(OtherUserProfile.this, "Failed to load bio", Toast.LENGTH_SHORT).show());
        requestQueue.add(request);
    }

    /** @author Graysen Schwaller
     * NOTE: Cayden Olsen did NOT do anything with UserTags
     **/
    private void loadUserTags() {
        tagService.getUsersTags(new TagManager.TagListCallback() {
            @Override
            public void onSuccess(JSONArray tags) {
                Log.i("LoadUserTags tags JSON Array", tags.toString());
                userTagUIManager.displayOtherUserTags(tags, tagService);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(OtherUserProfile.this, "Failed to load tags", Toast.LENGTH_SHORT).show();
            }
        }, otherUserId);
    }
}
