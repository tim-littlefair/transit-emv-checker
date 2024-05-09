package net.heretical_camelid.transit_emv_checker.library;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.devnied.emvnfccard.iso7816emv.EmvTags;
import com.github.devnied.emvnfccard.iso7816emv.ITag;
import com.github.devnied.emvnfccard.iso7816emv.ITerminal;
import com.github.devnied.emvnfccard.iso7816emv.TagAndLength;
import com.github.devnied.emvnfccard.model.enums.CountryCodeEnum;
import com.github.devnied.emvnfccard.model.enums.CurrencyEnum;
import com.github.devnied.emvnfccard.model.enums.TransactionTypeEnum;

import fr.devnied.bitlib.BytesUtils;

public class TransitTerminal implements ITerminal {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransitTerminal.class);
    /**
     * Random
     */
    private static final SecureRandom random = new SecureRandom();

    // Values we might want to override...
    private CountryCodeEnum m_countryCode;
    private CurrencyEnum m_currencyCode;
    private byte[] m_terminalCapabilities;
    private byte[] m_additionalTerminalCapabilities;
    private byte m_terminalType;
    private byte[] m_amountAuthorizedBCD;
    private byte[] m_amountOtherBCD;
    private byte[] m_unpredictableNumber;
    private byte[] m_terminalVerificationResults;
    private byte[] m_merchantCategoryCode;

    public TransitTerminal() {
        m_countryCode = CountryCodeEnum.AU;
        m_currencyCode = CurrencyEnum.AUD;

        // byte 1: Interfaces: no manual key entry, no magnetic stripe, no CT
        // byte 2: CVMs: no plaintext PIN, no enciphered PIN (offline or online), 
        //         no signature, "No CVM" accepted as CVM
        // byte 3: SDA, DDA, CDA supported no card capture
        m_terminalCapabilities = BytesUtils.fromString("0008C8");


        // TODO : write up default value
        m_additionalTerminalCapabilities = BytesUtils.fromString("6200001001");

        // Terminal type is "Unattended, offline with online capability"
        m_terminalType = (byte) 0x25;

        m_amountAuthorizedBCD = BytesUtils.fromString("000000000000");
        m_amountOtherBCD = BytesUtils.fromString("000000000000");
        
        m_unpredictableNumber = new byte[4];
        random.nextBytes(m_unpredictableNumber);

        m_terminalVerificationResults = BytesUtils.fromString("000000000000");

        // ref: https://github.com/greggles/mcc-codes/blob/main/mcc_codes.csv
        m_merchantCategoryCode = BytesUtils.fromString("4111");
    }

    private void setArrayBit(byte[] array, int byteIndex, int bitIndex, boolean bitValue) {
        // The parameters to this function are intended to conform 
        // to EMV conventions, i.e. both bytes and bits are indexed 
        // from 1.
        array[byteIndex-1] = BytesUtils.setBit(array[byteIndex-1], bitIndex-1, bitValue); 
    }

    /**
     * Method used to construct value from tag and length
     *
     * @param pTagAndLength
     *            tag and length value
     * @return tag value in byte
     */
    @Override
    public byte[] constructValue(final TagAndLength pTagAndLength) {
        ITag tag = pTagAndLength.getTag();
        byte ret[] = new byte[pTagAndLength.getLength()];
        byte val[] = null;
        if (tag == EmvTags.TERMINAL_TRANSACTION_QUALIFIERS) {
            val = new byte[4];
            populateDefaultTTQ(val);
        } else if (tag == EmvTags.TERMINAL_COUNTRY_CODE) {
            val = BytesUtils.fromString(StringUtils.leftPad(String.valueOf(
                m_countryCode.getNumeric()), 
                pTagAndLength.getLength() * 2,"0"
            ));
        } else if (pTagAndLength.getTag() == EmvTags.TRANSACTION_CURRENCY_CODE) {
            val = BytesUtils.fromString(StringUtils.leftPad(
                String.valueOf(CurrencyEnum.find(m_countryCode, m_currencyCode).getISOCodeNumeric()),
                pTagAndLength.getLength() * 2, "0"
            ));
        } else if (pTagAndLength.getTag() == EmvTags.TRANSACTION_DATE) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
            val = BytesUtils.fromString(sdf.format(new Date()));
        } else if (
            pTagAndLength.getTag() == EmvTags.TRANSACTION_TYPE || 
            pTagAndLength.getTag() == EmvTags.TERMINAL_TRANSACTION_TYPE
        ) {
            val = new byte[] { (byte) TransactionTypeEnum.PURCHASE.getKey() };
        } else if (pTagAndLength.getTag() == EmvTags.TERMINAL_TYPE) {
            val = new byte[] { m_terminalType };
        } else if (pTagAndLength.getTag() == EmvTags.TERMINAL_CAPABILITIES) {
            val = m_terminalCapabilities;
        } else if (pTagAndLength.getTag() == EmvTags.ADDITIONAL_TERMINAL_CAPABILITIES) {
            val = m_additionalTerminalCapabilities;
        } else if (pTagAndLength.getTag() == EmvTags.UNPREDICTABLE_NUMBER) {
            val = m_unpredictableNumber;                
        } else if (pTagAndLength.getTag() == EmvTags.AMOUNT_AUTHORISED_NUMERIC) {
            val = m_amountAuthorizedBCD;
        } else if (pTagAndLength.getTag() == EmvTags.AMOUNT_OTHER_NUMERIC) {
            val = m_amountOtherBCD;
        } else if (pTagAndLength.getTag() == EmvTags.AMOUNT_OTHER_NUMERIC) {
            val = m_amountOtherBCD;
        } else if (pTagAndLength.getTag() == EmvTags.TERMINAL_VERIFICATION_RESULTS) {
            val = m_terminalVerificationResults;
        } else if (pTagAndLength.getTag() == EmvTags.TERMINAL_VERIFICATION_RESULTS) {
            val = m_terminalVerificationResults;
/*
        } else if (pTagAndLength.getTag() == EmvTags.DS_REQUESTED_OPERATOR_ID) {
            val = BytesUtils.fromString("7A45123EE59C7F40");
        } else if (pTagAndLength.getTag() == EmvTags.MERCHANT_TYPE_INDICATOR) {
            val = new byte[] { 0x01 };
        } else if (pTagAndLength.getTag() == EmvTags.TERMINAL_TRANSACTION_INFORMATION) {
            val = new byte[] { (byte) 0xC0, (byte) 0x80, 0 };
*/
        } else {
            val = generateValueForUnlistedTag(pTagAndLength, true);
        }
        if (val != null) {
            System.arraycopy(val, 0, ret, Math.max(ret.length - val.length, 0), Math.min(val.length, ret.length));
        }            
        LOGGER.debug(
            pTagAndLength.toString() + ": " + BytesUtils.bytesToString(ret)
        );
        return ret;
    }

    /**
     * The following function generates a value for a tag which is requested, where 
     * This class does not explicitly define a value.
     * The function is used in two conditions:
     * 1) For tags which are not unexpected, but for the value to be used
     * is not meaningful (e.g. unique serial numbers for devices, merchants)
     * 2) For tags which are requested but were not expected by the implementation.
     * For both types of tags, the value returned is derived from the tag identifier
     * in the hope that this makes it easier to recognize tags which have not explicitly
     * set without needing to check the code of the current class.
     * @param pTagAndLength
     * The code for the tag and expected (maximum) length.
     * @param warn
     * @return 
     * the generated value
     */
    private byte[] generateValueForUnlistedTag(final TagAndLength pTagAndLength, boolean warn) {
        ITag tag = pTagAndLength.getTag();

        byte[] val;
        // value will be equal to the hex bytes of the tag, 
        // left-padded with zeros or truncated to 
        // the required length
        int valueLength = pTagAndLength.getLength();
        val = new byte[valueLength];
        int tagLength = tag.getTagBytes().length;
        if(tagLength>=valueLength) {
            // truncated preserving rightmost bytes (if necessary)
            val = Arrays.copyOfRange(tag.getTagBytes(),valueLength-tagLength,valueLength);
        } else {
            // left padded
            System.arraycopy(
                tag.getTagBytes(),0,
                val, valueLength-tagLength,
                tagLength
            );
        }
        LOGGER.warn(String.format(
            "Unexpected tag %s(%s) requested from TransitTerminal, returning '%s'",
            tag.getName(), 
            BytesUtils.bytesToStringNoSpace(tag.getTagBytes()),
            BytesUtils.bytesToString(val)
        ));
        return val;
    }

    private void populateDefaultTTQ(byte[] val) {
        // references:
        // https://paymentcardtools.com/emv-tag-decoders/ttq
        // EMV: 
        // Visa: EMV Book C.3 v2.11 p113-114
        // TODO: determine whether this needs to be different between Visa 
        // and other brands

        setArrayBit(val,1, 8, false); // MSD not supported
        // ret[0] bit 7 RFU = 0 
        setArrayBit(val,1, 6, true);  // Visa: qVSDC supported
        setArrayBit(val,1, 5, false); // Contact not supported
        // Transit terminals are do all taps offline, but might pretend to 
        // be online-capable so that the card generates an ARQC for 
        // deferred authorisation at the payment gateway.
        setArrayBit(val,1, 4, false); // not offline only
        setArrayBit(val,1, 3, false); // Online PIN not supported
        setArrayBit(val,1, 2, false); // Signature not supported
        setArrayBit(val,1, 1, true);  // ODA for online supported

        setArrayBit(val,2, 8, true);  // Online cryptogram required
        setArrayBit(val,2, 7, false); // CVM not required by terminal
        setArrayBit(val,2, 6, false); // Offline PIN not supported
        // byte 2 bits 5-1 RFU = 0 for Visa
        // TODO: Work out whether this is OK for other brands

        setArrayBit(val,3, 8, false);  // Issuer updates not supported
        // Turn on CDCVM so that we can see whether it is triggered
        setArrayBit(val,3, 7, true);   // Consumer device CDM supported

        // byte 3 bits 6-1 RFU = 0 for Visa

        // TODO: Work out whether this is OK for other brands

        // byte 4 all bits RFU = 0
    }
}
