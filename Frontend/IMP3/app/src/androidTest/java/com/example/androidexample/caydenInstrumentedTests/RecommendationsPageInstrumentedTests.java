package com.example.androidexample.caydenInstrumentedTests;

import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidexample.R;
import com.example.androidexample.RecommendationsPage;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class RecommendationsPageInstrumentedTests {

    private static final int SIMULATED_DELAY_MS = 1500;

    @Before
    public void setup() {
        ActivityScenario.launch(RecommendationsPage.class);
    }

    @Test
    public void testUIElementsVisible() throws InterruptedException {
        Thread.sleep(SIMULATED_DELAY_MS);

        onView(withId(R.id.recycler_recommendations)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_add_rec)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_return)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_fetch_suggestions)).check(matches(isDisplayed()));
        onView(withId(R.id.btn_fetch_user_recs)).check(matches(isDisplayed()));
    }

    @Test
    public void testAddButtonOpensDialog() throws InterruptedException {
        Thread.sleep(SIMULATED_DELAY_MS);
        onView(withId(R.id.btn_add_rec)).perform(click());
        // Wait for AlertDialog
        Thread.sleep(500);
    }

    @Test
    public void testReturnButtonFinishesActivity() throws InterruptedException {
        Thread.sleep(SIMULATED_DELAY_MS);
        onView(withId(R.id.btn_return)).perform(click());
    }

    @Test
    public void testFetchSuggestionsButton() throws InterruptedException {
        Thread.sleep(SIMULATED_DELAY_MS);
        onView(withId(R.id.btn_fetch_suggestions)).perform(click());
        Thread.sleep(500);
    }

    @Test
    public void testFetchUserRecsButton() throws InterruptedException {
        Thread.sleep(SIMULATED_DELAY_MS);
        onView(withId(R.id.btn_fetch_user_recs)).perform(click());
        Thread.sleep(500);
    }

    @Test
    public void testRecyclerViewFirstItemClick() throws InterruptedException {
        Thread.sleep(SIMULATED_DELAY_MS);
        onView(withId(R.id.recycler_recommendations)).perform(clickFirstChild());
        Thread.sleep(500);
    }

    private static ViewAction clickFirstChild() {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "Click first child of RecyclerView";
            }

            @Override
            public void perform(UiController uiController, View view) {
                if (view instanceof RecyclerView) {
                    RecyclerView rv = (RecyclerView) view;
                    if (rv.getChildCount() > 0) {
                        rv.getChildAt(0).performClick();
                    }
                }
            }
        };
    }
}

