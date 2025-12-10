/** @Author Cayden Olsen **/

package com.example.androidexample;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.LruCache;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import android.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MusicCatalogue extends AppCompatActivity {
    private int userId;
    private final String baseUrl = "http://coms-3090-027.class.las.iastate.edu:8080/music/";
    private RequestQueue requestQueue;
    private ImageLoader imageLoader;
    private LinearLayout recommendedAlbumContainer, generalListContainer;
    private SearchView searchBar;
    private Button filterSongsBtn, filterAlbumsBtn, clearFiltersBtn, loginBtn;
    private JSONArray allAlbums = new JSONArray();
    private JSONArray allSongs = new JSONArray();
    private enum FilterMode {ALL, SONGS, ALBUMS}
    private FilterMode currentFilter = FilterMode.ALL;
    private Map<Integer, Bitmap> albumArtCache = new HashMap<>();
    private final Map<Integer, String> artistCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.music_catalogue);

        Intent intent = getIntent();
        userId = intent.getIntExtra("userId", -1);

        requestQueue = Volley.newRequestQueue(this);
        imageLoader = new ImageLoader(requestQueue, new ImageLoader.ImageCache() {
            private final LruCache<String, Bitmap> cache = new LruCache<>(30);

            @Override
            public Bitmap getBitmap(String url) {
                return cache.get(url);
            }

            @Override
            public void putBitmap(String url, Bitmap bitmap) {
                cache.put(url, bitmap);
            }
        });

        recommendedAlbumContainer = findViewById(R.id.recommendedAlbumContainer);
        generalListContainer = findViewById(R.id.generalListContainer);
        searchBar = findViewById(R.id.searchBar);
        filterSongsBtn = findViewById(R.id.filterSongsBtn);
        filterAlbumsBtn = findViewById(R.id.filterAlbumsBtn);
        clearFiltersBtn = findViewById(R.id.clearFiltersBtn);
        ImageButton ProfileButton = findViewById(R.id.profileBtn);
        ImageButton ChatButton = findViewById(R.id.chatBtn);
        ImageButton NotificationsButton = findViewById(R.id.notificationsBtn);
        ImageButton RecommendationsButton = findViewById(R.id.recommendationsBtn);
        ImageButton sotdButton = findViewById(R.id.sotdBtn); // Added by Graysen
        loginBtn = findViewById(R.id.login_btn);

        if (userId == -1) loginBtn.setVisibility(View.VISIBLE);
        else loginBtn.setVisibility(View.GONE);

        ProfileButton.setOnClickListener(v -> {
            Intent FollowerView = new Intent(MusicCatalogue.this, ProfilePage.class);
            FollowerView.putExtra("userId", userId);
            startActivity(FollowerView);
        });

        ChatButton.setOnClickListener(v -> {
            Intent chat = new Intent(MusicCatalogue.this, ChatActivity.class);
            chat.putExtra("userId", userId);
            startActivity(chat);
        });

        NotificationsButton.setOnClickListener(v -> {
            Intent notif = new Intent(MusicCatalogue.this, NotificationsPage.class);
            notif.putExtra("userId", userId);
            startActivity(notif);
        });

        // Button added by Graysen
        sotdButton.setOnClickListener(v -> {
            Intent sotd = new Intent(MusicCatalogue.this, DayTrackPage.class);
            sotd.putExtra("userId", userId);
            startActivity(sotd);
        });

        RecommendationsButton.setOnClickListener(v -> {
            Intent reco = new Intent(MusicCatalogue.this, RecommendationsPage.class);
            reco.putExtra("userId", userId);
            startActivity(reco);
        });

        loginBtn.setOnClickListener(v -> startActivity(new Intent(MusicCatalogue.this, LoginPage.class)));

        fetchAlbums();
        fetchSongs();

        filterSongsBtn.setOnClickListener(v -> {
            currentFilter = FilterMode.SONGS;
            refreshGeneralList();
        });
        filterAlbumsBtn.setOnClickListener(v -> {
            currentFilter = FilterMode.ALBUMS;
            refreshGeneralList();
        });
        clearFiltersBtn.setOnClickListener(v -> {
            currentFilter = FilterMode.ALL;
            searchBar.setQuery("", false);
            refreshGeneralList();
        });

        searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterSearch(newText);
                return true;
            }
        });
    }

    // Fetch Data
    private void fetchAlbums() {
        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, baseUrl + "albums", null,
                response -> {
                    allAlbums = response;
                    buildAlbumArtCache();
                    fillRecommendedAlbums();
                    refreshGeneralList();
                },
                error -> Log.e("MusicCatalogue", "Error fetching albums", error));
        requestQueue.add(req);
    }

    private void fetchSongs() {
        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, baseUrl + "tracks", null,
                response -> {
                    allSongs = response;
                    refreshGeneralList();
                },
                error -> Log.e("MusicCatalogue", "Error fetching songs", error));
        requestQueue.add(req);
    }

    //Album Art
    private Bitmap decodeAlbumArt(JSONObject item) {
        if (!item.has("albumArt") || item.isNull("albumArt")) return null;

        try {
            Object art = item.get("albumArt");
            if (art instanceof JSONArray) {
                JSONArray arr = (JSONArray) art;
                byte[] bytes = new byte[arr.length()];
                for (int i = 0; i < arr.length(); i++) {
                    bytes[i] = (byte) arr.optInt(i, 0);
                }
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            } else if (art instanceof String) {
                byte[] decodedBytes = Base64.decode((String) art, Base64.DEFAULT);
                return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void buildAlbumArtCache() {
        albumArtCache.clear();
        for (int i = 0; i < allAlbums.length(); i++) {
            JSONObject album = allAlbums.optJSONObject(i);
            if (album == null) continue;

            int albumId = album.optInt("id", -1);
            if (albumId == -1) continue;

            Bitmap albumBitmap = decodeAlbumArt(album);
            albumArtCache.put(albumId, albumBitmap);
        }
    }

    // Recommended Albums
    private void fillRecommendedAlbums() {
        recommendedAlbumContainer.removeAllViews();
        int count = Math.min(allAlbums.length(), 6);

        // Convert JSONArray to List
        List<JSONObject> albumList = new ArrayList<>();
        for (int i = 0; i < allAlbums.length(); i++) {
            albumList.add(allAlbums.optJSONObject(i));
        }

        Collections.shuffle(albumList);

        for (int i = 0; i < count; i++) {
            JSONObject album = albumList.get(i);
            if (album == null) continue;

            int albumId = album.optInt("id", -1);
            Bitmap albumBitmap = albumArtCache.get(albumId);

            View card = createCard(album, true, albumBitmap);
            recommendedAlbumContainer.addView(card);
        }
    }

    private Bitmap getSongAlbumArt(JSONObject song) {
        JSONArray albumIds = song.optJSONArray("albumIds");
        if (albumIds != null) {
            for (int j = 0; j < albumIds.length(); j++) {
                int albumId = albumIds.optInt(j, -1);
                Bitmap art = albumArtCache.get(albumId);
                if (art != null) return art;
            }
        }
        return null;
    }

    private View createCard(JSONObject item, boolean isAlbum, Bitmap albumArt) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View card = inflater.inflate(R.layout.card_music, null);

        ImageView img = card.findViewById(R.id.cardImage);
        TextView title = card.findViewById(R.id.cardTitle);
        TextView artist = card.findViewById(R.id.cardArtist);

        int id = item.optInt("id");
        String titleStr = item.optString("name", "Untitled");
        Object artistIds = item.opt("artistIds");

        if (albumArt != null) {
            img.setImageBitmap(albumArt);
        } else {
            img.setImageResource(R.drawable.imp3);
        }

        title.setText(titleStr);
        artist.setText("Loading artist...");
        fetchArtistNames(artistIds, artist);

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, isAlbum ? AlbumsPage.class : SongsPage.class);
            intent.putExtra(isAlbum ? "albumId" : "songId", id);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        return card;
    }

    private void fetchArtistNames(Object artistIds, TextView artistTextView) {
        if (artistIds == null) {
            artistTextView.setText("Unknown");
            return;
        }

        try {
            if (artistIds instanceof JSONArray) {
                JSONArray idsArray = (JSONArray) artistIds;
                StringBuilder artistNames = new StringBuilder();

                for (int i = 0; i < idsArray.length(); i++) {
                    int artistId = idsArray.optInt(i, -1);
                    if (artistId == -1) continue;
                    if (artistCache.containsKey(artistId)) {
                        if (artistNames.length() > 0) artistNames.append(", ");
                        artistNames.append(artistCache.get(artistId));
                        artistTextView.setText(artistNames.toString());
                        continue;
                    }

                    String url = baseUrl + "artists/" + artistId;
                    JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                            response -> {
                                String name = response.optString("name", "Unknown");
                                artistCache.put(artistId, name);
                                if (artistNames.length() > 0) artistNames.append(", ");
                                artistNames.append(name);
                                artistTextView.setText(artistNames.toString());
                            },
                            error -> Log.e("MusicCatalogue", "Error fetching artistId=" + artistId, error)
                    );
                    requestQueue.add(req);
                }
            }
        } catch (Exception e) {
            artistTextView.setText("Unknown");
        }
    }

    // General Catalogue
    private void refreshGeneralList() {
        generalListContainer.removeAllViews();
        List<JSONObject> combinedList = new ArrayList<>();

        try {
            if (currentFilter == FilterMode.ALL || currentFilter == FilterMode.ALBUMS)
                for (int i = 0; i < allAlbums.length(); i++)
                    combinedList.add(allAlbums.getJSONObject(i).put("type", "album"));
            if (currentFilter == FilterMode.ALL || currentFilter == FilterMode.SONGS)
                for (int i = 0; i < allSongs.length(); i++)
                    combinedList.add(allSongs.getJSONObject(i).put("type", "song"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Collections.shuffle(combinedList);

        JSONArray combined = new JSONArray();
        for (JSONObject obj : combinedList) combined.put(obj);

        addItemsToGrid(combined);
    }

    private void filterSearch(String query) {
        query = query.toLowerCase().trim();
        if (query.isEmpty()) {
            refreshGeneralList();
            return;
        }

        JSONArray filtered = new JSONArray();
        try {
            JSONArray source = new JSONArray();
            if (currentFilter == FilterMode.ALL || currentFilter == FilterMode.ALBUMS)
                for (int i = 0; i < allAlbums.length(); i++)
                    source.put(allAlbums.getJSONObject(i).put("type", "album"));
            if (currentFilter == FilterMode.ALL || currentFilter == FilterMode.SONGS)
                for (int i = 0; i < allSongs.length(); i++)
                    source.put(allSongs.getJSONObject(i).put("type", "song"));

            for (int i = 0; i < source.length(); i++) {
                JSONObject obj = source.getJSONObject(i);
                if (obj.optString("name", "").toLowerCase().contains(query))
                    filtered.put(obj);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        generalListContainer.removeAllViews();
        addItemsToGrid(filtered);
    }

    private void addItemsToGrid(JSONArray items) {
        int perRow = 3;
        LinearLayout currentRow = null;

        for (int i = 0; i < items.length(); i++) {
            if (i % perRow == 0) {
                currentRow = new LinearLayout(this);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                currentRow.setWeightSum(perRow);
                generalListContainer.addView(currentRow);
            }

            try {
                JSONObject item = items.getJSONObject(i);
                boolean isAlbum = "album".equals(item.optString("type"));

                // Determine the correct album art
                Bitmap albumArtBitmap;
                if (isAlbum) {
                    int albumId = item.optInt("id", -1);
                    albumArtBitmap = albumArtCache.get(albumId);
                } else {
                    albumArtBitmap = getSongAlbumArt(item); // <-- important for songs
                }

                View card = createCard(item, isAlbum, albumArtBitmap);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                );
                params.setMargins(4, 4, 4, 4);
                card.setLayoutParams(params);

                if (currentRow != null) {
                    currentRow.addView(card);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}




















