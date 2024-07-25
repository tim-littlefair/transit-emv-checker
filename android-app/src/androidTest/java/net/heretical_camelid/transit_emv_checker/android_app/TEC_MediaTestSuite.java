package net.heretical_camelid.transit_emv_checker.android_app;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Basic sample for unbundled UiAutomator.
 */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class TEC_MediaTestSuite extends TECTestSuiteBase {

    @Test
    public void testSimpleRunNoMediaPresented() {

        assertThat(mDevice, notNullValue());

        // Navigate to each of the other pages in turn
        checkNavigationPageContent(1, R.id.navigation_transit, "Transit", "Card");
        checkNavigationPageContent(2, R.id.navigation_emv_details, "EMV", "Card");
        checkNavigationPageContent(3, R.id.navigation_about, "About", "Version");
        checkNavigationPageContent(0, R.id.navigation_home, "Home", null);
    }

}

