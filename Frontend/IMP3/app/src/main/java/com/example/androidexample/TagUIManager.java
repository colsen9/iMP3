package com.example.androidexample;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

public class TagUIManager {

    private final Context context;
    private final LinearLayout container;

    public TagUIManager(Context context, LinearLayout container) {
        this.context = context;
        this.container = container;
    }

    public void displayTags(JSONArray tags, com.example.androidexample.TagManager service) {
        container.removeAllViews();

        for (int i = 0; i < tags.length(); i++) {
            JSONObject obj = tags.optJSONObject(i);
            if (obj != null) {
                addTagChip(obj, service);
            }
        }
    }

    private void addTagChip(JSONObject tagObj, TagManager service) {
        int id = tagObj.optInt("tagId");
        String name = tagObj.optString("name");

        TextView tv = new TextView(context);
        tv.setText(name);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setPadding(35, 20, 35, 20);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(0xFFFFFFFF);

        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF4C9AFF, 0xFFB45CFF}
        );
        bg.setCornerRadius(50);

        tv.setBackground(bg);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

        params.setMargins(20, 20, 20, 20);
        tv.setLayoutParams(params);

        // Long press = delete
        int userTagId = tagObj.optInt("userTagId");

        tv.setOnLongClickListener(v -> {
            service.deleteUserTag(userTagId, new TagManager.EmptyCallback() {
                @Override public void onSuccess() { tv.setVisibility(TextView.GONE); }
                @Override public void onError(String msg) {}
            });
            return true;
        });

        container.addView(tv);
    }
}
