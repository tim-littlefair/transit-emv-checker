package net.heretical_camelid.transit_emv_checker.library;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.TreeSet;

import com.github.devnied.emvnfccard.iso7816emv.TLV;

import fr.devnied.bitlib.BytesUtils;

public class PCIMaskingAgent {

    void maskWholeValueIfSensitive(CommandAndResponse carItem, TLV possiblySensitiveTLV) {
        // There are a small number of tags which may contain sensitive data
        // in an obfuscated state, or encrypted with publicly available keys.
        // ref:
        // https://medium.com/@androidcrypto/talk-to-your-credit-card-part-7-find-and-print-out-the-application-primary-account-number-52b24b396082
        int tagAsInt = BytesUtils.byteArrayToInt(possiblySensitiveTLV.getTagBytes());
        switch(tagAsInt) {
            // Tags which may contain PCI-DSS 4.0 account data 
            // Subdivided into CHD (cardholder data) or SAD (sensitive authentication data)
            case 0x56:   // Track 1 - contains PAN encoded as ASCII (=> both CHD and SAD)
            case 0x5F30: // Service code (classified as SAD)
            case 0x5F21: // copy of Track 1
            case 0x5F22: // copy of Track 2
            case 0x9F20: // Track 2 discretionary data (includes Service Code)
            case 0x9F46: // ICC Public certificate - encrypted with known key, cleartext contains PAN
            case 0x9F5E: // Data Storage identifier (contains PAN)

            // None of tags 0x57 track 2, 0x5A/0x9F5A application PAN  are masked out at this point
            // because the value retrieved for these tags from by the upstream package
            // com.github.devnied.emvnfccard:library will be used later to ensure that any unexpected
            // occurrence of the PAN (even one caused by random fate, e.g. in encrypted data) is
            // masked before reporting is started.
            // case 0x57: // Track 2 equivalent data - also contains PAN (=> both CHD and SAD)
            // case 0x5A: // Application PAN (CHD)
            // case 0x9F5A: // Application PAN (CHD)

            // Tag 0x5F24, (application) expiration date is listed as CHD, but is permitted 
            // to be stored under PCI-DSS v4.0 providing the PAN is not stored or is only 
            // stored in irreversibly truncated form.
            // We choose to retain the value of this tag as it potentially forms part of the unique 
            // key of the card/media (in the event that the card issuer issues a replacement card with 
            // the same PAN and PSN but a different expiry date, e.g. at the end of the original 
            // card's validity period).  
            // In transit systems where a deny list based on hashes of the PAN and 
            // other card/media tags is in used, the expiration date may form part 
            // of the hash so that a specific physical media item can be blocked without 
            // blocking other media with the same PAN and a different expiry date.

            // PCI would also permit us to store tag 0x5F20, cardholder name, despite its
            // classification as CHD, but the content of this tag does not contribute to 
            // evaluation of the media's transit capabilities, so it is included in the suppression
            // list.
            case 0x5F20: // Cardholder name (classified as CHD)

            // Tag 0x42, known as the Issuer Identification Number (IIN), also sometimes
            // referred to as BIN (B standing for Bank), is not explicitly listed as 
            // CHD, but may be either 6 or 8 digits long.
            // The PAN of any given card/media will start with the IIN.
            // If the IIN is 6 digits long, it is OK for all of the digits in the IIN 
            // to be stored or displayed as they correspond with digits which are visible
            // in a PAN truncated in the standard way (first 6 + last 4 digits visible,
            // other digits replaced by a placeholder such as '*' or 'F').
            // If the IIN is 8 digits long, the last two digits of the IIN contain digits 
            // from the PAN which should not be visible in this format.
            // For the sake of certainty, in the present version, the whole IIN will
            // masked out whatever length it is - but at a later date it might be
            // worth returning to this and only masking out the 7th and 8th digits
            // when those are present.
            case 0x42: // Issuer Identification Number (IIN)

            // Others below this point probably do not contain sensitive data - but we mask them
            // for safety's sake because they are not easy to inspect
            case 0x9081: // Issuer public key certificate
            case 0x9F4B: // Signed dynamic application data
                // Continue and complete this function to mask values associated with the tags above
                APDUObserver.LOGGER.debug(String.format(
                    "Masking whole value of tag %s(%X)",
                    possiblySensitiveTLV.getTag().getName(), tagAsInt)
                );
                break;
            
            default:
                // not sensitive, no masking required, return now
                return;
        }
    
        byte[] bytesToMask = possiblySensitiveTLV.getValueBytes();
        byte[] bytesAfterMasking = new byte[bytesToMask.length];
        Arrays.fill(bytesAfterMasking,(byte) 0xFF);
    
        carItem.rawResponse = BytesUtils.fromString(
            BytesUtils.bytesToString(carItem.rawResponse).replace(
                BytesUtils.bytesToString(bytesToMask),
                BytesUtils.bytesToString(bytesAfterMasking)
            )
        );

        // If the type of any of the tags in the current TLV response is ASCII,
        // carItem.interpretedResponseBody will contain the tag with a suffix
        // of the form "(=...)" where '...' is the ASCII rendering of the tag.
        String asciiRendering = new String(bytesToMask, StandardCharsets.US_ASCII);
        // For now we assume that if any of the bytes data didn't render as ASCII,
        // the tag can't be typed as ASCII, and we don't need any special attention.

        // TODO:
        // Check what happens where cards are issued to cardholders
        // with non-ASCII accented characters in their names including
        // both characters in iso-latin-1 and characters in scripts like
        // Greek/Cyrillic/Arabic/Hindi/CJK/etc.

        char[] maskedAsciiRendering = new char[asciiRendering.length()];
        Arrays.fill(maskedAsciiRendering,'?');
        carItem.interpretedResponseBody = carItem.interpretedResponseBody.replace(
            "(=" + asciiRendering + ")",
            "(=" + new String(maskedAsciiRendering)+ ")"
        );




        // carItem.interpretedResponse needs to be masked in 16 bytes per chunk
        // as the prettyPrintAPDU function breaks the data into 16 bytes per line
        while(bytesToMask.length>0) {
            byte[] chunkBytes;
            if(bytesToMask.length>16) {
                chunkBytes = Arrays.copyOfRange(bytesToMask,0,16);
            } else {
                chunkBytes = Arrays.copyOfRange(bytesToMask,0,bytesToMask.length);
            }
            byte[] maskedChunkBytes = new byte[chunkBytes.length];
            Arrays.fill(maskedChunkBytes,(byte) 0xFF);
            if(chunkBytes.length==16) {
                carItem.interpretedResponseBody = carItem.interpretedResponseBody.replace(
                    BytesUtils.bytesToString(chunkBytes),
                    BytesUtils.bytesToString(maskedChunkBytes)
                );            
            } else {
                // The last line is usually shorter - we reduce but do not 
                // eliminate the danger that we will replace the same hex
                // sequence somewhere else by including the suffix " ("
                // which begins the type indicator (usually " (BINARY)")
                carItem.interpretedResponseBody = carItem.interpretedResponseBody.replace(
                    BytesUtils.bytesToString(chunkBytes) + " (",
                    BytesUtils.bytesToString(maskedChunkBytes) + " ("
                );
                break;
            }            
            bytesToMask = Arrays.copyOfRange(bytesToMask, chunkBytes.length, bytesToMask.length);
        }


        possiblySensitiveTLV.setValueBytes(bytesAfterMasking);
    }

