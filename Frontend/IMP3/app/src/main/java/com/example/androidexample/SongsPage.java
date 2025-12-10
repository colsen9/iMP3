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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SongsPage extends AppCompatActivity {
    private final String baseUrl = "http://coms-3090-027.class.las.iastate.edu:8080";
    private RequestQueue queue;
    private ImageView songCover;
    private TextView songTitle, songAlbum, songGenres, songLength, songMood, songBpm, songAvgRating, userReviewText;
    private LinearLayout artistContainer;
    private Button btnReturnCatalogue, btnReviewSong;
    private RatingBar songRatingBar;

    private int songId, albumId, userId;
    private String songTitleString = "Unknown Song";
    private int firstArtistId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.songs_page);

        // Initialize Volley
        queue = QueueApplication.getQueue();

        // Bind UI
        songCover = findViewById(R.id.songCover);
        songTitle = findViewById(R.id.songTitle);
        songAlbum = findViewById(R.id.songAlbum);
        artistContainer = findViewById(R.id.artistContainer);
        songGenres = findViewById(R.id.songGenres);
        songLength = findViewById(R.id.songLength);
        songMood = findViewById(R.id.songMood);
        songBpm = findViewById(R.id.songBpm);
        songAvgRating = findViewById(R.id.avgRatingText);
        songRatingBar = findViewById(R.id.songRatingBar);
        userReviewText = findViewById(R.id.userReviewText);
        btnReturnCatalogue = findViewById(R.id.btnReturnCatalogue);
        btnReviewSong = findViewById(R.id.btnReviewSong);

        // Get IDs
        songId = getIntent().getIntExtra("songId",-1);
        userId = getIntent().getIntExtra("userId",-1);

        if (songId != -1) loadSongData();

        btnReturnCatalogue.setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent().putExtra("userId", userId));
            finish();
        });

        btnReviewSong.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReviewsPage.class);
            intent.putExtra("itemId", songId);
            intent.putExtra("userId", userId);
            intent.putExtra("itemType", "tracks");
            intent.putExtra("itemName", songTitleString);
            intent.putExtra("albumId", albumId);
            startActivity(intent);
        });
    }

    /** Load TrackDTO info **/
    private void loadSongData() {
        String url = baseUrl + "/music/tracks/" + songId;

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        // Track info
                        songTitleString = response.optString("name", "Unknown Song");
                        songTitle.setText(songTitleString);
                        songGenres.setText("Genre: " + response.optString("genre", "Unknown"));
                        songLength.setText("Duration: " + formatDuration(response.optInt("duration",0)));
                        songMood.setText("Mood: " + response.optString("mood", "Unknown"));
                        songBpm.setText("BPM: " + response.optInt("bpm", 0));
                        songAvgRating.setText("Average: " + response.optDouble("averageRating", 0) + " stars");

                        // Artist(s)
                        JSONArray artistIds = response.optJSONArray("artistIds");
                        if (artistIds != null) fetchArtistNames(artistIds);

                        // Album(s)
                        JSONArray albumIds = response.optJSONArray("albumIds");
                        if (albumIds != null && albumIds.length() > 0) {
                            albumId = albumIds.getInt(0);
                            fetchAlbumData(albumId);
                        } else {
                            songAlbum.setText("Album: Unknown");
                        }

                        // User review
                        fetchUserReview();
                        fetchAverageRating();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    Toast.makeText(this, "Error loading song", Toast.LENGTH_SHORT).show();
                    error.printStackTrace();
                });

        queue.add(req);
    }

    /** Fetch album name & album art **/
    private void fetchAlbumData(int albumId) {
        String url = baseUrl + "/music/albums/" + albumId;
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    songAlbum.setText("Album: " + response.optString("name","Unknown Album"));

                    // Set album art
                    if (!response.isNull("albumArt")) {
                        Bitmap albumArtBitmap = decodeAlbumArt(response.opt("albumArt"));
                        if (albumArtBitmap != null) songCover.setImageBitmap(albumArtBitmap);
                    }

                    songAlbum.setOnClickListener(v -> {
                        Intent i = new Intent(this, AlbumsPage.class);
                        i.putExtra("albumId", albumId);
                        i.putExtra("userId", userId);
                        startActivity(i);
                    });
                },
                error -> {
                    songAlbum.setText("Album: Unknown");
                    error.printStackTrace();
                });
        queue.add(req);
    }

    /** Decode album art from JSONArray or Base64 String (API 24 compatible) **/
    private Bitmap decodeAlbumArt(Object artObj) {
        try {
            if (artObj instanceof JSONArray) {
                JSONArray arr = (JSONArray) artObj;
                byte[] bytes = new byte[arr.length()];
                for (int i = 0; i < arr.length(); i++) bytes[i] = (byte) arr.getInt(i);
                return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
            } else if (artObj instanceof String) {
                byte[] decoded = android.util.Base64.decode((String) artObj, android.util.Base64.DEFAULT);
                return BitmapFactory.decodeByteArray(decoded,0,decoded.length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Fetch artist names by IDs **/
    private void fetchArtistNames(JSONArray artistIds) throws JSONException {
        artistContainer.removeAllViews();
        for (int i = 0; i < artistIds.length(); i++) {
            int artistId = artistIds.getInt(i);
            String url = baseUrl + "/music/artists/" + artistId;

            JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                    res -> {
                        String name = res.optString("name", "Unknown Artist");
                        Button artistBtn = new Button(this);
                        artistBtn.setText(name);
                        styleArtistButton(artistBtn);

                        artistBtn.setOnClickListener(v -> {
                            Intent intent = new Intent(this, ArtistPage.class);
                            intent.putExtra("artistId", artistId);
                            intent.putExtra("userId", userId);
                            startActivity(intent);
                        });

                        artistContainer.addView(artistBtn);
                    },
                    error -> {
                        Button artistBtn = new Button(this);
                        artistBtn.setText("Unknown Artist");
                        styleArtistButton(artistBtn);
                        artistContainer.addView(artistBtn);
                    });
            queue.add(req);
        }
    }

    private void styleArtistButton(Button btn) {
        btn.setBackgroundResource(android.R.color.transparent);
        btn.setTextColor(getResources().getColor(android.R.color.darker_gray));
        btn.setTextSize(16);
        btn.setPadding(0,0,0,0);
        btn.setMinWidth(0);
        btn.setMinHeight(0);
        btn.setAllCaps(false);
    }

    private void fetchAverageRating() {
        String url = baseUrl + "/reviews/tracks/" + songId;

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.length() == 0) {
                            songAvgRating.setText("0 stars");
                            return;
                        }

                        double total = 0;
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject reviewObj = response.getJSONObject(i);
                            int rating = reviewObj.optInt("rating", 0);
                            total += rating;
                        }

                        double avg = total / response.length();
                        double avgStars = avg / 2.0;

                        songAvgRating.setText(String.format("%.1f stars", avgStars));

                    } catch (JSONException e) {
                        e.printStackTrace();
                        songAvgRating.setText("N/A");
                    }
                },
                error -> {
                    songAvgRating.setText("N/A");
                }
        );

        queue.add(req);
    }

    /** Fetch user review **/
    private void fetchUserReview() {
        if (userId == -1) {
            userReviewText.setText("User not logged in.");
            return;
        }

        String url = baseUrl + "/reviews/tracks/" + songId + "/" + userId;

        JsonArrayRequest req = new JsonArrayRequest(url,
                response -> {
                    if (response.length() > 0) {
                        try {
                            JSONObject review = response.getJSONObject(0); // take first review
                            float rating = (float) review.optDouble("rating", 0) / 2f;
                            songRatingBar.setStepSize(0.5f);
                            songRatingBar.setRating(rating);
                            userReviewText.setText(review.optString("review", "No review yet."));
                        } catch (JSONException e) {
                            e.printStackTrace();
                            userReviewText.setText("Error parsing review.");
                        }
                    } else {
                        userReviewText.setText("No review found for this song.");
                    }
                },
                error -> userReviewText.setText("No review found for this song.")
        );

        queue.add(req);
    }


    private String formatDuration(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", mins, secs);
    }
}










