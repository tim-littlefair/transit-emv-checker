package net.heretical_camelid.transit_emv_checker.android_app;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;

import com.github.devnied.emvnfccard.enums.SwEnum;
import com.github.devnied.emvnfccard.exception.CommunicationException;
import com.github.devnied.emvnfccard.iso7816emv.EmvTags;
import com.github.devnied.emvnfccard.iso7816emv.ITag;
import com.github.devnied.emvnfccard.iso7816emv.ITerminal;
import com.github.devnied.emvnfccard.iso7816emv.TLV;
import com.github.devnied.emvnfccard.iso7816emv.TagAndLength;
import com.github.devnied.emvnfccard.iso7816emv.TerminalTransactionQualifiers;
import com.github.devnied.emvnfccard.iso7816emv.impl.DefaultTerminalImpl;
import com.github.devnied.emvnfccard.model.Application;
import com.github.devnied.emvnfccard.model.EmvTrack2;
import com.github.devnied.emvnfccard.model.enums.ApplicationStepEnum;
import com.github.devnied.emvnfccard.model.enums.CardStateEnum;
import com.github.devnied.emvnfccard.parser.EmvTemplate;
import com.github.devnied.emvnfccard.model.EmvCard;
import com.github.devnied.emvnfccard.parser.IProvider;
import com.github.devnied.emvnfccard.parser.impl.EmvParser;
import com.github.devnied.emvnfccard.utils.ResponseUtils;
import com.github.devnied.emvnfccard.utils.TlvUtil;

import net.sf.scuba.tlv.TLVInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import fr.devnied.bitlib.BytesUtils;


public class EMVMediaAccess {
    private MainActivity m_mainActivity;
    private PendingIntent m_pendingIntent =null;

    private NfcAdapter m_nfcAdapter = null;
    private boolean m_nfcCheckDone = false;
    final static String TAG = "DETC.MainActivity";

    EMVMediaAccess(MainActivity mainActivity) {
        m_mainActivity = mainActivity;
    }

    PendingIntent prepareIntent(NfcAdapter adapter) {
        PendingIntent retval = null;

        // Following
        // https://developer.android.com/guide/topics/connectivity/nfc/advanced-nfc#java
        if(adapter != null) {
            // Log.i(LOG_TAG,"default NFC adapter found");
            retval = PendingIntent.getActivity(
                    m_mainActivity, 0,
                    new Intent(
                            m_mainActivity, m_mainActivity.getClass()
                    ).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_MUTABLE
            );
        } else {
            // Log.w(LOG_TAG, "default NFC adapter not found");
        }
        return retval;
    }

    NfcAdapter getNfcAdapter() {
        if(!m_nfcCheckDone) {
            m_nfcAdapter = NfcAdapter.getDefaultAdapter(m_mainActivity);
            m_nfcCheckDone = true;
        }
        return m_nfcAdapter;
    }

    public void disableNfcDetection() {
        if(getNfcAdapter() != null) {
            m_nfcAdapter.disableForegroundDispatch(m_mainActivity);
        }
    }

    public void enableNfcDetection() {
        if(getNfcAdapter() != null ) {
            if(m_pendingIntent ==null) {
                m_pendingIntent = prepareIntent(m_nfcAdapter);
            }
            m_nfcAdapter.enableForegroundDispatch(
                    m_mainActivity, m_pendingIntent, null, null
            );
            // Log.i(TAG,"onResume() enabled foreground dispatch");
        }
    }

    void processMedia() {

    }

}
