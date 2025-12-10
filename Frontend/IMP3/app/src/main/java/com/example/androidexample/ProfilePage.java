/** @author Cayden Olsen **/

package com.example.androidexample;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.flexbox.FlexboxLayout;

import org.json.JSONArray;
import org.json.JSONObject;

public class ProfilePage extends AppCompatActivity {
    private ImageButton settings, admin;
    private Button tempMain, music, lists;
    private Button friends;
    private Button login, signup;
    private Button spotifyLinkBtn;
    private Button spotifyUnlinkBtn;
    private ImageView profilePicture;
    private TextView bioView, fullname, spotifyStatusText;
    private int userId = -1;
    private RequestQueue requestQueue;
    private static final String BASE_URL = "http://coms-3090-027.class.las.iastate.edu:8080";
    private static final String SPOTIFY_URL = "https://coms-3090-027.class.las.iastate.edu";

    private FlexboxLayout userTagContainer;
    private TextView addUserTagButton;
    private Button spotifyGenerateTagsBtn;

    private TagManager tagService;
    private UserTagUIManager userTagUIManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_page);

        /* initialize UI elements */
        profilePicture = findViewById(R.id.profile_picture);
        settings = findViewById(R.id.settings_btn);
        friends = findViewById(R.id.friends_btn);
        tempMain = findViewById(R.id.temp_main_btn);
        login = findViewById(R.id.login_btn);
        bioView = findViewById(R.id.user_bio);
        music = findViewById(R.id.music_btn);
        lists = findViewById(R.id.lists_btn);
        fullname = findViewById(R.id.user_fullname);
        admin = findViewById(R.id.admin_btn);
        signup = findViewById(R.id.signup_btn);
        spotifyLinkBtn = findViewById(R.id.spotify_link_btn);
        spotifyUnlinkBtn = findViewById(R.id.spotify_unlink_btn);
        spotifyStatusText = findViewById(R.id.spotify_status_text);

        /* UserTag Initialization */
        userTagContainer = findViewById(R.id.userTagContainer);
        addUserTagButton = findViewById(R.id.addUserTagButton);
        spotifyGenerateTagsBtn = findViewById(R.id.spotify_generate_btn);

        spotifyGenerateTagsBtn.setOnClickListener(v -> generateSpotifyTags());
        tagService = new TagManager(this);
        userTagUIManager = new UserTagUIManager(this, userTagContainer);
        addUserTagButton.setOnClickListener(v -> showAddTagDialog());

        /* initialize Volley */
        requestQueue = QueueApplication.getQueue();

        admin.setVisibility(ImageButton.GONE);

        /* retrieve userId from previous activity */
        Intent intent = getIntent();
        if (intent != null && intent.getData() != null) {
            Uri data = intent.getData();
            String uidStr = data.getQueryParameter("uid");
            if (uidStr != null) {
                userId = Integer.parseInt(uidStr);
            }
        }

        if (userId == -1) {
            userId = intent.getIntExtra("userId", -1);
        }

        if (userId == -1 && savedInstanceState != null) {
            userId = savedInstanceState.getInt("userId", -1);
        }

        loadUserTags();

        /* set onClickListeners for edit user page and pass the current user id */
        settings.setOnClickListener(v -> {
            Intent editUser = new Intent(ProfilePage.this, EditUser.class);
            editUser.putExtra("userId", userId);
            startActivity(editUser);
        });

        /* set onClickListeners for admin page and pass the current user id */
        admin.setOnClickListener(v -> {
            Intent adminPage = new Intent(ProfilePage.this, AdminPage.class);
            adminPage.putExtra("userId", userId);
            startActivity(adminPage);
        });

        /* set onClickListeners for temporary main page and pass the current user id */
        tempMain.setOnClickListener(v -> {
            Intent mainPage = new Intent(ProfilePage.this, MainActivity.class);
            mainPage.putExtra("userId", userId);
            startActivity(mainPage);
        });

        /* set onClickListeners for music catalogue and pass the current user id */
        music.setOnClickListener(v -> {
            Intent tempMain = new Intent(ProfilePage.this, MusicCatalogue.class);
            tempMain.putExtra("userId", userId);
            startActivity(tempMain);
        });

        /* set onClickListeners for custom lists view and pass the current user id */
        lists.setOnClickListener(v -> {
            Intent tempMain = new Intent(ProfilePage.this, CustomListListPage.class);
            tempMain.putExtra("userId", userId);
            startActivity(tempMain);
        });

        /* set onClickListeners for follower view and pass the current user id */
        friends.setOnClickListener(v -> {
            Intent tempMain = new Intent(ProfilePage.this, FollowerView.class);
            tempMain.putExtra("userId", userId);
            startActivity(tempMain);
        });

        login.setOnClickListener(v -> {
            Intent loginUser = new Intent(ProfilePage.this, LoginPage.class);
            startActivity(loginUser);
        });

        signup.setOnClickListener(v -> {
            Intent loginUser = new Intent(ProfilePage.this, SignupPage.class);
            startActivity(loginUser);
        });

        spotifyLinkBtn.setOnClickListener(v -> {
            if (userId == -1) {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }
            openSpotifyLoginBrowser();
        });

        spotifyUnlinkBtn.setOnClickListener(v -> unlinkSpotify());

        if (userId != -1) {
            login.setVisibility(Button.GONE);
            signup.setVisibility(Button.GONE);
        }

        if (userId != -1) {
            loadUserBio(userId);
            loadProfilePicture(userId);
            loadSpotifyStatus(userId);
            loadUserPermissions(userId);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userId != -1) {
            loadSpotifyStatus(userId);
        }
    }

    private void openSpotifyLoginBrowser() {
        String loginUrl = SPOTIFY_URL + "/api/spotify/login?uid=" + userId;
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(loginUrl));
        startActivity(browserIntent);
    }

    /* Load Spotify link status */
    private void loadSpotifyStatus(int userId) {
        String url = BASE_URL + "/api/spotify/token?uid=" + userId;

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
                    Toast.makeText(ProfilePage.this, "Failed to load Spotify status", Toast.LENGTH_SHORT).show();
                    showNotLinkedUI();
                });

        requestQueue.add(request);
    }

    private void showNotLinkedUI() {
        spotifyLinkBtn.setVisibility(Button.VISIBLE);
        spotifyUnlinkBtn.setVisibility(Button.GONE);
        spotifyStatusText.setText("Spotify account not linked");

        spotifyGenerateTagsBtn.setVisibility(Button.GONE);
    }

    private void showLinkedUI(String spotifyId) {
        spotifyLinkBtn.setVisibility(Button.GONE);
        spotifyUnlinkBtn.setVisibility(Button.VISIBLE);
        spotifyStatusText.setText("Linked to Spotify \nID: " + spotifyId);

        spotifyGenerateTagsBtn.setVisibility(Button.VISIBLE);
    }

    /* Unlink Spotify account */
    private void unlinkSpotify() {
        if (userId == -1) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        String url = BASE_URL + "/api/spotify/unlink";
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.DELETE,
                url,
                null,
                response -> {
                    Toast.makeText(ProfilePage.this, "Spotify account unlinked", Toast.LENGTH_SHORT).show();
                    showNotLinkedUI();
                },
                error -> {
                    Log.e("UNLINK_ERROR", "Failed to unlink: " + error.toString());
                    Toast.makeText(ProfilePage.this, "Unlink failed", Toast.LENGTH_SHORT).show();
                }
        );
        request.setShouldCache(false);
        requestQueue.add(request);
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
        }, error -> Toast.makeText(ProfilePage.this, "Failed to load profile picture", Toast.LENGTH_SHORT).show());
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
        }, error -> Toast.makeText(ProfilePage.this, "Failed to load bio", Toast.LENGTH_SHORT).show());
        requestQueue.add(request);
    }

    /* Fetch user permissions from backend */
    private void loadUserPermissions(int id) {
        String url = BASE_URL + "/users/" + id;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
            String permission = response.optString("type", "");
            ImageButton adminBtn = findViewById(R.id.admin_btn);
            if (permission.equalsIgnoreCase("admin")) {
                adminBtn.setVisibility(ImageButton.VISIBLE);
                tempMain.setVisibility(Button.VISIBLE);
            } else {
                adminBtn.setVisibility(ImageButton.GONE);
                tempMain.setVisibility(Button.GONE);
            }
        }, error -> Toast.makeText(ProfilePage.this, "Failed to load permissions", Toast.LENGTH_SHORT).show());
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
                userTagUIManager.displayUserTags(tags, tagService);
            }

            @Override
            public void onError(String message) {
                Toast.makeText(ProfilePage.this, "Failed to load tags", Toast.LENGTH_SHORT).show();
            }
        }, userId);
    }

    /** @author Graysen Schwaller
     * NOTE: Cayden Olsen did NOT do anything with UserTags
     **/
    private void showAddTagDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Create Tag");

        final EditText input = new EditText(this);
        input.setHint("Tag name");

        b.setView(input);

        b.setPositiveButton("Create", (dialog, which) -> {
            String name = input.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Tag name required", Toast.LENGTH_SHORT).show();
                return;
            }

            tagService.createTag(name, null, null, new TagManager.TagCallback() {
                @Override
                public void onSuccess(JSONObject tag) {
                    int tagId = tag.optInt("tagId");

                    tagService.addTagToUser(userId, tagId, new TagManager.EmptyCallback() {
                        @Override
                        public void onSuccess() { loadUserTags(); }

                        @Override
                        public void onError(String msg) {
                            Toast.makeText(ProfilePage.this, "Failed to assign tag", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(ProfilePage.this, "Failed to create tag", Toast.LENGTH_SHORT).show();
                }
            });
        });

        b.setNegativeButton("Cancel", null);
        b.show();
    }

    /** @author Graysen Schwaller
     * NOTE: Cayden Olsen did NOT do anything with UserTags
     **/
    private void generateSpotifyTags() {
        String url = BASE_URL + "/usertag/generate";

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                url,
                null,
                response -> {
                    Toast.makeText(this, "Generated tags!", Toast.LENGTH_SHORT).show();
                    loadUserTags();
                },
                error -> Toast.makeText(this, "Failed to generate tags", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(req);
    }


}







