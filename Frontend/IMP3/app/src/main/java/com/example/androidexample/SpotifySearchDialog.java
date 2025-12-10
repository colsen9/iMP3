/** @author Cayden Olsen **/

package com.example.androidexample;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpotifySearchDialog {

    public interface OnTrackSelectedListener {
        void onTrackSelected(String trackId, String trackName);
    }

    private final Context context;
    private final String spotifyToken;
    private final OnTrackSelectedListener listener;

    public SpotifySearchDialog(@NonNull Context context, String spotifyToken, OnTrackSelectedListener listener) {
        this.context = context;
        this.spotifyToken = spotifyToken;
        this.listener = listener;
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Search Spotify");

        View layout = LayoutInflater.from(context).inflate(R.layout.dialog_spotify_search, null);
        builder.setView(layout);

        EditText inputSearch = layout.findViewById(R.id.input_search);
        ListView listView = layout.findViewById(R.id.list_tracks);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1);
        listView.setAdapter(adapter);

        List<String> trackIds = new ArrayList<>();

        Button btnSearch = layout.findViewById(R.id.btn_search);
        btnSearch.setOnClickListener(v -> {
            String query = inputSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                searchSpotify(query, adapter, trackIds);
            }
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String trackId = trackIds.get(position);
            String trackName = adapter.getItem(position);
            listener.onTrackSelected(trackId, trackName);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void searchSpotify(String query, ArrayAdapter<String> adapter, List<String> trackIds) {
        adapter.clear();
        trackIds.clear();

        String url = "https://api.spotify.com/v1/search?q=" + query.replace(" ", "%20") + "&type=track&limit=10";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray tracks = response.getJSONObject("tracks").getJSONArray("items");
                        for (int i = 0; i < tracks.length(); i++) {
                            JSONObject track = tracks.getJSONObject(i);
                            String name = track.getString("name");
                            String id = track.getString("id");
                            JSONArray artists = track.getJSONArray("artists");
                            String artistName = artists.getJSONObject(0).getString("name");

                            adapter.add(name + " - " + artistName);
                            trackIds.add(id);
                        }
                        adapter.notifyDataSetChanged();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(context, "Failed to parse Spotify response", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(context, "Failed to search Spotify", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + spotifyToken);
                return headers;
            }
        };

        QueueApplication.getQueue().add(request);
    }
}
