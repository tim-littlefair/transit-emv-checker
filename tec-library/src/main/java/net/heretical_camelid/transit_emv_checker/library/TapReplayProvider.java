package net.heretical_camelid.transit_emv_checker.library;

import com.github.devnied.emvnfccard.exception.CommunicationException;
import com.github.devnied.emvnfccard.parser.IProvider;
import fr.devnied.bitlib.BytesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.smartcardio.Card;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import javax.xml.stream.XMLStreamConstants;

public class TapReplayProvider extends MyProviderBase implements IProvider {
    static final Logger LOGGER = LoggerFactory.getLogger(TapReplayProvider.class);

    ArrayList<CommandAndResponse> m_commandsAndResponses;

    public TapReplayProvider(InputStream captureXml, APDUObserver apduStore) {
        super(apduStore);
        m_commandsAndResponses = new ArrayList<>();
        parseXml(captureXml);
    }

    @Override
    protected byte[] implementationTransceive(byte[] pCommand, ByteBuffer receiveBuffer) throws CommunicationException {
        return new byte[0];
    }

    @Override
    public byte[] getAt() {
        return new byte[0];
    }

    private void parseXml(InputStream captureXml) {
        CommandAndResponse carItem = null;
        try {
            XMLInputFactory xmlInFact = XMLInputFactory.newInstance();
            XMLStreamReader reader = xmlInFact.createXMLStreamReader(captureXml);
            while(reader.hasNext()) {
                int nextToken = reader.next();
                if(nextToken == XMLStreamConstants.START_ELEMENT) {
                    String elementName=String.valueOf(reader.getName());
                    if(elementName.equals("command_and_response")) {
                        assert carItem == null;
                        carItem = new CommandAndResponse();
                        carItem.stepName = reader.getAttributeValue(null, "step_name");
                    } else if(elementName.equals("raw_command")) {
                        carItem.rawCommand = BytesUtils.fromString(reader.getElementText());
                    } else if(elementName.equals("raw_response")) {
                        carItem.rawResponse = BytesUtils.fromString(reader.getElementText());
                    }
                } else if(nextToken == XMLStreamConstants.END_ELEMENT) {
                    assert carItem != null;
                    String elementName=String.valueOf(reader.getName());
                    if(elementName.equals("command_and_response")) {
                        assert carItem.rawCommand != null;
                        assert carItem.rawResponse != null;
                        LOGGER.info("Adding step: " + carItem.stepName);
                        m_commandsAndResponses.add(carItem);
                        carItem = null;
                    }
                }
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        LOGGER.info(String.join("\n", args));

        try {
            TapReplayProvider trp = new TapReplayProvider(
                new FileInputStream(args[0]),
                null
            );
            LOGGER.info("step count: " + trp.m_commandsAndResponses.size());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
