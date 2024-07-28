package net.heretical_camelid.transit_emv_checker.android_app;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;

import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.SdkSuppress;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Basic sample for unbundled UiAutomator.
 */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class TEC_UiTestSuite extends TECTestSuiteBase {

    @Test
    public void testSimpleRunNoMediaPresented() {

        assertThat(mDevice, notNullValue());

        // Navigate to each of the other pages in turn
        checkNavigationPageContent(1, R.id.navigation_transit, "Transit", "Card");
        saveScreenshot("simple_run-transit");
        checkNavigationPageContent(2, R.id.navigation_emv_details, "EMV", "Card");
        saveScreenshot("simple_run-emv_details");
        checkNavigationPageContent(3, R.id.navigation_about, "About", "Version");
        saveScreenshot("simple_run-about");
        checkNavigationPageContent(0, R.id.navigation_home, "Home", null);
        saveScreenshot("simple_run-home");
    }
}

