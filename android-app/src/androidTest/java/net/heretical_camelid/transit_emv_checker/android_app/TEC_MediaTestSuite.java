package net.heretical_camelid.transit_emv_checker.android_app;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import androidx.annotation.NonNull;
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
        CapturedTapResult result = getCapturedTapResult("visa-exp2402-5406");

        // No tests implemented for diagnosticXml or captureOnlyXml yet,
        // but at least check they are non-null.

        // Assertions related to transitCapabilities
        assertTrue(result.transitCapabilities().contains(
            "AIP byte 1 bit 1 not set => CDA not supported (but DDA is)"
        ));
        assertTrue(result.transitCapabilities().contains(
            "ODA supported - using CAPK #09"
        ));
        assertTrue(result.transitCapabilities().contains(
            "Application validity period ended 24 02 29"
        ));
        checkNavigationPageContent(
            1,
            R.id.navigation_transit,
            "Transit",
            result.transitCapabilities(),
            "simple_tap"
        );

        // Assertion related to summary (appears on 'EMV Details' tab)
        assertFalse(result.summary().contains("4065890016415406")); // Actual PAN simulated in visa-exp2402-5406.xml
        assertTrue(result.summary().contains("406589FFFFFF5406"));  // Truncated PAN
        checkNavigationPageContent(
            2,
            R.id.navigation_emv_details,
            "EMV",
            result.summary(),
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

    // Additional tests for a variety of captured media

    @Test
    public void testReplay_0884() {
        CapturedTapResult result = getCapturedTapResult("visa-exp2202-0884");
    }

    @Test
    public void testReplay_5398() {
        CapturedTapResult result = getCapturedTapResult("visa-exp2402-5398");
    }

    @Test
    public void testReplay_3033() {
        CapturedTapResult result = getCapturedTapResult("visa_auspost-exp1708-3033");
    }

    @Test
    public void testReplay_5720() {
        CapturedTapResult result = getCapturedTapResult("visa_velocity-exp1810-5720");
    }

    @Test
    public void testReplace_connnection_lost_scenarios1() {
        getCapturedTapResult("connection_lost_before_PPSE_response");
    }

    @Test
    public void testReplace_connnection_lost_scenarios2() {
        getCapturedTapResult("connection_lost_during_PPSE_response");
    }

    @Test
    public void testReplace_connnection_lost_scenarios3() {
        getCapturedTapResult("connection_lost_during_SELECT_APP_command");
    }

    @Test
    public void testReplace_connnection_lost_scenarios4() {
        getCapturedTapResult("connection_lost_before_GPO_response");
    }

    private @NonNull CapturedTapResult getCapturedTapResult(String mediaAssetName) {
        ITerminal terminal = null;
        assertThat(mDevice, notNullValue());

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
        CapturedTapResult result = new CapturedTapResult(summary, transitCapabilities, diagnosticXml, captureOnlyXml);
        assertNotNull(result.summary());
        assertNotNull(result.transitCapabilities());
        assertNotNull(result.diagnosticXml());
        assertNotNull(result.captureOnlyXml());
        return result;
    }

    private record CapturedTapResult(String summary, String transitCapabilities, String diagnosticXml, String captureOnlyXml) {
    }
}

