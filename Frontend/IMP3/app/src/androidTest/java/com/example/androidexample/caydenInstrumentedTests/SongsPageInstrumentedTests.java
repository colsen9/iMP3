package com.example.androidexample.caydenInstrumentedTests;

import android.view.View;
import android.widget.LinearLayout;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.androidexample.R;
import com.example.androidexample.SongsPage;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SongsPageInstrumentedTests {

    private static final int SIMULATED_DELAY_MS = 1500;

    @Before
    public void setup() {
        ActivityScenario.launch(SongsPage.class);
    }

    @Test
    public void testSongUIElementsVisible() throws InterruptedException {
        Thread.sleep(SIMULATED_DELAY_MS);

        onView(withId(R.id.songCover)).check(matches(isDisplayed()));
        onView(withId(R.id.songTitle)).check(matches(isDisplayed()));
        onView(withId(R.id.songAlbum)).check(matches(isDisplayed()));
        onView(withId(R.id.artistContainer)).check(matches(isDisplayed()));
        onView(withId(R.id.songGenres)).check(matches(isDisplayed()));
        onView(withId(R.id.songLength)).check(matches(isDisplayed()));
        onView(withId(R.id.songMood)).check(matches(isDisplayed()));
        onView(withId(R.id.songBpm)).check(matches(isDisplayed()));
        onView(withId(R.id.avgRatingText)).check(matches(isDisplayed()));
        onView(withId(R.id.songRatingBar)).check(matches(isDisplayed()));
        onView(withId(R.id.userReviewText)).check(matches(isDisplayed()));
        onView(withId(R.id.btnReturnCatalogue)).check(matches(isDisplayed()));
        onView(withId(R.id.btnReviewSong)).check(matches(isDisplayed()));
    }

    @Test
    public void testReturnCatalogueButton() throws InterruptedException {
        Thread.sleep(SIMULATED_DELAY_MS);
        onView(withId(R.id.btnReturnCatalogue)).perform(click());
    }

    @Test
    public void testReviewSongButtonNavigates() throws InterruptedException {
        Thread.sleep(SIMULATED_DELAY_MS);
        onView(withId(R.id.btnReviewSong)).perform(click());
        Thread.sleep(500);
        pressBack();
    }

    @Test
    public void testAlbumClickNavigatesToAlbumsPage() throws InterruptedException {
        Thread.sleep(SIMULATED_DELAY_MS);
        onView(withId(R.id.songAlbum)).perform(click());
        Thread.sleep(500);
        pressBack();
    }

    @Test
    public void testArtistButtonsClickable() throws InterruptedException {
        Thread.sleep(SIMULATED_DELAY_MS);

        onView(withId(R.id.artistContainer)).perform(clickFirstChild());
        Thread.sleep(500);
        pressBack();
    }

    private static ViewAction clickFirstChild() {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "Click the first child of a LinearLayout";
            }

            @Override
            public void perform(UiController uiController, View view) {
                if (view instanceof LinearLayout && ((LinearLayout) view).getChildCount() > 0) {
                    ((LinearLayout) view).getChildAt(0).performClick();
                }
            }
        };
    }
}


