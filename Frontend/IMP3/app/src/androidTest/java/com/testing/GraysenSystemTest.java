package com.testing;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.*;

import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.androidexample.ChatActivity;
import com.example.androidexample.ChatPage;
import com.example.androidexample.CustomListListPage;
import com.example.androidexample.CustomListPage;
import com.example.androidexample.DayTrackPage;
import com.example.androidexample.FollowerView;
import com.example.androidexample.LoginPage;
import com.example.androidexample.ReviewsPage;
import com.example.androidexample.TagChipView;
import com.example.androidexample.TagManager;
import com.example.androidexample.TagUIManager;
import com.example.androidexample.UserTagUIManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * GraysenSystemTest - final version obeying Option C:
 * - Does NOT access private or package-private inner classes
 * - Only exercises public Activities, Managers, and UI helpers
 * - Uses ActivityScenario + Espresso and ApplicationProvider only
 */
@RunWith(AndroidJUnit4.class)
public class GraysenSystemTest {

    // helper to build intents
    private Intent intentFor(Class<?> cls) {
        return new Intent(ApplicationProvider.getApplicationContext(), cls);
    }

    // -------------------------
    // Activities: launch + simple UI interactions
    // -------------------------
    @Test
    public void loginPage_launch_and_buttons_present() {
        try (ActivityScenario<LoginPage> sc = ActivityScenario.launch(LoginPage.class)) {
            sc.onActivity(activity -> {
                // ensure the activity is not null and UI elements exist via resource ids
                assertNotNull(activity);
                View email = activity.findViewById(com.example.androidexample.R.id.login_email_edit);
                View pass = activity.findViewById(com.example.androidexample.R.id.login_password_edit);
                View loginBtn = activity.findViewById(com.example.androidexample.R.id.login_login_btn);
                View signupBtn = activity.findViewById(com.example.androidexample.R.id.login_signup_btn);

                assertNotNull(email);
                assertNotNull(pass);
                assertNotNull(loginBtn);
                assertNotNull(signupBtn);
            });

            // Click signup and login just to exercise listeners (won't hit network in test)
            onView(withId(com.example.androidexample.R.id.login_signup_btn)).perform(click());
            onView(withId(com.example.androidexample.R.id.login_login_btn)).perform(click());
        }
    }

    @Test
    public void reviewsPage_launch_with_required_extras() {
        Intent i = intentFor(ReviewsPage.class);
        i.putExtra("userId", 2);
        i.putExtra("itemId", 3);
        i.putExtra("itemType", "albums"); // required by onCreate to avoid .equals on null
        i.putExtra("itemName", "UnitTestAlbum");
        i.putExtra("albumId", 3);

        try (ActivityScenario<ReviewsPage> sc = ActivityScenario.launch(i)) {
            sc.onActivity(activity -> {
                assertNotNull(activity.findViewById(com.example.androidexample.R.id.tvItemName));
                assertNotNull(activity.findViewById(com.example.androidexample.R.id.btnPostReview));
            });

            // click back to exercise listener
            onView(withId(com.example.androidexample.R.id.btnBack)).perform(click());
        }
    }

    @Test
    public void chatActivities_launch_and_sendButton() {
        Intent a1 = intentFor(ChatActivity.class);
        a1.putExtra("userId", 5);
        try (ActivityScenario<ChatActivity> sc = ActivityScenario.launch(a1)) {
            sc.onActivity(activity -> assertNotNull(activity.findViewById(com.example.androidexample.R.id.chatActivity_listLayout)));
        }

        Intent a2 = intentFor(ChatPage.class);
        a2.putExtra("userId", 5);
        a2.putExtra("friendId", 6);
        a2.putExtra("friendName", "Pal");
        try (ActivityScenario<ChatPage> sc = ActivityScenario.launch(a2)) {
            sc.onActivity(activity -> {
                assertNotNull(activity.findViewById(com.example.androidexample.R.id.rvChat));
                assertNotNull(activity.findViewById(com.example.androidexample.R.id.etMessage));
                assertNotNull(activity.findViewById(com.example.androidexample.R.id.btnSend));
            });
            // press send to exercise click path (doesn't require access to private inner classes)
            onView(withId(com.example.androidexample.R.id.btnSend)).perform(click());
        }
    }

    @Test
    public void customListPages_launch_and_buttons() {
        Intent listList = intentFor(CustomListListPage.class);
        listList.putExtra("userId", 7);
        try (ActivityScenario<CustomListListPage> sc = ActivityScenario.launch(listList)) {
            sc.onActivity(a -> assertNotNull(a.findViewById(com.example.androidexample.R.id.createListButton)));
        }

        Intent list = intentFor(CustomListPage.class);
        list.putExtra("userId", 7);
        try (ActivityScenario<CustomListPage> sc = ActivityScenario.launch(list)) {
            sc.onActivity(a -> {
                assertNotNull(a.findViewById(com.example.androidexample.R.id.saveButton));
                assertNotNull(a.findViewById(com.example.androidexample.R.id.coverImageButton));
            });
        }
    }

