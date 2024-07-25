package net.heretical_camelid.transit_emv_checker.tests;
import com.github.devnied.emvnfccard.parser.EmvTemplate;
import net.heretical_camelid.transit_emv_checker.library.TapReplayConductor;
import net.heretical_camelid.transit_emv_checker.library.APDUObserver;
import net.heretical_camelid.transit_emv_checker.library.TransitCapabilityChecker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BasicReplayTest  {
    @Test
    @Tag("run_with_gradle")
    public void testTapReplay() {
        TapReplayConductor trc;
        try {
            trc = new TapReplayConductor(
                new FileInputStream("src/main/assets/visa-exp2402-5406.xml"),
                null
            );
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        EmvTemplate template = trc.build();
        try {
            trc.play(template);
        }
        catch(IllegalArgumentException e) {
            System.err.println(String.format("", e.getMessage()));
            e.printStackTrace(System.err);
        }

        assertTrue(trc.doPCIMasking());

        System.out.println("");
        APDUObserver apduObserver = trc.getAPDUObserver();
        String summary = apduObserver.summary();
        System.out.println("Summary:\n" + summary);

        TransitCapabilityChecker tcc = new TransitCapabilityChecker(apduObserver);
        String transitCapabilities = tcc.capabilityReport();
        System.out.println("Transit capabilities:\n" + transitCapabilities);

        final boolean _NOT_CAPTURE_ONLY = false;
        String diagnosticXml = apduObserver.toXmlString(_NOT_CAPTURE_ONLY);
        System.out.println("Diagnostic XML:\n" + diagnosticXml);

        /* 
         * Assertions related to the summary, transitCapabilities 
         * and diagnosticXml content
         */

        // Assertion related to summary
        assertTrue(!summary.contains("4065890016415406")); // Actual PAN simulated in visa-exp2402-5406.xml
        assertTrue(summary.contains("406589FFFFFF5406"));  // Truncated PAN 

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

        // Assertions related to the diagnostic XML ()

        // PAN masking
        assertTrue(!diagnosticXml.contains("4065890016415406"));
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
    }
}
