/** @author Cayden Olsen **/

package com.example.androidexample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

public class SignupPage extends AppCompatActivity {
    /* define username edit text variable */
    private EditText emailEditText;
    /* define password edit text variable */
    private EditText passwordEditText;
    /* define confirm edit text variable */
    private EditText confirmEditText;
    /* define username edit text variable */
    private EditText usernameEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup_page);

        /* initialize UI elements */
        /* link to email text edit in the Signup XML */
        emailEditText = findViewById(R.id.email_edit);
        /* link to password text edit in the Signup XML */
        passwordEditText = findViewById(R.id.password_edit);
        /* link to confirm text edit in the Signup XML */
        confirmEditText = findViewById(R.id.confirm_edit);
        /* link to username text edit in the Signup XML */
        usernameEditText = findViewById(R.id.username_edit);

        /* define login button variable & link to login button in the Signup XML */
        Button loginButton = findViewById(R.id.login_btn);
        /* define signup button variable & link to signup button in the Signup XML */
        Button signupButton = findViewById(R.id.signup_btn);

        /* click listener on login button pressed */
        loginButton.setOnClickListener(v -> {
            startActivity(new Intent(SignupPage.this, LoginPage.class));
        });

        /* click listener on signup button pressed */
        signupButton.setOnClickListener(v -> {

            /* grab strings from user inputs */
            String username = usernameEditText.getText().toString();
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();
            String confirm = confirmEditText.getText().toString();

            /* define error variable */
            boolean hasError = false;

            /* Reset errors */
            usernameEditText.setError(null);
            emailEditText.setError(null);
            passwordEditText.setError(null);
            confirmEditText.setError(null);

            // Check if username is valid
            if (username.isEmpty()) {
                usernameEditText.setError("Username is required");
                hasError = true;
            } else if (username.length() < 3) {
                usernameEditText.setError("Username must be at least 3 characters");
                hasError = true;
            }

            /* Check if email is valid */
            if (email.isEmpty()) {
                emailEditText.setError("Email is required");
                hasError = true;
            } else if (!isValidEmail(email)) {
                emailEditText.setError("Invalid email format");
                hasError = true;
            }

            /* Check if password is valid */
            String passwordError = isValidPassword(password);
            if (password.isEmpty()) {
                passwordEditText.setError("Password is required");
                hasError = true;
            } else if (passwordError != null) {
                passwordEditText.setError(passwordError);
                hasError = true;
            }

            /* Check if password and confirm match */
            if (confirm.isEmpty()) {
                confirmEditText.setError("Please confirm your password");
                hasError = true;
            } else if (!password.equals(confirm)) {
                confirmEditText.setError("Passwords do not match");
                hasError = true;
            }

            /* Proceed to profile page if all inputs are valid */
            if (!hasError) {
                volleySignup(email, username, password);
            }
        });
    }

    /* Signup with backend using Volley */
    private void volleySignup(String email, String username, String password) {
        String url = "http://coms-3090-027.class.las.iastate.edu:8080/users/signup";

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("username", username);
            requestBody.put("email", email);
            requestBody.put("password", password);
            requestBody.put("type", "user");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                requestBody,
                response -> {
                    try {
                        int userId = response.getInt("id");
                        String userEmail = response.getString("email");

                        Intent intent = new Intent(SignupPage.this, ProfilePage.class);
                        intent.putExtra("userId", userId);
                        intent.putExtra("email", userEmail);
                        startActivity(intent);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        emailEditText.setError("Response parse error");
                    }
                },
                this::handleVolleyError
        );

        QueueApplication.getQueue().add(request);
    }

    /* Handle backend errors */
    private void handleVolleyError(VolleyError error) {
        if (error.networkResponse != null && error.networkResponse.statusCode == 409) {

            // Backend returns 409 for username OR email conflict
            String msg = "Email or username already exists";

            emailEditText.setError(msg);
            usernameEditText.setError(msg);
        } else {
            emailEditText.setError("Signup failed: " + error.toString());
        }
    }

    /* Check if email is valid */
    private boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /* Check if password is valid */
    private String isValidPassword(String password) {
        if (password.length() < 8) {
            return "Password must be at least 8 characters long";
        } else if (!password.matches(".*[A-Z].*")) {
            return "Password must contain at least one uppercase letter";
        } else if (!password.matches(".*[a-z].*")) {
            return "Password must contain at least one lowercase letter";
        } else if (!password.matches(".*[0-9].*")) {
            return "Password must contain at least one digit";
        } else {
            return null;
        }
    }
}








