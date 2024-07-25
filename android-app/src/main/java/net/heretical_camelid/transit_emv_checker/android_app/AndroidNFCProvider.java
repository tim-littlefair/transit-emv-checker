package net.heretical_camelid.transit_emv_checker.android_app;

import android.nfc.tech.IsoDep;
import com.github.devnied.emvnfccard.exception.CommunicationException;
import com.github.devnied.emvnfccard.parser.IProvider;

import net.heretical_camelid.transit_emv_checker.library.MyProviderBase;

import java.io.IOException;
import java.nio.ByteBuffer;

class AndroidNFCProvider extends MyProviderBase implements IProvider {
    private final IsoDep m_tagAsIsoDep;

    public AndroidNFCProvider(IsoDep tagAsIsoDep) {
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
        retval = m_tagAsIsoDep.getHistoricalBytes();

        // If that didn't work, try the NFC-B way
        if (retval == null) {
            retval = m_tagAsIsoDep.getHiLayerResponse();
        }

        return retval;
    }
}
