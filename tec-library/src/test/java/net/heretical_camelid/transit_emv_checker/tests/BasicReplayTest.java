package net.heretical_camelid.transit_emv_checker.tests;
import com.github.devnied.emvnfccard.parser.EmvTemplate;
import net.heretical_camelid.transit_emv_checker.library.TapReplayConductor;
import net.heretical_camelid.transit_emv_checker.library.APDUObserver;
import net.heretical_camelid.transit_emv_checker.library.TransitCapabilityChecker;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BasicReplayTest  {
    @Test
    public void testTapReplay() {
        TapReplayConductor trc;
        try {
            trc = new TapReplayConductor(
                new FileInputStream("src/main/resources/visa-exp2402-5406.xml"),
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
        assertTrue(summary.contains("5406"));

        TransitCapabilityChecker tcc = new TransitCapabilityChecker(apduObserver);
        String transitCapabilities = tcc.capabilityReport();
        System.out.println("Transit capabilities:\n" + transitCapabilities);
        assertTrue(transitCapabilities.contains("ODA supported - using CAPK #09"));

        final boolean _NOT_CAPTURE_ONLY = false;
        String diagnosticXml = apduObserver.toXmlString(_NOT_CAPTURE_ONLY);
        System.out.println("Diagnostic XML:\n" + diagnosticXml);

    }
}
