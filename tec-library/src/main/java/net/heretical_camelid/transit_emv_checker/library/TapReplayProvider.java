package net.heretical_camelid.transit_emv_checker.library;

import com.github.devnied.emvnfccard.exception.CommunicationException;
import com.github.devnied.emvnfccard.parser.IProvider;
import fr.devnied.bitlib.BytesUtils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import javax.xml.stream.XMLStreamConstants;

public class TapReplayProvider extends MyProviderBase implements IProvider {

    ArrayList<CommandAndResponse> m_commandsAndResponses;

    public TapReplayProvider(TapReplayConductor trc) {
        super(trc.getAPDUObserver());
        m_commandsAndResponses = new ArrayList<>();
    }

    @Override
    protected byte[] implementationTransceive(byte[] pCommand, ByteBuffer receiveBuffer) throws CommunicationException {
        return new byte[0];
    }

    @Override
    public byte[] getAt() {
        return new byte[0];
    }
}
