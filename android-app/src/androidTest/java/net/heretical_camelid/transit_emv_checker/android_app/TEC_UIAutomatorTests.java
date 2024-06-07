package net.heretical_camelid.transit_emv_checker.android_app;

// This test class is based on the AOSP provided example project:
// https://github.com/android/testing-samples/blob/main/ui/uiautomator/BasicSample/app/src/androidTest/java/com/example/android/testing/uiautomator/BasicSample/ChangeTextBehaviorTest.java
// and is subject to the same Apache 2.0 license as the upstream file

// Documentation on the uiautomator framework is at:
// https://developer.android.com/training/testing/other-components/ui-automator

import android.content.ComponentName;
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


    private static final int LAUNCH_TIMEOUT = 10000;

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
        mDevice.wait(Until.hasObject(By.pkg(launcherPackage).depth(0)), LAUNCH_TIMEOUT);

        // Launch the TEC application
        Context context = getApplicationContext();
        final Intent launchIntent = context.getPackageManager()
                                  .getLaunchIntentForPackage(TEC_ANDROID_APP_PACKAGE);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);    // Clear out any previous instances
        context.startActivity(launchIntent);

        // Wait for the app to appear
        mDevice.wait(Until.hasObject(By.pkg(TEC_ANDROID_APP_PACKAGE).depth(0)), LAUNCH_TIMEOUT);
    }

    @Test
    public void testSimpleRunNoCardPresented() {

        assertThat(mDevice, notNullValue());

        // First screen displayed is the system file picker, for selection under
        // user control of the folder to be opened as the app's documents directory
        final int _SFP_TIMEOUT_MS = 1000;
        UiObject2 sfpSelectButton = mDevice.wait(Until.findObject(
            By.res(ANDROID_DOCUMENTSUI_PACKAGE, "R.id.button1")
        ), _SFP_TIMEOUT_MS);
        assertThat(sfpSelectButton, is(notNullValue()));
        assertThat(sfpSelectButton.getText(),is(equalTo("SELECT")));
        sfpSelectButton.click();

        /*
            .setText(STRING_TO_BE_TYPED);
        mDevice.findObject(By.res(TEC_ANDROID_APP_PACKAGE, "changeTextBt"))
            .click();

        // Verify the test is displayed in the Ui
        UiObject2 changedText = mDevice
                                    .wait(Until.findObject(By.res(TEC_ANDROID_APP_PACKAGE, "textToBeChanged")),
                                        500 );
        assertThat(changedText.getText(), is(equalTo(STRING_TO_BE_TYPED)));
         */
    }
/*
    @Test
    public void testChangeText_newActivity() {
        // Type text and then press the button.
        mDevice.findObject(By.res(TEC_ANDROID_APP_PACKAGE, "editTextUserInput"))
            .setText(STRING_TO_BE_TYPED);
        mDevice.findObject(By.res(TEC_ANDROID_APP_PACKAGE, "activityChangeTextBtn"))
            .click();

        // Verify the test is displayed in the Ui
        UiObject2 changedText = mDevice
                                    .wait(Until.findObject(By.res(TEC_ANDROID_APP_PACKAGE, "show_text_view")),
                                        500 );
        assertThat(changedText.getText(), is(equalTo(STRING_TO_BE_TYPED)));
    }
*/

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
}

