package net.heretical_camelid.transit_emv_checker.android_app;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;

import com.github.devnied.emvnfccard.exception.CommunicationException;
import com.github.devnied.emvnfccard.parser.EmvTemplate;

import net.heretical_camelid.transit_emv_checker.library.*;

import java.io.IOException;

public class EMVMediaAgent implements NfcAdapter.ReaderCallback {
    public static final int TIMEOUT_5000_MS = 5000;
    private final MainActivity m_mainActivity;
    private final NfcAdapter m_nfcAdapter;
    private final EmvTemplate.Builder m_templateBuilder;

    public EMVMediaAgent(MainActivity mainActivity) {
        m_mainActivity = mainActivity;
        m_nfcAdapter = NfcAdapter.getDefaultAdapter(m_mainActivity);
        if(m_nfcAdapter!=null) {
            m_mainActivity.homePageLogAppend("Adapter initialized: " + m_nfcAdapter);
        } else {
            m_mainActivity.homePageLogAppend("No NFC adapter found");
        }
        m_templateBuilder = createTemplateBuilder();
    }

    public void enableDetection() {
        if(m_nfcAdapter != null) {
            m_mainActivity.homePageLogAppend("About to enable NFC reader");
            m_nfcAdapter.enableReaderMode(
                m_mainActivity, this,
                (
                    NfcAdapter.FLAG_READER_NFC_A |
                        NfcAdapter.FLAG_READER_NFC_B |
                        NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK |
                        NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
                ), null
            );
            m_mainActivity.homePageLogAppend("NFC reader enabled");
        } else {
            m_mainActivity.homePageLogAppend("No NFC adapter => cant enable detection");
        }

    }

    public void disableDetection() {
        m_nfcAdapter.disableReaderMode(m_mainActivity);
    }


    @Override
    public void onTagDiscovered(Tag tag) {
        m_mainActivity.homePageLogAppend("Tag discovered: " + tag.describeContents());
        disableDetection();
        processNfcMedia(tag);
    }

    public void processNfcMedia(Tag emvMediaTag) {
        IsoDep tagAsIsoDep = IsoDep.get(emvMediaTag);
        if(tagAsIsoDep == null) {
            m_mainActivity.homePageLogAppend("Detected media is not compatible with EMV");
            m_mainActivity.setInitialState();
        } else {
            m_mainActivity.homePageLogAppend("Attempting to connect to EMV media");
            try {
                tagAsIsoDep.setTimeout(TIMEOUT_5000_MS);
                tagAsIsoDep.connect(); /* Can throw IOException */
                if (tagAsIsoDep.isConnected()) {
                    m_mainActivity.homePageLogAppend("Successfully connected to EMV media");
                }

                // Create a provider to read the media
                AndroidNFCProvider provider = new AndroidNFCProvider(tagAsIsoDep);

                processMedia(provider);
            } catch (CommunicationException e) {
                m_mainActivity.homePageLogAppend(
                    "Reading or parsing of EMV media content failed with error:\n" +
                        e.getMessage()
                );
                m_mainActivity.setInitialState();
            } catch (IOException e) {
                m_mainActivity.homePageLogAppend(
                    "Attempt to connect to EMV media failed with exception:\n" +
                        e.getMessage()
                );
                m_mainActivity.setInitialState();
            }

            try {
                tagAsIsoDep.close();
            } catch (java.io.IOException e) {
                // Handle exception silently
            }
        }
    }

    private void processMedia(MyProviderBase provider) throws CommunicationException {

        // Create an observer object for APDUs and data extracted from them
        PCIMaskingAgent pciMaskingAgent = new PCIMaskingAgent();
        APDUObserver apduObserver = new APDUObserver(pciMaskingAgent);

        // Connect the provider to the observer
        provider.setApduStore(apduObserver);

        EmvTemplate template = m_templateBuilder.setProvider(provider).build();
        MyParser mParser = new MyParser(template, apduObserver);
        template.addParsers(mParser);

        m_mainActivity.homePageLogAppend("About to read and parse media EMV content");
        template.readEmvCard();
        pciMaskingAgent.maskAccountData(apduObserver);
        TransitCapabilityChecker tcc = new TransitCapabilityChecker(apduObserver);
        String transitCapabilityReport = tcc.capabilityReport();
        String emvDetailsSummary = apduObserver.summary();
        if(transitCapabilityReport==null) {
            m_mainActivity.homePageLogAppend("Failed to retrieve transit capabilities");
            m_mainActivity.setInitialState();
        } else if (emvDetailsSummary==null) {
            m_mainActivity.homePageLogAppend("Failed to retrieve EMV details");
            m_mainActivity.setInitialState();
        } else {
            m_mainActivity.homePageLogAppend("Media EMV content read and parsed successfully");
            String diagnosticXml = apduObserver.toXmlString(false);
            String xmlFileBaseName = apduObserver.mediumStateId();
            m_mainActivity.setDisplayMediaDetailsState(transitCapabilityReport,emvDetailsSummary, diagnosticXml, xmlFileBaseName);
            m_mainActivity.homePageLogAppend("Switch to Transit tab to see Transit Capabilities of this card");
        }
    }

    private static EmvTemplate.Builder createTemplateBuilder() {
        EmvTemplate.Config config = EmvTemplate.Config()
            .setContactLess(true)
            .setReadAllAids(true)
            .setReadTransactions(false)
            .setReadCplc(false)
            .setRemoveDefaultParsers(true)
            .setReadAt(true)
            ;
        return EmvTemplate.Builder()
            .setConfig(config)
            .setTerminal(new TransitTerminal());
    }
}
