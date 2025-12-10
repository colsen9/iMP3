package com.example.androidexample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class CounterActivity extends AppCompatActivity {

    private TextView numberTxt; // define number textview variable
    private Button increaseBtn; // define increase button variable
    private Button decreaseBtn; // define decrease button variable
    private Button backBtn;     // define back button variable
    private int counter = 0;    // counter variable
    private long lastPressTimeIncrease = 0;
    private long lastPressTimeDecrease = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counter);

        /* initialize UI elements */
        numberTxt = findViewById(R.id.number);
        increaseBtn = findViewById(R.id.counter_increase_btn);
        decreaseBtn = findViewById(R.id.counter_decrease_btn);
        backBtn = findViewById(R.id.counter_back_btn);

        updateNumber();

        // Style back button as nav
        styleNavButton(backBtn);

        increaseBtn.setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            int step = (now - lastPressTimeIncrease < 500) ? 2 : 1;
            counter += step;
            lastPressTimeIncrease = now;
            updateNumber();
            if (counter % 5 == 0) showMotivationalToast();
        });

        decreaseBtn.setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            int step = (now - lastPressTimeDecrease < 500) ? 2 : 1;
            counter -= step;
            lastPressTimeDecrease = now;
            updateNumber();
            if (counter < 0) shakeNumber();
        });

        backBtn.setOnClickListener(v -> {
            animateButton(backBtn);
            Intent intent = new Intent(CounterActivity.this, MainActivity.class);
            intent.putExtra("NUM", String.valueOf(counter));
            startActivity(intent);
        });
    }

    private void updateNumber() {
        numberTxt.setText(String.valueOf(counter));
        if (counter > 0)
            numberTxt.setTextColor(getResources().getColor(android.R.color.holo_green_dark, getTheme()));
        else if (counter < 0)
            numberTxt.setTextColor(getResources().getColor(android.R.color.holo_red_dark, getTheme()));
        else
            numberTxt.setTextColor(getResources().getColor(android.R.color.black, getTheme()));
    }

    private void shakeNumber() {
        Animation shake = new TranslateAnimation(-10, 10, 0, 0);
        shake.setDuration(50);
        shake.setRepeatCount(3);
        shake.setRepeatMode(Animation.REVERSE);
        numberTxt.startAnimation(shake);
    }

    private void showMotivationalToast() {
        Toast.makeText(this, "Great job! You reached " + counter, Toast.LENGTH_SHORT).show();
    }

    private void styleNavButton(Button btn) {
        btn.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark, getTheme()));
        btn.setTextColor(getResources().getColor(android.R.color.white, getTheme()));
        btn.setAllCaps(false);
        btn.setPadding(30, 20, 30, 20);
    }

    private void animateButton(Button btn) {
        ScaleAnimation scale = new ScaleAnimation(
                1f, 1.1f, 1f, 1.1f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        );
        scale.setDuration(100);
        scale.setRepeatCount(1);
        scale.setRepeatMode(ScaleAnimation.REVERSE);
        btn.startAnimation(scale);
    }

}