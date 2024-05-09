package net.heretical_camelid.transit_emv_checker.library;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.devnied.emvnfccard.enums.SwEnum;
import com.github.devnied.emvnfccard.parser.IProvider;
import com.github.devnied.emvnfccard.utils.TlvUtil;
import com.github.devnied.emvnfccard.exception.CommunicationException;

import fr.devnied.bitlib.BytesUtils;

public abstract class MyProviderBase implements IProvider {
	static final Logger LOGGER = LoggerFactory.getLogger(MyProviderBase.class);

    final APDUObserver m_apduStore;
    protected MyProviderBase(APDUObserver apduStore) {
        m_apduStore = apduStore;
    }

    protected abstract byte[] implementationTransceive(final byte[] pCommand, ByteBuffer receiveBuffer) throws CommunicationException;

	private final ByteBuffer buffer = ByteBuffer.allocate(1024);   
    @Override
	public byte[] transceive(final byte[] pCommand) throws CommunicationException {
        long startMillis = System.currentTimeMillis();
        CommandAndResponse newCommandAndResponse = new CommandAndResponse();
		byte[] ret = null;
		buffer.clear();
        newCommandAndResponse.rawCommand = pCommand;
		try {
            ret = implementationTransceive(pCommand, buffer);
            newCommandAndResponse.rawResponse = ret;
		} catch (CommunicationException e) {
            newCommandAndResponse.interpretedResponseStatus = "Exception: " + e.getMessage();
		}
        long commsEndMillis = System.currentTimeMillis();
        if(m_apduStore != null) {
            m_apduStore.interpretCommand(newCommandAndResponse);
            m_apduStore.interpretResponse(newCommandAndResponse);
            m_apduStore.add(newCommandAndResponse);
            m_apduStore.extractTags(newCommandAndResponse);
            long analysisEndMillis = System.currentTimeMillis();
            LOGGER.debug(String.format(
                "Command: %s: comms time: %d msec, analysis time: %d msec",
                newCommandAndResponse.stepName, 
                commsEndMillis - startMillis,
                analysisEndMillis - startMillis
            ));
        }

		return ret;
	}

	abstract public byte[] getAt();    

}
