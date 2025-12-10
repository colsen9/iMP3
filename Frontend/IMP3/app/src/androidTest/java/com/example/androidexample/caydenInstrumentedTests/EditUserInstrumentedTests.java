package com.example.androidexample.caydenInstrumentedTests;

import android.widget.EditText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import com.example.androidexample.EditUser;
import com.example.androidexample.R;

@RunWith(AndroidJUnit4.class)
public class EditUserInstrumentedTests {

    @Rule
    public ActivityScenarioRule<EditUser> activityRule =
            new ActivityScenarioRule<>(EditUser.class);

    @Test
    public void testSaveButton() {
        onView(withId(R.id.first_name)).perform(replaceText("TestFirst"));
        onView(withId(R.id.last_name)).perform(replaceText("TestLast"));
        onView(withId(R.id.email)).perform(replaceText("test@example.com"));
        onView(withId(R.id.user_name)).perform(replaceText("testuser"));

        onView(withId(R.id.save_btn)).perform(click());
    }

    @Test
    public void testCancelButton() {
        onView(withId(R.id.cancel_btn)).perform(click());
    }

    @Test
    public void testDeleteButton() {
        onView(withId(R.id.delete_user_btn)).perform(click());
    }

    @Test
    public void testChangePasswordDialog() {
        onView(withId(R.id.change_password_btn)).perform(click());
    }

    @Test
    public void testInvalidEmailShowsError() {
        onView(withId(R.id.email)).perform(replaceText("bademail"));
        onView(withId(R.id.save_btn)).perform(click());

        activityRule.getScenario().onActivity(activity -> {
            EditText emailField = activity.findViewById(R.id.email);
            assert(emailField.getError() != null);
        });
    }

    @Test
    public void testInvalidUsernameShowsError() {
        onView(withId(R.id.user_name)).perform(replaceText("bad user"));
        onView(withId(R.id.save_btn)).perform(click());

        activityRule.getScenario().onActivity(activity -> {
            EditText usernameField = activity.findViewById(R.id.user_name);
            assert(usernameField.getError() != null);
        });
    }
}



