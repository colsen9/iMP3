package com.example.androidexample;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.flexbox.FlexboxLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class UserTagUIManager {

    private final Context context;
    private final FlexboxLayout container;

    public UserTagUIManager(Context context, FlexboxLayout container) {
        this.context = context;
        this.container = container;
    }

    public void displayUserTags(JSONArray tags, TagManager service) {
        container.removeAllViews();
        Log.i("JSON Array Display user tags tags:", tags.toString());
        for (int i = 0; i < tags.length(); i++) {
            JSONObject obj = tags.optJSONObject(i);
            if (obj != null) {
                addUserTagChip(obj, service);
            }
        }
    }

    public void displayOtherUserTags(JSONArray tags, TagManager service) {
        container.removeAllViews();
        Log.i("JSON Array Display user tags tags:", tags.toString());
        for (int i = 0; i < tags.length(); i++) {
            JSONObject obj = tags.optJSONObject(i);
            if (obj != null) {
                addOtherUserTagChip(obj, service);
            }
        }
    }


    private void addUserTagChip(JSONObject tagObj, TagManager service) {
        JSONObject tag = tagObj.optJSONObject("tag");
        if (tag == null) return;
        Log.i("addUserTagChip tag info", tag.toString());
        int id = tagObj.optInt("userTagId");
        String name = tag.optString("name");

        TextView tv = TagChipView.create(context, name);

        // Delete on long press
        tv.setOnLongClickListener(v -> {
            service.deleteUserTag(id, new TagManager.EmptyCallback() {
                @Override public void onSuccess() {
                    Log.i("Successful deletion of UserTag", "yippee");
                    Toast.makeText(context, "Successfully deleted tag", Toast.LENGTH_SHORT).show();
                    tv.setVisibility(TextView.GONE);
                    container.removeView(tv);
                    container.refreshDrawableState(); //TODO: Force refresh to make tag no longer display
                }
                @Override public void onError(String msg) {}
            });
            return true;
        });
        tv.setOnClickListener(v -> {
            showEditTagDialog(id,name,service,tv);
        });

        container.addView(tv);
    }
    private void addOtherUserTagChip(JSONObject tagObj, TagManager service) {
        JSONObject tag = tagObj.optJSONObject("tag");
        if (tag == null) return;
        Log.i("addUserTagChip tag info", tag.toString());
        int id = tagObj.optInt("userTagId");
        String name = tag.optString("name");

        TextView tv = TagChipView.create(context, name);

        container.addView(tv);
    }
    private void showEditTagDialog(int tagId, String oldPrivacy, TagManager service, TextView tv) {
        AlertDialog.Builder b = new AlertDialog.Builder(context);
        b.setTitle("Edit Tag Privacy");

        final Spinner input = new Spinner(context);
        List<String> privacyChoices = new ArrayList<>();
        privacyChoices.add("PRIVATE");
        privacyChoices.add("PUBLIC");
        // privacyChoices.add("MUTUALS"); // :TODO
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                privacyChoices
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        input.setAdapter(adapter);

        b.setView(input);

        b.setPositiveButton("Save", (d, w) -> {
            String newPrivacy = (String)input.getSelectedItem();
            if (newPrivacy.isEmpty()) return;

            service.updateTag(tagId, newPrivacy, null, null, new TagManager.TagCallback() {
                @Override public void onSuccess(JSONObject tag) {
                    Log.i("edit tag button press", "");
                }
                @Override public void onError(String msg) {}
            });
        });

        b.setNegativeButton("Cancel", null);
        b.show();
    }
}

