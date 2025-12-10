package com.example.androidexample.caydenInstrumentedTests;

import android.view.View;
import android.widget.LinearLayout;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.androidexample.ArtistPage;
import com.example.androidexample.R;

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
public class ArtistPageInstrumentedTests {

    private static final int SIMULATED_DELAY_MS = 1500;

    @Before
    public void setup() {
        ActivityScenario.launch(ArtistPage.class);
    }

    @Test
    public void testArtistUIElementsVisible() throws InterruptedException {
        Thread.sleep(SIMULATED_DELAY_MS);

        onView(withId(R.id.artistImage)).check(matches(isDisplayed()));
        onView(withId(R.id.artistName)).check(matches(isDisplayed()));
        onView(withId(R.id.artistYears)).check(matches(isDisplayed()));
        onView(withId(R.id.artistBio)).check(matches(isDisplayed()));
        onView(withId(R.id.albumContainer)).check(matches(isDisplayed()));
        onView(withId(R.id.songContainer)).check(matches(isDisplayed()));
        onView(withId(R.id.tagsContainer)).check(matches(isDisplayed()));
        onView(withId(R.id.btnReturn)).check(matches(isDisplayed()));
    }

    @Test
    public void testReturnButtonFinishesActivity() throws InterruptedException {
        Thread.sleep(SIMULATED_DELAY_MS);
        onView(withId(R.id.btnReturn)).perform(click());
    }

    @Test
    public void testAlbumCardsClickable() throws InterruptedException {
        Thread.sleep(SIMULATED_DELAY_MS);

        onView(withId(R.id.albumContainer)).perform(clickFirstChild());
        Thread.sleep(500);
        pressBack();
    }

    @Test
    public void testSongCardsClickable() throws InterruptedException {
        Thread.sleep(SIMULATED_DELAY_MS);

        onView(withId(R.id.songContainer)).perform(clickFirstChild());
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

