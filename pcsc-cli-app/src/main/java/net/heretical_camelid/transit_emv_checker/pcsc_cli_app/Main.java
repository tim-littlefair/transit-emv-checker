package net.heretical_camelid.transit_emv_checker.pcsc_cli_app;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import javax.xml.stream.XMLInputFactory;

import net.heretical_camelid.transit_emv_checker.library.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

//import org.sl4j.Logger;
//import org.sl4j.Level;
//import org.sl4j.LoggerFactory;

import com.github.devnied.emvnfccard.exception.CommunicationException;
import com.github.devnied.emvnfccard.iso7816emv.ITerminal;
import com.github.devnied.emvnfccard.model.EmvCard;
import com.github.devnied.emvnfccard.parser.EmvTemplate;
import com.github.devnied.emvnfccard.parser.EmvTemplate.Config;
import com.github.devnied.emvnfccard.parser.IProvider;

@SuppressWarnings("restriction")
public class Main {
	private static final Logger LOGGER = Logger.getLogger(Main.class);

	public static void main(final String[] args) {
		Logger.getRootLogger().setLevel(Level.DEBUG);
		try {
			TapConductor tc;

			ITerminal transitTerminal = new TransitTerminal();
			if (args.length == 0) {
				tc = getTapConductorForPhysicalCard(transitTerminal);
			} else if (args.length == 1){
				XMLInputFactory xmlInFact = XMLInputFactory.newFactory();
				FileInputStream fis = new FileInputStream(args[0]);
				tc = TapConductor.createReplayTapConductor(
					transitTerminal, xmlInFact, fis
				);
			} else {
				LOGGER.error("Unexpected number of arguments");
				System.exit(1001);
				return;
			}
			APDUObserver apduObserver = tc.getAPDUObserver();

			// TODO?: Allow args to control XML output directory/filename
			LOGGER.info("Summary:");
			LOGGER.info(apduObserver.summary());

			String outDirName = "_work/";

			try {
				Files.createDirectories(Paths.get(outDirName));

				String outPathPrefix = outDirName + apduObserver.mediumStateId();

				String fullXmlText = apduObserver.toXmlString(false);
				writeReportToFile(outPathPrefix + "-full.xml", fullXmlText);

				String captureOnlyXmlText = apduObserver.toXmlString(true);
				writeReportToFile(outPathPrefix + "-capture.xml", captureOnlyXmlText);

				//System.out.println(
				LOGGER.info(
					"Full and capture-only reports have been dumped to: " +
					outPathPrefix + "-*.xml"
				);

				TransitCapabilityChecker tcc = new TransitCapabilityChecker(apduObserver);
				LOGGER.info("Transit capabilities:");
				LOGGER.info(tcc.capabilityReport());
			} catch (IOException e) {
				LOGGER.error("Problem writing reports out",e);
				System.exit(1002);
			}
		} catch (IOException e) {
			LOGGER.error("Problem before attempting to write reports out",e);
			System.exit(1003);
		}
	}

	private static TapConductor getTapConductorForPhysicalCard(ITerminal emvTerminal) {
		try {
			TerminalFactory factory = TerminalFactory.getDefault();
			List<CardTerminal> terminals = factory.terminals().list();
			if (terminals == null) {
				LOGGER.error("Null PCSC reader list returned - check your hardware supports PCSC");
				return null;
			} else if (terminals.isEmpty()) {
				LOGGER.error("No PCSC readers found - check you have a compatible reader and it is connected");
				return null;
			}
			CardTerminal terminal = terminals.get(0);
			if (terminal.waitForCardPresent(0) == false) {
				LOGGER.error("Wait for PCSC card failed");
				return null;
			}

			Card card = terminal.connect("*");
			PCSCProvider pcscProvider = new PCSCProvider(card);
			TapConductor tc = TapConductor.createRealTapConductor(emvTerminal, pcscProvider);
			//pcscProvider.setApduStore(tc.getAPDUObserver());
			return tc;
		} catch (CardException e) {
			reportException(e);
		}
		return null;
	}

	private static void reportException(Throwable e) {
		LOGGER.error("Caught exception",e);
	}

	private static void writeReportToFile(String outPath, String outXmlText) throws IOException {
		File outFile = new File(outPath);
		FileWriter outFileWriter = new FileWriter(outFile.getAbsoluteFile()); 
		outFileWriter.write(outXmlText);
		outFileWriter.close();
	}
}
