package com.example.androidexample;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ViewListPage extends AppCompatActivity {

    private TextView titleText, descText;
    private ListView songListView;
    private Button editButton, backButton;

    private ArrayList<String> songTitles = new ArrayList<>();

    private int listId, userId;

    private static final String BASE_URL = "http://coms-3090-027.class.las.iastate.edu:8080";
    private static final String LISTS_ENDPOINT = BASE_URL + "/lists";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_list_page);

        titleText = findViewById(R.id.viewListTitle);
        descText = findViewById(R.id.viewListDesc);
        songListView = findViewById(R.id.viewListSongs);
        editButton = findViewById(R.id.editListButton);
        backButton = findViewById(R.id.viewListBack);

        listId = getIntent().getIntExtra("listId", -1);
        userId = getIntent().getIntExtra("userId", -1);

        if (listId == -1) {
            Toast.makeText(this, "Invalid list", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fetchListDetails();
        fetchListSongs();

        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(ViewListPage.this, CustomListPage.class);
            intent.putExtra("listId", listId);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        backButton.setOnClickListener(v -> finish());
    }

    private void fetchListDetails() {
        String url = LISTS_ENDPOINT + "/" + listId;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    titleText.setText(response.optString("title"));
                    descText.setText(response.optString("description"));
                },
                error -> Toast.makeText(this, "Failed to load list", Toast.LENGTH_SHORT).show()
        );

        QueueApplication.getQueue().add(request);
    }

    private void fetchListSongs() {
        String url = LISTS_ENDPOINT + "/" + listId + "/tracks";

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET, url, null,
                response -> {
                    songTitles.clear();

                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject song = response.getJSONObject(i);
                            songTitles.add(song.getString("name"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_list_item_1,
                            songTitles
                    );

                    songListView.setAdapter(adapter);
                },
                error -> {
                    Log.e("ViewListPage", "Failed to load songs");
                    Toast.makeText(this, "Failed to load songs", Toast.LENGTH_SHORT).show();
                }
        );

        QueueApplication.getQueue().add(request);
    }
}
