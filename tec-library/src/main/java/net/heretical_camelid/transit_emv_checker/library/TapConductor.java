package net.heretical_camelid.transit_emv_checker.library;

import com.github.devnied.emvnfccard.exception.CommunicationException;
import com.github.devnied.emvnfccard.iso7816emv.ITerminal;
import com.github.devnied.emvnfccard.model.EmvCard;
import com.github.devnied.emvnfccard.parser.EmvTemplate;
import com.github.devnied.emvnfccard.parser.IProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLInputFactory;

import java.io.InputStream;

public class TapConductor {
    public static final Logger LOGGER = LoggerFactory.getLogger(TapConductor.class);

    private final PCIMaskingAgent m_pciMaskingAgent;
    private final APDUObserver m_apduObserver;
    private final TapReplayAgent m_tapReplayAgent;
    private final ITerminal m_terminal;
    private IProvider m_provider;
    private String m_summary = null;
    private String m_transitCapabilities = null;
    private String m_diagnosticXml = null;
    private String m_captureOnlyXml = null;

    public TapConductor(
        ITerminal terminal,
        IProvider provider,
        TapReplayAgent tapReplayAgent) {
        m_pciMaskingAgent = new PCIMaskingAgent();
        m_apduObserver = new APDUObserver(m_pciMaskingAgent);

        if(terminal!=null) {
            m_terminal = terminal;
        } else {
            m_terminal = new TransitTerminal();
        }

        m_tapReplayAgent = tapReplayAgent;
        if(m_tapReplayAgent != null) {
            assert provider == null;
            m_tapReplayAgent.setTapConductor(this);
            assert m_provider != null;
        } else {
            assert provider != null;
            m_provider = provider;
        }
    }

    public static TapConductor createRealTapConductor(
        ITerminal terminal,
        IProvider provider
    ) {
        TapConductor trc = new TapConductor(terminal, provider, null);
        finalizeTap(trc);
        return trc;
    }

    public static TapConductor createReplayTapConductor(
        ITerminal terminal,
        XMLInputFactory xmlInputFactory,
        InputStream is
    ) {
        TapReplayAgent tra = new TapReplayAgent(xmlInputFactory, is);
        TapConductor trc = new TapConductor(terminal, null, tra);
        finalizeTap(trc);
        return trc;
    }

    private static void finalizeTap(TapConductor trc) {
        EmvTemplate template = trc.build();
        try {
            trc.play(template);
            assert true == trc.doPCIMasking();
        }
        catch(IllegalArgumentException e) {
            System.err.println(String.format("", e.getMessage()));
            e.printStackTrace(System.err);
        }
    }


    public APDUObserver getAPDUObserver() {
        return m_apduObserver;
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
            boolean maskingSucceeded = m_pciMaskingAgent.maskAccountData(m_apduObserver);
            assert maskingSucceeded = true;

            m_summary = m_apduObserver.summary();
            TransitCapabilityChecker tcc = new TransitCapabilityChecker(m_apduObserver);
            m_transitCapabilities = tcc.capabilityReport();
            final boolean _CAPTURE_ONLY_FALSE = false;
            m_diagnosticXml = m_apduObserver.toXmlString(_CAPTURE_ONLY_FALSE);
            final boolean _CAPTURE_ONLY_TRUE = true;
            m_captureOnlyXml = m_apduObserver.toXmlString(_CAPTURE_ONLY_TRUE);
        } catch (CommunicationException e) {
            // For failed connection scenarios this is acceptable
            LOGGER.warn(e.getMessage());
        }


    }

    public boolean doPCIMasking() {
        return m_pciMaskingAgent.maskAccountData(m_apduObserver);
    }

    public String summary() { return m_summary; }
    public String transitCapabilities() { return m_transitCapabilities; }
    public String diagnosticXml() { return m_diagnosticXml; }
    public String captureOnlyXml() { return m_captureOnlyXml; }

    public void setProvider(IProvider provider) {
        m_provider = provider;
    }
}
