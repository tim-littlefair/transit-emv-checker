package net.heretical_camelid.transit_emv_checker.android_app;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.test.espresso.accessibility.AccessibilityChecks;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public class TECTestSuiteBase {
    static final Logger LOGGER = LoggerFactory.getLogger(TEC_UiTestSuite.class);
    private static final String TEC_ANDROID_APP_PACKAGE =
        "net.heretical_camelid.transit_emv_checker.android_app";
    private static final int _LAUNCH_TIMEOUT_SECONDS = 30;
    // At any point where we are waiting for a UI element to
    // appear, we use this timeout
    private static final int _UI_APPEAR_TIMEOUT_SECONDS = 10;
    // At any point where we are waiting for a UI element to change
    // state, we use this unconditional sleep
    protected static final int _UI_CHANGE_SLEEP_SECONDS = 2;
    protected UiDevice mDevice;

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
            sleep(_UI_CHANGE_SLEEP_SECONDS);
            sleep(_UI_CHANGE_SLEEP_SECONDS);
            visibleButton = mDevice.wait(Until.findObject(
                By.text(buttonPattern)
            ), _UI_APPEAR_TIMEOUT_SECONDS * 1000);
            assertThat(visibleButton, is(notNullValue()));
            sleep(_UI_CHANGE_SLEEP_SECONDS);
            sleep(_UI_CHANGE_SLEEP_SECONDS);
            sleep(_UI_CHANGE_SLEEP_SECONDS);
            visibleButtonText = visibleButton.getText();
            LOGGER.info("Button text: " + visibleButtonText);
            visibleButton.click();
        } while(visibleButtonText.equals("START EMV MEDIA DETECTION") == false);

        sleep(_UI_CHANGE_SLEEP_SECONDS);

        // counter-intuitive, but button is still clickable
        assertThat(visibleButton.isClickable(),is(equalTo(true)));
    }
}
