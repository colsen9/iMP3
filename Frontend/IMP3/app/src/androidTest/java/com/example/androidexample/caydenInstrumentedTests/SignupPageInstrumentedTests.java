package com.example.androidexample.caydenInstrumentedTests;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.androidexample.R;
import com.example.androidexample.SignupPage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class SignupPageInstrumentedTests {

    @Before
    public void setUp() {
        ActivityScenario.launch(SignupPage.class);
    }

    @Test
    public void testEmptyFieldsShowsErrors() {
        onView(withId(R.id.signup_btn)).perform(click());

        onView(withId(R.id.username_edit))
                .check(matches(hasErrorText("Username is required")));
        onView(withId(R.id.email_edit))
                .check(matches(hasErrorText("Email is required")));
        onView(withId(R.id.password_edit))
                .check(matches(hasErrorText("Password is required")));
        onView(withId(R.id.confirm_edit))
                .check(matches(hasErrorText("Please confirm your password")));
    }

    @Test
    public void testInvalidEmailShowsError() {
        onView(withId(R.id.email_edit))
                .perform(typeText("invalidemail"), closeSoftKeyboard());
        onView(withId(R.id.signup_btn)).perform(click());

        onView(withId(R.id.email_edit))
                .check(matches(hasErrorText("Invalid email format")));
    }

    @Test
    public void testPasswordMismatchShowsError() {
        onView(withId(R.id.password_edit))
                .perform(typeText("Password1"), closeSoftKeyboard());
        onView(withId(R.id.confirm_edit))
                .perform(typeText("Password2"), closeSoftKeyboard());
        onView(withId(R.id.signup_btn)).perform(click());

        onView(withId(R.id.confirm_edit))
                .check(matches(hasErrorText("Passwords do not match")));
    }

    @Test
    public void testWeakPasswordShowsError() {
        onView(withId(R.id.password_edit))
                .perform(typeText("pass"), closeSoftKeyboard());
        onView(withId(R.id.confirm_edit))
                .perform(typeText("pass"), closeSoftKeyboard());
        onView(withId(R.id.signup_btn)).perform(click());

        onView(withId(R.id.password_edit))
                .check(matches(hasErrorText("Password must be at least 8 characters long")));
    }

    @Test
    public void testShortUsernameShowsError() {
        onView(withId(R.id.username_edit))
                .perform(typeText("ab"), closeSoftKeyboard());
        onView(withId(R.id.signup_btn)).perform(click());

        onView(withId(R.id.username_edit))
                .check(matches(hasErrorText("Username must be at least 3 characters")));
    }

    @Test
    public void testSuccessfulSignupNavigatesToProfile() throws InterruptedException {
        String uniqueEmail = "testuser" + System.currentTimeMillis() + "@test.com";

        onView(withId(R.id.username_edit))
                .perform(typeText("TestUser"), closeSoftKeyboard());
        onView(withId(R.id.email_edit))
                .perform(typeText(uniqueEmail), closeSoftKeyboard());
        onView(withId(R.id.password_edit))
                .perform(typeText("Password1"), closeSoftKeyboard());
        onView(withId(R.id.confirm_edit))
                .perform(typeText("Password1"), closeSoftKeyboard());

        onView(withId(R.id.signup_btn)).perform(click());

        Thread.sleep(1000);

        onView(withId(R.id.user_bio)).check(matches(isDisplayed()));
    }

    @Test
    public void testEmailConflictShowsError() throws InterruptedException {
        String existingEmail = "existinguser@test.com";

        onView(withId(R.id.username_edit))
                .perform(typeText("ConflictUser"), closeSoftKeyboard());
        onView(withId(R.id.email_edit))
                .perform(typeText(existingEmail), closeSoftKeyboard());
        onView(withId(R.id.password_edit))
                .perform(typeText("Password1"), closeSoftKeyboard());
        onView(withId(R.id.confirm_edit))
                .perform(typeText("Password1"), closeSoftKeyboard());

        onView(withId(R.id.signup_btn)).perform(click());

        Thread.sleep(1000);

        onView(withId(R.id.email_edit))
                .check(matches(hasErrorText("Email or username already exists")));
        onView(withId(R.id.username_edit))
                .check(matches(hasErrorText("Email or username already exists")));
    }
}




