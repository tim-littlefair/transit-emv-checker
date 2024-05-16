package net.heretical_camelid.transit_emv_checker.android_app;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;

import android.nfc.Tag;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
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


public class EMVMediaActivity extends FragmentActivity {
    private NfcAdapter m_nfcAdapter = null;

    //private MainActivity m_mainActivity;
    //private PendingIntent m_pendingIntent =null;

    // private boolean m_nfcCheckDone = false;
    // final static String TAG = "DETC.MainActivity";


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        m_nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        enableDetection();
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag nfcMediaTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        processMedia(nfcMediaTag);
    }

    public void enableDetection() {
        PendingIntent pi = PendingIntent.getActivity(
            this, 0,
            new Intent(
                this, this.getClass()
            ).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        );
        m_nfcAdapter.enableForegroundDispatch(
            this, pi, null, null
        );
    }

    void processMedia(Tag emvMediaTag) {
        Toast.makeText(this, "Something detected",Toast.LENGTH_LONG);
    }

}
