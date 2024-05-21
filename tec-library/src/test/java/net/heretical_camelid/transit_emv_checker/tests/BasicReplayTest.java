package net.heretical_camelid.transit_emv_checker.tests;
import com.github.devnied.emvnfccard.parser.EmvTemplate;
import net.heretical_camelid.transit_emv_checker.library.TapReplayConductor;
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
        trc.play(template);

        assertTrue(trc.doPCIMasking());

        String summary = trc.getAPDUObserver().summary();
        assertTrue(summary.contains("5406"));
    }
}
