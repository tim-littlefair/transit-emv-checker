package net.heretical_camelid.transit_emv_checker.library;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.devnied.emvnfccard.enums.SwEnum;
import com.github.devnied.emvnfccard.parser.IProvider;
import com.github.devnied.emvnfccard.utils.TlvUtil;
import com.github.devnied.emvnfccard.exception.CommunicationException;
import com.github.devnied.emvnfccard.exception.TlvException;
import com.github.devnied.emvnfccard.iso7816emv.EmvTags;
import com.github.devnied.emvnfccard.iso7816emv.TLV;
import com.github.devnied.emvnfccard.iso7816emv.TagAndLength;

import fr.devnied.bitlib.BytesUtils;
import net.sf.scuba.tlv.TLVInputStream;

public class APDUObserver {
	static final Logger LOGGER = LoggerFactory.getLogger(APDUObserver.class);

    PCIMaskingAgent m_PCIMaskingAgent;

    ArrayList<CommandAndResponse> m_commandsAndResponses = new ArrayList<CommandAndResponse>();
    TreeSet<EMVTagEntry> m_emvTagEntries = new TreeSet<EMVTagEntry>();
    TreeMap<AppSelectionContext,AppAccountIdentifier> m_accountIdentifiers = new TreeMap<AppSelectionContext,AppAccountIdentifier>();

    AppSelectionContext m_currentAppSelectionContext = null;
    AppAccountIdentifier m_currentAppAccountIdentifier = null;
    int m_mediumTransactionCounterNow = -1;
    int m_mediumTransactionCounterLastOnline = -1;

    boolean m_pciMaskingDone = false;

    public APDUObserver(PCIMaskingAgent pma) { 
        m_PCIMaskingAgent = pma;
    }

    public void openAppSelectionContext(String aid) {
        if(m_currentAppSelectionContext == null) {
            m_currentAppSelectionContext = new AppSelectionContext(aid);
            m_currentAppAccountIdentifier = new AppAccountIdentifier();
        } else if(!m_currentAppSelectionContext.aid.equals(aid)) {
            closeAppSelectionContext();
            m_currentAppSelectionContext = new AppSelectionContext(aid);
            m_currentAppAccountIdentifier = new AppAccountIdentifier();
        }
    }

    public void closeAppSelectionContext() {
        if(m_currentAppSelectionContext == null) {
            return;
        }
        if(m_accountIdentifiers.containsKey(m_currentAppSelectionContext)) {
            LOGGER.warn(String.format(
                "PPSE contains multiple records for AID %s at priority %s",
                m_currentAppSelectionContext.aid,
                m_currentAppSelectionContext.priority
            ));
            LOGGER.warn(String.format(
                "The PPSE record for selection context %s will not be captured", 
                m_currentAppSelectionContext.toString()
            ));
        } else {
            // Check whether any prior records exist in the collections with only 
            // the same AID set.
            // If such records do exist, they need to be removed/updated to 
            // reflect the full current selection context (which should contain 
            // an appPriorityIndicator at a minimum alongside the AID).
            AppSelectionContext priorIncompleteAsc = 
                new AppSelectionContext(m_currentAppSelectionContext.aid);
            if(m_accountIdentifiers.containsKey(priorIncompleteAsc)) {
                m_accountIdentifiers.remove(priorIncompleteAsc);
            } 

            // As removal and reinsertion invalidates the iterator
            // we use to scan the collection, we create a filtered 
            // copy of the original list rather than trying to change 
            // it directly.
            TreeSet<EMVTagEntry> updatedEmvTagEntries = new TreeSet<EMVTagEntry>();
            for(EMVTagEntry ete: m_emvTagEntries) {
                if(
                    ( ete.scope != null ) &&
                    ( ete.scope.equals(priorIncompleteAsc.toString()) )
                ) {
                    // Update the scope on the item before copying it across
                    ete.scope = m_currentAppSelectionContext.toString();
                    updatedEmvTagEntries.add(ete);
                } else {
                    // Copy the item across unchanged
                    updatedEmvTagEntries.add(ete);
                }
            }
            m_emvTagEntries = updatedEmvTagEntries;

            m_accountIdentifiers.put(
                m_currentAppSelectionContext,
                m_currentAppAccountIdentifier
            );
        }
        m_currentAppAccountIdentifier = null;
        m_currentAppSelectionContext = null;
    }

