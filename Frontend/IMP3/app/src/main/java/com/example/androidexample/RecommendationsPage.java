/** @author Cayden Olsen **/

package com.example.androidexample;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecommendationsPage extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecommendationAdapter adapter;
    private String sessionCookie;
    private Button btnAdd, btnReturn, btnFetchSuggestions, btnFetchUserRecs;
    private int userId;
    private RequestQueue requestQueue = QueueApplication.getQueue();
    private List<Recommendation> recommendationList = new ArrayList<>();
    private static final String BASE_URL = "http://coms-3090-027.class.las.iastate.edu:8080/rec";
    private String spotifyAccessToken = null;
    private JSONObject selectedSpotifyJson = null;
    private String selectedSpotifyType = null;
    private Integer selectedRecipientUid = null;
    List<JSONObject> rawJsonList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recommendation_page);

        userId = getIntent().getIntExtra("userId", -1);
        sessionCookie = getSessionCookie();

        recyclerView = findViewById(R.id.recycler_recommendations);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnAdd = findViewById(R.id.btn_add_rec);
        btnReturn = findViewById(R.id.btn_return);
        btnFetchSuggestions = findViewById(R.id.btn_fetch_suggestions);
        btnFetchUserRecs = findViewById(R.id.btn_fetch_user_recs);

        btnAdd.setOnClickListener(v -> {
            selectedRecipientUid = null;
            fetchAllUsersForRecipientSelection();
        });

        btnReturn.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putExtra("userId", userId);
            setResult(RESULT_OK, intent);
            finish();
        });

        btnFetchSuggestions.setOnClickListener(v -> fetchSuggestedTracks());
        btnFetchUserRecs.setOnClickListener(v -> fetchAllUsers());

        fetchRecommendations();
    }

    private String getSessionCookie() {
        String sid = QueueApplication.getSessionId();
        if (sid != null && !sid.isEmpty()) {
            return "JSESSIONID=" + sid;
        }
        return "";
    }

    private void fetchRecommendations() {
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, BASE_URL, null,
                response -> {
                    recommendationList.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject obj = response.getJSONObject(i);
                            recommendationList.add(parseRecommendation(obj));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    setupRecyclerView();
                },
                error -> Toast.makeText(this, "Error fetching recommendations", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return getAuthHeaders();
            }
        };

        requestQueue.add(request);
    }

    private Recommendation parseRecommendation(JSONObject obj) throws JSONException {
        Recommendation rec = new Recommendation();
        rec.recId = obj.getInt("recId");
        rec.senderUid = obj.getInt("senderUid");
        rec.recipientUid = obj.getInt("recipientUid");
        rec.type = obj.getString("type");
        rec.itemId = obj.getString("itemId");
        rec.title = obj.getString("title");
        rec.source = obj.getString("source");
        rec.rationale = obj.optString("rationale");
        rec.privacy = obj.getString("privacy");
        return rec;
    }

    private void setupRecyclerView() {
        adapter = new RecommendationAdapter(recommendationList, new RecommendationAdapter.OnItemClickListener() {
            @Override
            public void onEditClick(Recommendation rec) {
                fetchSpotifyTokenAndShowDialog(rec);
            }

            @Override
            public void onDeleteClick(Recommendation rec) {
                deleteRecommendation(rec.recId);
            }

            @Override
            public void onSpotifyClick(Recommendation rec) {
                fetchSpotifyInfo(rec.recId);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void fetchAllUsersForRecipientSelection() {
        String url = "http://coms-3090-027.class.las.iastate.edu:8080/users/all";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    List<User> users = new ArrayList<>();
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);
                            int uid = obj.getInt("id");
                            if (uid == userId) continue; // Don't list yourself
                            String username = obj.getString("username");

                            byte[] pictureBytes = null;
                            if (!obj.isNull("picture")) {
                                String base64 = obj.getString("picture");
                                if (base64 != null && !base64.isEmpty() && !base64.equals("null")) {
                                    pictureBytes = Base64.decode(base64, Base64.DEFAULT);
                                }
                            }

                            users.add(new User(username, uid, pictureBytes, obj.optString("type", "user")));
                        }
                        showRecipientSelectionDialog(users);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to parse users", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Failed to fetch users", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return getAuthHeaders();
            }
        };

        requestQueue.add(request);
    }

    /* Fetch all users from the backend */
    private void fetchAllUsers() {
        String url = "http://coms-3090-027.class.las.iastate.edu:8080/users/all";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    List<User> users = new ArrayList<>();
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);
                            String username = obj.getString("username");
                            String type = obj.optString("type", "user");
                            int uid = obj.getInt("id"); // use 'id', not 'uid'
                            byte[] pictureBytes = null;
                            if (!obj.isNull("picture")) {
                                String base64 = obj.getString("picture");
                                if (base64 != null && !base64.isEmpty() && !base64.equals("null")) {
                                    pictureBytes = Base64.decode(base64, Base64.DEFAULT);
                                }
                            }
                            users.add(new User(username, uid, pictureBytes, type));
                        }
                        showUserSelectionDialog(users);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to parse users", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Failed to fetch users", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders () {
                return getAuthHeaders();
            }
        };

        requestQueue.add(request);
    }

    /* Show a simple dialog to select a user */
    private void showUserSelectionDialog(List<User> users) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a User");

        ListView listView = new ListView(this);
        builder.setView(listView);

        ArrayAdapter<User> adapter = new ArrayAdapter<User>(this, R.layout.dialog_users, R.id.txt_username, users) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = convertView;
                if (view == null) {
                    view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_users, parent, false);
                }

                User user = getItem(position);
                TextView txtUsername = view.findViewById(R.id.txt_username);
                ImageView imgUser = view.findViewById(R.id.img_user);

                txtUsername.setText(user.getUsername());

                if (user.getPicture() != null && user.getPicture().length > 0) {
                    Bitmap bmp = BitmapFactory.decodeByteArray(user.getPicture(), 0, user.getPicture().length);
                    imgUser.setImageBitmap(bmp);
                } else {
                    imgUser.setImageResource(R.drawable.imp3);
                }

                imgUser.setBackgroundResource(R.drawable.circle);
                imgUser.setClipToOutline(true);

                return view;
            }
        };

        listView.setAdapter(adapter);

        AlertDialog dialog = builder.create();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            User selectedUser = users.get(position);
            fetchUserPublicRecommendations(selectedUser.getId());
            dialog.dismiss();
        });

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", (d, which) -> dialog.dismiss());

        dialog.show();
    }

    private void fetchUserPublicRecommendations(int profileUid) {
        String url = BASE_URL + "/users/" + profileUid;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    recommendationList.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            recommendationList.add(parseRecommendation(response.getJSONObject(i)));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    setupRecyclerView();
                },
                error -> Toast.makeText(this, "Failed to fetch user's public recommendations", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return getAuthHeaders();
            }
        };
        requestQueue.add(request);
    }

    /* Fetch suggested tracks using Spotify + Gemini pipeline */
    private void fetchSuggestedTracks() {
        String url = BASE_URL + "/suggestions";

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    recommendationList.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject obj = response.getJSONObject(i);
                            Recommendation rec = new Recommendation();
                            rec.recId = obj.optInt("trackId", -1);
                            rec.title = obj.optString("name");
                            rec.itemId = obj.optString("spotifyId");
                            rec.type = "track";
                            rec.rationale = obj.optString("reason"); // or other field depending on SuggestedTrackResponse
                            recommendationList.add(rec);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    setupRecyclerView();
                },
                error -> {
                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        String body = new String(error.networkResponse.data);
                        Log.e("FetchSuggestions", "Error " + statusCode + ": " + body);
                    } else {
                        Log.e("FetchSuggestions", "No network response", error);
                    }
                    Toast.makeText(this, "Failed to fetch suggested tracks", Toast.LENGTH_SHORT).show();
                }

        ) {
            @Override
            public Map<String, String> getHeaders() {
                return getAuthHeaders();
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                10000, // 10 seconds timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

    private void showRecipientSelectionDialog(List<User> users) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Send Recommendation To:");

        ListView lv = new ListView(this);
        builder.setView(lv);

        ArrayAdapter<User> adapter = new ArrayAdapter<User>(
                this, R.layout.dialog_users, R.id.txt_username, users) {

            @Override public View getView(int pos, View convertView, ViewGroup parent) {
                View view = convertView;
                if (view == null)
                    view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_users, parent, false);

                User user = getItem(pos);
                TextView txtUsername = view.findViewById(R.id.txt_username);
                ImageView imgUser = view.findViewById(R.id.img_user);

                txtUsername.setText(user.getUsername());
                if (user.getPicture() != null) {
                    Bitmap bmp = BitmapFactory.decodeByteArray(user.getPicture(), 0, user.getPicture().length);
                    imgUser.setImageBitmap(bmp);
                } else imgUser.setImageResource(R.drawable.imp3);

                imgUser.setBackgroundResource(R.drawable.circle);
                imgUser.setClipToOutline(true);

                return view;
            }
        };

        lv.setAdapter(adapter);

        AlertDialog dialog = builder.create();

        lv.setOnItemClickListener((parent, view, pos, id) -> {
            selectedRecipientUid = users.get(pos).getId();
            dialog.dismiss();

            // Continue flow: get Spotify token → show recommendation dialog
            fetchSpotifyTokenAndShowDialog(null);
        });

        dialog.show();
    }

    private void fetchSpotifyTokenAndShowDialog(Recommendation rec) {
        String url = "http://coms-3090-027.class.las.iastate.edu:8080/api/spotify/token?uid=" + userId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    spotifyAccessToken = response.optString("accessToken", null);
                    if (spotifyAccessToken == null) {
                        Toast.makeText(this, "Spotify not linked", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showAddEditDialog(rec);
                },
                error -> Toast.makeText(this, "Spotify not linked", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return getAuthHeaders();
            }
        };

        requestQueue.add(request);
    }

    private void showAddEditDialog(Recommendation rec) {
        boolean isEdit = rec != null;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isEdit ? "Edit Recommendation" : "Add Recommendation");

        View layout = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_rec, null);
        builder.setView(layout);

        EditText inputTitle = layout.findViewById(R.id.input_title);
        EditText inputType = layout.findViewById(R.id.input_type);
        EditText inputRationale = layout.findViewById(R.id.input_rationale);
        EditText inputPrivacy = layout.findViewById(R.id.input_privacy);
        Button btnSelectSpotify = layout.findViewById(R.id.btn_select_spotify);

        if (isEdit) {
            selectedRecipientUid = rec.recipientUid;
            inputTitle.setText(rec.title);
            inputType.setText(rec.type);
            inputRationale.setText(rec.rationale);
            inputPrivacy.setText(rec.privacy);
        } else {
            inputPrivacy.setText("PUBLIC");
        }

        btnSelectSpotify.setOnClickListener(v -> showSpotifySearchDialog(inputTitle, inputType));

        builder.setPositiveButton(isEdit ? "Update" : "Add", (dialog, which) -> {
            try {
                if (selectedSpotifyJson == null) {
                    Toast.makeText(this, "Please select a Spotify track or album", Toast.LENGTH_SHORT).show();
                    return;
                }

                JSONObject requestBody = new JSONObject();
                requestBody.put("senderUid", userId);
                requestBody.put("recipientUid", selectedRecipientUid);
                requestBody.put("type", selectedSpotifyType);
                requestBody.put("title", inputTitle.getText().toString());
                requestBody.put("rationale", inputRationale.getText().toString());
                requestBody.put("privacy", inputPrivacy.getText().toString());
                requestBody.put("spotifyData", selectedSpotifyJson.toString());

                if (isEdit) {
                    updateRecommendation(rec.recId, requestBody);
                } else {
                    createRecommendation(requestBody);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // Spotify search dialog
    private void showSpotifySearchDialog(EditText inputTitle, EditText inputType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Search Spotify Track or Album");

        final EditText input = new EditText(this);
        input.setHint("Enter song or album name");
        builder.setView(input);

        builder.setPositiveButton("Search", (dialog, which) -> {
            String query = input.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(this, "Enter a search term", Toast.LENGTH_SHORT).show();
                return;
            }
            searchSpotify(query, inputTitle, inputType);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // Perform Spotify search via backend proxy
    private void searchSpotify(String query, EditText inputTitle, EditText inputType) {
        if (spotifyAccessToken == null || spotifyAccessToken.isBlank()) {
            Toast.makeText(this, "Spotify token missing — please try again", Toast.LENGTH_SHORT).show();
            return;
        }

        rawJsonList.clear();

        String url = "http://coms-3090-027.class.las.iastate.edu:8080/api/spotify/search"
                + "?uid=" + userId
                + "&q=" + query.replace(" ", "%20")
                + "&type=track,album"
                + "&limit=20";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        List<String> labels = new ArrayList<>();
                        List<String> ids = new ArrayList<>();
                        List<String> types = new ArrayList<>();

                        // Tracks
                        if (response.has("tracks") && response.getJSONObject("tracks").has("items")) {
                            JSONArray tracks = response.getJSONObject("tracks").getJSONArray("items");
                            for (int i = 0; i < tracks.length(); i++) {
                                JSONObject t = tracks.getJSONObject(i);
                                rawJsonList.add(t);
                                String name = t.optString("name", "Unknown Track");
                                String artist = "Unknown Artist";
                                try {
                                    artist = t.getJSONArray("artists").getJSONObject(0).optString("name", artist);
                                } catch (Exception ignored) {}
                                labels.add("TRACK: " + name + " — " + artist);
                                ids.add(t.getString("id"));
                                types.add("track");
                            }
                        }

                        // Albums
                        if (response.has("albums") && response.getJSONObject("albums").has("items")) {
                            JSONArray albums = response.getJSONObject("albums").getJSONArray("items");
                            for (int i = 0; i < albums.length(); i++) {
                                JSONObject a = albums.getJSONObject(i);
                                rawJsonList.add(a);
                                String name = a.optString("name", "Unknown Album");
                                String artist = "Unknown Artist";
                                try {
                                    artist = a.getJSONArray("artists").getJSONObject(0).optString("name", artist);
                                } catch (Exception ignored) {}
                                labels.add("ALBUM: " + name + " — " + artist);
                                ids.add(a.getString("id"));
                                types.add("album");
                            }
                        }

                        if (labels.isEmpty()) {
                            Toast.makeText(this, "No results found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String[] labelsArray = labels.toArray(new String[0]);
                        String[] idsArray = ids.toArray(new String[0]);
                        String[] typesArray = types.toArray(new String[0]);

                        AlertDialog.Builder selectBuilder = new AlertDialog.Builder(this);
                        selectBuilder.setTitle("Select Track or Album");
                        selectBuilder.setItems(labelsArray, (d, which) -> {
                            selectedSpotifyJson = rawJsonList.get(which);
                            selectedSpotifyType = typesArray[which];

                            inputType.setText(selectedSpotifyType);

                            if (selectedSpotifyType.equals("album")) {
                                inputTitle.setText(labelsArray[which].replaceFirst("^ALBUM:\\s*", ""));
                            } else {
                                inputTitle.setText(labelsArray[which].replaceFirst("^TRACK:\\s*", ""));
                            }
                        });
                        selectBuilder.show();

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to parse search results", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(this, "Spotify search failed", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return getAuthHeaders();
            }
        };

        requestQueue.add(request);
    }

    private void deleteRecommendation(int recId) {
        String url = BASE_URL + "/" + recId;
        StringRequest request = new StringRequest(Request.Method.DELETE, url,
                response -> {
                    Toast.makeText(this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                    fetchRecommendations();
                },
                error -> Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return getAuthHeaders();
            }
        };
        requestQueue.add(request);
    }

    private void createRecommendation(JSONObject body) {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, BASE_URL, body,
                response -> {
                    Toast.makeText(this, "Recommendation Sent!", Toast.LENGTH_SHORT).show();
                    fetchRecommendations();
                },
                error -> {
                    if (error.networkResponse != null) {
                        Log.e("VolleyError", "Status: " + error.networkResponse.statusCode +
                                " Data: " + new String(error.networkResponse.data));
                    } else {
                        Log.e("VolleyError", "Network error", error);
                    }
                    Toast.makeText(this, "Failed to add recommendation", Toast.LENGTH_SHORT).show();
                }) {

            @Override
            public Map<String, String> getHeaders() {
                return getAuthHeaders();
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(
                15000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        requestQueue.add(request);
    }

    private void updateRecommendation(int recId, JSONObject requestBody) {
        String url = BASE_URL + "/" + recId;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.PUT, url, requestBody,
                response -> {
                    Toast.makeText(this, "Updated successfully", Toast.LENGTH_SHORT).show();
                    fetchRecommendations();
                },
                error -> Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return getAuthHeaders();
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(
                15000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        requestQueue.add(request);
    }

    private void fetchSpotifyInfo(int recId) {
        String url = BASE_URL + "/" + recId + "/spotify";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String trackName = response.optString("name", "Unknown Track");
                        JSONArray artists = response.getJSONArray("artists");
                        String artistName = artists.getJSONObject(0).optString("name", "Unknown Artist");
                        String albumName = response.getJSONObject("album").optString("name", "Unknown Album");

                        String message = "Track: " + trackName + "\nArtist: " + artistName + "\nAlbum: " + albumName;

                        new AlertDialog.Builder(this)
                                .setTitle("Spotify Info")
                                .setMessage(message)
                                .setPositiveButton("OK", null)
                                .show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to parse Spotify info", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Failed to fetch Spotify info", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders () {
                return getAuthHeaders();
            }
        };

        requestQueue.add(request);
    }

    private Map<String, String> getAuthHeaders() {
        Map<String, String> headers = new HashMap<>();
        if (sessionCookie != null && !sessionCookie.isEmpty()) {
            headers.put("Cookie", sessionCookie);
        }
        return headers;
    }
}








