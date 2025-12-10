package com.example.androidexample;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Base64;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class CustomListPage extends AppCompatActivity {

    private EditText titleInput, descInput;
    private Button saveButton, deleteButton;
    private ListView songListView;
    private ImageButton coverImageButton;
    private String coverImageBase64 = null;
    private RequestQueue queue;

    private ArrayList<Integer> selectedSongIds = new ArrayList<>();
    private ArrayList<JSONObject> allSongs = new ArrayList<>();
    private ArrayList<Integer> existingTrackIds = new ArrayList<>();

    private boolean songsLoaded = false;
    private boolean existingTracksLoaded = false;

    private int listId, userId;

    private static final String BASE_URL = "http://coms-3090-027.class.las.iastate.edu:8080";
    private static final String LISTS_ENDPOINT = BASE_URL + "/lists";
    private static final String SONGS_ENDPOINT = BASE_URL + "/music/tracks";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_list_page);

        titleInput = findViewById(R.id.titleInput);
        descInput = findViewById(R.id.descInput);
        coverImageButton = findViewById(R.id.coverImageButton);

        coverImageButton.setOnClickListener(v -> {
            Intent pickImage = new Intent(Intent.ACTION_PICK);
            pickImage.setType("image/*");
            startActivityForResult(pickImage, 1001);
        });

        songListView = findViewById(R.id.songListView);
        saveButton = findViewById(R.id.saveButton);
        deleteButton = findViewById(R.id.deleteButton);

        queue = QueueApplication.getQueue();
        listId = getIntent().getIntExtra("listId", -1);
        userId = getIntent().getIntExtra("userId",-1);

        if (listId != -1) {
            fetchListDetails(listId);
        } else {
            deleteButton.setVisibility(Button.GONE);
        }

        findViewById(R.id.custom_list_back).setOnClickListener(v -> {
            Intent back = new Intent();
            back.putExtra("userId", userId);
            setResult(RESULT_OK, back);
            finish();
        });

        fetchSongs();

        saveButton.setOnClickListener(v -> saveList());
        deleteButton.setOnClickListener(v -> deleteListWithTracks(listId));
    }

    private void fetchSongs() {
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                SONGS_ENDPOINT,
                null,
                this::populateSongs,
                error -> Toast.makeText(this, "Error fetching songs", Toast.LENGTH_SHORT).show()
        );
        queue.add(request);
    }

    private void populateSongs(JSONArray songs) {
        ArrayList<String> songTitles = new ArrayList<>();
        for (int i = 0; i < songs.length(); i++) {
            try {
                JSONObject song = songs.getJSONObject(i);
                songTitles.add(song.getString("name"));
                allSongs.add(song);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_multiple_choice,
                songTitles
        );

        songListView.setAdapter(adapter);
        songListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        songListView.setOnItemClickListener((parent, view, position, id) -> {
            try {
                int songId = allSongs.get(position).getInt("id");
                if (songListView.isItemChecked(position)) {
                    if (!selectedSongIds.contains(songId)) selectedSongIds.add(songId);
                } else {
                    selectedSongIds.remove(Integer.valueOf(songId));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        songsLoaded = true;
    }

    private void fetchListDetails(int id) {
        String url = LISTS_ENDPOINT + "/" + id;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    titleInput.setText(response.optString("title"));
                    descInput.setText(response.optString("description"));
                    fetchExistingTracks(id);
                },
                error -> Toast.makeText(this, "Error loading list", Toast.LENGTH_SHORT).show()
        );
        queue.add(request);
    }

    private void saveList() {
        try {
            JSONObject body = new JSONObject();
            body.put("title", titleInput.getText().toString());
            body.put("description", descInput.getText().toString());
            body.put("privacy", "PUBLIC");
            if (coverImageBase64 != null) body.put("coverImage", coverImageBase64);

            int method = (listId == -1) ? Request.Method.POST : Request.Method.PUT;
            String url = (listId == -1) ? LISTS_ENDPOINT : LISTS_ENDPOINT + "/" + listId;

            JsonObjectRequest request = new JsonObjectRequest(method, url, body,
                    response -> {
                        try {
                            int savedListId = response.getInt("listId");
                            addSongsToList(savedListId);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    },
                    error -> Toast.makeText(this, "Error saving list", Toast.LENGTH_SHORT).show()
            );

            queue.add(request);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addSongsToList(int listId) {
        for (Integer songId : selectedSongIds) {
            String url = LISTS_ENDPOINT + "/" + listId + "/songs/" + songId;
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, url, new JSONObject(),
                    response -> Log.d("AddSong", "Added song " + songId),
                    error -> Log.e("AddSong", "Failed to add song " + songId)
            );
            queue.add(request);
        }
        Toast.makeText(this, "List saved successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void fetchExistingTracks(int id) {
        String url = LISTS_ENDPOINT + "/" + id + "/tracks";
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    existingTrackIds.clear();
                    selectedSongIds.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            int trackId = response.getJSONObject(i).getInt("id");
                            existingTrackIds.add(trackId);
                            selectedSongIds.add(trackId);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    existingTracksLoaded = true;
                    if (songsLoaded) filterOutExistingSongs();
                },
                error -> Log.e("CustomListPage", "Error fetching existing tracks")
        );
        queue.add(request);
    }

    private void filterOutExistingSongs() {
        ArrayList<String> filteredTitles = new ArrayList<>();
        ArrayList<JSONObject> filteredSongs = new ArrayList<>();
        for (JSONObject song : allSongs) {
            try {
                int songId = song.getInt("id");
                if (!existingTrackIds.contains(songId)) {
                    filteredSongs.add(song);
                    filteredTitles.add(song.getString("name"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        allSongs.clear();
        allSongs.addAll(filteredSongs);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_multiple_choice,
                filteredTitles
        );
        songListView.setAdapter(adapter);
    }

    private void deleteListWithTracks(int listId) {
        // 1️⃣ Fetch all tracks
        JsonArrayRequest tracksRequest = new JsonArrayRequest(
                Request.Method.GET,
                LISTS_ENDPOINT + "/" + listId + "/tracks",
                null,
                tracksResponse -> {
                    if (tracksResponse.length() == 0) {
                        deleteListOnly(listId);
                        return;
                    }

                    final int[] deletedCount = {0};
                    for (int i = 0; i < tracksResponse.length(); i++) {
                        try {
                            int trackId = tracksResponse.getJSONObject(i).getInt("id");
                            String url = LISTS_ENDPOINT + "/" + listId + "/songs/" + trackId;

                            StringRequest deleteTrackRequest = new StringRequest(
                                    Request.Method.DELETE,
                                    url,
                                    response -> {
                                        deletedCount[0]++;
                                        if (deletedCount[0] == tracksResponse.length()) deleteListOnly(listId);
                                    },
                                    error -> {
                                        deletedCount[0]++;
                                        if (deletedCount[0] == tracksResponse.length()) deleteListOnly(listId);
                                    }
                            ) {
                                @Override
                                public java.util.Map<String, String> getHeaders() {
                                    java.util.Map<String, String> headers = new java.util.HashMap<>();
                                    headers.put("Cookie", "JSESSIONID=" + QueueApplication.getSessionId());
                                    return headers;
                                }
                            };
                            queue.add(deleteTrackRequest);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                error -> Toast.makeText(this, "Failed to fetch tracks", Toast.LENGTH_SHORT).show()
        );
        queue.add(tracksRequest);
    }

    private void deleteListOnly(int listId) {
        String url = LISTS_ENDPOINT + "/" + listId;
        StringRequest deleteRequest = new StringRequest(
                Request.Method.DELETE,
                url,
                response -> Toast.makeText(this, "List deleted successfully", Toast.LENGTH_SHORT).show(),
                error -> Toast.makeText(this, "Failed to delete list", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("Cookie", "JSESSIONID=" + QueueApplication.getSessionId());
                return headers;
            }
        };
        queue.add(deleteRequest);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 512, 512, true);
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.JPEG, 70, buffer);
                coverImageBase64 = Base64.encodeToString(buffer.toByteArray(), Base64.NO_WRAP);
                coverImageButton.setImageBitmap(scaled);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

