package com.example.androidexample;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DayTrackPage extends AppCompatActivity {

    private RequestQueue requestQueue;
    private final String serverUrl = "http://coms-3090-027.class.las.iastate.edu:8080";

    private int userId;
    private boolean userAdmin = false;
    private LocalDate lastSelectedDate;

    private int sotdId = -1;
    private int sotdTrackId = -1;
    private int sotdScore = 0;
    private List<Integer> sotdVoters = new ArrayList<>();

    private Bitmap sotdTrackArt = null;
    private String sotdTitle = "No Song of the Day";
    private String sotdArtist = "Please check again tomorrow!";

    private Integer[] artistIds = new Integer[]{};
    private Integer[] albumId = new Integer[]{};

    private boolean userVoted = false;

    // UI
    private ImageView SOTD_Art;
    private TextView SOTD_Title;
    private TextView SOTD_Artist;
    private Button SOTD_VotesRefresh;
    private ImageButton SOTD_Upvote;
    private ImageButton SOTD_Downvote;
    private Button SOTD_AdminDate;
    private EditText SOTD_AdminTrackID;
    private Button SOTD_AdminDelete;
    private Button SOTD_Admin_SetSong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        userId = intent.getIntExtra("userId", -1);

        requestQueue = QueueApplication.getQueue();

        setContentView(R.layout.daytrack_page);

        initUI();
        getUserInfo(this::startup);
    }

    private void initUI() {
        SOTD_Art = findViewById(R.id.SOTD_AlbumArt);
        SOTD_Title = findViewById(R.id.SOTD_SongTitle);
        SOTD_Artist = findViewById(R.id.SOTD_ArtistName);
        SOTD_VotesRefresh = findViewById(R.id.SOTD_VotesRefresh);
        SOTD_Upvote = findViewById(R.id.SOTD_Upvote);
        SOTD_Downvote = findViewById(R.id.SOTD_Downvote);
        SOTD_AdminDate = findViewById(R.id.SOTD_AdminDate);
        SOTD_AdminTrackID = findViewById(R.id.SOTD_AdminTrackID);
        SOTD_AdminDelete = findViewById(R.id.SOTD_AdminDelete);
        SOTD_Admin_SetSong = findViewById(R.id.SOTD_AdminSetSong);

        findViewById(R.id.SOTD_BackButton).setOnClickListener(view -> {
            Intent back = new Intent();
            back.putExtra("userId", userId);
            setResult(RESULT_OK, back);
            finish();
        });

        SOTD_VotesRefresh.setOnClickListener(v -> {
            updateSOTDInformation(() -> renderScore());
        });

        SOTD_Upvote.setOnClickListener(v -> vote(true));
        SOTD_Downvote.setOnClickListener(v -> vote(false));

        SOTD_AdminDate.setText("Date Unselected");
        SOTD_AdminDate.setOnClickListener(v -> {
            LocalDate today = null;
            int year = 0;
            int month = 0; // DatePicker months are 0-based
            int day = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                today = LocalDate.now();
                year = today.getYear();
                month = today.getMonthValue() - 1;
                day = today.getDayOfMonth();
            }
            Log.i("Current Date:", today.toString());
            DatePickerDialog dialog = new DatePickerDialog(
                    v.getContext(),
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        LocalDate selectedDate = null;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            selectedDate = LocalDate.of(
                                    selectedYear,
                                    selectedMonth + 1,  // convert back to 1-based
                                    selectedDay
                            );
                        }
                        Log.i("Selected Date: ", selectedDate.toString());
                        lastSelectedDate = selectedDate;
                        SOTD_AdminDate.setText(lastSelectedDate.toString());
                    },
                    year, month, day
            );
            dialog.show();
        });
        SOTD_AdminDelete.setOnClickListener(v -> adminDelete());
        SOTD_Admin_SetSong.setOnClickListener(v -> adminCreate());
    }

    // -----------------------------------------------------------
    // STARTUP FLOW
    // -----------------------------------------------------------

    private void startup() {
        updateSOTDInformation(() ->
                getTrackInfo(() ->
                        getArtistInfoAll(() ->
                                getAlbumArt(() -> {
                                    renderSong();
                                    renderArtist();
                                    renderScore();
                                    adminCheck();
                                })
                        )
                )
        );
    }

    // -----------------------------------------------------------
    // NETWORK CALLS
    // -----------------------------------------------------------

    private void updateSOTDInformation(Runnable onDone) {
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                serverUrl + "/sotd/today",
                null,
                response -> {
                    Log.i("SOTD Information", response.toString());
                    try {
                        sotdId = response.getInt("id");
                        sotdTrackId = response.getInt("trackId");
                        sotdScore = response.getInt("score");

                        JSONArray votedArray = response.getJSONArray("voted");
                        sotdVoters.clear();
                        for (int i = 0; i < votedArray.length(); i++) {
                            Log.i("votedArray JSONObject?", votedArray.toString());
                            sotdVoters.add(votedArray.getInt(i));
                        }
                        Log.i("SOTD Voter information", sotdVoters.toString());
                    } catch (Exception ignored) {Log.e("Update SOTD Info error", ignored.toString());}

                    onDone.run();
                },
                error -> onDone.run()
        );

        requestQueue.add(req);
    }

    private void getTrackInfo(Runnable onDone) {
        if (sotdTrackId < 0) {
            onDone.run();
            return;
        }

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                serverUrl + "/music/tracks/" + sotdTrackId,
                null,
                response -> {
                    Log.i("GetTrackInfo JSON", response.toString());
                    try {
                        sotdTitle = response.getString("name");

                        JSONArray artistArray = response.getJSONArray("artistIds");
                        artistIds = new Integer[artistArray.length()];
                        for (int i = 0; i < artistArray.length(); i++) {
                            artistIds[i] = artistArray.getInt(i);
                        }

                        JSONArray albumArray = response.getJSONArray("albumIds");
                        albumId = new Integer[albumArray.length()];
                        for (int i = 0; i < albumArray.length(); i++) {
                            albumId[i] = albumArray.getInt(i);
                        }

                    } catch (Exception ignored) {}

                    onDone.run();
                },
                error -> onDone.run()
        );

        requestQueue.add(req);
    }

    private void getArtistInfoAll(Runnable onDone) {
        if (artistIds == null || artistIds.length == 0) {
            onDone.run();
            return;
        }

        sotdArtist = "";
        final int total = artistIds.length;
        final int[] doneCount = {0};

        for (int id : artistIds) {
            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.GET,
                    serverUrl + "/music/artists/" + id,
                    null,
                    response -> {
                        try {
                            if (!sotdArtist.isEmpty()) sotdArtist += ", ";
                            sotdArtist += response.getString("name");
                        } catch (Exception ignored) {}

                        doneCount[0]++;
                        if (doneCount[0] == total) onDone.run();
                    },
                    error -> {
                        doneCount[0]++;
                        if (doneCount[0] == total) onDone.run();
                    }
            );

            requestQueue.add(req);
        }
    }

    private void getAlbumArt(Runnable onDone) {
        if (albumId == null || albumId.length == 0) {
            onDone.run();
            return;
        }

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                serverUrl + "/music/albums/" + albumId[0],
                null,
                response -> {
                    try {
                        String base64 = response.getString("albumArt");
                        byte[] data = Base64.decode(base64, Base64.DEFAULT);
                        sotdTrackArt = BitmapFactory.decodeByteArray(data, 0, data.length);
                    } catch (Exception ignored) {}

                    onDone.run();
                },
                error -> onDone.run()
        );

        requestQueue.add(req);
    }

    private void getUserInfo(Runnable onDone) {
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                serverUrl + "/users/" + userId,
                null,
                response -> {
                    try {
                        userAdmin = response.getString("type").equals("admin");
                    } catch (Exception ignored) {}

                    onDone.run();
                },
                error -> onDone.run()
        );

        requestQueue.add(req);
    }

    private void vote(boolean upVote) {
        String voteType = upVote ? "upvote" : "downvote";

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.PUT,
                serverUrl + "/sotd/" + voteType,
                null,
                response -> {
                    userVoted = true;
                    sotdScore += (upVote ? 1 : -1);
                    renderScore();
                },
                error -> {}
        );

        requestQueue.add(req);
        renderScore();
    }

    private void adminCreate() {
        String url = serverUrl + "/sotd/" + lastSelectedDate.toString() + "/" + SOTD_AdminTrackID.getText();
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                url,
                null,
                response -> {
                    Log.i("Created SOTD", response.toString());
                },
                error -> {}
        );

        requestQueue.add(req);
    }

    private void adminDelete() {
        String url = serverUrl + "/sotd/" + lastSelectedDate.toString();
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.DELETE,
                url,
                null,
                response -> {
                    Log.i("Deleted SOTD", response.toString());
                },
                error -> {}
        );

        requestQueue.add(req);
        renderScore();
    }

    // -----------------------------------------------------------
    // UI RENDERING
    // -----------------------------------------------------------

    private void renderSong() {
        SOTD_Title.setText(sotdTitle);
        if (sotdTrackArt != null) {
            SOTD_Art.setImageBitmap(sotdTrackArt);
        }
    }

    private void renderArtist() {
        SOTD_Artist.setText(sotdArtist);
    }

    private void renderScore() {
        SOTD_VotesRefresh.setText(String.valueOf(sotdScore));

        userVoted = sotdVoters.contains(userId) || userId < 0 || userVoted;

        if (userVoted) {
            SOTD_Upvote.setVisibility(INVISIBLE);
            SOTD_Downvote.setVisibility(INVISIBLE);
        } else {
            SOTD_Upvote.setVisibility(VISIBLE);
            SOTD_Downvote.setVisibility(VISIBLE);
        }
    }

    private void adminCheck() {
        int vis = userAdmin ? VISIBLE : INVISIBLE;

        SOTD_Admin_SetSong.setVisibility(vis);
        SOTD_AdminDate.setVisibility(vis);
        SOTD_AdminTrackID.setVisibility(vis);
        SOTD_AdminDelete.setVisibility(vis);
    }
}
