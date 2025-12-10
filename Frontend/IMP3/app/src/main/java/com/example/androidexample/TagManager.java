package com.example.androidexample;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.androidexample.QueueApplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TagManager {

    private static final String BASE_URL = "http://coms-3090-027.class.las.iastate.edu:8080/tag";
    private static final String USERTAG_URL = "http://coms-3090-027.class.las.iastate.edu:8080/usertag";

    private final Context context;

    public interface TagListCallback {
        void onSuccess(JSONArray tags);
        void onError(String message);
    }

    public interface TagCallback {
        void onSuccess(JSONObject tag);
        void onError(String message);
    }

    public interface EmptyCallback {
        void onSuccess();
        void onError(String message);
    }

    public TagManager(Context context) {
        this.context = context;
    }

    // ----------------------------------------------------
    // GET /tag  → fetch all tags
    // ----------------------------------------------------
    public void getAllTags(TagListCallback cb) {
        String url = BASE_URL;

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    if (cb != null) cb.onSuccess(response);
                },
                error -> {
                    if (cb != null) cb.onError("Failed to load tags");
                    Toast.makeText(context, "Failed to load tags", Toast.LENGTH_SHORT).show();
                }
        );

        QueueApplication.getQueue().add(request);
    }

    // ----------------------------------------------------
    // GET /tag  → fetch all tags
    // ----------------------------------------------------
    public void getUsersTags(TagListCallback cb, int userId) {
        String url = USERTAG_URL;

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url + "/users/" + userId,
                null,
                response -> {
                    if (cb != null) cb.onSuccess(response);
                },
                error -> {
                    if (cb != null) cb.onError("Failed to load tags");
                    Toast.makeText(context, "Failed to load tags", Toast.LENGTH_SHORT).show();
                }
        )
        {
        @Override
        public java.util.Map<String, String> getHeaders() {
            return new java.util.HashMap<>();
        }
        };

        QueueApplication.getQueue().add(request);
    }


    // ----------------------------------------------------
    // POST /tag → create new tag
    // ----------------------------------------------------
    public void createTag(String name, String category, String description, TagCallback cb) {
        String url = BASE_URL;

        JSONObject body = new JSONObject();
        try {
            body.put("name", name);

            if (category != null) body.put("category", category);
            if (description != null) body.put("description", description);

        } catch (JSONException e) {
            if (cb != null) cb.onError("Invalid JSON");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                body,
                response -> {
                    if (cb != null) cb.onSuccess(response);
                },
                error -> {
                    if (cb != null) cb.onError("Failed to create tag");
                    Toast.makeText(context, "Failed to create tag", Toast.LENGTH_SHORT).show();
                }
        );

        QueueApplication.getQueue().add(request);
    }

    public void addTagToUser(int userId, int tagId, EmptyCallback cb) {
        String url = USERTAG_URL;

        JSONObject body = new JSONObject();
        try {
            body.put("userId", userId);
            body.put("tagId", tagId);
        } catch (JSONException e) {
            if (cb != null) cb.onError("JSON error");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                body,
                response -> { if (cb != null) cb.onSuccess(); },
                error -> { if (cb != null) cb.onError("Failed to add user tag"); }
        );

        QueueApplication.getQueue().add(request);
    }


    // ----------------------------------------------------
    // PUT /tag/{id} → update existing tag
    // ----------------------------------------------------
    public void updateTag(int tagId, String privacy, String category, String description, TagCallback cb) {
        String url = USERTAG_URL + "/" + tagId;

        JSONObject body = new JSONObject();
        try {
            if (privacy != null) body.put("privacy", privacy);
            if (category != null) body.put("category", category);
            if (description != null) body.put("description", description);

        } catch (JSONException e) {
            if (cb != null) cb.onError("Invalid JSON");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.PUT,
                url,
                body,
                response -> {
                    if (cb != null) cb.onSuccess(response);
                    Log.i("Successfully updated tag", response.toString());
                },
                error -> {
                    if (cb != null) cb.onError("Failed to update tag");
                    Toast.makeText(context, "Failed to update tag", Toast.LENGTH_SHORT).show();
                }
        );

        QueueApplication.getQueue().add(request);
    }


    // ----------------------------------------------------
    // DELETE /tag/{id}
    // ----------------------------------------------------
    public void deleteTag(int tagId, EmptyCallback cb) {
        String url = BASE_URL + "/" + tagId;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.DELETE,
                url,
                null,
                response -> {
                    if (cb != null) cb.onSuccess();
                },
                error -> {
                    if (cb != null) cb.onError("Failed to delete tag");
                    Toast.makeText(context, "Failed to delete tag", Toast.LENGTH_SHORT).show();
                }
        );

        QueueApplication.getQueue().add(request);
    }
    public void deleteUserTag(int userTagId, EmptyCallback cb) {
        String url = USERTAG_URL + "/" + userTagId;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.DELETE,
                url,
                null,
                response -> { if(cb != null) cb.onSuccess(); },
                error -> { if(cb != null) cb.onError("Failed to delete user tag"); }
        );

        QueueApplication.getQueue().add(request);
    }

}
