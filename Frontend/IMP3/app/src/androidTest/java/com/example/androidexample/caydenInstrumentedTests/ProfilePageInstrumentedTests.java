package com.example.androidexample.caydenInstrumentedTests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.androidexample.EditUser;
import com.example.androidexample.MainActivity;
import com.example.androidexample.ProfilePage;
import com.example.androidexample.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class ProfilePageInstrumentedTests {

    @Rule
    public ActivityScenarioRule<ProfilePage> activityRule =
            new ActivityScenarioRule<>(ProfilePage.class);

    @Before
    public void setup() {
        Intents.init();
    }

    @After
    public void cleanup() {
        Intents.release();
    }

    @Test
    public void testBioAndFullNameDisplayed() throws InterruptedException {
        Thread.sleep(1000);

        onView(withId(R.id.user_bio))
                .check(matches(isDisplayed()))
                .check(matches(withText(org.hamcrest.Matchers.not(""))));

        onView(withId(R.id.user_fullname))
                .check(matches(isDisplayed()))
                .check(matches(withText(org.hamcrest.Matchers.not(""))));
    }

    @Test
    public void testSpotifyButtonsVisibility() throws InterruptedException {
        Thread.sleep(1000);

        onView(withId(R.id.spotify_link_btn))
                .check(matches(isDisplayed()));

        onView(withId(R.id.spotify_status_text))
                .check(matches(isDisplayed()))
                .check(matches(withText(org.hamcrest.Matchers.containsString("Spotify"))));
    }

    @Test
    public void testNavigateToEditUser() {
        onView(withId(R.id.settings_btn)).perform(click());

        intended(hasComponent(EditUser.class.getName()));
    }

    @Test
    public void testNavigateToMainActivity() {
        onView(withId(R.id.temp_main_btn)).perform(click());

        intended(hasComponent(MainActivity.class.getName()));
    }

    @Test
    public void testNavigationToFollowers() {
        // Assuming FriendsActivity exists
        onView(withId(R.id.friends_btn)).perform(click());
    }

    @Test
    public void testNavigationToMusicCatalogue() {
        onView(withId(R.id.music_btn)).perform(click());
    }

    @Test
    public void testNavigationToCustomLists() {
        onView(withId(R.id.lists_btn)).perform(click());
    }
}




