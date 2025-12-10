/** @author Cayden Olsen **/

package com.example.androidexample;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Base64;

public class AlbumsPage extends AppCompatActivity {

    private int userId;
    private int albumId;
    private int artistId = -1;
    private final String baseUrl = "http://coms-3090-027.class.las.iastate.edu:8080";
    private RequestQueue queue;

    private ImageView albumCover;
    private TextView albumTitle, releaseDateText, durationText, avgRatingText, userReviewText;
    private Button albumArtistBtn, btnReturnCatalogue, btnReviewAlbum;
    private LinearLayout songListContainer;
    private RatingBar ratingBar;

    private String albumNameValue = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.albums_page);

        // Initialize Views
        albumCover = findViewById(R.id.albumCover);
        albumTitle = findViewById(R.id.albumTitle);
        albumArtistBtn = findViewById(R.id.albumArtistBtn);
        releaseDateText = findViewById(R.id.releaseDateText);
        durationText = findViewById(R.id.durationText);
        avgRatingText = findViewById(R.id.avgRatingText);
        userReviewText = findViewById(R.id.userReviewText);
        songListContainer = findViewById(R.id.songListContainer);
        ratingBar = findViewById(R.id.albumRatingBar);
        btnReturnCatalogue = findViewById(R.id.btnReturnCatalogue);
        btnReviewAlbum = findViewById(R.id.btnReviewAlbum);

        queue = Volley.newRequestQueue(this);

        Intent intent = getIntent();
        userId = intent.getIntExtra("userId", -1);
        albumId = intent.getIntExtra("albumId", -1);

        if (albumId != -1) {
            loadAlbumData();
        } else {
            Toast.makeText(this, "No albumId found!", Toast.LENGTH_SHORT).show();
        }

        btnReturnCatalogue.setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent().putExtra("userId", userId));
            finish();
        });

        btnReviewAlbum.setOnClickListener(v -> {
            Intent reviewIntent = new Intent(this, ReviewsPage.class);
            reviewIntent.putExtra("itemId", -1);
            reviewIntent.putExtra("albumId", albumId);
            reviewIntent.putExtra("itemName", albumNameValue);
            reviewIntent.putExtra("itemType", "albums");
            reviewIntent.putExtra("userId", userId);
            startActivity(reviewIntent);
        });

        albumArtistBtn.setOnClickListener(v -> {
            if (artistId != -1) {
                Intent artistIntent = new Intent(this, ArtistPage.class);
                artistIntent.putExtra("artistId", artistId);
                startActivity(artistIntent);
            } else {
                Toast.makeText(this, "Artist not found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadAlbumData() {
        String url = baseUrl + "/music/albums/" + albumId;

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        // Album name
                        albumNameValue = response.optString("name", "Unknown Album");
                        albumTitle.setText(albumNameValue);

                        // Album duration
                        int durationSec = response.optInt("duration", 0);
                        durationText.setText("Duration: " + formatDuration(durationSec));

                        // Release date
                        int release = response.optInt("releaseDate", 0);
                        releaseDateText.setText("Released: " + release);

                        // Album art
                        if (!response.isNull("albumArt")) {
                            Bitmap artBitmap = decodeAlbumArt(response.get("albumArt"));
                            if (artBitmap != null) albumCover.setImageBitmap(artBitmap);
                        }

                        // Artist(s)
                        if (!response.isNull("artistIds")) {
                            JSONArray artistsArray = response.getJSONArray("artistIds");
                            if (artistsArray.length() > 0) {
                                artistId = artistsArray.getInt(0);
                                fetchArtistName(artistId);
                            }
                        } else {
                            albumArtistBtn.setText("Unknown Artist");
                        }

                        // Songs
                        if (!response.isNull("trackIds")) {
                            JSONArray trackIds = response.getJSONArray("trackIds");
                            fetchSongNames(trackIds);
                        }

                        avgRatingText.setText(response.optDouble("averageRating", 0) + " stars");

                        // User review
                        fetchUserReview();
                        fetchAverageRating();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "Failed to load album", Toast.LENGTH_SHORT).show()
        );

        queue.add(req);
    }

    private Bitmap decodeAlbumArt(Object artObj) {
        try {
            if (artObj instanceof JSONArray) {
                JSONArray arr = (JSONArray) artObj;
                byte[] bytes = new byte[arr.length()];
                for (int i = 0; i < arr.length(); i++) {
                    bytes[i] = (byte) arr.getInt(i);
                }
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            } else if (artObj instanceof String) {
                byte[] decoded = android.util.Base64.decode((String) artObj, android.util.Base64.DEFAULT);
                return BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void fetchArtistName(int artistId) {
        String url = baseUrl + "/music/artists/" + artistId;
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> albumArtistBtn.setText(response.optString("name", "Unknown Artist")),
                error -> albumArtistBtn.setText("Unknown Artist")
        );
        queue.add(req);
    }

    private void fetchSongNames(JSONArray songIds) throws JSONException {
        songListContainer.removeAllViews();
        for (int i = 0; i < songIds.length(); i++) {
            int songId = songIds.getInt(i);
            String songUrl = baseUrl + "/music/tracks/" + songId;

            JsonObjectRequest songReq = new JsonObjectRequest(Request.Method.GET, songUrl, null,
                    response -> {
                        String songName = response.optString("name", "Unknown Song");
                        TextView songView = new TextView(this);
                        songView.setText("• " + songName);
                        songView.setPadding(0, 4, 0, 4);
                        songView.setTextSize(16);

                        songView.setOnClickListener(v -> {
                            Intent intent = new Intent(this, SongsPage.class);
                            intent.putExtra("songId", songId);
                            startActivity(intent);
                        });

                        songListContainer.addView(songView);
                    },
                    error -> {
                        TextView songView = new TextView(this);
                        songView.setText("• (Error loading song)");
                        songListContainer.addView(songView);
                    }
            );
            queue.add(songReq);
        }
    }

    private void fetchAverageRating() {
        String url = baseUrl + "/reviews/albums/" + albumId;

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.length() == 0) {
                            avgRatingText.setText("0 stars");
                            return;
                        }

                        double total = 0;
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject reviewObj = response.getJSONObject(i);
                            int rating = reviewObj.optInt("rating", 0);
                            total += rating;
                        }

                        // Backend ratings are /10, display as /5 stars
                        double avg = total / response.length();
                        double avgStars = avg / 2.0;

                        avgRatingText.setText(String.format("%.1f stars", avgStars));

                    } catch (JSONException e) {
                        e.printStackTrace();
                        avgRatingText.setText("N/A");
                    }
                },
                error -> {
                    avgRatingText.setText("N/A");
                }
        );

        queue.add(req);
    }

    private void fetchUserReview() {
        String url = baseUrl + "/reviews/albums/" + albumId + "/" + userId;

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    if (response.length() == 0) {
                        userReviewText.setText("No review yet.");
                        ratingBar.setRating(0);
                        return;
                    }

                    try {
                        JSONObject reviewObj = response.getJSONObject(0);

                        int rating = reviewObj.optInt("rating", 0);
                        ratingBar.setRating((float) rating / 2f);

                        String combined = reviewObj.optString("review", "No review yet.");
                        String[] parts = combined.split("\\|\\|", 2);
                        userReviewText.setText(parts.length > 0 ? parts[0] : combined);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        userReviewText.setText("Error reading review.");
                    }
                },
                error -> {
                    userReviewText.setText("No review found for this album.");
                    ratingBar.setRating(0);
                }
        );

        queue.add(req);
    }

    private String formatDuration(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", mins, secs);
    }
}





