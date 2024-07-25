package net.heretical_camelid.transit_emv_checker.android_app;

// This test class is based on the AOSP provided example project:
// https://github.com/android/testing-samples/blob/main/ui/uiautomator/BasicSample/app/src/androidTest/java/com/example/android/testing/uiautomator/BasicSample/ChangeTextBehaviorTest.java
// and is subject to the same Apache 2.0 license as the upstream file

// Documentation on the uiautomator framework is at:
// https://developer.android.com/training/testing/other-components/ui-automator

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
        checkNavigationPageContent(2, R.id.navigation_emv_details, "EMV", "Card");
        checkNavigationPageContent(3, R.id.navigation_about, "About", "Version");
        checkNavigationPageContent(0, R.id.navigation_home, "Home", null);
    }

    private void checkNavigationPageContent(
            int navigationPosition,
            int navigationResourceId,
            String pageNavigationText,
            String expectedDisplayedSubstring
    ) {
        ViewInteraction bottomNavigationItemView3 = onView(
                allOf(withId(navigationResourceId), withContentDescription(pageNavigationText),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.nav_view),
                                        0),
                                navigationPosition),
                        isDisplayed()));
        bottomNavigationItemView3.perform(click());

        if(expectedDisplayedSubstring==null) {
            // Home screen is not based on WebView so the logic
            // below is not applicable
        } else {
            ViewInteraction textView3 = onView(
                allOf(withSubstring(expectedDisplayedSubstring),
                    withParent(withParent(IsInstanceOf.<View>instanceOf(android.webkit.WebView.class))),
                    isDisplayed()));
            assertThat(textView3, is(notNullValue()));

        }
        // Allow the screen to be displayed briefly to the human test observer
        // if there is one
        sleep(3);
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}

