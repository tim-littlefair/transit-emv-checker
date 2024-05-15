package net.heretical_camelid.transit_emv_checker.library;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.TreeSet;

import com.github.devnied.emvnfccard.iso7816emv.TLV;

import fr.devnied.bitlib.BytesUtils;

public class PCIMaskingAgent {

    void maskWholeValueIfSensitive(APDUObserver apduObserver, CommandAndResponse carItem, TLV possiblySensitiveTLV) {
        // There are a small number of tags which may contain sensitive data
        // in an obfuscated state, or encrypted with publicly available keys.
        // ref:
        // https://medium.com/@androidcrypto/talk-to-your-credit-card-part-7-find-and-print-out-the-application-primary-account-number-52b24b396082
        int tagAsInt = BytesUtils.byteArrayToInt(possiblySensitiveTLV.getTagBytes());
        switch(tagAsInt) {
            case 0x9F46: // ICC Public certificate - encrypted with known key, contains PAN
            case 0x56:   // Track 1 - contains PAN encoded as ASCII
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
    
        // carItem.intepretedResponse needs to be masked in 16 bytes per chunk
        // as the prettyPrintAPDU function breaks the data into 16 bytes per line
        while(bytesToMask.length>0) {
            byte[] chunkBytes = Arrays.copyOfRange(bytesToMask,0,16);
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
    
            bytesToMask = Arrays.copyOfRange(bytesToMask, 16, bytesToMask.length);
        }
    
        possiblySensitiveTLV.setValueBytes(bytesAfterMasking);
    }

    public boolean maskAccountData(APDUObserver apduObserver) {
        // The following map will contain pairs of Strings, where
        // the key is a sensitive value which requires masking, 
        // and the associated value is the masked value
        TreeMap<String,String> maskPairs = new TreeMap<>();
    
        for(AppAccountIdentifier appAccountId: apduObserver.m_accountIdentifiers.values()) {
            String panWithoutSpaces = appAccountId.applicationPAN;
            char[] maskingChars = new char[panWithoutSpaces.length()-10];
            Arrays.fill(maskingChars,'F');
            String maskingString = new String(maskingChars);
            String maskedPanWithoutSpaces = String.format(
                "%s%s%s",panWithoutSpaces.substring(0,6),
                maskingString,
                panWithoutSpaces.substring(panWithoutSpaces.length()-4)
            );
            maskPairs.put(panWithoutSpaces, maskedPanWithoutSpaces);
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
    
        TreeMap<AppSelectionContext,AppAccountIdentifier> maskedAccountIdentifiers = new TreeMap<>();
        for(AppSelectionContext ascItem: apduObserver.m_accountIdentifiers.keySet()) {
            AppAccountIdentifier appAccountId = apduObserver.m_accountIdentifiers.get(ascItem);
            for(String sensitiveString: maskPairs.keySet()) {
                String maskedString = maskPairs.get(sensitiveString);
                appAccountId.applicationPAN = 
                    appAccountId.applicationPAN.replaceAll(sensitiveString,maskedString);
            }
            maskedAccountIdentifiers.put(ascItem, appAccountId);
        }
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
