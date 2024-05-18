package net.heretical_camelid.transit_emv_checker.android_app;

import android.nfc.tech.IsoDep;
import com.github.devnied.emvnfccard.exception.CommunicationException;
import com.github.devnied.emvnfccard.parser.IProvider;
import net.heretical_camelid.transit_emv_checker.library.APDUObserver;
import net.heretical_camelid.transit_emv_checker.library.MyProviderBase;

import java.io.IOException;
import java.nio.ByteBuffer;

class AndroidNFCProvider extends MyProviderBase implements IProvider {
    private IsoDep m_tagAsIsoDep;

    public AndroidNFCProvider(APDUObserver apduStore, IsoDep tagAsIsoDep) {
        super(apduStore);
        m_tagAsIsoDep = tagAsIsoDep;
    }

    @Override
    public byte[] implementationTransceive(final byte[] pCommand, ByteBuffer receiveBuffer) throws CommunicationException {

        byte[] response;
        try {
            response = m_tagAsIsoDep.transceive(pCommand);
            // TODO:
            // Work out whether post-call content of receiveBuffer matters
            // for this implementation - maybe refactor base and PCSC sibling
            // to remove ambiguity over whether receiveBuffer side effect
            // or return value is the true output of this function.
            receiveBuffer.put(response);
        } catch (IOException e) {
            throw new CommunicationException(e.getMessage());
        }
        return response;
    }

    @Override
    public byte[] getAt() {
        byte[] retval;

        // Try the NFC-A way
        try {
            retval = m_tagAsIsoDep.getHistoricalBytes();
        } catch (Exception e) {
            retval = null;
        }

        // If that didn't work, try the NFC-B way
        if (retval == null) {
            try {
                retval = m_tagAsIsoDep.getHiLayerResponse();
            } catch (Exception e) {
            }
        }

        return retval;
    }

    public void setmTagCom(final IsoDep mTagCom) {
        this.m_tagAsIsoDep = mTagCom;
    }

}
