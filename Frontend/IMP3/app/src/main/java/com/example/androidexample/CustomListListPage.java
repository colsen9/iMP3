package com.example.androidexample;
import com.android.volley.DefaultRetryPolicy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Graysen Schwaller
 * CustomListListPage shows the CustomLists of a user
 */
public class CustomListListPage extends AppCompatActivity {

    private RequestQueue queue;
    private LinearLayout listContainer;
    private int userId;
    private static final String BASE_URL = "http://coms-3090-027.class.las.iastate.edu:8080/lists";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_list_list_page);

        userId = getIntent().getIntExtra("userId", -1);

        listContainer = findViewById(R.id.listContainer);
        Button createListButton = findViewById(R.id.createListButton);
        queue = QueueApplication.getQueue();

        createListButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, CustomListPage.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        Button back = findViewById(R.id.custom_list_list_back); back.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent back = new Intent();
                back.putExtra("userId", userId);
                setResult(RESULT_OK, back);
                finish();
            }});

        fetchLists();
    }

    /**
     * Fetches all lists from the user and calls displayLists()
     */
    private void fetchLists() {
        String url = BASE_URL;

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                this::displayLists,
                error -> Toast.makeText(this, "Error fetching lists", Toast.LENGTH_SHORT).show()
        ){
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-type", "application/json");

                headers.put("Cookie", "JSESSIONID=" + QueueApplication.getSessionId());

                return headers;
            }

        };

        Log.d("SESSION_CHECK", "Session = " + QueueApplication.getSessionId());


        queue.add(request);
    }


    /**
     * @param lists A JSONArray that contains all customList objects
     *
     *              Removes all views and fills in the lists using the lists gotten from the JSONArray
     */
    private void displayLists(JSONArray lists) {
        listContainer.removeAllViews();

        for (int i = 0; i < lists.length(); i++) {
            try {
                JSONObject list = lists.getJSONObject(i);
                //Log.d("list object", list.toString());
                View item = getLayoutInflater().inflate(R.layout.custom_list_item, null);

                TextView listName = item.findViewById(R.id.listName);
                TextView listDesc = item.findViewById(R.id.listDesc);
                ImageView listImage = item.findViewById(R.id.listImage);

                String base64Image = list.optString("coverImage", null);

                if (base64Image != null && !base64Image.isEmpty()) {
                    try {
                        byte[] imageBytes = Base64.decode(base64Image, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                        listImage.setImageBitmap(bitmap);
                    } catch (Exception e) {
                        Log.e("ImageDecode", "Failed to decode cover image", e);
                    }
                }

                listName.setText(list.optString("title", "Untitled List"));
                listDesc.setText(list.optString("description", "No description"));

                item.setOnClickListener(v -> {
                    Intent intent = new Intent(this, ViewListPage.class);
                    intent.putExtra("listId", list.optInt("listId", -1));
                    intent.putExtra("userId", userId);
                    startActivity(intent);
                });

                listContainer.addView(item);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


}
