package net.heretical_camelid.transit_emv_checker.android_app;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;

import com.github.devnied.emvnfccard.iso7816emv.ITerminal;

import net.heretical_camelid.transit_emv_checker.library.TapReplayConductor;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Basic sample for unbundled UiAutomator.
 */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class TEC_MediaTestSuite extends TECTestSuiteBase {

    @Test
    public void testReplaySimpleTap() {

        assertThat(mDevice, notNullValue());

        String mediaAssetName = "visa-exp2402-5406";
        ITerminal terminal = null;

        // I desperately want to find a better way of navigating to the MainActivity
        // without making it expose an instance data member.
        MainActivity mainActivity = MainActivity.s_activeInstance;
        assert mainActivity != null;

        TapReplayConductor trc = mainActivity.replayCapturedTap(mediaAssetName, terminal);

        // Providing we reach this point the tap replay conductor has not only been constructed
        // but has also replayed the tap, masked all of the PCI data, and can be used
        // to generate the four output reports from the replay event.
        String summary = trc.summary();
        String transitCapabilities = trc.transitCapabilities();
        String diagnosticXml = trc.diagnosticXml();
        String captureOnlyXml = trc.captureOnlyXml();

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
        checkNavigationPageContent(
            1,
            R.id.navigation_transit,
            "Transit",
            transitCapabilities,
            "simple_tap"
        );

        // Assertion related to summary (appears on 'EMV Details' tab)
        assertFalse(summary.contains("4065890016415406")); // Actual PAN simulated in visa-exp2402-5406.xml
        assertTrue(summary.contains("406589FFFFFF5406"));  // Truncated PAN
        checkNavigationPageContent(
            2,
            R.id.navigation_emv_details,
            "EMV",
            summary,
            "simple_tap"
        );

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

