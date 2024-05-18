package net.heretical_camelid.transit_emv_checker.android_app;

import android.nfc.NfcAdapter;
import android.nfc.Tag;

public class EMVMediaAgent implements NfcAdapter.ReaderCallback {
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
                        NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
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
        m_mainActivity.homePageLogAppend("TODO: processing tag");
    }
}
