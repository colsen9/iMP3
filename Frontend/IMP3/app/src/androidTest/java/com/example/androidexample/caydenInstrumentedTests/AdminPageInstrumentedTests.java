package com.example.androidexample.caydenInstrumentedTests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import android.content.Intent;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.androidexample.AdminPage;
import com.example.androidexample.R;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AdminPageInstrumentedTests {

    private int testUserId = 126;

    @Before
    public void launchAdminPage() {
        Intent intent = new Intent(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext(), AdminPage.class);
        intent.putExtra("userId", testUserId);
        ActivityScenario.launch(intent);
    }

    @Test
    public void testOpenAndCloseAlbumForm() {
        onView(withId(R.id.btnAddAlbum)).perform(click());
        onView(withId(R.id.albumForm)).check(matches(isDisplayed()));
        onView(withId(R.id.btnCancelAlbum)).perform(click());
        onView(withId(R.id.albumForm)).check(matches(Matchers.not(isDisplayed())));
    }

    @Test
    public void testAddAndDeleteAlbum() {
        onView(withId(R.id.btnAddAlbum)).perform(click());
        onView(withId(R.id.etAlbumName)).perform(typeText("Espresso Album"), closeSoftKeyboard());
        onView(withId(R.id.etAlbumArtistIds)).perform(typeText("1"), closeSoftKeyboard());
        onView(withId(R.id.etAlbumTrackIds)).perform(typeText("1"), closeSoftKeyboard());
        onView(withId(R.id.btnSaveAlbum)).perform(click());
        waitForBackend();

        onView(withId(R.id.rvAlbumList)).perform(RecyclerViewActions.actionOnItemAtPosition(
                0, clickChildViewWithId(R.id.btnDeleteAlbum)
        ));
        waitForBackend();
    }

    @Test
    public void testAddAndDeleteTrack() {
        onView(withId(R.id.btnAddTrack)).perform(click());
        onView(withId(R.id.etTrackName)).perform(typeText("Espresso Track"), closeSoftKeyboard());
        onView(withId(R.id.etTrackArtistIds)).perform(typeText("1"), closeSoftKeyboard());
        onView(withId(R.id.etTrackAlbumIds)).perform(typeText("1"), closeSoftKeyboard());
        onView(withId(R.id.btnSaveTrack)).perform(click());
        waitForBackend();

        onView(withId(R.id.rvTrackList)).perform(RecyclerViewActions.actionOnItemAtPosition(
                0, clickChildViewWithId(R.id.btnDeleteTrack)
        ));
        waitForBackend();
    }

    @Test
    public void testAddAndDeleteArtist() {
        onView(withId(R.id.btnAddArtist)).perform(click());
        onView(withId(R.id.etArtistName)).perform(typeText("Espresso Artist"), closeSoftKeyboard());
        onView(withId(R.id.etArtistAlbumIds)).perform(typeText("1"), closeSoftKeyboard());
        onView(withId(R.id.etArtistTrackIds)).perform(typeText("1"), closeSoftKeyboard());
        onView(withId(R.id.btnSaveArtist)).perform(click());
        waitForBackend();

        onView(withId(R.id.rvArtistList)).perform(RecyclerViewActions.actionOnItemAtPosition(
                0, clickChildViewWithId(R.id.btnDeleteArtist)
        ));
        waitForBackend();
    }

    @Test
    public void testBanAndUnbanUser() {
        onView(withId(R.id.btnLoadUsers)).perform(click());
        waitForBackend();

        // Ban first user
        onView(withId(R.id.rvUserList)).perform(RecyclerViewActions.actionOnItemAtPosition(
                0, clickChildViewWithId(R.id.btnBan)
        ));
        waitForBackend();

        // Unban first user
        onView(withId(R.id.rvUserList)).perform(RecyclerViewActions.actionOnItemAtPosition(
                0, clickChildViewWithId(R.id.btnBan)
        ));
        waitForBackend();
    }

    private void waitForBackend() {
        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
    }

    /**
     * Clicks a child view inside a RecyclerView item by ID.
     */
    private ViewAction clickChildViewWithId(final int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return "Click on a child view with specified ID.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                View childView = view.findViewById(id);
                if (childView != null) childView.performClick();
            }
        };
    }
}