    @Test
    public void dayTrack_and_follower_launch() {
        Intent d = intentFor(DayTrackPage.class);
        d.putExtra("userId", 8);
        try (ActivityScenario<DayTrackPage> sc = ActivityScenario.launch(d)) {
            sc.onActivity(a -> {
                assertNotNull(a.findViewById(com.example.androidexample.R.id.SOTD_SongTitle));
                assertNotNull(a.findViewById(com.example.androidexample.R.id.SOTD_ArtistName));
            });
        }

        Intent f = intentFor(FollowerView.class);
        f.putExtra("userId", 8);
        try (ActivityScenario<FollowerView> sc = ActivityScenario.launch(f)) {
            sc.onActivity(a -> {
                assertNotNull(a.findViewById(com.example.androidexample.R.id.section_all_users));
                assertNotNull(a.findViewById(com.example.androidexample.R.id.section_followers));
            });
        }
    }

    // -------------------------
    // Tag UI: create chips, display tags, delete via fake manager
    // -------------------------
    @Test
    public void tagChipView_and_tagUIManager_display_and_delete() throws JSONException {
        // TagChipView.create (public)
        TextView chip = TagChipView.create(ApplicationProvider.getApplicationContext(), "unit-tag");
        assertNotNull(chip);
        assertEquals("unit-tag", chip.getText().toString());

        // TagUIManager.displayTags (public) -> uses addTagChip internally
        LinearLayout container = new LinearLayout(ApplicationProvider.getApplicationContext());
        TagUIManager tagUI = new TagUIManager(ApplicationProvider.getApplicationContext(), container);

        // build tags array
        JSONArray tags = new JSONArray();
        JSONObject tagObj = new JSONObject();
        tagObj.put("tagId", 11);
        tagObj.put("name", "test-tag");
        tagObj.put("userTagId", 77);
        tags.put(tagObj);

        // fake TagManager to simulate delete success (overrides network method)
        TagManager fakeMgr = new TagManager(ApplicationProvider.getApplicationContext()) {
            @Override
            public void deleteUserTag(int userTagId, EmptyCallback cb) {
                if (cb != null) cb.onSuccess();
            }
        };

        // display and then long-press to delete
        tagUI.displayTags(tags, fakeMgr);
        assertEquals(1, container.getChildCount());
        View child = container.getChildAt(0);
        assertTrue(child instanceof TextView);
        boolean longClicked = child.performLongClick();
        assertTrue("long click should be handled", longClicked);
        // TagUIManager's callback sets the view GONE
        assertEquals(View.GONE, child.getVisibility());
    }

    @Test
    public void userTagUIManager_displayUserTags_and_delete() throws JSONException {
        com.google.android.flexbox.FlexboxLayout flex = new com.google.android.flexbox.FlexboxLayout(ApplicationProvider.getApplicationContext());
        UserTagUIManager ut = new UserTagUIManager(ApplicationProvider.getApplicationContext(), flex);

        JSONArray arr = new JSONArray();
        JSONObject wrapper = new JSONObject();
        JSONObject nested = new JSONObject();
        nested.put("name", "u-tag");
        wrapper.put("tag", nested);
        wrapper.put("userTagId", 9);
        arr.put(wrapper);

        // fake manager to delete
        TagManager fake = new TagManager(ApplicationProvider.getApplicationContext()) {
            @Override
            public void deleteUserTag(int userTagId, EmptyCallback cb) {
                if (cb != null) cb.onSuccess();
            }
        };

        ut.displayUserTags(arr, fake);
        assertEquals(1, flex.getChildCount());
        View chip = flex.getChildAt(0);
        assertTrue(chip instanceof TextView);

        boolean longClicked = chip.performLongClick();
        assertTrue(longClicked);
        assertEquals(View.GONE, chip.getVisibility());
    }

    // -------------------------
    // TagManager: construct + override fake callbacks for other public methods
    // -------------------------
    @Test
    public void tagManager_publicMethods_canBeOverriddenForTests() {
        TagManager tm = new TagManager(ApplicationProvider.getApplicationContext()) {
            @Override
            public void getAllTags(TagListCallback cb) {
                JSONArray arr = new JSONArray();
                if (cb != null) cb.onSuccess(arr);
            }

            @Override
            public void createTag(String name, String category, String description, TagCallback cb) {
                JSONObject o = new JSONObject();
                if (cb != null) cb.onSuccess(o);
            }
        };

        final boolean[] got = {false};
        tm.getAllTags(new TagManager.TagListCallback() {
            @Override
            public void onSuccess(JSONArray tags) {
                got[0] = tags != null;
            }
            @Override public void onError(String msg) { got[0] = false; }
        });
        assertTrue(got[0]);

        final boolean[] created = {false};
        tm.createTag("a", null, null, new TagManager.TagCallback() {
            @Override
            public void onSuccess(JSONObject tag) { created[0] = tag != null; }
            @Override public void onError(String msg) { created[0] = false; }
        });
        assertTrue(created[0]);
    }
}
