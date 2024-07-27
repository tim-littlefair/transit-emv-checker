package net.heretical_camelid.transit_emv_checker.android_app;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;

import net.heretical_camelid.transit_emv_checker.library.TapReplayConductor;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.stream.XMLInputFactory;

/**
 * Basic sample for unbundled UiAutomator.
 */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class TEC_MediaTestSuite extends TECTestSuiteBase {

    @Test
    public void testReplaySimpleTap() {

        assertThat(mDevice, notNullValue());

        InputStream captureXmlStream;
        try {
            String assetFilename = "media_captures/visa-exp2402-5406.xml";
            Context context = ApplicationProvider.getApplicationContext();
            captureXmlStream = context.getAssets().open(assetFilename);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        TapReplayConductor trc = TapReplayConductor.createTapReplayConductor(
            XMLInputFactory.newInstance(),
            captureXmlStream,
            null
        );

        assertTrue(trc.doPCIMasking());

        String summary = trc.summary();
        String transitCapabilities = trc.transitCapabilities();

        /*
         * Assertions related to the summary, transitCapabilities
         * and diagnosticXml content
         */

        // Assertions related to transitCapabilities
        assertTrue(transitCapabilities.contains(
            "AIP byte 1 bit 1 not set => CDA not supported (but DDA is)"
        ));
        assertTrue(transitCapabilities.contains(
            "ODA supported - using CAPK #09"
        ));
        assertTrue(transitCapabilities.contains(
            "Application validity period ended 24 02 29"
        ));
        checkNavigationPageContent(1, R.id.navigation_transit, "Transit", transitCapabilities);

        // Assertion related to summary (appears on 'EMV Details' tab)
        assertFalse(summary.contains("4065890016415406")); // Actual PAN simulated in visa-exp2402-5406.xml
        assertTrue(summary.contains("406589FFFFFF5406"));  // Truncated PAN
        checkNavigationPageContent(2, R.id.navigation_emv_details, "EMV", "Summary");

        /*
        // Assertions related to the diagnostic XML ()
        String diagnosticXml = trc.diagnosticXml();

        // PAN masking
        assertFalse(diagnosticXml.contains("4065890016415406"));
        assertTrue(diagnosticXml.contains("406589FFFFFF5406"));
        assertTrue(!diagnosticXml.contains("40 65 89 00 16 41 54 06"));
        assertTrue(diagnosticXml.contains("40 65 89 FF FF FF 54 06"));

        // Cardholder name masking
        // The rendered data in the input XML file for tag 0x5F20
        // looks like: "20 2F (= /)"
        // The hex bytes '20 2F' should be replaced by 'FF FF'
        // The ASCII  rendering ' /' should be replaced by '??'
        assertTrue(!diagnosticXml.contains("20 2F (= /)"));
        assertTrue(diagnosticXml.contains("FF FF (=??)"));
        */
    }

}

