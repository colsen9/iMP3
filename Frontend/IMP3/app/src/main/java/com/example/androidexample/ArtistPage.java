/** @author Cayden Olsen **/

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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.HashMap;
import java.util.Map;

public class ArtistPage extends AppCompatActivity {
    private final String baseUrl = "http://coms-3090-027.class.las.iastate.edu:8080";
    private RequestQueue requestQueue;
    private Button btnReturn;
    private ImageLoader imageLoader;
    private int artistId, userId;
    private ImageView artistImage;
    private TextView artistName, artistYears, artistBio;
    private LinearLayout albumContainer, songContainer;
    private final Map<Integer, String> artistCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.artist_page);

        // Initialize UI
        artistImage = findViewById(R.id.artistImage);
        artistName = findViewById(R.id.artistName);
        artistYears = findViewById(R.id.artistYears);
        artistBio = findViewById(R.id.artistBio);
        albumContainer = findViewById(R.id.albumContainer);
        songContainer = findViewById(R.id.songContainer);
        btnReturn = findViewById(R.id.btnReturn);

        // Init Volley queue and image loader (same as MusicCatalogue)
        requestQueue = Volley.newRequestQueue(this);
        imageLoader = new ImageLoader(requestQueue, new ImageLoader.ImageCache() {
            private final LruCache<String, Bitmap> cache = new LruCache<>(30);
            @Override public Bitmap getBitmap(String url) { return cache.get(url); }
            @Override public void putBitmap(String url, Bitmap bitmap) { cache.put(url, bitmap); }
        });

        // Get artistId and userId
        Intent intent = getIntent();
        artistId = intent.getIntExtra("artistId", -1);
        userId = intent.getIntExtra("userId", -1);

        if (artistId != -1) fetchArtistInfo(artistId);
        else Toast.makeText(this, "Invalid artist", Toast.LENGTH_SHORT).show();

        btnReturn.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("userId", userId);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    /** Fetch artist info from backend **/
    private void fetchArtistInfo(int artistId) {
        String url = baseUrl + "/music/artists/" + artistId;
        Log.d("ArtistPage", "Fetching artist info: " + url);

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    Log.d("ArtistPage", "Artist JSON response: " + response.toString());

                    // Name and bio
                    artistName.setText(response.optString("name", "Unknown Artist"));
                    artistBio.setText(response.optString("bio", "No biography available."));

                    loadArtistPicture(response.optString("picture", null), artistImage);

                    // Years (List<Integer> -> JSON array)
                    JSONArray yearsArray = response.optJSONArray("years");
                    if (yearsArray != null && yearsArray.length() > 0) {
                        StringBuilder yearsStr = new StringBuilder();
                        for (int i = 0; i < yearsArray.length(); i++) {
                            yearsStr.append(yearsArray.optInt(i));
                            if (i < yearsArray.length() - 1) yearsStr.append(", ");
                        }
                        artistYears.setText("Years Active: " + yearsStr);
                    } else {
                        artistYears.setText("Years Active: N/A");
                    }

                    // Albums, tracks, tags
                    JSONArray albumIds = response.optJSONArray("albumIds");
                    JSONArray trackIds = response.optJSONArray("trackIds");
                    JSONArray tagsArray = response.optJSONArray("tags");

                    if (albumIds != null) fetchAlbums(albumIds);
                    if (trackIds != null) fetchSongs(trackIds);
                    if (tagsArray != null) displayTags(tagsArray);

                },
                error -> {
                    Log.e("ArtistPage", "Error fetching artist info", error);
                    Toast.makeText(this, "Error loading artist info", Toast.LENGTH_SHORT).show();
                });
        requestQueue.add(req);
    }

    /** Fetch albums in artist's discography **/
    private void fetchAlbums(JSONArray albumIds) {
        albumContainer.removeAllViews();
        for (int i = 0; i < albumIds.length(); i++) {
            int id = albumIds.optInt(i, -1);
            if (id == -1) continue;

            String url = baseUrl + "/music/albums/" + id;
            JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                    response -> {
                        try {
                            View card = createCard(response, true);
                            albumContainer.addView(card);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    },
                    error -> Log.e("ArtistPage", "Error fetching album " + id, error));
            requestQueue.add(req);
        }
    }

    /** Fetch songs in artist's discography **/
    private void fetchSongs(JSONArray songIds) {
        songContainer.removeAllViews();
        for (int i = 0; i < songIds.length(); i++) {
            int id = songIds.optInt(i, -1);
            if (id == -1) continue;

            String url = baseUrl + "/music/tracks/" + id;
            JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                    response -> {
                        try {
                            View card = createCard(response, false);
                            songContainer.addView(card);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    },
                    error -> Log.e("ArtistPage", "Error fetching song " + id, error));
            requestQueue.add(req);
        }
    }

    /** Create a card for album or song **/
    private LinearLayout createCard(JSONObject item, boolean isAlbum) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(16, 16, 16, 16);

        ImageView img = new ImageView(this);
        img.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
        card.addView(img);

        // Text container
        LinearLayout textBox = new LinearLayout(this);
        textBox.setOrientation(LinearLayout.VERTICAL);
        textBox.setPadding(16, 0, 0, 0);
        card.addView(textBox);

        // Name
        TextView name = new TextView(this);
        name.setText(item.optString("name", "Unknown"));
        name.setTextSize(18);
        textBox.addView(name);

        TextView artistText = new TextView(this);
        artistText.setText("Loading artist...");
        textBox.addView(artistText);

        int id = item.optInt("id", -1);

        // Navigate
        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, isAlbum ? AlbumsPage.class : SongsPage.class);
            intent.putExtra(isAlbum ? "albumId" : "songId", id);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        if (isAlbum) {
            Object albumArtObj = item.opt("albumArt");
            String albumArtBase64 = albumArtObj != null && albumArtObj != JSONObject.NULL ? albumArtObj.toString() : null;
            decodePicture(albumArtBase64, img);
        } else {
            JSONArray albumIds = item.optJSONArray("albumIds");
            if (albumIds != null && albumIds.length() > 0) {
                int albumId = albumIds.optInt(0, -1);
                loadAlbumArtForTrack(albumId, img);
            } else {
                img.setImageResource(R.drawable.imp3);
            }
        }

        // Load artist names
        fetchArtistNames(item.opt("artistIds"), artistText);

        return card;
    }

    /** Display tags in the UI **/
    private void displayTags(JSONArray tagsArray) {
        LinearLayout tagsContainer = findViewById(R.id.tagsContainer);
        tagsContainer.removeAllViews();

        for (int i = 0; i < tagsArray.length(); i++) {
            String tag = tagsArray.optString(i, null);
            if (tag == null) continue;

            TextView tagView = new TextView(this);
            tagView.setText(tag);
            tagView.setPadding(8, 4, 8, 4);
            tagView.setBackgroundResource(R.drawable.tag_background); // optional rounded bg
            tagsContainer.addView(tagView);
        }
    }

    private void decodePicture(Object picObj, ImageView imageView) {
        if (picObj == null || picObj == JSONObject.NULL) {
            imageView.setImageResource(R.drawable.imp3);
            return;
        }

        try {
            byte[] bytes;

            if (picObj instanceof JSONArray) {
                JSONArray arr = (JSONArray) picObj;
                bytes = new byte[arr.length()];
                for (int i = 0; i < arr.length(); i++) {
                    bytes[i] = (byte)(arr.getInt(i) & 0xFF);  // <-- IMPORTANT: mask to 0–255
                }
            } else if (picObj instanceof String) {
                bytes = android.util.Base64.decode((String) picObj, android.util.Base64.DEFAULT);
            } else {
                imageView.setImageResource(R.drawable.imp3);
                return;
            }

            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bmp != null) imageView.setImageBitmap(bmp);
            else imageView.setImageResource(R.drawable.imp3);

        } catch (Exception e) {
            e.printStackTrace();
            imageView.setImageResource(R.drawable.imp3);
        }
    }

    /** Fetch artist names for the card **/
    private void fetchArtistNames(Object artistIds, TextView artistTextView) {

        if (artistIds == null || artistIds == JSONObject.NULL) {
            artistTextView.setText("Unknown");
            return;
        }

        // Case A: JSONArray of IDs
        if (artistIds instanceof JSONArray) {
            JSONArray arr = (JSONArray) artistIds;
            if (arr.length() == 0) {
                artistTextView.setText("Unknown");
                return;
            }

            final StringBuilder sb = new StringBuilder();
            final int n = arr.length();

            for (int i = 0; i < n; i++) {
                final int idx = i;
                final int aid = arr.optInt(i, -1);
                if (aid == -1) continue;

                fetchArtistNameById(aid, name -> {
                    // append with comma when needed
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(name);
                    artistTextView.setText(sb.toString());
                });
            }
            return;
        }

        if (artistIds instanceof Number) {
            int id = ((Number) artistIds).intValue();
            fetchArtistNameById(id, artistTextView::setText);
            return;
        }

        try {
            int id = Integer.parseInt(artistIds.toString());
            fetchArtistNameById(id, artistTextView::setText);
        } catch (Exception e) {
            artistTextView.setText("Unknown");
        }
    }

    /** Fetch a single artist's name by id and return it via callback **/
    private void fetchArtistNameById(int artistId, NameCallback callback) {
        if (artistId == -1) {
            callback.onName("Unknown");
            return;
        }

        // Check cache first
        if (artistCache.containsKey(artistId)) {
            callback.onName(artistCache.get(artistId));
            return;
        }

        String url = baseUrl + "/music/artists/" + artistId;
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    String name = response.optString("name", "Unknown");
                    artistCache.put(artistId, name);
                    callback.onName(name);
                },
                error -> {
                    Log.e("ArtistPage", "Error fetching artistId=" + artistId, error);
                    callback.onName("Unknown");
                });
        requestQueue.add(req);
    }

    /** Loads albumArt for a track by fetching its album **/
    private void loadAlbumArtForTrack(int albumId, ImageView img) {
        if (albumId == -1) {
            img.setImageResource(R.drawable.imp3);
            return;
        }

        String url = baseUrl + "/music/albums/" + albumId;
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    Object albumArtObj = response.opt("albumArt");
                    String albumArtBase64 = albumArtObj != null && albumArtObj != JSONObject.NULL ? albumArtObj.toString() : null;
                    decodePicture(albumArtBase64, img);
                },
                error -> img.setImageResource(R.drawable.imp3)
        );
        requestQueue.add(req);
    }

    private void loadArtistPicture(String base64, ImageView imageView) {

        if (base64 == null || base64.equals("null") || base64.isEmpty()) {
            imageView.setImageResource(R.drawable.imp3);
            return;
        }

        try {
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);

            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

            if (bmp != null) {
                imageView.setImageBitmap(bmp);
            } else {
                imageView.setImageResource(R.drawable.imp3);
            }

        } catch (Exception e) {
            e.printStackTrace();
            imageView.setImageResource(R.drawable.imp3);
        }
    }

    /** Simple callback interface used for returning an artist's name asynchronously */
    private interface NameCallback {
        void onName(String name);
    }
}


