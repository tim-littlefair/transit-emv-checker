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

    protected APDUObserver m_apduStore;
    protected MyProviderBase() { m_apduStore = null; }
    public void setApduStore(APDUObserver apduStore) {  m_apduStore = apduStore; }

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
            if(ret!=null) {
                // The raw response stored in m_apduStore will be
                // updated to mask out PCI CHD and SAD, so we clone
                // the original response array so that we are able
                // to return the unmasked value to the upstream
                // code in package com.github.devnied.emvnfccard:library
                // allowing the library to capture the PAN.
                newCommandAndResponse.rawResponse = Arrays.copyOfRange(ret, 0, ret.length);
            } else {
                newCommandAndResponse.rawResponse = null;
            }
		} catch (CommunicationException e) {
            newCommandAndResponse.interpretedResponseStatus = "Exception: " + e.getMessage();
		}
        long commsEndMillis = System.currentTimeMillis();
        if(m_apduStore != null) {
            m_apduStore.interpretCommand(newCommandAndResponse);
            m_apduStore.interpretResponse(newCommandAndResponse);
            m_apduStore.extractTags(newCommandAndResponse);
            m_apduStore.add(newCommandAndResponse);
            long analysisEndMillis = System.currentTimeMillis();
            LOGGER.debug(String.format(
                "Command: %s: comms time: %d msec, analysis time: %d msec",
                newCommandAndResponse.stepName, 
                commsEndMillis - startMillis,
                analysisEndMillis - startMillis
            ));
            if(newCommandAndResponse.rawResponse==null) {
                throw new CommunicationException(
                    "Null response to command sent in step " +
                    newCommandAndResponse.stepName
                );
            }
        }
        return ret;
    }

    abstract public byte[] getAt();

}
