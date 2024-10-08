package net.heretical_camelid.transit_emv_checker.android_app;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.core.graphics.BitmapStorage.writeToTestStorage;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import androidx.test.core.app.DeviceCapture;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.accessibility.AccessibilityChecks;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import androidx.test.core.graphics.BitmapStorage;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.regex.Pattern;
import junit.framework.AssertionFailedError;

// Some logic in this test suite base class is based on the AOSP provided example project:
// https://github.com/android/testing-samples/blob/main/ui/uiautomator/BasicSample/app/src/androidTest/java/com/example/android/testing/uiautomator/BasicSample/ChangeTextBehaviorTest.java
// and is subject to the same Apache 2.0 license as the upstream file

// Documentation on the uiautomator framework is at:
// https://developer.android.com/training/testing/other-components/ui-automator

public class TECTestSuiteBase {
    static final Logger LOGGER = LoggerFactory.getLogger(TEC_UiTestSuite.class);
    private static final String TEC_ANDROID_APP_PACKAGE =
        "net.heretical_camelid.transit_emv_checker.android_app";
    private static final int _LAUNCH_TIMEOUT_SECONDS = 15;
    // At any point where we are waiting for a UI element to
    // appear, we use this timeout
    private static final int _UI_APPEAR_TIMEOUT_SECONDS = 15;
    // At any point where we are waiting for a UI element to change
    // state, we use this unconditional sleep
    protected static final int _UI_CHANGE_SLEEP_SECONDS = 3;
    protected UiDevice mDevice;

    static Matcher<View> childAtPosition(
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
            _LAUNCH_TIMEOUT_SECONDS * 1000
        );

        // Launch the TEC application
        Context context = getApplicationContext();
        final Intent launchIntent = context.getPackageManager()
                                        .getLaunchIntentForPackage(TEC_ANDROID_APP_PACKAGE);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);    // Clear out any previous instances
        context.startActivity(launchIntent);

        // Wait for the app to appear
        mDevice.wait(Until.hasObject(By.pkg(TEC_ANDROID_APP_PACKAGE).depth(0)), _LAUNCH_TIMEOUT_SECONDS);

        // Enable Espresso accessibility checks
        AccessibilityChecks.enable();

        // Process the startup disclaimer etc.
        navigateFromStartupToHomeTab();
    }

    @After
    public void waitOnFinalScreenForVisualInspection() {
        final int _END_OF_RUN_SLEEP_SECONDS = 2;
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

    protected void sleep(int numSeconds) {
        try {
            Thread.sleep(numSeconds * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void navigateFromStartupToHomeTab() {
        Pattern buttonPattern = Pattern.compile(
                "ALLOW|I UNDERSTAND AND AGREE|START EMV MEDIA DETECTION"
        );

        UiObject2 visibleButton;
        String visibleButtonText;
        do {
            sleep(_UI_CHANGE_SLEEP_SECONDS);
            visibleButton = mDevice.wait(Until.findObject(
                By.text(buttonPattern)
            ), _UI_APPEAR_TIMEOUT_SECONDS * 1000);
            assertThat(visibleButton, is(notNullValue()));
            sleep(_UI_CHANGE_SLEEP_SECONDS);
            visibleButtonText = visibleButton.getText();
            LOGGER.info("Button text: " + visibleButtonText);
            visibleButton.click();
        } while(visibleButtonText.equals("START EMV MEDIA DETECTION") == false);

        sleep(_UI_CHANGE_SLEEP_SECONDS);

        // counter-intuitive, but button is still clickable
        assertThat(visibleButton.isClickable(),is(equalTo(true)));
    }

    protected void checkNavigationPageContent(
        int navigationPosition,
        int navigationResourceId,
        String pageNavigationText,
        String expectedDisplayedSubstring,
        String screenshotContextPrefix
    ) {
        ViewInteraction bottomNavigationItemView3 = onView(
                allOf(withId(navigationResourceId), withContentDescription(pageNavigationText),
                        TECTestSuiteBase.childAtPosition(
                                TECTestSuiteBase.childAtPosition(
                                        withId(R.id.nav_view),
                                        0),
                                navigationPosition),
                        isDisplayed()));
        bottomNavigationItemView3.perform(click());

        if(expectedDisplayedSubstring==null) {
            // Home screen is not based on WebView so the logic
            // below is not applicable
        } else {
            sleep(_UI_CHANGE_SLEEP_SECONDS);
            ViewInteraction textView3 = onView(allOf(
                isDisplayed(),
                IsInstanceOf.<View>instanceOf(android.webkit.WebView.class)
            ));
            assertThat(textView3, is(notNullValue()));
            ViewAssertion substringChecker = (view, noViewFoundException) -> {
                WebView wv = (WebView) view;
                if(wv==null) {
                    throw noViewFoundException;
                }
                wv.evaluateJavascript(
                    "document.documentElement.outerHTML",
                    new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            value = value.replace("\\u003C","<")
                                         .replace("\\n","\n")
                                         .replace("&gt;",">");
                            if(value.contains(expectedDisplayedSubstring)) {
                                LOGGER.debug(
                                    "Expected string %s found",
                                    expectedDisplayedSubstring
                                );
                            } else {
                                String assertionMessage = String.format(
                                    "Expected string %s not found in HTML:\n%s",
                                    expectedDisplayedSubstring, value
                                );
                                LOGGER.error(assertionMessage);
                                throw new AssertionFailedError(assertionMessage);
                            }

                        }
                    }
                );
            };
            textView3.check(substringChecker);
        }
        if(screenshotContextPrefix != null) {
            saveScreenshot(screenshotContextPrefix + "-" + pageNavigationText);
        }
    }

    public void saveScreenshot(String ssName) {
        Bitmap bm = DeviceCapture.takeScreenshot();
        try {
            BitmapStorage.writeToTestStorage(bm,ssName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
