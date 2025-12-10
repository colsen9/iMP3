package com.example.androidexample;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

//import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.RequestQueue;

import org.java_websocket.handshake.ServerHandshake;


public class MainActivity extends AppCompatActivity implements WebSocketListener {

    private int userId;
    private Button mainProfileButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        TextView idDebug = findViewById(R.id.main_id_debug);
        Button mainLoginButton = findViewById(R.id.main_login_btn);
        Button mainSignupButton = findViewById(R.id.main_signup_btn);
        Button mainNotificationsButton = findViewById(R.id.main_notifications_btn);
        mainProfileButton = findViewById(R.id.main_profile_btn);
        Button mainFollowerButton = findViewById(R.id.main_follower_btn);
        Button mainMusicCatalogueButton = findViewById(R.id.main_music_catalogue_btn);
        Button mainDebugIDButton = findViewById(R.id.main_profile_debugID_btn);
        Button mainFriendsButton = findViewById(R.id.main_friends_btn);
        Button mainCustomListButton = findViewById(R.id.main_custom_lists_btn);
        Button mainRecommendationsButton = findViewById(R.id.main_recommendation_btn);
        EditText setIdText = findViewById(R.id.debugSetID);

        mainDebugIDButton.setOnClickListener(v -> {
            userId = Integer.parseInt(setIdText.getText().toString());
            idDebug.setText("DEBUG: Current attached ID is: " +userId);
        });

        RequestQueue requestQueue = QueueApplication.getQueue();
        
        /* Get userId from previous activity */
        Intent intent = getIntent();
        userId = intent.getIntExtra("userId", -1);
        idDebug.setText("DEBUG: Current attached ID is: " +userId);

        mainLoginButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, LoginPage.class)));
        mainSignupButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SignupPage.class)));

        mainProfileButton.setOnClickListener(v -> {
            Intent profilePage = new Intent(MainActivity.this, ProfilePage.class);
            profilePage.putExtra("userId", userId);
            startActivity(profilePage);
        });
        mainFriendsButton.setOnClickListener(v -> {
            Intent profilePage = new Intent(MainActivity.this, ChatActivity.class);
            profilePage.putExtra("userId", userId);
            startActivity(profilePage);
        });
        mainFollowerButton.setOnClickListener(v -> {
            Intent FollowerView = new Intent(MainActivity.this, FollowerView.class);
            FollowerView.putExtra("userId", userId);
            startActivity(FollowerView);
        });
        mainNotificationsButton.setOnClickListener(v -> {
            Intent FollowerView = new Intent(MainActivity.this, NotificationsPage.class);
            FollowerView.putExtra("userId", userId);
            startActivity(FollowerView);
        });
        mainCustomListButton.setOnClickListener(v -> {
            Intent FollowerView = new Intent(MainActivity.this, CustomListListPage.class);
            FollowerView.putExtra("userId", userId);
            startActivity(FollowerView);
        });
        mainMusicCatalogueButton.setOnClickListener(v -> {
            Intent FollowerView = new Intent(MainActivity.this, MusicCatalogue.class);
            FollowerView.putExtra("userId", userId);
            startActivity(FollowerView);
        });
        mainRecommendationsButton.setOnClickListener(v -> {
            Intent recommendationPage = new Intent(MainActivity.this, RecommendationsPage.class);
            recommendationPage.putExtra("userId", userId);
            startActivity(recommendationPage);
        });
    }

    @Override
    public void onWebSocketMessage(String message) {}

    @Override
    public void onWebSocketClose(int code, String reason, boolean remote) {}

    @Override
    public void onWebSocketOpen(ServerHandshake handshakedata) {}

    @Override
    public void onWebSocketError(Exception ex) {}
}