    public boolean maskAccountData(APDUObserver apduObserver) {
        // The following map will contain pairs of Strings, where
        // the key is a sensitive value which requires masking, 
        // and the associated value is the masked value
        TreeMap<String,String> maskPairs = new TreeMap<>();
    
        for(AppAccountIdentifier appAccountId: apduObserver.m_accountIdentifiers.keySet()) {
            if(appAccountId.applicationPAN==null) {
                continue;
            }
            String panWithoutSpaces = appAccountId.applicationPAN;
            char[] maskingChars = new char[panWithoutSpaces.length()-10];
            Arrays.fill(maskingChars,'F');
            String maskingString = new String(maskingChars);
            String maskedPanWithoutSpaces = String.format(
                "%s%s%s",panWithoutSpaces.substring(0,6),
                maskingString,
                panWithoutSpaces.substring(panWithoutSpaces.length()-4)
            );
            // When we use the TapReplay{Conductor/Provider/Arbiter} classes,
            // we will read in PANs from captured data which have already been
            // masked, so if there is no difference between the input to the
            // masking above and its output, we do not need to include this
            // PAN in the store of sensitive maskable values.
            if(!panWithoutSpaces.equals(maskedPanWithoutSpaces)) {
                maskPairs.put(panWithoutSpaces, maskedPanWithoutSpaces);
            }
        }
    
        ArrayList<CommandAndResponse> maskedCommandsAndResponses = new ArrayList<>();
        for(CommandAndResponse carItem: apduObserver.m_commandsAndResponses) {
            for(String sensitiveString: maskPairs.keySet()) {
                String maskedString = maskPairs.get(sensitiveString);
    
                carItem.rawResponse = BytesUtils.fromString(
                    BytesUtils.bytesToStringNoSpace(
                        carItem.rawResponse
                    ).replaceAll(sensitiveString,maskedString)
                );
    
                String sensitiveStringWithSpaces = apduObserver.hexReinsertSpacesBetweenBytes(sensitiveString);
                String maskedStringWithSpaces = apduObserver.hexReinsertSpacesBetweenBytes(maskedString);
                carItem.interpretedResponseBody = 
                    carItem.interpretedResponseBody.replaceAll(sensitiveStringWithSpaces,maskedStringWithSpaces);
            }
            maskedCommandsAndResponses.add(carItem);
        }
        apduObserver.m_commandsAndResponses = maskedCommandsAndResponses;
    
        TreeSet<EMVTagEntry> maskedEmvTagEntries = new TreeSet<>();
        for(EMVTagEntry ete: apduObserver.m_emvTagEntries) {
            for(String sensitiveString: maskPairs.keySet()) {
                String maskedString = maskPairs.get(sensitiveString);
                String sensitiveStringWithSpaces = apduObserver.hexReinsertSpacesBetweenBytes(sensitiveString);
                String maskedStringWithSpaces = apduObserver.hexReinsertSpacesBetweenBytes(maskedString);
                ete.valueHex = 
                    ete.valueHex.replaceAll(sensitiveStringWithSpaces,maskedStringWithSpaces);
            }
            maskedEmvTagEntries.add(ete);
        }
        apduObserver.m_emvTagEntries = maskedEmvTagEntries;
    
        TreeMap<AppAccountIdentifier,AppSelectionContext> maskedAccountIdentifiers = new TreeMap<>();
        for(AppAccountIdentifier aai: apduObserver.m_accountIdentifiers.keySet()) {
            if(aai.applicationPAN==null) {
                continue;
            }
            for(String sensitiveString: maskPairs.keySet()) {
                String maskedString = maskPairs.get(sensitiveString);
                aai.applicationPAN =
                    aai.applicationPAN.replaceAll(sensitiveString,maskedString);
            }
            maskedAccountIdentifiers.put(aai, apduObserver.m_accountIdentifiers.get(aai));
        }
/*
        for(AppSelectionContext ascItem: apduObserver.m_accountIdentifiers.values()) {
            AppAccountIdentifier appAccountId = apduObserver.m_accountIdentifiers.get(ascItem);
            if(appAccountId.applicationPAN==null) {
                continue;
            }
            for(String sensitiveString: maskPairs.keySet()) {
                String maskedString = maskPairs.get(sensitiveString);
                appAccountId.applicationPAN = 
                    appAccountId.applicationPAN.replaceAll(sensitiveString,maskedString);
            }
            maskedAccountIdentifiers.put(ascItem, appAccountId);
        }
 */
        apduObserver.m_accountIdentifiers = maskedAccountIdentifiers;
    
        // Provisionally set the flag which indicates that masking has been
        // done (this will be set back to false if the XML output check
        // immediately below finds any sensitive data)
        apduObserver.m_pciMaskingDone = true;
    
        // Finally, do an XML serialization and report if any of the string which are supposed
        // to be masked are present
        String[] xmlLines = apduObserver.toXmlString(false).split("\n");
        for(int xmlLineNumber=0; xmlLineNumber<xmlLines.length; ++xmlLineNumber ) {
            String xmlLine = xmlLines[xmlLineNumber];
            for(String sensitiveString: maskPairs.keySet()) {
                String sensitiveStringWithSpaces = apduObserver.hexReinsertSpacesBetweenBytes(sensitiveString);
                if(
                    xmlLine.contains(sensitiveString) || 
                    xmlLine.contains(sensitiveStringWithSpaces) 
                ) {
                    String maskedString = maskPairs.get(sensitiveString);
                    String maskedStringWithSpaces = apduObserver.hexReinsertSpacesBetweenBytes(maskedString);
                    APDUObserver.LOGGER.error(String.format(
                        "Masking failed for XML line %d, if properly masked should be:\n%s",
                        xmlLineNumber, 
                        xmlLine
                            .replaceAll(sensitiveString, maskedString)
                            .replaceAll(sensitiveStringWithSpaces,maskedStringWithSpaces)
                        
                    ));
                    apduObserver.m_pciMaskingDone = false;
                }
            }
        }
        return apduObserver.m_pciMaskingDone;
    }
}
