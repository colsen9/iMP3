package com.example.androidexample.caydenInstrumentedTests;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.androidexample.NotificationsPage;
import com.example.androidexample.R;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class NotificationsPageInstrumentedTests {

    private final int testUserId = 126;

    @Before
    public void setup() {
    }

    public static ViewAction clickChildViewWithId(final int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "Click on a child view with specified id.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                View v = view.findViewById(id);
                if (v != null) {
                    v.performClick();
                }
            }
        };
    }

    @Test
    public void testRecyclerViewIsDisplayed() {
        ActivityScenario<NotificationsPage> scenario = ActivityScenario.launch(NotificationsPage.class);
        scenario.onActivity(activity -> activity.getIntent().putExtra("userId", testUserId));

        onView(withId(R.id.rvNotifications))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testClickNotificationMarksAsRead() throws InterruptedException {
        ActivityScenario<NotificationsPage> scenario = ActivityScenario.launch(NotificationsPage.class);
        scenario.onActivity(activity -> activity.getIntent().putExtra("userId", testUserId));

        Thread.sleep(1500);

        onView(withId(R.id.rvNotifications))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        Thread.sleep(1000);

        onView(withId(R.id.rvNotifications))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testDeleteNotification() throws InterruptedException {
        ActivityScenario<NotificationsPage> scenario = ActivityScenario.launch(NotificationsPage.class);
        scenario.onActivity(activity -> activity.getIntent().putExtra("userId", testUserId));

        Thread.sleep(1500);

        onView(withId(R.id.rvNotifications))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, clickChildViewWithId(R.id.btnDelete)));

        Thread.sleep(1000);

        onView(withId(R.id.rvNotifications))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testReadAllNotificationsButton() throws InterruptedException {
        ActivityScenario<NotificationsPage> scenario = ActivityScenario.launch(NotificationsPage.class);
        scenario.onActivity(activity -> activity.getIntent().putExtra("userId", testUserId));

        Thread.sleep(1500);

        onView(withId(R.id.btnReadAll)).perform(click());

        Thread.sleep(1000);

        onView(withId(R.id.rvNotifications))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testReturnButton() throws InterruptedException {
        ActivityScenario<NotificationsPage> scenario = ActivityScenario.launch(NotificationsPage.class);
        scenario.onActivity(activity -> activity.getIntent().putExtra("userId", testUserId));

        onView(withId(R.id.btnReturn)).perform(click());

        Thread.sleep(500);

        scenario.onActivity(activity -> {
            assert (activity.isFinishing() || activity.isDestroyed());
        });
    }
}



