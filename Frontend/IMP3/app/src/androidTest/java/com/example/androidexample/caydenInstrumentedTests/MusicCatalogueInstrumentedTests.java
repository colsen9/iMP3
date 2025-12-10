package com.example.androidexample.caydenInstrumentedTests;

import android.view.View;
import android.view.ViewGroup;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import com.example.androidexample.MusicCatalogue;
import com.example.androidexample.R;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class MusicCatalogueInstrumentedTests {

    private static final int SIMULATED_DELAY_MS = 1500;

    @Before
    public void setup() {
        ActivityScenario.launch(MusicCatalogue.class);
    }

    @Test
    public void testRecommendedAndGeneralListVisible() throws InterruptedException {
        Thread.sleep(SIMULATED_DELAY_MS);

        onView(withId(R.id.recommendedAlbumContainer)).check(matches(isDisplayed()));
        onView(withId(R.id.generalListContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void testNavigationButtons() throws InterruptedException {
        Thread.sleep(SIMULATED_DELAY_MS);

        onView(withId(R.id.profileBtn)).perform(click());
        Thread.sleep(500);
        pressBack();

        onView(withId(R.id.chatBtn)).perform(click());
        Thread.sleep(500);
        pressBack();

        onView(withId(R.id.notificationsBtn)).perform(click());
        Thread.sleep(500);
        pressBack();

        onView(withId(R.id.recommendationsBtn)).perform(click());
        Thread.sleep(500);
        pressBack();

        onView(withId(R.id.sotdBtn)).perform(click());
        Thread.sleep(500);
        pressBack();
    }

    @Test
    public void testLoginButtonVisibilityForGuest() throws InterruptedException {
        ActivityScenario.launch(MusicCatalogue.class);
        Thread.sleep(SIMULATED_DELAY_MS);

        onView(withId(R.id.login_btn)).check(matches(isDisplayed()));
        onView(withId(R.id.login_btn)).perform(click());
        Thread.sleep(500);
        pressBack();
    }

    @Test
    public void testFilterButtonsAndClearFilters() throws InterruptedException {
        Thread.sleep(SIMULATED_DELAY_MS);

        onView(withId(R.id.filterSongsBtn)).perform(click());
        Thread.sleep(500);

        onView(withId(R.id.filterAlbumsBtn)).perform(click());
        Thread.sleep(500);

        onView(withId(R.id.clearFiltersBtn)).perform(click());
        Thread.sleep(500);
    }

    @Test
    public void testSearchBarFiltering() throws InterruptedException {
        Thread.sleep(SIMULATED_DELAY_MS);

        String searchQuery = "love";
        onView(withId(R.id.searchBar))
                .perform(typeText(searchQuery), closeSoftKeyboard());

        Thread.sleep(500);
    }

    @Test
    public void testClickFirstAlbumOrSongCard() throws InterruptedException {
        Thread.sleep(SIMULATED_DELAY_MS);

        onView(withId(R.id.generalListContainer)).perform(clickChildAtPosition(0));

        Thread.sleep(500);
        pressBack();
    }

    private static ViewAction clickChildAtPosition(final int position) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "Click on child at index " + position;
            }

            @Override
            public void perform(UiController uiController, View view) {
                if (view instanceof ViewGroup) {
                    ViewGroup vg = (ViewGroup) view;
                    if (vg.getChildCount() > position) {
                        vg.getChildAt(position).performClick();
                    }
                }
            }
        };
    }
}


