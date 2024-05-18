package net.heretical_camelid.transit_emv_checker.android_app;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;

import com.github.devnied.emvnfccard.exception.CommunicationException;
import com.github.devnied.emvnfccard.model.EmvCard;
import com.github.devnied.emvnfccard.parser.EmvTemplate;

import net.heretical_camelid.transit_emv_checker.library.*;

import java.io.IOException;

public class EMVMediaAgent implements NfcAdapter.ReaderCallback {
    public static final int TIMEOUT_5000_MS = 5000;
    private final MainActivity m_mainActivity;
    private final NfcAdapter m_nfcAdapter;

    public EMVMediaAgent(MainActivity mainActivity) {
        m_mainActivity = mainActivity;
        m_nfcAdapter = NfcAdapter.getDefaultAdapter(m_mainActivity);
        if(m_nfcAdapter!=null) {
            m_mainActivity.homePageLogAppend("Adapter initialized: " + m_nfcAdapter);
        } else {
            m_mainActivity.homePageLogAppend("No NFC adapter found");
        }
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
        processMedia(tag);
        m_nfcAdapter.disableReaderMode(m_mainActivity);
    }

    void processMedia(Tag emvMediaTag) {
        IsoDep tagAsIsoDep = IsoDep.get(emvMediaTag);
        if(tagAsIsoDep == null) {
            m_mainActivity.homePageLogAppend("Media does not support IsoDep mode");
        } else {
            do {
                m_mainActivity.homePageLogAppend("Attempting to connect to tag in IsoDep mode");
                try {
                    tagAsIsoDep.setTimeout(TIMEOUT_5000_MS);
                    tagAsIsoDep.connect();
                    if (tagAsIsoDep.isConnected()) {
                        m_mainActivity.homePageLogAppend("Successfully connected to tag in IsoDep mode");
                    }
                } catch (IOException e) {
                    m_mainActivity.homePageLogAppend(
                        "Attempt to connect to tag in IsoDep mode failed with exception:\n" +
                            e.getMessage()
                    );
                    continue;
                }

                // Create an observer object for APDUs and data extracted from them
                PCIMaskingAgent pciMaskingAgent = new PCIMaskingAgent();
                APDUObserver apduObserver = new APDUObserver(pciMaskingAgent);

                AndroidNFCProvider provider = new AndroidNFCProvider(apduObserver, tagAsIsoDep);
                TransitTerminal terminal = new TransitTerminal();

                EmvTemplate.Config config = EmvTemplate.Config()
                    .setContactLess(true)
                    .setReadAllAids(true)
                    .setReadTransactions(false)
                    .setReadCplc(false)
                    .setRemoveDefaultParsers(true)
                    .setReadAt(true)
                    ;
                EmvTemplate template = EmvTemplate.Builder() //
                    .setProvider(provider)
                    .setConfig(config)
                    .setTerminal(terminal)
                    .build();
                MyParser mParser = new MyParser(template, apduObserver);
                template.addParsers(mParser);

                try {
                    m_mainActivity.homePageLogAppend("About to read and parse media EMV content");
                    EmvCard card = template.readEmvCard();
                    m_mainActivity.homePageLogAppend("Media EMV content read and parsed successfully");
                } catch (CommunicationException e) {
                    m_mainActivity.homePageLogAppend(
                        "Reading or parsing of EMV media content failed with error:\n" +
                        e.getMessage()
                    );
                    continue;
                }
                pciMaskingAgent.maskAccountData(apduObserver);
                TransitCapabilityChecker tcc = new TransitCapabilityChecker(apduObserver);
                m_mainActivity.setDisplayMediaDetailsState(tcc.capabilityReport(),apduObserver.summary());
            }
            while (false);

            if (tagAsIsoDep != null) {
                try {
                    tagAsIsoDep.close();
                } catch (java.io.IOException e) {
                    // Handle exception silently
                }
            }
        }
    }
}
