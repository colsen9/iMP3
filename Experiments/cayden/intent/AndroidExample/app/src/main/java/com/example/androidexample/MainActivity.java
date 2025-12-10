package com.example.androidexample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.Calendar;
import java.util.Random;

/*

1. To run this project, open the directory "Android Example", otherwise it may not recognize the file structure properly

2. Ensure you are using a compatible version of gradle, to do so you need to check 2 files.

    AndroidExample/Gradle Scripts/build.gradle
    Here, you will have this block of code. Ensure it is set to a compatible version,
    in this case 8.12.2 should be sufficient:
        plugins {
            id 'com.android.application' version '8.12.2' apply false
        }

    Gradle Scripts/gradle-wrapper.properties

3. This file is what actually determines the Gradle version used, 8.13 should be sufficient.
    "distributionUrl=https\://services.gradle.org/distributions/gradle-8.13-bin.zip" ---Edit the version if needed

4. You might be instructed by the plugin manager to upgrade plugins, accept it and you may execute the default selected options.

5. Press "Sync project with gradle files" located at the top right of Android Studio,
   once this is complete you will be able to run the app

   This version is compatible with both JDK 17 and 21. The Java version you want to use can be
   altered in Android Studio->Settings->Build, Execution, Deployment->Build Tools->Gradle

 */


public class MainActivity extends AppCompatActivity {

    private TextView messageText;     // define message textview variable
    private Button counterButton;     // define counter button variable

    private String[] welcomeMessages = {
            "Ready to count your day?",
            "Counting adventures await!",
            "Numbers never lie 😉",
            "Let's see how high you can go!"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);             // link to Main activity XML

        /* initialize UI elements */
        messageText = findViewById(R.id.main_msg_txt);      // link to message textview in the Main activity XML
        counterButton = findViewById(R.id.main_counter_btn);// link to counter button in the Main activity XML

        // Pick random welcome message
        Random rand = new Random();
        messageText.setText(welcomeMessages[rand.nextInt(welcomeMessages.length)]);

        // Change text color based on time
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            messageText.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
        } else if (hour < 18) {
            messageText.setTextColor(getResources().getColor(android.R.color.holo_blue_light));
        } else {
            messageText.setTextColor(getResources().getColor(android.R.color.holo_purple));
        }

        // Handle data passed from CounterActivity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String number = extras.getString("NUM");
            messageText.setText("Last count was: " + number);
        }

        // Style counter button as nav
        styleNavButton(counterButton);

        // Counter button click
        counterButton.setOnClickListener(v -> {
            animateButton(counterButton);
            startActivity(new Intent(MainActivity.this, CounterActivity.class));
        });
    }

    // Apply nav-style appearance
    private void styleNavButton(Button btn) {
        btn.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark, getTheme()));
        btn.setTextColor(getResources().getColor(android.R.color.white, getTheme()));
        btn.setAllCaps(false);
        btn.setPadding(30, 20, 30, 20);
    }

    // Button scaling animation
    private void animateButton(Button btn) {
        ScaleAnimation scale = new ScaleAnimation(
                1f, 1.1f, 1f, 1.1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scale.setDuration(100);
        scale.setRepeatCount(1);
        scale.setRepeatMode(Animation.REVERSE);
        btn.startAnimation(scale);
    }

}