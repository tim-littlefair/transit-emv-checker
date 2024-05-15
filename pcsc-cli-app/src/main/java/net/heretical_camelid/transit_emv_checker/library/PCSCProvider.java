package net.heretical_camelid.transit_emv_checker.library;

import java.nio.ByteBuffer;

import javax.smartcardio.TerminalFactory;
import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardChannel;
// import static javax.smartcardio.CardChannel.getBasicChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.devnied.emvnfccard.enums.SwEnum;
import com.github.devnied.emvnfccard.exception.CommunicationException;
import com.github.devnied.emvnfccard.parser.IProvider;
import com.github.devnied.emvnfccard.utils.TlvUtil;


import fr.devnied.bitlib.BytesUtils;

public class PCSCProvider extends MyProviderBase {

	/**
	 * CardChanel
	 */
	private final CardChannel channel;


	/**Source option 5 is no longer supported. Use 7 or later.
	 * Constructor using field
	 *
	 * @param pChannel
	 *            card channel
	 */
	public PCSCProvider(Card card, APDUObserver apduStore) {
		super(apduStore);
		channel = card.getBasicChannel();
	}

    protected byte[] implementationTransceive(final byte[] pCommand, ByteBuffer receiveBuffer) throws CommunicationException {
		try {
			int nbByte = channel.transmit(ByteBuffer.wrap(pCommand), receiveBuffer);
			byte[] ret = new byte[nbByte];
			System.arraycopy(receiveBuffer.array(), 0, ret, 0, ret.length);
			return ret;
		} catch(CardException e) {
			throw new CommunicationException(e.getMessage());
		}
	}

	@Override
	public byte[] getAt() {
		return channel.getCard().getATR().getBytes();
	}

}
