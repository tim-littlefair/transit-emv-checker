package net.heretical_camelid.transit_emv_checker.tests;
import net.heretical_camelid.transit_emv_checker.library.TapConductor;
import net.heretical_camelid.transit_emv_checker.library.APDUObserver;
import net.heretical_camelid.transit_emv_checker.library.TransitCapabilityChecker;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.xml.stream.XMLInputFactory;

public class BasicReplayTest  {
    @Test
    @Tag("run_with_gradle")
    public void testTapReplay() {
        String mediaCaptureBasename = "visa-exp2402-5406";
        Result result = replayMediaCapture(mediaCaptureBasename);

        /*
         * Assertions related to the summary, transitCapabilities
         * and diagnosticXml content
         */

        // Assertion related to summary
        assertFalse(result.summary().contains("4065890016415406")); // Actual PAN simulated in visa-exp2402-5406.xml
        assertTrue(result.summary().contains("406589FFFFFF5406"));  // Truncated PAN

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

        // Assertions related to the diagnostic XML ()

        // PAN masking
        assertTrue(!result.diagnosticXml().contains("4065890016415406"));
        assertTrue(result.diagnosticXml().contains("406589FFFFFF5406"));
        assertTrue(!result.diagnosticXml().contains("40 65 89 00 16 41 54 06"));
        assertTrue(result.diagnosticXml().contains("40 65 89 FF FF FF 54 06"));

        // Cardholder name masking
        // The rendered data in the input XML file for tag 0x5F20
        // looks like: "20 2F (= /)"
        // The hex bytes '20 2F' should be replaced by 'FF FF'
        // The ASCII  rendering ' /' should be replaced by '??'
        assertTrue(!result.diagnosticXml().contains("20 2F (= /)"));
        assertTrue(result.diagnosticXml().contains("FF FF (=??)"));
    }

    @Test
    @Tag("run_with_gradle")
    public void testReplayBug20() {
        // This bug can be reproduced by replaying a simulated
        // card containing the following applications:
        // Mastercard Debit (global)
        // EFTPOS (Australian)
        String mediaCaptureBasename = "bug20-full";
        Result result = replayMediaCapture(mediaCaptureBasename);

        // Check that the PPSE record has not been misidentified the DF
        // for an AID
        assertFalse(result.summary.contains("325041592E5359532E4444463031"));

        // Check that the masked PAN is visible and the unmasked PAN is not
        // (the unmasked PAN of the sensitive card is 5413339999998720)
        assertTrue(result.summary.contains(
            "Account Identifier:\n MPAN=541333FFFFFF8720"
        ));
        assertFalse(result.summary.contains(
            "Account Identifier:\n MPAN=5413339999998720"
        ));

        // Check that the right AIDs are found
        assertTrue(result.summary.contains("A0000000041010"));
        assertTrue(result.summary.contains("priority=02"));
        assertTrue(result.summary.contains("A00000038410"));
        assertTrue(result.summary.contains("priority=02"));
    }

    @Test
    @Tag("run_with_gradle")
    public void testReplay_0884() {
        String mediaCaptureBasename = "visa-exp2202-0884";
        Result result = replayMediaCapture(mediaCaptureBasename);
    }

    @Test
    @Tag("run_with_gradle")
    public void testReplay_5398() {
        String mediaCaptureBasename = "visa-exp2402-5398";
        Result result = replayMediaCapture(mediaCaptureBasename);
    }

    @Test
    @Tag("run_with_gradle")
    public void testReplay_3033() {
        String mediaCaptureBasename = "visa_auspost-exp1708-3033";
        Result result = replayMediaCapture(mediaCaptureBasename);
    }

    @Test
    @Tag("run_with_gradle")
    public void testReplay_5720() {
        String mediaCaptureBasename = "visa_velocity-exp1810-5720";
        Result result = replayMediaCapture(mediaCaptureBasename);
    }

    @Test
    @Tag("run_with_gradle")
    public void testReplay_ConnectionLostBeforeGPOResponse() {
        String mediaCaptureBasename = "connection_lost_before_GPO_response";
        Result result = replayMediaCapture(mediaCaptureBasename);
    }


    private static Result replayMediaCapture(String mediaCaptureBasename) {
        Logger.getLogger(
            "net.heretical_camelid.transit_emv_checker.library.APDUObserver"
        ).setLevel(Level.DEBUG);
        FileInputStream captureXmlStream;
        try {
            captureXmlStream = new FileInputStream(String.format(
                "src/main/assets/media_captures/%s.xml",
                mediaCaptureBasename
            ));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        XMLInputFactory xmlInFact = XMLInputFactory.newFactory();

        TapConductor trc = TapConductor.createReplayTapConductor(
            null, xmlInFact, captureXmlStream
        );

        assertTrue(trc.doPCIMasking());

        System.out.println("");
        String summary = trc.summary();
        System.out.println("Summary:\n" + summary);

        String transitCapabilities = trc.transitCapabilities();
        System.out.println("Transit capabilities:\n" + transitCapabilities);

        String diagnosticXml = trc.diagnosticXml();
        System.out.println("Diagnostic XML:\n" + diagnosticXml);
        Result result = new Result(summary, transitCapabilities, diagnosticXml);
        return result;
    }

    private record Result(String summary, String transitCapabilities, String diagnosticXml) {
    }

}
