package net.heretical_camelid.transit_emv_checker.android_app;

import android.app.Activity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class TEC_EspressoTest {

    @Rule public ActivityScenarioRule<MainActivity> activityScenarioRule =
        new ActivityScenarioRule<>(MainActivity.class);

    @Test public void startDetection() {
        sleep(5);
        onView(withId(R.id.button_home)).perform(click());
        sleep(5);
        onView(withId(R.id.navigation_transit)).perform(click());
        sleep(5);
    }
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("net.heretical_camelid.transit_emv_checker.android_app", appContext.getPackageName());
    }

    private void sleep(int numSeconds) {
        try {
            Thread.sleep(numSeconds * 1000 );
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}