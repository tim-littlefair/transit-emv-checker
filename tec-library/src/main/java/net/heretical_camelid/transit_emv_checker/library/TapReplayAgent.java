package net.heretical_camelid.transit_emv_checker.library;

import com.github.devnied.emvnfccard.parser.IProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import fr.devnied.bitlib.BytesUtils;

public class TapReplayAgent {
    public static final Logger LOGGER = LoggerFactory.getLogger(TapReplayAgent.class);

    private final ArrayList<CommandAndResponse> m_commandsAndResponses;
    private final TapReplayArbiter m_arbiter;
    private TapReplayProvider m_provider = null;
    private TapConductor m_tapConductor = null;


    public TapReplayAgent(
        XMLInputFactory xmlInputFactory,
        InputStream captureXml
    ) {
        m_commandsAndResponses = parseXml(captureXml, xmlInputFactory);
        m_arbiter = new TapReplayArbiter();
    }

    public void setTapConductor(TapConductor tapConductor) {
        m_tapConductor = tapConductor;
        m_provider = new TapReplayProvider(this, m_tapConductor);
        m_provider.setApduStore(m_tapConductor.getAPDUObserver());
        m_tapConductor.setProvider(m_provider);
    }

    public TapReplayArbiter getArbiter() {
        return m_arbiter;
    }

    private ArrayList<CommandAndResponse> parseXml(
        InputStream captureXml, XMLInputFactory xmlInFact
    ) {
        ArrayList<CommandAndResponse> commandsAndResponses = new ArrayList<>();
        CommandAndResponse carItem = null;
        try {
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
                        if (elementName.equals("raw_command")) {
                            assert carItem != null;
                            String elementTextNoSpaces = reader.getElementText().replace(" ", "");
                            carItem.rawCommand = BytesUtils.fromString(elementTextNoSpaces);
                        } else if (elementName.equals("raw_response")) {
                            assert carItem != null;
                            String elementTextNoSpaces = reader.getElementText().replace(" ", "");
                            try {
                                carItem.rawResponse = BytesUtils.fromString(elementTextNoSpaces);
                            } catch (IllegalArgumentException e) {
                                LOGGER.warn(
                                    "elementTextNoSpaces = %s length %d",
                                    elementTextNoSpaces, elementTextNoSpaces.length()
                                );
                                carItem.rawResponse = null;
                            }
                        } else if (elementName.equals("interpreted_response_status")) {
                            assert carItem != null;
                            carItem.interpretedResponseStatus = reader.getElementText();
                        }
                    }
                } else if (nextToken == XMLStreamConstants.END_ELEMENT) {
                    String elementName = String.valueOf(reader.getName());
                    if (elementName.equals("command_and_response")) {
                        assert carItem != null;
                        assert carItem.rawCommand != null;
                        // assert carItem.rawResponse != null;
                        TapConductor.LOGGER.info("Adding step: " + carItem.stepName);
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

    public ArrayList<CommandAndResponse> getCommandsAndResponses() {
        return m_commandsAndResponses;
    }

    public IProvider getProvider() {
        return m_provider;
    }
}
