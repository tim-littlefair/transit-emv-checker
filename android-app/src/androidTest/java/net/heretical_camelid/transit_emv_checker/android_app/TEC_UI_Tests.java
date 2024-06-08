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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.SdkSuppress;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Basic sample for unbundled UiAutomator.
 */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class TEC_UI_Tests {

    static final Logger LOGGER = LoggerFactory.getLogger(TEC_UI_Tests.class);

    private static final String TEC_ANDROID_APP_PACKAGE =
        "net.heretical_camelid.transit_emv_checker.android_app";
    private static final String ANDROID_DOCUMENTSUI_PACKAGE =
        "com.android.documentsui";


    private static final int _LAUNCH_TIMEOUT_SECONDS = 30;

    // At any point where we are waiting for a UI element to
    // appear, we use this timeout
    private static final int _UI_APPEAR_TIMEOUT_SECONDS = 5;

    // At any point where we are waiting for a UI element to change
    // state, we use this unconditional sleep
    private static final int _UI_CHANGE_SLEEP_SECONDS = 2;

    private UiDevice mDevice;

    @Before
    public void startMainActivityFromHomeScreen() {


        // Initialize UiDevice instance
        mDevice = UiDevice.getInstance(getInstrumentation());

        // Start from the home screen
        mDevice.pressHome();

        // Wait for launcher
        final String launcherPackage = getLauncherPackageName();
        assertThat(launcherPackage, notNullValue());
        mDevice.wait(
            Until.hasObject(By.pkg(launcherPackage).depth(0)),
            _LAUNCH_TIMEOUT_SECONDS*1000
        );

        // Launch the TEC application
        Context context = getApplicationContext();
        final Intent launchIntent = context.getPackageManager()
                                  .getLaunchIntentForPackage(TEC_ANDROID_APP_PACKAGE);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);    // Clear out any previous instances
        context.startActivity(launchIntent);

        // Wait for the app to appear
        mDevice.wait(Until.hasObject(By.pkg(TEC_ANDROID_APP_PACKAGE).depth(0)), _LAUNCH_TIMEOUT_SECONDS);
    }

    @Test
    public void testSimpleRunNoCardPresented() {

        assertThat(mDevice, notNullValue());

        Pattern buttonPattern = Pattern.compile(
                "USE THIS FOLDER|ALLOW|SELECT|START EMV MEDIA DETECTION"
        );

        UiObject2 visibleButton;
        String visibleButtonText;
        do {
            visibleButton = mDevice.wait(Until.findObject(
                By.text(buttonPattern)
            ), _UI_APPEAR_TIMEOUT_SECONDS * 1000);
            assertThat(visibleButton, is(notNullValue()));
            visibleButtonText = visibleButton.getText();
            LOGGER.info("Button text: " + visibleButtonText);
            visibleButton.click();
        } while(visibleButtonText.equals("START EMV MEDIA DETECTION") == false);

        sleep(_UI_CHANGE_SLEEP_SECONDS);

        // counter-intuitive, but button is still clickable
        assertThat(visibleButton.isClickable(),is(equalTo(true)));

        // Navigate to each of the other pages in turn
        checkNavigationPageContent(1, R.id.navigation_transit, "Transit", "Card");
        checkNavigationPageContent(2, R.id.navigation_emv_details, "EMV", "Card");
        checkNavigationPageContent(3, R.id.navigation_about, "About", "Version");
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

        ViewInteraction textView3 = onView(
                allOf(withSubstring(expectedDisplayedSubstring),
                        withParent(withParent(IsInstanceOf.<View>instanceOf(android.webkit.WebView.class))),
                        isDisplayed()));
        assertThat(textView3,is(notNullValue()));
        // textView3.check(matches(withText(expectedDisplayedSubstring)));
    }

    @After
    public void waitOnFinalScreenForVisualInspection() {
        final int _END_OF_RUN_SLEEP_SECONDS = 5;
        sleep(_END_OF_RUN_SLEEP_SECONDS);
    }

    /**
     * Uses package manager to find the package name of the device launcher. Usually this package
     * is "com.android.launcher" but can be different at times. This is a generic solution which
     * works on all platforms.`
     */
    private String getLauncherPackageName() {
        // Create launcher Intent
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);

        // Use PackageManager to get the launcher package name
        PackageManager pm = getApplicationContext().getPackageManager();
        ResolveInfo resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo.activityInfo.packageName;
    }

    private void sleep(int numSeconds) {
        try {
            Thread.sleep(numSeconds * 1000 );
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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

