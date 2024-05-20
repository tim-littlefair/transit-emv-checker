package net.heretical_camelid.transit_emv_checker.library;

import com.github.devnied.emvnfccard.iso7816emv.ITerminal;
import fr.devnied.bitlib.BytesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

public class TapReplayConductor {
    public static final Logger LOGGER = LoggerFactory.getLogger(TapReplayConductor.class);

    final ArrayList<CommandAndResponse> m_commandsAndResponses;
    APDUObserver m_apduObserver;
    TapReplayArbiter m_arbiter;
    TapReplayProvider m_provider;
    ITerminal m_terminal;

    public TapReplayConductor(
        InputStream captureXml,
        ITerminal terminal
    ) {
        m_commandsAndResponses = parseXml(captureXml);
        m_apduObserver = new APDUObserver(new PCIMaskingAgent());
        m_arbiter = new TapReplayArbiter();
        m_provider = new TapReplayProvider(this);
        if(terminal!=null) {
            m_terminal = terminal;
        } else {
            m_terminal = new TransitTerminal();
        }
    }

    private ArrayList<CommandAndResponse> parseXml(InputStream captureXml) {
        ArrayList<CommandAndResponse> commandsAndResponses = new ArrayList<>();
        CommandAndResponse carItem = null;
        try {
            XMLInputFactory xmlInFact = XMLInputFactory.newInstance();
            XMLStreamReader reader = xmlInFact.createXMLStreamReader(captureXml);
            while (reader.hasNext()) {
                int nextToken = reader.next();
                if (nextToken == XMLStreamConstants.START_ELEMENT) {
                    String elementName = String.valueOf(reader.getName());
                    if (elementName.equals("command_and_response")) {
                        assert carItem == null;
                        carItem = new CommandAndResponse();
                        carItem.stepName = reader.getAttributeValue(null, "step_name");
                    } else {
                        assert carItem != null;
                        if (elementName.equals("raw_command")) {
                            carItem.rawCommand = BytesUtils.fromString(reader.getElementText());
                        } else if (elementName.equals("raw_response")) {
                            carItem.rawResponse = BytesUtils.fromString(reader.getElementText());
                        }
                    }
                } else if (nextToken == XMLStreamConstants.END_ELEMENT) {
                    assert carItem != null;
                    String elementName = String.valueOf(reader.getName());
                    if (elementName.equals("command_and_response")) {
                        assert carItem.rawCommand != null;
                        assert carItem.rawResponse != null;
                        TapReplayConductor.LOGGER.info("Adding step: " + carItem.stepName);
                        commandsAndResponses.add(carItem);
                        carItem = null;
                    }
                }
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
        return commandsAndResponses;
    }

    public static void main(String[] args) {
        MyProviderBase.LOGGER.info(String.join("\n", args));

        try {
            TapReplayConductor trc= new TapReplayConductor(
                new FileInputStream(args[0]),
                null
            );
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public APDUObserver getAPDUObserver() {
        return m_apduObserver;
    }

    public TapReplayArbiter getArbiter() {
        return m_arbiter;
    }
}
