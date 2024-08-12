package net.heretical_camelid.transit_emv_checker.library;

import com.github.devnied.emvnfccard.exception.CommunicationException;
import com.github.devnied.emvnfccard.iso7816emv.ITerminal;
import com.github.devnied.emvnfccard.model.EmvCard;
import com.github.devnied.emvnfccard.parser.EmvTemplate;
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
    private final PCIMaskingAgent m_pciMaskingAgent;
    private final APDUObserver m_apduObserver;
    private final TapReplayArbiter m_arbiter;
    private final TapReplayProvider m_provider;
    private final ITerminal m_terminal;
    private String m_summary = null;
    private String m_transitCapabilities = null;
    private String m_diagnosticXml = null;
    private String m_captureOnlyXml = null;

    public TapReplayConductor(
        XMLInputFactory xmlInputFactory,
        InputStream captureXml,
        ITerminal terminal) {
        m_commandsAndResponses = parseXml(captureXml, xmlInputFactory);
        m_pciMaskingAgent = new PCIMaskingAgent();
        m_apduObserver = new APDUObserver(m_pciMaskingAgent);
        m_arbiter = new TapReplayArbiter();
        m_provider = new TapReplayProvider(this);
        if(terminal!=null) {
            m_terminal = terminal;
        } else {
            m_terminal = new TransitTerminal();
        }
    }

    public static TapReplayConductor createTapReplayConductor(
        XMLInputFactory xmlInputFactory,
        InputStream is, ITerminal terminal
    ) {
        TapReplayConductor trc = new TapReplayConductor(xmlInputFactory, is, terminal);
        EmvTemplate template = trc.build();
        try {
            trc.play(template);
        }
        catch(IllegalArgumentException e) {
            System.err.println(String.format("", e.getMessage()));
            e.printStackTrace(System.err);
        }
        return trc;
    }

    public APDUObserver getAPDUObserver() {
        return m_apduObserver;
    }

    public TapReplayArbiter getArbiter() {
        return m_arbiter;
    }

    public EmvTemplate build() {
        EmvTemplate.Config config = EmvTemplate.Config()
            .setContactLess(true)
            .setReadAllAids(true)
            .setReadTransactions(false)
            .setReadCplc(false)
            .setRemoveDefaultParsers(true)
            .setReadAt(true)
            ;
        EmvTemplate template = EmvTemplate.Builder() //
            .setProvider(m_provider)
            .setConfig(config)
            .setTerminal(m_terminal)
            .build();
        MyParser parser = new MyParser(template, m_apduObserver);
        template.addParsers(parser);
        return template;
    }

    public void play(EmvTemplate template) {
        try {
            EmvCard card = template.readEmvCard();
        } catch (CommunicationException e) {
            throw new RuntimeException(e);
        }

        boolean maskingSucceeded = m_pciMaskingAgent.maskAccountData(m_apduObserver);
        assert maskingSucceeded = true;

        m_summary = m_apduObserver.summary();
        TransitCapabilityChecker tcc = new TransitCapabilityChecker(m_apduObserver);
        m_transitCapabilities = tcc.capabilityReport();
        final boolean _CAPTURE_ONLY_FALSE = false;
        m_diagnosticXml = m_apduObserver.toXmlString(_CAPTURE_ONLY_FALSE);
        final boolean _CAPTURE_ONLY_TRUE = true;
        m_captureOnlyXml = m_apduObserver.toXmlString(_CAPTURE_ONLY_TRUE);
    }

    public boolean doPCIMasking() {
        return m_pciMaskingAgent.maskAccountData(m_apduObserver);
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
                            carItem.rawCommand = BytesUtils.fromString(reader.getElementText());
                        } else if (elementName.equals("raw_response")) {
                            assert carItem != null;
                            carItem.rawResponse = BytesUtils.fromString(reader.getElementText());
                        }
                    }
                } else if (nextToken == XMLStreamConstants.END_ELEMENT) {
                    String elementName = String.valueOf(reader.getName());
                    if (elementName.equals("command_and_response")) {
                        assert carItem != null;
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

/*
    public static void main(String[] args) {
        TapReplayConductor trc;

        try {
            trc = new TapReplayConductor(
                XMLInputFactory.newFactory(),
                new FileInputStream(args[0]),
                null
            );
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        EmvTemplate template = trc.build();
        trc.play(template);
    }
*/
    public ArrayList<CommandAndResponse> getCommandsAndResponses() {
        return m_commandsAndResponses;
    }

    public String summary() { return m_summary; }
    public String transitCapabilities() { return m_transitCapabilities; }
    public String diagnosticXml() { return m_diagnosticXml; }
    public String captureOnlyXml() { return m_captureOnlyXml; }
}
