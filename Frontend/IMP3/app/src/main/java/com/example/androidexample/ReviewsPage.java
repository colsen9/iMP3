package com.example.androidexample;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.RequestQueue;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
/**
 * @author Graysen Schwaller
 * Reviews page allows a user to see and edit their own review, as well as see other reviews for the chosen object
 */
public class ReviewsPage extends AppCompatActivity {
    private final String TAG = "ReviewsPage";
    private final String serverBase = "http://coms-3090-027.class.las.iastate.edu:8080";

    private RecyclerView rv;
    private ReviewsAdapter adapter;
    private List<ReviewModel> reviews = new ArrayList<>();
    private RequestQueue requestQueue;

    private int usersReviewId;
    private TextView tvItemName;
    private EditText etSummary, etFullReview;
    private RatingBar ratingBar;
    private Button btnPost, btnBack;

    private ImageButton btnDel;

    private int userId;
    private int itemId, albumId;
    private String itemType, itemName;
    private boolean postedReview = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reviews_page);

        userId = getIntent().getIntExtra("userId", -1);
        itemId = getIntent().getIntExtra("itemId", -1);
        itemType = getIntent().getStringExtra("itemType");
        itemName = getIntent().getStringExtra("itemName");
        albumId = getIntent().getIntExtra("albumId",-1);
        requestQueue = QueueApplication.getQueue();

        rv = findViewById(R.id.rvReviews);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReviewsAdapter(reviews);
        rv.setAdapter(adapter);

        tvItemName = findViewById(R.id.tvItemName);
        tvItemName.setText(itemName + " Reviews");

        etSummary = findViewById(R.id.etSummary);
        etFullReview = findViewById(R.id.etFullReview);
        ratingBar = findViewById(R.id.ratingBar);
        btnPost = findViewById(R.id.btnPostReview);
        btnBack = findViewById(R.id.btnBack);
        btnDel = findViewById(R.id.btnTrashReview);

        if (itemType.equals("albums")) {
            fetchUserAlbumReview(albumId, userId);
        }

        btnBack.setOnClickListener(v -> {
            Intent back = new Intent();
            back.putExtra("userId", userId);
            setResult(RESULT_OK, back);
            finish();
        });

        btnPost.setOnClickListener(v -> {
            Log.i("post", "Post button pressed");

            if (!postedReview) {
                createReview();
            } else {
                editReview();
            }
        });


        btnDel.setVisibility(Button.INVISIBLE);
        fetchReviews();
        // getReviewDebug();

    }

    /**
     * Fetches all reviews and displays them in their appropriate area.
     * Displays current user's review in text box for editing
     * Clears reviews first thing when running
     */
    private void fetchReviews() {
        String url;

        if (itemType.equals("albums")) {
            url = serverBase + "/reviews/albums/" + albumId;
        } else { // songs
            url = serverBase + "/reviews/tracks/" + itemId;
        }

        JsonArrayRequest req = new JsonArrayRequest(
                com.android.volley.Request.Method.GET,
                url,
                null,
                response -> {
                    reviews.clear();
                    postedReview = false;
                    ratingBar.setRating(0);
                    etSummary.setText("");
                    etFullReview.setText("");
                    btnPost.setText("Post Review");
                    btnDel.setVisibility(Button.INVISIBLE);
                    for (int i = response.length() - 1; i >= 0; i--) {
                        try {
                            JSONObject o = response.getJSONObject(i);
                            Log.d("review JSONObject",o.toString());
                            Log.d("reviews JSONArray",response.toString());
                            // The check for if the user reviewed this object
                            if(o.getInt("userId") == userId && userId != -1){
                                postedReview = true;
                                usersReviewId= o.getInt("id");
                                ratingBar.setRating( (float)o.getInt("rating") / 2);
                                String combined = o.optString("review", "");
                                String[] parts = combined.split("\\|\\|", 2);
                                etSummary.setText(parts.length > 0 ? parts[0] : "");
                                etFullReview.setText(parts.length > 1 ? parts[1] : "");
                                btnPost.setText("Edit Review");
                                btnDel.setVisibility(Button.VISIBLE);
                                btnDel.setOnClickListener(v -> {
                                    deleteReview(o);
                                });
                            }
                            reviews.add(ReviewModel.fromJson(o));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    adapter.notifyDataSetChanged();
                    for (ReviewModel review : reviews) {
                        fetchUserInfo(review);
                    }
                },
                error -> Log.e(TAG, "fetchReviews error: " + error.toString())
        );
        requestQueue.add(req);
    }

    /**
     * Creates a review object to then send to the database
     * review: "summary fake || review fake"
     * rating: (int) 1-10
     * songId
     * albumId
     */
    private void createReview() {
        String summary = etSummary.getText().toString().trim();
        String reviewText = etFullReview.getText().toString().trim();
        int rating = (int)(ratingBar.getRating() * 2) ;

        if (TextUtils.isEmpty(summary) || rating == 0) {
            Toast.makeText(this, "Enter summary and rating", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = serverBase + "/reviews/new";
        JSONObject obj = new JSONObject();
        try {
            // obj.put("id",reviews.size());
            obj.put("userId", userId);
            String combinedReview = summary + "||" + reviewText;
            obj.put("review", combinedReview);
            obj.put("rating", rating);
            if (itemType.equals("albums")) {
                obj.put("songId", JSONObject.NULL);
                obj.put("albumId", albumId);
            } else {
                obj.put("songId", itemId);
                obj.put("albumId", albumId);
            }
            Log.d("Create review", "Json Object sent: " + obj.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest req = new JsonObjectRequest(
                com.android.volley.Request.Method.POST,
                url,
                obj,
                response -> {
                    try {
                        reviews.add(0, ReviewModel.fromJson(response));
                        adapter.notifyItemInserted(0);
                        rv.scrollToPosition(0);
                        etSummary.setText("");
                        etFullReview.setText("");
                        ratingBar.setRating(0);
                        fetchReviews();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e(TAG, "postReview error: " + error.toString())
        );
        requestQueue.add(req);
    }

    /**
     * Allows for the editing of reviews already in database
     * Takes given review and sends put request to replace the database review
     */
    private void editReview() {
        String summary = etSummary.getText().toString().trim();
        String reviewText = etFullReview.getText().toString().trim();
        int rating = (int)(ratingBar.getRating() * 2) ;

        if (TextUtils.isEmpty(summary) || rating == 0) {
            Toast.makeText(this, "Enter summary and rating", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = serverBase + "/reviews/edit";
        JSONObject obj = new JSONObject();
        try {
            obj.put("id",usersReviewId);
            obj.put("userId", userId);
            String combinedReview = summary + "||" + reviewText;
            obj.put("review", combinedReview);
            obj.put("text", reviewText);
            obj.put("rating", rating);
            if (itemType.equals("albums")) {
                obj.put("songId", JSONObject.NULL);
                obj.put("albumId", albumId);
            } else {
                obj.put("songId", itemId);
                obj.put("albumId", albumId);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("Review error", e.toString());
        }

        JsonObjectRequest req = new JsonObjectRequest(
                com.android.volley.Request.Method.PUT,
                url,
                obj,
                response -> {
                    try {
                        reviews.add(0, ReviewModel.fromJson(response));
                        adapter.notifyItemInserted(0);
                        rv.scrollToPosition(0);
                        etSummary.setText("");
                        etFullReview.setText("");
                        ratingBar.setRating(0);
                        fetchReviews();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e(TAG, "putReview error: " + error.toString())
        );
        requestQueue.add(req);
    }

    private void deleteReview(JSONObject o) {
        String url = serverBase + "/reviews/delete/" + usersReviewId;

        JsonObjectRequest req = new JsonObjectRequest(
                com.android.volley.Request.Method.DELETE,
                url,
                o,
                response -> {
                    fetchReviews();

                },
                error -> Log.e(TAG, "deleteReview error: " + error.toString())
        );
        requestQueue.add(req);
    }

    /** MODEL **/
    static class ReviewModel {
        String firstName, userType, username, summary, text;
        int rating, itemId, userId, ratingId;
        Bitmap profileBitmap;

        static ReviewModel fromJson(JSONObject o) throws JSONException {
            ReviewModel r = new ReviewModel();
            r.firstName = o.optString("firstname", "Anonymous");
            r.username = "@" + o.optString("username", "user");   // ← ADD THIS
            r.userType = o.optString("userType", "User");

            String combined = o.optString("review", "");
            String[] parts = combined.split("\\|\\|", 2);
            r.summary = parts.length > 0 ? parts[0] : "";
            r.text = parts.length > 1 ? parts[1] : "";

            r.rating = o.optInt("rating", 0);
            r.ratingId = o.optInt("id",-1);
            r.userId = o.optInt("userId",-1);

            return r;
        }

    }

    /**
     * A method that fetches a user's info to then fill in review objects
     */
    private void fetchUserInfo(ReviewModel review) {
        if (review.userId == -1) return;

        String url = serverBase + "/users/" + review.userId;

        JsonObjectRequest userReq = new JsonObjectRequest(
                com.android.volley.Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        review.firstName = response.optString("firstname", "Anonymous");
                        review.username = "@" + response.optString("username", "@user");
                        review.userType = response.optString("userType", "User");
                        adapter.notifyDataSetChanged(); // update UI once info arrives
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e(TAG, "fetchUserInfo error: " + error.toString())
        );

        requestQueue.add(userReq);

        // Fetch profile picture
        String picUrl = serverBase + "/users/" + review.userId + "/picture";
        ImageRequest imageRequest = new ImageRequest(
                picUrl,
                bitmap -> {
                    review.profileBitmap = bitmap;
                    adapter.notifyDataSetChanged();
                },
                0, 0, ImageView.ScaleType.CENTER_CROP,
                Bitmap.Config.ARGB_8888,
                error -> Log.e(TAG, "fetchProfilePic error: " + error.toString())
        );
        requestQueue.add(imageRequest);
    }

    private void fetchUserAlbumReview(int albumId, int userId) {
        String url = serverBase + "/reviews/albums/" + albumId + "/" + userId;

        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    if (response.length() == 0) return;

                    try {
                        JSONObject o = response.getJSONObject(0);

                        postedReview = true;
                        usersReviewId = o.getInt("id");

                        ratingBar.setRating((float)o.getInt("rating") / 2);

                        String combined = o.optString("review", "");
                        String[] parts = combined.split("\\|\\|", 2);
                        etSummary.setText(parts.length > 0 ? parts[0] : "");
                        etFullReview.setText(parts.length > 1 ? parts[1] : "");

                        btnPost.setText("Edit Review");
                        btnDel.setVisibility(Button.VISIBLE);

                        btnDel.setOnClickListener(v -> deleteReview(o));

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e(TAG, "Could not load user's album review")
        );

        requestQueue.add(req);
    }

    /** ADAPTER **/
    private static class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.VH> {
        private final List<ReviewModel> items;

        ReviewsAdapter(List<ReviewModel> items) { this.items = items; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            ReviewModel r = items.get(pos);
            h.tvName.setText(r.firstName);
            h.tvUserType.setText(r.userType);
            h.tvUsername.setText(r.username);
            h.tvSummary.setText(r.summary);
            h.tvReview.setText(r.text);
            h.reviewRating.setStepSize(0.5f);
            h.reviewRating.setRating((float) r.rating / 2.0f);

            if (r.profileBitmap != null) {
                h.ivProfile.setImageBitmap(r.profileBitmap);
            } else {
                h.ivProfile.setImageResource(R.drawable.imp3);
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            ImageView ivProfile;
            TextView tvName, tvUserType, tvUsername, tvSummary, tvReview;
            RatingBar reviewRating;

            VH(@NonNull View itemView) {
                super(itemView);
                ivProfile = itemView.findViewById(R.id.ivProfilePic);
                tvName = itemView.findViewById(R.id.tvName);
                tvUserType = itemView.findViewById(R.id.tvUserType);
                tvUsername = itemView.findViewById(R.id.tvUsername);
                tvSummary = itemView.findViewById(R.id.tvSummary);
                tvReview = itemView.findViewById(R.id.tvReview);
                reviewRating = itemView.findViewById(R.id.reviewRating);
            }
        }

    }

}
