package net.heretical_camelid.transit_emv_checker.android_app;

// This test class is based on the AOSP provided example project:
// https://github.com/android/testing-samples/blob/main/ui/uiautomator/BasicSample/app/src/androidTest/java/com/example/android/testing/uiautomator/BasicSample/ChangeTextBehaviorTest.java
// and is subject to the same Apache 2.0 license as the upstream file

// Documentation on the uiautomator framework is at:
// https://developer.android.com/training/testing/other-components/ui-automator

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
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
public class TEC_UIAutomatorTests {

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

        // First screen displayed is the system file picker, for selection under
        // user control of the folder to be opened as the app's documents directory
        UiObject2 sfpSelectButton = mDevice.wait(Until.findObject(

            // Depending on whether this is the first run of the app after a
            // clean install or not, the system file picker may present either
            // a "USE THIS FOLDER" or "SELECT" button which needs to be clicked
            // to progress.

            // We would prefer to identify the button by its resource ID but
            // this does not work (presumably because the system file picker does
            // not make its resource id's visible in the same way as our own app).
            // We are falling back to identifying it by displayed text, but must bear
            // in mind that this will probably fail for a device with a non-English
            // language setting.

            // By.res(ANDROID_DOCUMENTSUI_PACKAGE, "R.id.button1")
            By.text(Pattern.compile("SELECT|USE THIS FOLDER"))

        ), _UI_APPEAR_TIMEOUT_SECONDS * 1000);
        assertThat(sfpSelectButton, is(notNullValue()));
        String sfpSelectButtonText = sfpSelectButton.getText();
        sfpSelectButton.click();
        if(sfpSelectButtonText.equals("USE THIS FOLDER")) {
            UiObject2 allowButton = mDevice.wait(Until.findObject(
                By.text(Pattern.compile("ALLOW"))
            ), _UI_APPEAR_TIMEOUT_SECONDS * 1000);
            assertThat(allowButton, is(notNullValue()));
            allowButton.click();
        } else {
            assertThat(sfpSelectButton.getText(), is(equals("SELECT")));
        }

        // Second screen displayed is the home screen, where the user is required
        // to click on the 'Start Detection' button to enable NFC polling
        UiObject2 startDetectionButton = mDevice.wait(Until.findObject(
            //By.res(TEC_ANDROID_APP_PACKAGE, "R.id.button_home")
            By.text("START EMV MEDIA DETECTION")
        ), _UI_APPEAR_TIMEOUT_SECONDS);
        assertThat(startDetectionButton, is(notNullValue()));
        assertThat(startDetectionButton.isClickable(),is(equalTo(true)));
        assertThat(startDetectionButton.getText(),is(equalTo("START EMV MEDIA DETECTION")));
        startDetectionButton.click();
        sleep(_UI_CHANGE_SLEEP_SECONDS);
        // counter-intuitive, but button is still clickable
        assertThat(startDetectionButton.isClickable(),is(equalTo(true)));
        assertThat(startDetectionButton.getText(),is(equalTo("WAITING FOR EMV MEDIA")));

        // Navigate to each of the other pages in turn
        //checkNavigationPageContent("Transit", ".*No media presented.*");
        //checkNavigationPageContent("EMV Details", ".*No media presented.*");
        checkNavigationPageContent("About", ".*Version.*");
    }

    private void checkNavigationPageContent(String pageNavigationText, String expectedDisplayedRegex) {
        Pattern expectedDisplayedPattern = Pattern.compile(expectedDisplayedRegex,Pattern.MULTILINE);
        UiObject2 pageNavigationButton = mDevice.wait(Until.findObject(
            By.text(pageNavigationText)
        ), _UI_APPEAR_TIMEOUT_SECONDS);

        /*
         * The logic above does not work yet
        assertThat(pageNavigationButton, is(notNullValue()));
        assertThat(pageNavigationButton.isClickable(),is(equalTo(true)));
        pageNavigationButton.click();
        UiObject2 pageDisplayObject = mDevice.wait(Until.findObject(
            By.text(expectedDisplayedPattern)
        ), _UI_APPEAR_TIMEOUT_SECONDS);
        assertThat(pageDisplayObject, is(notNullValue()));
         */
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
}

