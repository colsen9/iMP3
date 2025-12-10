/** @author Cayden Olsen **/

package com.example.androidexample;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AdminPage extends AppCompatActivity {
    private static final int ALBUM_FORM = 1;
    private static final int ARTIST_FORM = 2;
    private int currentForm = 0;
    private RecyclerView rvAlbums, rvTracks, rvArtists, rvUserList;
    private LinearLayout albumForm, trackForm, artistForm;
    private Button btnAddAlbum, btnSaveAlbum, btnCancelAlbum;
    private Button btnAddTrack, btnSaveTrack, btnCancelTrack;
    private Button btnAddArtist, btnSaveArtist, btnCancelArtist, btnSelectArtistPicture;
    private Button btnReturnProfile, btnSelectAlbumArt, btnLoadUsers;
    private Button btnCloseUserList;
    private RequestQueue requestQueue;
    private EditText etAlbumName, etAlbumArtistIds, etAlbumTrackIds, etAlbumDuration, etAlbumReleaseDate;
    private EditText etTrackName, etTrackArtistIds, etTrackAlbumIds, etTrackMood;
    private EditText etTrackGenre, etTrackDuration, etTrackTrackNumber, etTrackBPM;
    private EditText etArtistName, etArtistBio, etArtistAlbumIds, etArtistTrackIds, etArtistYears;
    private EditText etTrackSearch;
    private Button btnTrackSearch;
    private EditText etAlbumSearch;
    private Button btnAlbumSearch;
    private AlbumAdapter albumAdapter;
    private TrackAdapter trackAdapter;
    private ArtistAdapter artistAdapter;
    private UserAdapter userAdapter;
    private ArrayList<Album> albumList = new ArrayList<>();
    private ArrayList<Track> trackList = new ArrayList<>();
    private ArrayList<Artist> artistList = new ArrayList<>();
    private ArrayList<User> userList = new ArrayList<>();
    private Set<Integer> backendArtistIds = new HashSet<>();
    private int userId;
    private String selectedSpotifyId = null;
    private Integer editingAlbumId = null;
    private Integer editingTrackId = null;
    private Integer editingArtistId = null;
    private String albumImageBase64 = null;
    private String artistImageBase64 = null;
    private final int PICK_IMAGE_REQUEST = 1;
    private final String BASE_URL = "http://coms-3090-027.class.las.iastate.edu:8080";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_view);

        userId = getIntent().getIntExtra("userId", -1);
        requestQueue = QueueApplication.getQueue();

        setupViews();
        setupRecyclerViews();
        setupListeners();

        loadAlbums();
        loadTracks();
        loadArtists();
        loadBackendArtists();
    }

    private void setupViews() {
        rvAlbums = findViewById(R.id.rvAlbumList);
        rvTracks = findViewById(R.id.rvTrackList);
        rvArtists = findViewById(R.id.rvArtistList);
        rvUserList = findViewById(R.id.rvUserList);

        albumForm = findViewById(R.id.albumForm);
        trackForm = findViewById(R.id.trackForm);
        artistForm = findViewById(R.id.artistForm);

        btnAddAlbum = findViewById(R.id.btnAddAlbum);
        btnSaveAlbum = findViewById(R.id.btnSaveAlbum);
        btnCancelAlbum = findViewById(R.id.btnCancelAlbum);

        btnAddTrack = findViewById(R.id.btnAddTrack);
        btnSaveTrack = findViewById(R.id.btnSaveTrack);
        btnCancelTrack = findViewById(R.id.btnCancelTrack);

        btnAddArtist = findViewById(R.id.btnAddArtist);
        btnSaveArtist = findViewById(R.id.btnSaveArtist);
        btnCancelArtist = findViewById(R.id.btnCancelArtist);
        btnSelectArtistPicture = findViewById(R.id.btnSelectArtistPicture);

        btnLoadUsers = findViewById(R.id.btnLoadUsers);
        btnCloseUserList = findViewById(R.id.btnCloseUserList);
        btnCloseUserList.setVisibility(View.GONE);

        btnReturnProfile = findViewById(R.id.btnReturnProfile);
        btnSelectAlbumArt = findViewById(R.id.btnSelectAlbumArt);

        etAlbumName = findViewById(R.id.etAlbumName);
        etAlbumArtistIds = findViewById(R.id.etAlbumArtistIds);
        etAlbumTrackIds = findViewById(R.id.etAlbumTrackIds);
        etAlbumDuration = findViewById(R.id.etAlbumDuration);
        etAlbumReleaseDate = findViewById(R.id.etAlbumReleaseDate);

        etTrackName = findViewById(R.id.etTrackName);
        etTrackArtistIds = findViewById(R.id.etTrackArtistIds);
        etTrackAlbumIds = findViewById(R.id.etTrackAlbumIds);
        etTrackMood = findViewById(R.id.etTrackMood);
        etTrackGenre = findViewById(R.id.etTrackGenre);
        etTrackDuration = findViewById(R.id.etTrackDuration);
        etTrackTrackNumber = findViewById(R.id.etTrackTrackNumber);
        etTrackBPM = findViewById(R.id.etTrackBPM);

        etArtistName = findViewById(R.id.etArtistName);
        etArtistBio = findViewById(R.id.etArtistBio);
        etArtistAlbumIds = findViewById(R.id.etArtistAlbumIds);
        etArtistTrackIds = findViewById(R.id.etArtistTrackIds);
        etArtistYears = findViewById(R.id.etArtistYears);

        etAlbumSearch = findViewById(R.id.etAlbumSearch);
        btnAlbumSearch = findViewById(R.id.btnAlbumSearch);

        etTrackSearch = findViewById(R.id.etTrackSearch);
        btnTrackSearch = findViewById(R.id.btnTrackSearch);
    }

    private void setupRecyclerViews() {
        albumAdapter = new AlbumAdapter(albumList, new AlbumAdapter.OnAlbumClickListener() {
            @Override public void onEdit(Album album) { openEditAlbum(album); }
            @Override public void onDelete(Album album) { deleteAlbum(album); }
        });
        rvAlbums.setLayoutManager(new LinearLayoutManager(this));
        rvAlbums.setAdapter(albumAdapter);

        trackAdapter = new TrackAdapter(trackList, new TrackAdapter.OnTrackClickListener() {
            @Override public void onEdit(Track track) { openEditTrack(track); }
            @Override public void onDelete(Track track) { deleteTrack(track); }
        });
        rvTracks.setLayoutManager(new LinearLayoutManager(this));
        rvTracks.setAdapter(trackAdapter);

        artistAdapter = new ArtistAdapter(artistList, new ArtistAdapter.OnArtistClickListener() {
            @Override public void onEdit(Artist artist) { openEditArtist(artist); }
            @Override public void onDelete(Artist artist) { deleteArtist(artist); }
        });
        rvArtists.setLayoutManager(new LinearLayoutManager(this));
        rvArtists.setAdapter(artistAdapter);

        userAdapter = new UserAdapter(userList, new UserAdapter.OnUserActionListener() {
            @Override
            public void onPerms(User user) {
                showEditUserTypeDialog(user);
            }

            @Override
            public void onBan(User user) {
                if ("banned".equalsIgnoreCase(user.getType())) {
                    unbanUser(user.getId());
                } else {
                    banUser(user.getId());
                }
            }
        });
        rvUserList.setLayoutManager(new LinearLayoutManager(this));
        rvUserList.setAdapter(userAdapter);

    }

    private void setupListeners() {
        btnReturnProfile.setOnClickListener(v -> {
            Intent tempMain = new Intent(AdminPage.this, ProfilePage.class);
            tempMain.putExtra("userId", userId);
            startActivity(tempMain);
        });

        btnLoadUsers.setOnClickListener(v -> {
            loadUsers();
            btnLoadUsers.setVisibility(View.GONE);
            btnCloseUserList.setVisibility(View.VISIBLE);
        });

        btnCloseUserList.setOnClickListener(v -> {
            rvUserList.setVisibility(View.GONE);
            btnCloseUserList.setVisibility(View.GONE);
            btnLoadUsers.setVisibility(View.VISIBLE);
        });

        btnAddAlbum.setOnClickListener(v -> openAddAlbumForm());
        btnCancelAlbum.setOnClickListener(v -> closeAlbumForm());
        btnSaveAlbum.setOnClickListener(v -> saveAlbum());

        btnAddTrack.setOnClickListener(v -> openAddTrackForm());
        btnCancelTrack.setOnClickListener(v -> closeTrackForm());
        btnSaveTrack.setOnClickListener(v -> saveTrack());

        btnAddArtist.setOnClickListener(v -> openAddArtistForm());
        btnCancelArtist.setOnClickListener(v -> closeArtistForm());
        btnSaveArtist.setOnClickListener(v -> saveArtist());

        btnAlbumSearch.setOnClickListener(v -> performAlbumSpotifySearch());
        btnTrackSearch.setOnClickListener(v -> performTrackSpotifySearch());

        btnSelectAlbumArt.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Album Art"), PICK_IMAGE_REQUEST);
        });

        btnSelectArtistPicture.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Artist Picture"), PICK_IMAGE_REQUEST);
        });
    }

    private void performAlbumSpotifySearch() {
        String query = etAlbumSearch.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "Enter a search term", Toast.LENGTH_SHORT).show();
            return;
        }

        String tokenUrl = BASE_URL + "/api/spotify/token?uid=" + userId;

        JsonObjectRequest tokenRequest = new JsonObjectRequest(Request.Method.GET, tokenUrl, null,
                tokenResponse -> {
                    String accessToken = tokenResponse.optString("accessToken", null);
                    if (accessToken == null) {
                        Toast.makeText(this, "Spotify not linked or token expired", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String searchUrl = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        searchUrl = BASE_URL + "/api/spotify/search?uid=" + userId + "&q=" +
                                URLEncoder.encode(query, StandardCharsets.UTF_8) + "&type=album";
                    }

                    JsonObjectRequest searchRequest = new JsonObjectRequest(Request.Method.GET, searchUrl, null,
                            response -> {
                                try {
                                    JSONObject albumsObj = response.optJSONObject("albums");
                                    JSONArray items = albumsObj.optJSONArray("items");
                                    if (items == null || items.length() == 0) {
                                        Toast.makeText(this, "No Spotify results", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    JSONObject album = items.getJSONObject(0);
                                    autofillAlbumFromSpotify(album);
                                } catch (Exception e) {
                                    Toast.makeText(this, "Failed to parse Spotify response", Toast.LENGTH_SHORT).show();
                                    e.printStackTrace();
                                }
                            },
                            error -> Toast.makeText(this, "Spotify album search failed", Toast.LENGTH_SHORT).show()
                    ) {
                        @Override
                        public Map<String, String> getHeaders() {
                            Map<String, String> headers = new HashMap<>();
                            headers.put("Authorization", "Bearer " + accessToken);
                            return headers;
                        }
                    };

                    requestQueue.add(searchRequest);

                },
                error -> Toast.makeText(this, "Failed to get Spotify token", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(tokenRequest);
    }

    private void performTrackSpotifySearch() {
        String query = etTrackSearch.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "Enter a search term", Toast.LENGTH_SHORT).show();
            return;
        }

        String tokenUrl = BASE_URL + "/api/spotify/token?uid=" + userId;

        JsonObjectRequest tokenRequest = new JsonObjectRequest(Request.Method.GET, tokenUrl, null,
                tokenResponse -> {
                    String accessToken = tokenResponse.optString("accessToken", null);
                    if (accessToken == null) {
                        Toast.makeText(this, "Spotify not linked or token expired", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String searchUrl = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        searchUrl = BASE_URL + "/api/spotify/search?uid=" + userId + "&q=" +
                                URLEncoder.encode(query, StandardCharsets.UTF_8) + "&type=track";
                    }

                    JsonObjectRequest searchRequest = new JsonObjectRequest(Request.Method.GET, searchUrl, null,
                            response -> {
                                try {
                                    JSONObject tracksObj = response.optJSONObject("tracks");
                                    JSONArray items = tracksObj.optJSONArray("items");
                                    if (items == null || items.length() == 0) {
                                        Toast.makeText(this, "No Spotify results", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    JSONObject track = items.getJSONObject(0);
                                    autofillTrackFromSpotify(track);
                                } catch (Exception e) {
                                    Toast.makeText(this, "Failed to parse Spotify response", Toast.LENGTH_SHORT).show();
                                    e.printStackTrace();
                                }
                            },
                            error -> Toast.makeText(this, "Spotify search failed", Toast.LENGTH_SHORT).show()
                    ) {
                        @Override
                        public Map<String, String> getHeaders() {
                            Map<String, String> headers = new HashMap<>();
                            headers.put("Authorization", "Bearer " + accessToken);
                            return headers;
                        }
                    };

                    requestQueue.add(searchRequest);

                },
                error -> Toast.makeText(this, "Failed to get Spotify token", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(tokenRequest);
    }

    private void autofillAlbumFromSpotify(JSONObject album) {
        try {
            // Store Spotify ID
            selectedSpotifyId = album.optString("id");

            // Album name
            String name = album.optString("name");
            etAlbumName.setText(name);

            // Release year
            int releaseYear = 0;
            String releaseDateStr = album.optString("release_date");
            if (!releaseDateStr.isEmpty() && releaseDateStr.length() >= 4) {
                releaseYear = Integer.parseInt(releaseDateStr.substring(0, 4));
            }
            etAlbumReleaseDate.setText(releaseYear > 0 ? String.valueOf(releaseYear) : "");

            // Clear artist and track IDs (user will fill manually)
            etAlbumArtistIds.setText("");
            etAlbumTrackIds.setText("");

            // Load the first album image as Base64
            JSONArray images = album.optJSONArray("images");
            if (images != null && images.length() > 0) {
                String imageUrl = images.getJSONObject(0).optString("url");
                if (!imageUrl.isEmpty()) {
                    new Thread(() -> {
                        try {
                            InputStream in = new java.net.URL(imageUrl).openStream();
                            Bitmap bitmap = BitmapFactory.decodeStream(in);
                            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                            byte[] bytes = baos.toByteArray();
                            albumImageBase64 = Base64.encodeToString(bytes, Base64.DEFAULT);
                            runOnUiThread(() -> Toast.makeText(this, "Album image loaded", Toast.LENGTH_SHORT).show());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            }

            Toast.makeText(this, "Spotify album info loaded", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to autofill album from Spotify", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void autofillTrackFromSpotify(JSONObject track) {
        try {
            // Track basic info
            String name = track.optString("name");
            int durationMs = track.optInt("duration_ms");
            int trackNum = track.optInt("track_number");

            selectedSpotifyId = track.optString("id");

            etTrackName.setText(name);
            etTrackDuration.setText(String.valueOf(durationMs));
            etTrackTrackNumber.setText(String.valueOf(trackNum));

            etTrackBPM.setText("");

            etTrackGenre.setText("");
            etTrackMood.setText("");
            etTrackArtistIds.setText("");
            etTrackAlbumIds.setText("");

            Toast.makeText(this, "Spotify track info loaded", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to autofill track from Spotify", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void loadAlbums() {
        String url = BASE_URL + "/music/albums";

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {

                    albumList.clear();
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject obj = response.optJSONObject(i);
                        Album album = jsonToAlbum(obj);
                        albumList.add(album);
                        Log.d("ALBUM_LOG", "ID: " + album.getId() + ", Name: " + album.getName());
                    }
                    albumAdapter.notifyDataSetChanged();
                },
                error -> Toast.makeText(this, "Failed to load albums", Toast.LENGTH_SHORT).show()
        );
        requestQueue.add(req);
    }

    private void loadTracks() {
        String url = BASE_URL + "/music/tracks";

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    trackList.clear();
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject obj = response.optJSONObject(i);
                        Track track = jsonToTrack(obj);
                        trackList.add(track);
                        Log.d("TRACK_LOG", "ID: " + track.getId() + ", Name: " + track.getName());
                    }
                    trackAdapter.notifyDataSetChanged();
                },
                error -> Toast.makeText(this, "Failed to load tracks", Toast.LENGTH_SHORT).show()
        );
        requestQueue.add(req);
    }

    private void loadArtists() {
        String url = BASE_URL + "/music/artists/";

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    artistList.clear();
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject obj = response.optJSONObject(i);
                        Artist artist = jsonToArtist(obj);
                        artistList.add(artist);
                        Log.d("ARTIST_LOG", "ID: " + artist.getId() + ", Name: " + artist.getName());
                    }
                    artistAdapter.notifyDataSetChanged();
                },
                error -> Toast.makeText(this, "Failed to load artists", Toast.LENGTH_SHORT).show()
        );
        requestQueue.add(req);
    }
    private void loadBackendArtists() {
        String url = BASE_URL + "/music/artists/";

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    backendArtistIds.clear();
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject obj = response.optJSONObject(i);
                        backendArtistIds.add(obj.optInt("id"));
                    }
                },
                error -> Toast.makeText(this, "Failed to load artists", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(req);
    }

    private void loadUsers() {
        String url = BASE_URL + "/users/all";

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    userList.clear();
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject obj = response.optJSONObject(i);
                        if (obj != null) {
                            String username = obj.optString("username");
                            int id = obj.optInt("id");
                            String type = obj.optString("type", "user");

                            byte[] pictureBytes = null;
                            if (!obj.isNull("picture")) {
                                String base64 = obj.optString("picture");
                                if (base64 != null && !base64.isEmpty() && !base64.equals("null")) {
                                    pictureBytes = Base64.decode(base64, Base64.DEFAULT);
                                }
                            } else if (!obj.isNull("profilePicture")) {
                                JSONArray picArr = obj.optJSONArray("profilePicture");
                                if (picArr != null && picArr.length() > 0) {
                                    pictureBytes = new byte[picArr.length()];
                                    for (int j = 0; j < picArr.length(); j++) {
                                        pictureBytes[j] = (byte) (picArr.optInt(j) & 0xFF);
                                    }
                                }
                            }

                            userList.add(new User(username, id, pictureBytes, type));
                        }
                    }

                    userAdapter.notifyDataSetChanged();
                    rvUserList.setVisibility(View.VISIBLE);
                },
                error -> Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(req);
    }

    private void showEditUserTypeDialog(User user) {
        EditText etUserType = new EditText(this);
        etUserType.setHint("admin, user, or artist");

        String url = BASE_URL + "/users/" + user.getId();
        JsonObjectRequest getRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    String type = response.optString("type", "user");
                    etUserType.setText(type);
                },
                error -> Toast.makeText(this, "Failed to fetch user details", Toast.LENGTH_SHORT).show()
        );
        requestQueue.add(getRequest);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Edit User Type: " + user.getUsername())
                .setView(etUserType)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newType = etUserType.getText().toString().trim();
                    if (newType.isEmpty()) {
                        Toast.makeText(this, "Type cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateUserType(user.getId(), newType);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateUserType(int userId, String newType) {
        String url = BASE_URL + "/users/" + userId;

        JSONObject body = new JSONObject();
        try {
            body.put("type", newType);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to create request", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest putRequest = new JsonObjectRequest(Request.Method.PUT, url, body,
                response -> Toast.makeText(this, "User type updated", Toast.LENGTH_SHORT).show(),
                error -> Toast.makeText(this, "Failed to update user type", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(putRequest);
    }

    private void banUser(int userId) {
        String url = BASE_URL + "/users/ban/" + userId;

        JSONObject emptyBody = new JSONObject();

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.PUT,
                url,
                emptyBody,
                response -> {
                    Toast.makeText(this, "User banned", Toast.LENGTH_SHORT).show();
                    loadUsers();
                },
                error -> {
                    Toast.makeText(this, "Failed to ban user", Toast.LENGTH_SHORT).show();
                }
        );

        requestQueue.add(request);
    }

    private void unbanUser(int userId) {
        String url = BASE_URL + "/users/unban/" + userId;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.PUT,
                url,
                new JSONObject(),
                response -> {
                    Toast.makeText(this, "User unbanned", Toast.LENGTH_SHORT).show();
                    loadUsers();
                },
                error -> {
                    Toast.makeText(this, "Failed to unban user", Toast.LENGTH_SHORT).show();
                }
        );

        requestQueue.add(request);
    }

    private void openAddAlbumForm() {
        editingAlbumId = null;
        clearAlbumForm();
        albumForm.setVisibility(View.VISIBLE);
        currentForm = ALBUM_FORM;

    }

    private void openEditAlbum(Album album) {
        editingAlbumId = album.getId();
        albumForm.setVisibility(View.VISIBLE);
        currentForm = ALBUM_FORM;

        etAlbumName.setText(album.getName());
        etAlbumArtistIds.setText(setToCSV(album.getArtistIds()));
        etAlbumTrackIds.setText(setToCSV(album.getTrackIds()));
        etAlbumDuration.setText(album.getDuration() != null ? album.getDuration().toString() : "");
        etAlbumReleaseDate.setText(album.getReleaseDate() != null ? album.getReleaseDate().toString() : "");
    }

    private void closeAlbumForm() {
        albumForm.setVisibility(View.GONE);
        clearAlbumForm();
        editingAlbumId = null;
        currentForm = 0;
    }

    private void openAddTrackForm() {
        editingTrackId = null;
        clearTrackForm();
        trackForm.setVisibility(View.VISIBLE);
    }

    private void openEditTrack(Track track) {
        editingTrackId = track.getId();
        trackForm.setVisibility(View.VISIBLE);

        etTrackName.setText(track.getName());
        etTrackMood.setText(track.getMood());
        etTrackGenre.setText(track.getGenre());
        etTrackDuration.setText(track.getDuration() != null ? track.getDuration().toString() : "");
        etTrackTrackNumber.setText(track.getTrackNumber() != null ? track.getTrackNumber().toString() : "");
        etTrackBPM.setText(track.getBpm() != null ? track.getBpm().toString() : "");
        etTrackArtistIds.setText(setToCSV(track.getArtistIds()));
        etTrackAlbumIds.setText(setToCSV(track.getAlbumIds()));
    }

    private void closeTrackForm() {
        trackForm.setVisibility(View.GONE);
        clearTrackForm();
        editingTrackId = null;
    }

    private void openAddArtistForm() {
        editingArtistId = null;
        clearArtistForm();
        artistForm.setVisibility(View.VISIBLE);
        currentForm = ARTIST_FORM;
    }

    private void openEditArtist(Artist artist) {
        editingArtistId = artist.getId();
        artistForm.setVisibility(View.VISIBLE);
        currentForm = ARTIST_FORM;

        etArtistName.setText(artist.getName());
        etArtistBio.setText(artist.getBio());
        etArtistAlbumIds.setText(setToCSV(artist.getAlbumIds()));
        etArtistTrackIds.setText(setToCSV(artist.getTrackIds()));
        etArtistYears.setText(listToCSV(artist.getYears()));
    }

    private void closeArtistForm() {
        artistForm.setVisibility(View.GONE);
        clearArtistForm();
        editingArtistId = null;
        currentForm = 0;
    }

    private void saveAlbum() {
        JSONObject body = new JSONObject();
        try {
            // Basic album info
            body.put("name", etAlbumName.getText().toString().trim());
            body.put("spotifyId", selectedSpotifyId != null ? selectedSpotifyId : "");
            body.put("albumArtUrl", JSONObject.NULL);  // permanent null

            // Duration & release date
            body.put("duration", parseIntSafe(etAlbumDuration.getText().toString().trim()));
            body.put("releaseDate", parseIntSafe(etAlbumReleaseDate.getText().toString().trim()));

            // Artist & track IDs
            String artistCsv = etAlbumArtistIds.getText().toString().trim();
            Set<Integer> artistSet = new HashSet<>();
            if (!artistCsv.isEmpty()) {
                String[] parts = artistCsv.split(",");
                for (String s : parts) {
                    try {
                        int id = Integer.parseInt(s.trim());
                        if (backendArtistIds.contains(id)) {  // only include valid backend artist IDs
                            artistSet.add(id);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }

            JSONArray artistArray = new JSONArray();
            for (int id : artistSet) artistArray.put(id);

            body.put("artistIds", artistArray);
            body.put("trackIds", csvToJSONArray(etAlbumTrackIds.getText().toString().trim()));

            // Tags (empty for now)
            body.put("tags", new JSONArray());

            // Album art
            if (albumImageBase64 != null) {
                byte[] bytes = Base64.decode(albumImageBase64, Base64.DEFAULT);
                JSONArray byteArrayJson = new JSONArray();
                for (byte b : bytes) byteArrayJson.put(b & 0xFF);
                body.put("albumArt", byteArrayJson);
            } else {
                body.put("albumArt", new JSONArray()); // send empty array if no art
            }

            // Editing album
            if (editingAlbumId != null) body.put("id", editingAlbumId);

        } catch (JSONException ignored) {}

        String url = editingAlbumId == null ? BASE_URL + "/music/albums/new" : BASE_URL + "/music/albums/edit";
        int method = editingAlbumId == null ? Request.Method.POST : Request.Method.PUT;

        JsonObjectRequest req = new JsonObjectRequest(method, url, body,
                response -> {
                    Toast.makeText(this, "Album saved", Toast.LENGTH_SHORT).show();
                    closeAlbumForm();
                    loadAlbums();
                },
                error -> Toast.makeText(this, "Failed to save album", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(req);
    }

    private void deleteAlbum(Album album) {
        String url = BASE_URL + "/music/albums/delete/" + album.getId();
        StringRequest req = new StringRequest(Request.Method.DELETE, url,
                response -> { Toast.makeText(this, "Album deleted", Toast.LENGTH_SHORT).show(); loadAlbums(); },
                error -> Toast.makeText(this, "Failed to delete album", Toast.LENGTH_SHORT).show()
        );
        requestQueue.add(req);
    }

    private void saveTrack() {
        String artistCsv = etTrackArtistIds.getText().toString().trim();
        if (artistCsv.isEmpty()) {
            Toast.makeText(this, "Track must have at least one artist", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!validateArtistIds(artistCsv)) return;

        JSONObject body = new JSONObject();
        try {
            body.put("name", etTrackName.getText().toString().trim());
            body.put("mood", etTrackMood.getText().toString().trim());
            body.put("genre", etTrackGenre.getText().toString().trim());
            body.put("duration", parseIntSafe(etTrackDuration.getText().toString().trim()));
            body.put("trackNumber", parseIntSafe(etTrackTrackNumber.getText().toString().trim()));
            body.put("bpm", parseIntSafe(etTrackBPM.getText().toString().trim()));
            body.put("artistIds", csvToJSONArray(artistCsv));
            body.put("albumIds", csvToJSONArray(etTrackAlbumIds.getText().toString().trim()));
            body.put("spotifyId", selectedSpotifyId != null ? selectedSpotifyId : "");
            body.put("tags", new JSONArray());

            if (editingTrackId != null) body.put("id", editingTrackId);
        } catch (JSONException ignored) {}

        String url = editingTrackId == null ? BASE_URL + "/music/tracks/new" : BASE_URL + "/music/tracks/edit";
        int method = editingTrackId == null ? Request.Method.POST : Request.Method.PUT;

        JsonObjectRequest req = new JsonObjectRequest(method, url, body,
                response -> { Toast.makeText(this, "Track saved", Toast.LENGTH_SHORT).show(); closeTrackForm(); loadTracks(); },
                error -> Toast.makeText(this, "Failed to save track", Toast.LENGTH_SHORT).show()
        );
        requestQueue.add(req);
    }

    private void deleteTrack(Track track) {
        String url = BASE_URL + "/music/tracks/delete/" + track.getId();
        StringRequest req = new StringRequest(Request.Method.DELETE, url,
                response -> { Toast.makeText(this, "Track deleted", Toast.LENGTH_SHORT).show(); loadTracks(); },
                error -> Toast.makeText(this, "Failed to delete track", Toast.LENGTH_SHORT).show()
        );
        requestQueue.add(req);
    }

    private void saveArtist() {
        JSONObject body = new JSONObject();
        try {
            body.put("name", etArtistName.getText().toString().trim());
            body.put("bio", etArtistBio.getText().toString().trim());
            body.put("spotifyId", "");
            body.put("pictureUrl", JSONObject.NULL); // permanent null

            body.put("albumIds", csvToJSONArray(etArtistAlbumIds.getText().toString().trim()));
            body.put("trackIds", csvToJSONArray(etArtistTrackIds.getText().toString().trim()));
            body.put("years", csvToJSONArray(etArtistYears.getText().toString().trim()));
            body.put("tags", new JSONArray());

            // Picture as byte array (like albums)
            if (artistImageBase64 != null) {
                byte[] bytes = Base64.decode(artistImageBase64, Base64.DEFAULT);
                JSONArray byteArrayJson = new JSONArray();
                for (byte b : bytes) {
                    byteArrayJson.put(b & 0xFF);
                }
                body.put("picture", byteArrayJson);
            } else {
                body.put("picture", new JSONArray()); // empty array if no picture
            }

            if (editingArtistId != null) body.put("id", editingArtistId);

        } catch (JSONException ignored) {}

        String url = editingArtistId == null ? BASE_URL + "/music/artists/new" : BASE_URL + "/music/artists/edit";
        int method = editingArtistId == null ? Request.Method.POST : Request.Method.PUT;

        JsonObjectRequest req = new JsonObjectRequest(method, url, body,
                response -> {
                    Toast.makeText(this, "Artist saved", Toast.LENGTH_SHORT).show();
                    closeArtistForm();
                    loadArtists();
                },
                error -> Toast.makeText(this, "Failed to save artist", Toast.LENGTH_SHORT).show()
        );
        requestQueue.add(req);
    }

    private void deleteArtist(Artist artist) {
        String url = BASE_URL + "/music/artists/delete/" + artist.getId();
        StringRequest req = new StringRequest(Request.Method.DELETE, url,
                response -> { Toast.makeText(this, "Artist deleted", Toast.LENGTH_SHORT).show(); loadArtists(); },
                error -> Toast.makeText(this, "Failed to delete artist", Toast.LENGTH_SHORT).show()
        );
        requestQueue.add(req);
    }

    private boolean validateArtistIds(String csv) {
        if (csv == null || csv.trim().isEmpty()) return false;

        String[] ids = csv.split(",");
        for (String idStr : ids) {
            try {
                int id = Integer.parseInt(idStr.trim());
                if (!backendArtistIds.contains(id)) {
                    Toast.makeText(this, "Artist ID " + id + " does not exist", Toast.LENGTH_SHORT).show();
                    return false;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid artist ID: " + idStr, Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private String listToCSV(List<Integer> list) {
        if (list == null || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i : list) sb.append(i).append(",");
        return sb.substring(0, sb.length() - 1);
    }

    private JSONArray csvToJSONArray(String csv) throws JSONException {
        JSONArray arr = new JSONArray();
        if (csv == null || csv.trim().isEmpty()) return arr;

        String[] split = csv.split(",");
        for (String s : split) {
            s = s.trim();
            if (!s.isEmpty()) {
                try { arr.put(Integer.parseInt(s)); } catch (NumberFormatException ignored) {}
            }
        }
        return arr;
    }

    private String setToCSV(Set<Integer> set) {
        if (set == null || set.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i : set) sb.append(i).append(",");
        return sb.substring(0, sb.length() - 1);
    }

    private Album jsonToAlbum(JSONObject obj) {
        Album a = new Album();
        a.setId(obj.optInt("id"));
        a.setName(obj.optString("name"));
        a.setArtistIds(jsonArrayToSet(obj.optJSONArray("artistIds")));
        a.setTrackIds(jsonArrayToSet(obj.optJSONArray("trackIds")));
        a.setDuration(obj.has("duration") ? obj.optInt("duration") : null);
        a.setReleaseDate(obj.has("releaseDate") ? obj.optInt("releaseDate") : null);
        return a;
    }

    private Track jsonToTrack(JSONObject obj) {
        Track t = new Track();
        t.setId(obj.optInt("id"));
        t.setName(obj.optString("name"));
        t.setMood(obj.optString("mood"));
        t.setGenre(obj.optString("genre"));
        t.setDuration(obj.has("duration") ? obj.optInt("duration") : null);
        t.setTrackNumber(obj.has("trackNumber") ? obj.optInt("trackNumber") : null);
        t.setBpm(obj.has("bpm") ? obj.optInt("bpm") : null);
        t.setArtistIds(jsonArrayToSet(obj.optJSONArray("artistIds")));
        t.setAlbumIds(jsonArrayToSet(obj.optJSONArray("albumIds")));
        return t;
    }

    private Artist jsonToArtist(JSONObject obj) {
        Artist a = new Artist();
        a.setId(obj.optInt("id"));
        a.setName(obj.optString("name"));
        a.setBio(obj.optString("bio"));
        a.setYears(jsonArrayToList(obj.optJSONArray("years")));
        a.setAlbumIds(jsonArrayToSet(obj.optJSONArray("albumIds")));
        a.setTrackIds(jsonArrayToSet(obj.optJSONArray("trackIds")));
        // picture can be handled separately
        return a;
    }

    private List<Integer> jsonArrayToList(JSONArray arr) {
        List<Integer> list = new ArrayList<>();
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) list.add(arr.optInt(i));
        return list;
    }

    private Set<Integer> jsonArrayToSet(JSONArray arr) {
        Set<Integer> set = new HashSet<>();
        if (arr == null) return set;
        for (int i = 0; i < arr.length(); i++) set.add(arr.optInt(i));
        return set;
    }

    private void clearAlbumForm() {
        etAlbumName.setText("");
        etAlbumArtistIds.setText("");
        etAlbumTrackIds.setText("");
        etAlbumDuration.setText("");
        etAlbumReleaseDate.setText("");
        albumImageBase64 = null;
    }

    private void clearTrackForm() {
        etTrackName.setText("");
        etTrackMood.setText("");
        etTrackGenre.setText("");
        etTrackDuration.setText("");
        etTrackTrackNumber.setText("");
        etTrackBPM.setText("");
        etTrackArtistIds.setText("");
        etTrackAlbumIds.setText("");
        etTrackSearch.setText("");
        selectedSpotifyId = null;
    }

    private void clearArtistForm() {
        etArtistName.setText("");
        etArtistBio.setText("");
        etArtistAlbumIds.setText("");
        etArtistTrackIds.setText("");
        etArtistYears.setText("");
        artistImageBase64 = null;
    }

    private Integer parseIntSafe(String str) {
        try { return Integer.parseInt(str.trim()); } catch (Exception e) { return null; }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(data.getData());
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                byte[] bytes = baos.toByteArray();
                if (currentForm == ALBUM_FORM) albumImageBase64 = Base64.encodeToString(bytes, Base64.DEFAULT);
                else if (currentForm == ARTIST_FORM) artistImageBase64 = Base64.encodeToString(bytes, Base64.DEFAULT);

                Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