    public void extractTags(CommandAndResponse carItem) {
        // Interpretation of PDOL tags attached to the GPO 
        // command is done in interpretCommand

        final byte[] responseTlvBytes = Arrays.copyOfRange(
            carItem.rawResponse,0,carItem.rawResponse.length-2
        );
        extractTags(responseTlvBytes, carItem);
    }

    void extractTags(byte[] tlvBytes, CommandAndResponse carItem) {
		TLVInputStream stream = new TLVInputStream(new ByteArrayInputStream(tlvBytes));
        ArrayList<EMVTagEntry> newTagList = new ArrayList<EMVTagEntry>();
        extractTagsRecursively(stream, newTagList,carItem);
        for(EMVTagEntry ete: newTagList) {
            // We defer setting ete.scope until here so that m_currentAid
            // reflects all attributes of the selected AID entry
            // (NB the same AID may be selected more than once at 
            // different priorities)
            if(m_currentAppSelectionContext != null) {
                ete.scope = m_currentAppSelectionContext.toString();
            } else {
                ete.scope = null;
            }
            ete.source = "medium";
            m_emvTagEntries.add(ete);
        }
    }

    void extractTagsRecursively(TLVInputStream stream, ArrayList<EMVTagEntry> newTagList,CommandAndResponse carItem) {
        try {
			while (stream.available() > 0) {
                stream.mark(1024);
				TLV tlv = TlvUtil.getNextTLV(stream);
				if (tlv == null) {
                    stream.reset();
                    byte[] dataAtTlvFail = new byte[stream.available()]; 
                    stream.read(dataAtTlvFail);
					LOGGER.warn(String.format(
                        "TLV format error processing %s",BytesUtils.bytesToString(dataAtTlvFail)
                    ));
					break;
				} else if(tlv.getTag().isConstructed()) {
                    TLVInputStream stream2 = new TLVInputStream(new ByteArrayInputStream(tlv.getValueBytes()));
                    extractTagsRecursively(stream2,newTagList,carItem);
                } else {
                    m_PCIMaskingAgent.maskWholeValueIfSensitive(this, carItem, tlv);
                    EMVTagEntry newEmvTagEntry = new EMVTagEntry();
                    newEmvTagEntry.tagHex = BytesUtils.bytesToStringNoSpace(tlv.getTagBytes());
                    newEmvTagEntry.valueHex = BytesUtils.bytesToString(tlv.getValueBytes());
                    newTagList.add(newEmvTagEntry);
                    reflectTagInSelectionContextAndAccountIdentifier(newEmvTagEntry.tagHex,newEmvTagEntry.valueHex);
                }
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (TlvException exce) {
            LOGGER.warn(exce.getMessage(), exce);
        } 

        try {
            stream.close();
        }
        catch(IOException e) {
            LOGGER.warn("IOException caught and ignored while closing TLV stream");
        }
    }

    void reflectTagInSelectionContextAndAccountIdentifier(String tagHex, String tagValueHex) {
        tagValueHex = tagValueHex.replaceAll(" ","");
        if(tagHex.equals("4F")) {
            openAppSelectionContext(tagValueHex.replaceAll(" ",""));
        } else if(tagHex.equals("50")) {
            m_currentAppSelectionContext.label = new String(
                BytesUtils.fromString(tagValueHex)
            );
        } else if(tagHex.equals("87")) {
            m_currentAppSelectionContext.priority = tagValueHex;
        } else if(tagHex.equals("9F2A")) {
            m_currentAppSelectionContext.appKernelId = tagValueHex;
        } else if(tagHex.equals("9F08")) {
            m_currentAppSelectionContext.appVersionNumber = tagValueHex;
        } else if(tagHex.equals("9F38")) {
            m_currentAppSelectionContext.pdol = 
                TlvUtil.parseTagAndLength(BytesUtils.fromString(tagValueHex));
        } else if(tagHex.equals("57") || tagHex.equals("9F6B")) {
            // track 2 equivalent data
            int separatorPos = tagValueHex.indexOf("D");
            if(separatorPos > 0) {
                m_currentAppAccountIdentifier.applicationPAN = tagValueHex.substring(0,separatorPos);
                m_currentAppAccountIdentifier.applicationExpiryMonth = tagValueHex.substring(separatorPos+1, separatorPos+5);
            } else {
                LOGGER.warn("Invalid track 2 equivalent ignored");
                return;
            }
        } else if(tagHex.equals("5F34")) {
            m_currentAppAccountIdentifier.applicationPSN = tagValueHex;
        } else if(tagHex.equals("9F36")) {
            byte[] atcBytes = BytesUtils.fromString(tagValueHex);
            m_mediumTransactionCounterNow = (0xFF&atcBytes[0]*0x100) + (0xFF&atcBytes[1]); 
        } else if(tagHex.equals("9F17")) {
            byte[] lotcBytes = BytesUtils.fromString(tagValueHex);
            switch(lotcBytes.length) {
                case 2:
                    m_mediumTransactionCounterLastOnline =  (0xFF&lotcBytes[0]*0x100) + (0xFF&lotcBytes[1]); 
                    break;
                case 1:
                    m_mediumTransactionCounterLastOnline =  (int) lotcBytes[0]; 
                    break;
                default:
                    LOGGER.warn(
                        "Unexpected last online transaction counter: " + 
                        BytesUtils.bytesToString(lotcBytes)
                    );
            }
        }
}

    void interpretCommand(CommandAndResponse cr) {
        int cla_ins = BytesUtils.byteArrayToInt(cr.rawCommand,0,2);
        int p1_p2 = BytesUtils.byteArrayToInt(cr.rawCommand,2,2);
        StringBuffer commandInterpretation = new StringBuffer();

        ArrayList<Integer> acceptableStatusWordValues = new ArrayList<>();
        acceptableStatusWordValues.add(new Integer(0x9000));

        switch(cla_ins) {
            case 0x00a4: {
                int lengthOfExtraBytes = cr.rawCommand[4];
                byte[] extraBytes = Arrays.copyOfRange(cr.rawCommand,5,5+lengthOfExtraBytes);
                if(p1_p2 == 0x0400) {
                    if(Arrays.equals(extraBytes,"2PAY.SYS.DDF01".getBytes())) {
                        commandInterpretation.append("SELECT CONTACTLESS PPSE");
                    } else if(Arrays.equals(extraBytes,"1PAY.SYS.DDF01".getBytes())) {
                        commandInterpretation.append("SELECT CONTACT PPE");
                    } else {
                        commandInterpretation.append("SELECT APPLICATION BY AID ");
                        String aid = BytesUtils.bytesToStringNoSpace(extraBytes);
                        commandInterpretation.append(aid);
                    }
                    cr.interpretedCommand = commandInterpretation.toString();
                } else {
                    // Don't expect this but ISO 7816 does define other modes of select
                    // selected via p1_p2 so interpret in case we ever see them used.
                    commandInterpretation.append("SELECT_BY_???? ");
                    commandInterpretation.append(
                        String.format("p1_p2=%04x extra_bytes=%s", 
                        p1_p2, BytesUtils.bytesToString(extraBytes)
                    ));
                    cr.interpretedCommand = commandInterpretation.toString();
                }
            }
            break;

            case 0x80A8: {
                int lengthOfExtraBytes = cr.rawCommand[4];
                byte[] extraBytes = Arrays.copyOfRange(cr.rawCommand,5,5+lengthOfExtraBytes);
                cr.stepName = "GET_PROCESSING_OPTIONS for " + m_currentAppSelectionContext.toString();
                commandInterpretation.append(cr.stepName + "\n");

                if(m_currentAppSelectionContext.pdol != null) {
                    int gpoDolOffset = 2; // We expect that the first two bytes are 83 21
                    commandInterpretation.append("Tags requested in previously received PDOL:\n");
                    for(TagAndLength tagAndLength: m_currentAppSelectionContext.pdol) {
                        int nextTagLength = tagAndLength.getLength();
                        if(gpoDolOffset + nextTagLength > lengthOfExtraBytes) {
                            String warningLine1 = String.format(
                                "GPO PDOL item processing failed at offset %d expecting %d bytes for tag %s",
                                gpoDolOffset, nextTagLength, tagAndLength.getTag().toString()
                            );
                            String warningLine2 = "GPO extra bytes: " + BytesUtils.bytesToString(extraBytes);
                            LOGGER.warn(warningLine1);
                            LOGGER.warn(warningLine2);
                            commandInterpretation.append(warningLine1 + "\n");
                            commandInterpretation.append(warningLine2 + "\n");
                            break;
                        }
                        byte[] valueBytes = Arrays.copyOfRange(extraBytes, gpoDolOffset, gpoDolOffset + nextTagLength);
                        EMVTagEntry newEmvTagEntry = new EMVTagEntry();
                        newEmvTagEntry.tagHex = BytesUtils.bytesToStringNoSpace(tagAndLength.getTag().getTagBytes());
                        newEmvTagEntry.valueHex = BytesUtils.bytesToString(valueBytes);
                        newEmvTagEntry.scope = m_currentAppSelectionContext.toString();
                        newEmvTagEntry.source = "terminal";
                        m_emvTagEntries.add(newEmvTagEntry);
                        commandInterpretation.append(String.format(
                            "tag: %s length: %02x value: %s\n",
                            newEmvTagEntry.tagHex, nextTagLength, newEmvTagEntry.valueHex
                        ));
                        
                        gpoDolOffset += nextTagLength;
                    }
                    // Once the PDOL in the context has been consumed (even if 
                    // deserialization failed), we don't need to dump it again 
                    m_currentAppSelectionContext.pdol = null;
                }
                cr.interpretedCommand = commandInterpretation.toString();
            }
            break;

            case 0x80CA: {
                // Tags accessed via GET DATA belong to the medium, not a specific
                // application, so close off the app selection context if it is open.
                closeAppSelectionContext();

                String tagHex = String.format("%X",p1_p2);
                commandInterpretation.append("GET_DATA for tag " + tagHex);
                cr.interpretedCommand = commandInterpretation.toString();

                // It is acceptable to get status word 6A81 or 6A88 when 
                // requesting tag 9F17 (this item reflects the ATC at the 
                // most recent online tap handled, and is not set if there 
                // has never been such a tap)
                if (p1_p2 == 0x9F17) {
                    acceptableStatusWordValues.add(new Integer(0x6A81));                    
                    acceptableStatusWordValues.add(new Integer(0x6A88));                    
                }
            }
            break;

            case 0x00B2: {
                commandInterpretation.append(String.format(
                    "READ_RECORD %02d.%02d", 
                    (p1_p2 & 0x00FF) >> 3,
                    (p1_p2 & 0xFF00) >> 8 
                ));
                cr.interpretedCommand = commandInterpretation.toString();
            }
            break;

            default:
                cr.interpretedCommand = String.format("Unexpected CLA/INS %04x", cla_ins);
        }

        // Some commands will have multi-line interpretations - for these, cr.stepName 
        // should have been set to a single-line string before the first 
        // carriage return was inserted.
        // Otherwise, hopefully commandInterpretation contains a single line string
        // which we can use as the name of the command/response pair. 
        if(cr.stepName == null) {
            cr.stepName = commandInterpretation.toString();
        }

        // Check that the status word is one of the expected values
        // (for most commands the expected value is 0x9000, but 
        // GET DATA is expected to fail with 0x6892 if the 
        // last online ATC has never been set)
        int status_word = BytesUtils.byteArrayToInt(
            cr.rawResponse,cr.rawResponse.length-2,2
        );
        if(!acceptableStatusWordValues.contains(status_word)) {
            LOGGER.warn(String.format(
                "Unexpected status word %04x for step %s",
                status_word, cr.stepName
            ));
        }
    }

    String prettyPrintCommandExtraData(byte[] commandExtraBytes) {
        // The GPO command sends some EMV tags related to the terminal
        // configuration, this function is provided to pretty-print 
        // these.
        // We hijack the behaviour of devied's prettyPrintAPDUResponse for
        // this.  Responses are expected to end with a two-byte status 
        // word, so we fake one up.
        byte[] fakeResponseBuffer = new byte[commandExtraBytes.length + 2];
        System.arraycopy(commandExtraBytes,0,fakeResponseBuffer,0,commandExtraBytes.length);
        fakeResponseBuffer[commandExtraBytes.length] = (byte) 0x90;
        fakeResponseBuffer[commandExtraBytes.length + 1] = (byte) 0x00;

        String prettyApdu = TlvUtil.prettyPrintAPDUResponse(fakeResponseBuffer);
        prettyApdu = prettyApdu.strip();
        int lastCarriageReturnPosition = prettyApdu.lastIndexOf("\n");

        return prettyApdu.substring(0,lastCarriageReturnPosition);
    }

    void interpretResponse(CommandAndResponse cr) {
        if(cr.interpretedResponseStatus != null) {
            // If this is already filled in it describes an exception,
            // there is nothing more to be done
            return;
        }
        SwEnum swval = SwEnum.getSW(cr.rawResponse);
        if (swval != null) {
            cr.interpretedResponseStatus = swval.toString();
            String prettyApdu = TlvUtil.prettyPrintAPDUResponse(cr.rawResponse);

            // remove the status word as we already have it.
            prettyApdu = prettyApdu.strip();
            int lastCarriageReturnPosition = prettyApdu.lastIndexOf("\n");
            if(lastCarriageReturnPosition > 0) {
                prettyApdu = prettyApdu.substring(0,lastCarriageReturnPosition);
            }
    		cr.interpretedResponseBody = prettyApdu;
        } else {
            cr.interpretedResponseStatus = "Status word not found";
            cr.interpretedResponseBody = "Not parsed";
        }
    }

    public void add(CommandAndResponse newCommandAndResponse) {
        newCommandAndResponse.stepNumber = m_commandsAndResponses.size() + 1;
        m_commandsAndResponses.add(newCommandAndResponse);
    }

    String hexReinsertSpacesBetweenBytes(String hexWithoutSpaces) {
        StringBuilder hexWithSpacesSB = new StringBuilder();
        while(true) {
            hexWithSpacesSB.append(hexWithoutSpaces.substring(0,2));
            hexWithoutSpaces = hexWithoutSpaces.substring(2);
            if(hexWithoutSpaces.length()>0) {
                hexWithSpacesSB.append(" ");
            } else {
                break;
            } 
        }
        return hexWithSpacesSB.toString();
    }

    public String summary() {
        final String indentString = " ";
        if(m_pciMaskingDone != true) {
            return "Summary not available unless PCI masking completed successfully";
        }
        StringBuilder summarySB = new StringBuilder();
        AppAccountIdentifier mediumAccountIdentifier = primaryAccountIdentifier();
        AppAccountIdentifier[] otherAccountIdentifiers = nonPrimaryAccountIdentifiers();

        String accountIdLabel = "Account identifier";
        if(otherAccountIdentifiers != null) {
            accountIdLabel = "Primary account identifier";
        }

        if(mediumAccountIdentifier.applicationPSN.length()==0) {
            summarySB.append(String.format(
                "%s:\n%sPAN=%s\n%sEXP=%s\n%s(no PSN)\n",
                accountIdLabel,
                indentString, mediumAccountIdentifier.applicationPAN, 
                indentString, mediumAccountIdentifier.applicationExpiryMonth,
                indentString
            ));
        } else {
            summarySB.append(String.format(
                "%s:\n%sMPAN=%s\n%sEXP=%s\n%sPSN=%s\n",
                accountIdLabel,
                indentString,mediumAccountIdentifier.applicationPAN, 
                indentString,mediumAccountIdentifier.applicationExpiryMonth,
                indentString,mediumAccountIdentifier.applicationPSN
            ));
        }

        summarySB.append("Application Configurations:\n");
        for(AppSelectionContext ascItem: m_accountIdentifiers.keySet()) {
            AppAccountIdentifier aai = m_accountIdentifiers.get(ascItem);
            if(!aai.toString().equals(mediumAccountIdentifier.toString())) {
                // This application is associated with a non-primary 
                // account id - it will be dumped later
                continue;
            }
            summarySB.append(indentString + ascItem.toString() + ":\n");
            summarySB.append(indentString + indentString + "Label=" + ascItem.label + ":\n");
            summarySB.append(indentString + indentString + "AID=" + ascItem.aid + "\n");
            if(ascItem.priority.length()>0) {
                summarySB.append(indentString + indentString + "priority=" + ascItem.priority + "\n");
            }
            if(ascItem.appKernelId!=null) {
                summarySB.append(indentString + indentString + "kernelID=" + ascItem.appKernelId + "\n");
            }
            if(ascItem.appVersionNumber!=null) {
                summarySB.append(indentString + indentString + "appVersionNumber=" + ascItem.appVersionNumber + "\n");
            }
        } 
        
        if(otherAccountIdentifiers != null) {
            summarySB.append("TODO: handle media with non-primary account id's\n");
        }

        if(m_mediumTransactionCounterNow != -1) {
            summarySB.append("Counters:\n");
            summarySB.append(
                indentString + "Lifetime transactions: " + 
                m_mediumTransactionCounterNow + "\n"
            );
            if(m_mediumTransactionCounterLastOnline != -1) {
                summarySB.append(
                    indentString + 
                    "Offline transactions since last online: " + (
                        m_mediumTransactionCounterNow - 
                        m_mediumTransactionCounterLastOnline
                    ) + "\n"
                );
            } else {
                summarySB.append(
                    indentString + "Last online transaction: never\n" 
                );
            }
        }

        return summarySB.toString();
    }

    public String toXmlString(boolean captureOnly) {
        final String indentString = "    ";
        StringBuffer xmlBuffer = new StringBuffer();

        xmlBuffer.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        xmlBuffer.append("<emv_medium>\n");

        if(m_pciMaskingDone != true) {
            xmlBuffer.append(
                indentString + 
                "<!-- PCI masking not done yet - no data can be returned -->\n"
            );
        } else {
            for(CommandAndResponse carItem: m_commandsAndResponses) {
                xmlBuffer.append(carItem.toXmlFragment(indentString, captureOnly));
            }

            if(captureOnly == false) {

                for(AppSelectionContext asc: m_accountIdentifiers.keySet()) {
                    xmlBuffer.append(String.format(
                        "%s<app_account_id selection_context=\"%s\" account_id=\"%s\" />\n",
                        indentString, asc, m_accountIdentifiers.get(asc)
                    ));
                }

                String currentTagHex = "";
                for(EMVTagEntry eteItem: m_emvTagEntries) {
                    if( !eteItem.tagHex.equals(currentTagHex) ) {
                        if(currentTagHex.length()>0) {
                            xmlBuffer.append(indentString + "</emv_tag>\n");
                        }
                        currentTagHex = eteItem.tagHex;
                        String currentTagName = EmvTags.getNotNull(
                            BytesUtils.fromString(currentTagHex)
                        ).getName();
                        xmlBuffer.append(String.format(
                            "%s<emv_tag tag=\"%s\" name=\"%s\">\n",
                            indentString, currentTagHex, currentTagName
                        ));
                        xmlBuffer.append(
                            indentString + indentString + "<value"
                        );
                        if(eteItem.source!=null) {
                            xmlBuffer.append(String.format(" source=\"%s\"", eteItem.source));
                        }
                        if(eteItem.scope!=null) {
                            xmlBuffer.append(String.format(" scope=\"%s\"", eteItem.scope));
                        }
                        xmlBuffer.append(">\n");
                        
                        xmlBuffer.append(indentString + indentString + indentString + eteItem.valueHex + "\n");
                        
                        xmlBuffer.append(indentString + indentString + "</value>\n");
                
                    }
                }
                if(currentTagHex.length()>0) {
                    xmlBuffer.append(indentString + "</emv_tag>\n");
                }

            }
        }

        xmlBuffer.append("</emv_medium>\n");

        return xmlBuffer.toString();
    }

    public AppAccountIdentifier primaryAccountIdentifier() {
        AppAccountIdentifier retval = null;
        for(AppAccountIdentifier appAccId: m_accountIdentifiers.values()) {
            // Only interested in first item returned
            retval = appAccId;
            break;
        }
        return retval;
    }

    /**
     * The data model of the card permits applications to have different
     * PAN, expiry, PSN values.
     * At present we don't know whether this is common, rare or non-existent, 
     * but this function allows us to handle this situation if it comes up.
     * @return an array of account identifiers which differ from the primary
     *         (or null if the array would be empty)
     */
    public AppAccountIdentifier[] nonPrimaryAccountIdentifiers() {
        ArrayList<AppAccountIdentifier> retval = new ArrayList<>(m_accountIdentifiers.values());

        // ArrayList.remove() will only remove one instance of the primary account identifier
        // so we use removeAll() which removes all instances, but requires a collection as 
        // a parameter.
        ArrayList<AppAccountIdentifier> primaryAccIdList = new ArrayList<>();
        primaryAccIdList.add(primaryAccountIdentifier());
        retval.removeAll(primaryAccIdList);

        if(retval.size()>0) {
            return (AppAccountIdentifier[]) retval.toArray();
        } else {
            return null;
        }
    }

    public String mediumStateId() {
        AppAccountIdentifier mediumAccountIdentifier = primaryAccountIdentifier();
        if(mediumAccountIdentifier!=null) {
            return String.format(
                "%s@atc=%04d",
                mediumAccountIdentifier,m_mediumTransactionCounterNow
            );
        } else {
            return null;
        }
    }
}
