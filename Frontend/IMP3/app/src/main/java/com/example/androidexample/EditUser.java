/** @author Cayden Olsen **/

package com.example.androidexample;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class EditUser extends AppCompatActivity {

    private EditText firstName, lastName, email, bio, username;
    private TextView usernameView;
    private Button saveBtn, cancelBtn, deleteBtn, signoutBtn, changePasswordBtn;
    private ImageButton profilePictureBtn;
    private RequestQueue requestQueue;
    private static final String BASE_URL = "http://coms-3090-027.class.las.iastate.edu:8080";
    private int userId;
    private String selectedProfilePictureBase64 = "";
    private String userType;
    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_user);

        requestQueue = QueueApplication.getQueue();

        // Initialize UI components
        firstName = findViewById(R.id.first_name);
        lastName = findViewById(R.id.last_name);
        email = findViewById(R.id.email);
        bio = findViewById(R.id.bio);
        signoutBtn = findViewById(R.id.signout_btn);
        saveBtn = findViewById(R.id.save_btn);
        cancelBtn = findViewById(R.id.cancel_btn);
        deleteBtn = findViewById(R.id.delete_user_btn);
        changePasswordBtn = findViewById(R.id.change_password_btn);
        profilePictureBtn = findViewById(R.id.profile_picture_btn);
        usernameView = findViewById(R.id.username);
        username = findViewById(R.id.user_name);

        /* Get userId from previous activity */
        Intent intent = getIntent();
        userId = intent.getIntExtra("userId", -1);

        if (userId == -1) {
            Toast.makeText(this, "No user ID provided!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        registerPickImageLauncher();

        /* Load user data from backend */
        loadUser(userId);

        /* Set onClickListeners for save, cancel, and delete buttons */
        saveBtn.setOnClickListener(v -> updateUser(userId));
        cancelBtn.setOnClickListener(v -> finish());
        deleteBtn.setOnClickListener(v -> confirmDelete(userId));
        changePasswordBtn.setOnClickListener(v -> confirmChangePassword(userId));

        signoutBtn.setOnClickListener(v -> {
            Intent editUser = new Intent(EditUser.this, ProfilePage.class);
            userId = -1;
            editUser.putExtra("userId", userId);
            startActivity(editUser);
            finish();
        });

        profilePictureBtn.setOnClickListener(v -> pickImageFromGallery());
    }

    /* Fetch user data from backend */
    private void loadUser(int id) {
        String url = BASE_URL + "/users/" + id;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        userType = response.optString("type", "");
                        usernameView.setText(response.optString("username", ""));
                        username.setText(response.optString("username", ""));
                        firstName.setText(response.optString("firstname", ""));
                        lastName.setText(response.optString("lastname", ""));
                        email.setText(response.optString("email", ""));
                        bio.setText(response.optString("bio", ""));

                        String profilePicBase64 = response.optString("picture", "");
                        selectedProfilePictureBase64 = profilePicBase64;

                        if (!profilePicBase64.isEmpty()) {
                            byte[] decodedString = Base64.decode(profilePicBase64, Base64.NO_WRAP);
                            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            profilePictureBtn.setScaleType(ImageButton.ScaleType.CENTER_CROP);
                            profilePictureBtn.setImageBitmap(decodedBitmap);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(EditUser.this, "Error parsing user data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(EditUser.this, "Failed to load user data", Toast.LENGTH_SHORT).show()
        );

        requestQueue.add(request);
    }

    /* Update user info via PUT */
    private void updateUser(int id) {
        String url = BASE_URL + "/users/" + id;

        String usernameStr = username.getText().toString().trim();
        String emailStr = email.getText().toString().trim();

        boolean hasError = false;

        if (!isValidUsername(usernameStr)) {
            username.setError("Username cannot be empty or contain spaces");
            hasError = true;
        }

        if (!isValidEmail(emailStr)) {
            email.setError("Enter a valid email");
            hasError = true;
        }

        if (hasError) {
            return; // stop update
        }

        if (userType == null || userType.isEmpty()) {
            userType = "user";
        }

        JSONObject userJson = new JSONObject();
        try {
            userJson.put("firstname", firstName.getText().toString());
            userJson.put("lastname", lastName.getText().toString());
            userJson.put("username", usernameStr);
            userJson.put("email", emailStr);
            userJson.put("bio", bio.getText().toString());
            userJson.put("type", userType);

            if (selectedProfilePictureBase64 != null && !selectedProfilePictureBase64.isEmpty()) {
                userJson.put("picture", selectedProfilePictureBase64);
                Log.d("EDIT_USER", "Profile picture Base64 length: " + selectedProfilePictureBase64.length());
            }

            Log.d("EDIT_USER", "Profile picture Base64 length: " + selectedProfilePictureBase64.length());

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating JSON", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.PUT, url, userJson,
                response -> {
                    Toast.makeText(EditUser.this, "User updated successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(EditUser.this, ProfilePage.class);
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                    finish();
                },
                error -> {
                    int code = (error.networkResponse != null) ? error.networkResponse.statusCode : -1;
                    if (code == 409) {
                        username.setError("Username or email already exists");
                        email.setError("Username or email already exists");
                    }
                    Toast.makeText(EditUser.this, "Failed to update user", Toast.LENGTH_SHORT).show();
                }
        );
        requestQueue.add(request);
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void registerPickImageLauncher() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            try {
                                // Persist permissions
                                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                                getContentResolver().takePersistableUriPermission(imageUri, takeFlags);

                                // Load bitmap safely
                                Bitmap bitmap;
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    bitmap = ImageDecoder.decodeBitmap(
                                            ImageDecoder.createSource(getContentResolver(), imageUri)
                                    );
                                } else {
                                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                                }

                                bitmap = resizeBitmap(bitmap, 400);

                                // Update ImageButton
                                profilePictureBtn.setScaleType(ImageButton.ScaleType.CENTER_CROP);
                                profilePictureBtn.setImageBitmap(bitmap);

                                // Convert bitmap to Base64 (NO_WRAP!)
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                                selectedProfilePictureBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

                                Log.d("EDIT_USER", "Selected picture Base64 length: " + selectedProfilePictureBase64.length());

                            } catch (IOException e) {
                                e.printStackTrace();
                                Toast.makeText(EditUser.this, "Image error", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        Toast.makeText(EditUser.this, "No image selected", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scale = Math.min((float) maxSize / width, (float) maxSize / height);
        if (scale >= 1.0f) return bitmap;
        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    /* Ask for confirmation before deleting user */
    private void confirmDelete(int id) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to permanently delete this account?")
                .setPositiveButton("Yes", (dialog, which) -> deleteUser(id))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /* Delete user from backend */
    private void deleteUser(int id) {
        String url = BASE_URL + "/users/" + id;

        StringRequest request = new StringRequest(
                Request.Method.DELETE, url,
                response -> {
                    Intent intent = new Intent(EditUser.this, ProfilePage.class);
                    startActivity(intent);
                    finish();
                },
                error -> Toast.makeText(EditUser.this, "Failed to delete user", Toast.LENGTH_SHORT).show()
        );
        requestQueue.add(request);
    }

    /* Check if email is valid */
    private boolean isValidEmail(String email) {
        return email != null && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /* Check if username is valid */
    private boolean isValidUsername(String username) {
        return username != null && !username.trim().isEmpty() && !username.contains(" ");
    }

    /* Show dialog for old/new password input */
    private void confirmChangePassword(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Password");

        final EditText oldPassInput = new EditText(this);
        oldPassInput.setHint("Old Password");
        oldPassInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        final EditText newPassInput = new EditText(this);
        newPassInput.setHint("New Password");
        newPassInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(oldPassInput);
        layout.addView(newPassInput);
        layout.setPadding(50, 40, 50, 10);
        builder.setView(layout);

        builder.setPositiveButton("Change", (dialog, which) -> {
            String oldPass = oldPassInput.getText().toString().trim();
            String newPass = newPassInput.getText().toString().trim();

            if (oldPass.isEmpty() || newPass.isEmpty()) {
                Toast.makeText(EditUser.this, "Please fill both fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidPassword(newPass)) {
                Toast.makeText(
                        EditUser.this,
                        "New password must be 8+ chars, include upper, lower, number, and special character.",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            changePassword(id, oldPass, newPass);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /* Send PUT request to change password */
    private void changePassword(int id, String oldPassword, String newPassword) {
        String url = BASE_URL + "/users/" + id + "/password";

        JSONObject passwordJson = new JSONObject();
        try {
            passwordJson.put("password_old", oldPassword);
            passwordJson.put("password_new", newPassword);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating password JSON", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.PUT,
                url,
                passwordJson,
                response -> Toast.makeText(EditUser.this,
                        "Password changed successfully!",
                        Toast.LENGTH_SHORT).show(),

                error -> {
                    int statusCode = (error.networkResponse != null)
                            ? error.networkResponse.statusCode
                            : -1;

                    if (statusCode == 401) {
                        Toast.makeText(EditUser.this,
                                "Old password incorrect",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(EditUser.this,
                                "Failed to change password",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );

        requestQueue.add(request);
    }

    /* Check if password is valid — same rules as Signup page */
    private boolean isValidPassword(String password) {
        // Must contain at least: 1 uppercase, 1 lowercase, 1 number, 1 special char, 8+ chars
        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,}$";
        return password.matches(passwordRegex);
    }
}