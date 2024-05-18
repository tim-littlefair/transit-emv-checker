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
            m_mainActivity.logOnHomePage("Adapter initialized: " + m_nfcAdapter);
        }
    }

    public void enableDetection() {
        m_nfcAdapter.enableReaderMode(
            m_mainActivity, this,
            (
                NfcAdapter.FLAG_READER_NFC_A |
                NfcAdapter.FLAG_READER_NFC_B |
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
            ),null
        );
    }

    public void disableDetection() {
        m_nfcAdapter.disableReaderMode(m_mainActivity);
    }


    @Override
    public void onTagDiscovered(Tag tag) {
        m_mainActivity.logOnHomePage("Tag discovered: " + tag.describeContents());
        processMedia(tag);
        m_nfcAdapter.ignore(tag, 1000, null, null);
        m_nfcAdapter.disableReaderMode(m_mainActivity);
    }

    void processMedia(Tag emvMediaTag) {
        m_mainActivity.logOnHomePage("TODO: processing tag");
    }
}
