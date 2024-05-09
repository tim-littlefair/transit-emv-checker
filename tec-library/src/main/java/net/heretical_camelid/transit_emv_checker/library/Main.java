package net.heretical_camelid.transit_emv_checker.library;

import java.util.List;
import java.nio.file.Paths;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.smartcardio.TerminalFactory;
import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.devnied.emvnfccard.exception.CommunicationException;
import com.github.devnied.emvnfccard.model.EmvCard;
import com.github.devnied.emvnfccard.parser.EmvTemplate;
import com.github.devnied.emvnfccard.parser.EmvTemplate.Config;

@SuppressWarnings("restriction")
public class Main {
	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	public static void main(final String[] args) throws CardException, CommunicationException {

		System.out.println("");

		TerminalFactory factory = TerminalFactory.getDefault();
		List<CardTerminal> terminals = factory.terminals().list();
		if (terminals.isEmpty()) {
			throw new CardException("No card terminals available");
		}

		PCIMaskingAgent thePCIMaskingAgent = new PCIMaskingAgent();
		APDUObserver apduObserver = new APDUObserver(thePCIMaskingAgent);

		if (terminals != null && !terminals.isEmpty()) {
			// Use the first terminal
			CardTerminal terminal = terminals.get(0);

			if (terminal.waitForCardPresent(0)) {
				// Connect with the card
				Card card = terminal.connect("*");

				// Create provider
				PCSCProvider provider = new PCSCProvider(card, apduObserver);
				
				// Define config
				Config config = EmvTemplate.Config()
						.setContactLess(true)
						.setReadAllAids(true)
						.setReadTransactions(false)
						.setReadAt(false)
						// Reading CPLC is presently disabled for two reasons:
						// 1) It is not interesting for the purposes of this application
						// 2) With some of the cards I have to hand, devnied's implementation 
						//    in v3.0.2-SNAPSHOT of this throws an exception because the 
						//    two byte pattern 0xFF 0xFF is not accepted as a placeholder 
						//    for an undefined date.  I plan to raise a PR on devnied's
						//    github project to tolerate this value in the same way the value
						//    0x00 0x00 is tolerated.
						.setReadCplc(false)
						// This application substitutes an alternate implementation of 
						// the parser, see the comment on MyParser.extractCommonsCardData
						// for why the local implementation is chosen over devnied's 
						// EmvParser.
						.setRemoveDefaultParsers(true);
				
				// Create Parser
				EmvTemplate template = EmvTemplate.Builder() //
						.setProvider(provider) // Define provider
						.setConfig(config) // Define config
						.setTerminal(new TransitTerminal())
						.build();
				template.addParsers(new MyParser(template, apduObserver));
				
				// Read card
				EmvCard emvCard = template.readEmvCard();
				
				// Disconnect the card
				card.disconnect(false);

				// At this point apduObserver contains raw data relating to the 
				// transaction - before we can dump this in a PCI-compliant 
				// environment we need to mask all occurrences of the PAN
				// and the cardholder name.
				boolean pciMaskingOk = thePCIMaskingAgent.maskAccountData(apduObserver);

				if(pciMaskingOk == true) {
					// TODO?: Allow args to control XML output directory/filename

					System.out.println("Summary:\n\n" + apduObserver.summary());

					String outDirName = "_work/";

					try {
						Files.createDirectories(Paths.get(outDirName));

						String outPathPrefix = outDirName + apduObserver.mediumStateId();

						String fullXmlText = apduObserver.toXmlString(false);
						writeReportToFile(outPathPrefix+"-full.xml", fullXmlText);

						String captureOnlyXmlText = apduObserver.toXmlString(true);
						writeReportToFile(outPathPrefix+"-capture.xml", captureOnlyXmlText);

						System.out.println(
							"Full and capture-only reports have been dumped to:\n" + 
							outPathPrefix + "-*.xml"
						);

						TransitCapabilityChecker tcc = new TransitCapabilityChecker(apduObserver);
						System.out.println("\n\nTransit capabilities:\n\n" + tcc.capabilityReport());

					} catch (IOException e) {
						LOGGER.error("Problem writing reports out");
						e.printStackTrace();
					}
				} else {
					LOGGER.info("No summary or tap dumps because PCI masking failed");
				}
			}
		} else {
			LOGGER.error("No pcsc terminal found");
		}
		System.out.println("");
	}

	private static void writeReportToFile(String outPath, String outXmlText) throws IOException {
		File outFile = new File(outPath);
		FileWriter outFileWriter = new FileWriter(outFile.getAbsoluteFile()); 
		outFileWriter.write(outXmlText);
		outFileWriter.close();
	}
}
