package com.example.androidexample;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import java.util.Map;



/**
 * @author Graysen Schwaller
 * LoginPage allows a user to login to get to the Music Catalogue, or choose to signup
 */
public class LoginPage extends AppCompatActivity {

    private EditText emailEditText; // Email EditText
    private EditText passwordEditText; // Password EditText
    private TextView errorText; // Error message TextView
    private Button loginButton; // "Login" button Button
    private Button signupButton; // "Signup" button Button

    // Error codes and Error information
    static int lastError;
    static int errorNumber;
    public static int ERROR_FORBIDDEN;
    public static int ERROR_UNAUTHORIZED;
    public static int STATUS_ACCEPTED;
    public static int ERROR_UNKNOWN;

    // Constant Variables
    static String serverUrl;

    private static int userID;

    static {
        serverUrl = "http://coms-3090-027.class.las.iastate.edu:8080/users/login";
        // serverUrl = "https://29cdf532-e52c-4146-a2fd-0365337ea5fa.mock.pstmn.io";
        errorNumber = -1;
        lastError = 0;

        ERROR_FORBIDDEN = 403;
        ERROR_UNAUTHORIZED = 401;
        ERROR_UNKNOWN = -1;
        STATUS_ACCEPTED = 200;
    }


    // Volley Request Queue
    // private static Context ctx;
    private RequestQueue requestQueue; // = Volley.newRequestQueue(ctx.getApplicationContext());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);

        /* initialize UI elements */
        emailEditText = findViewById(R.id.login_email_edit);
        passwordEditText = findViewById(R.id.login_password_edit);
        loginButton = findViewById(R.id.login_login_btn);
        signupButton = findViewById(R.id.login_signup_btn);
        errorText = findViewById(R.id.login_error_txt);

        /* click listener on login button press */
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /* grab strings from user inputs */
                String email = emailEditText.getText().toString();
                String password = passwordEditText.getText().toString();
                HashMap<String, String> creds = new HashMap<>();
                creds.put("email", email);
                creds.put("password", password);
                makeJsonObjPOSTReq("",new JSONObject(creds));
            }
        });

        /* click listener on signup button press */
        signupButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                sendToSignup();
            }
        });
    }


    /**
     * @param urlAdd A string to add to the server's basic URL, usually a path
     * @param sendJSON A JSONObject to be sent to the server as a part of the request body | null accepting
     */
    private void makeJsonObjPOSTReq(String urlAdd, JSONObject sendJSON) {

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(
                Request.Method.POST,
                serverUrl + urlAdd,
                sendJSON,

                response -> {
                    try {
                        int status = response.getInt("status");

                        if (status == STATUS_ACCEPTED) {
                            userID = response.getInt("user");
                            sendToProfile(userID);
                        }
                        else if (status == ERROR_FORBIDDEN) {
                            errorText.setText("Error: Credentials not recognized by server");
                        }
                        else if (status == ERROR_UNAUTHORIZED) {
                            errorText.setText("Error: Email or password unrecognized");
                        }
                        else {
                            errorText.setText("Error: Unknown Error");
                        }

                    } catch (Exception e) {
                        errorText.setText("Error: " + e.toString());
                    }
                },

                error -> errorText.setText("Error: " + error.toString())

        ) {
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                Map<String, String> headers = response.headers;

                String cookie = headers.get("Set-Cookie");
                if (cookie != null && cookie.contains("JSESSIONID")) {
                    String sessionId = cookie.split(";")[0].replace("JSESSIONID=", "");
                    QueueApplication.setSessionId(sessionId);
                }

                return super.parseNetworkResponse(response);
            }
        };

        if (requestQueue == null) {
            requestQueue = QueueApplication.getQueue();
        }

        requestQueue.add(jsonObjReq);
    }



    /**
     *  Changes intent view to Signup
     *  Used when user selects sign-up
     */
    protected void sendToSignup(){
        startActivity(new Intent(LoginPage.this, SignupPage.class)); // Sends user to signup page
    }
    /**
     *  Changes intent view to Profile Page
     *  Used when user logs in
     */
    protected void sendToProfile(int id) {
        Intent profilePage = new Intent(LoginPage.this, ProfilePage.class);
        profilePage.putExtra("userId", id);
        startActivity(profilePage);
    }
}
