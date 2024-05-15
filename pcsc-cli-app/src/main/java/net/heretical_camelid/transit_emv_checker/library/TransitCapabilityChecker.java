package net.heretical_camelid.transit_emv_checker.library;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeMap;
import java.util.TreeSet;

import fr.devnied.bitlib.BytesUtils;
import com.github.devnied.emvnfccard.model.enums.CountryCodeEnum;

public class TransitCapabilityChecker {
    TreeSet<String> m_appSelectionContexts;
    TreeMap<String,String> m_emvTagEntryIndex;

    public TransitCapabilityChecker(APDUObserver apduObserver) {
        m_appSelectionContexts = new TreeSet<>();
        m_emvTagEntryIndex = new TreeMap<>();
        for(EMVTagEntry ete: apduObserver.m_emvTagEntries) {
            String eteScope = ete.scope;
            if(eteScope == null) {
                // counters - maybe relevant for velocity checks
            } else if(!m_appSelectionContexts.contains(eteScope)) {
                m_appSelectionContexts.add(eteScope);
            }
            String eteKey = ete.scope + "." + ete.tagHex;
            m_emvTagEntryIndex.put(eteKey,ete.valueHex);
        }
    }

    public String capabilityReport() {
        StringBuilder capabilityNotes = new StringBuilder();
        for(String ascKey: m_appSelectionContexts) {
            checkCapability(ascKey,capabilityNotes);
        }
        return capabilityNotes.toString();
    }

    private void checkCapability(String ascKey, StringBuilder capabilityNotes) {

        String[] _OVERALL_CHECK_OUTCOMES = {
            "No impediments to transit use discovered",
            "Potential impediment(s) to transit use discovered",
            "Fatal impediment(s) to transit use discovered"
        };
        int outcomeIndex = 0;

        capabilityNotes.append("Application configuration " + ascKey + "\n");

        outcomeIndex = checkODACapability(ascKey, outcomeIndex, capabilityNotes);
        outcomeIndex = checkUsageRestrictions(ascKey, outcomeIndex, capabilityNotes);
        outcomeIndex = checkValidityPeriod(ascKey, outcomeIndex, capabilityNotes);

        capabilityNotes.append(_OVERALL_CHECK_OUTCOMES[outcomeIndex] + "\n");
    }

    private String getValueHex(String scope, String tagHexString) {
        return m_emvTagEntryIndex.get(scope + "." + tagHexString);
    }

    private byte[] getValueBytes(String scope, String tagHexString) {
        String valueHex = getValueHex(scope, tagHexString);
        if(valueHex != null) {
            return BytesUtils.fromString(valueHex);
        }
        return null;
    }


    private int checkODACapability(
        String ascKey, int outcomeIndex, StringBuilder capabilityNotes
    ) {
        int odaOutcomeIndex = 0;

        // AIP = Application Interchange Profile
        byte[] aipValueBytes = getValueBytes(ascKey,"82");

        // CAPK = Certificate Authority Public Key
        String capkIndexHex = getValueHex(ascKey, "8F");

        if(aipValueBytes == null) {
            capabilityNotes.append("AIP not found => unable to check if CDA supported\n");
            odaOutcomeIndex = 1;
        } else if(aipValueBytes.length != 2) {
            capabilityNotes.append("AIP has unexpected length => unable to check if CDA supported\n");
            odaOutcomeIndex = 1;
        } else if( (aipValueBytes[0]&0x21) == 0x00 ) {
            capabilityNotes.append("AIP byte 1 bits 1 and 6 not set => neither CDA nor DDA supported\n");
            odaOutcomeIndex = 2;
        } else if( (aipValueBytes[0]&0x01) == 0x00 ) {
            capabilityNotes.append("AIP byte 1 bit 1 not set => CDA not supported (but DDA is)\n");
            odaOutcomeIndex = 1;
        } else if( (aipValueBytes[1]&(byte)0x80) == 0x00) {
            capabilityNotes.append("AIP byte 2 bit 8 not set => MSD only, EMV not supported\n");
            odaOutcomeIndex = 2;
        }

        if(odaOutcomeIndex==2) {
            // We already know that ODA will not be supported so 
            // there's no point in checking whether the CAPK index
            // is found.
        } else if (capkIndexHex == null) {
            capabilityNotes.append(
                "ODA not supported - CAPK index not found\n"
            );
            odaOutcomeIndex = 2;
        } else {
            capabilityNotes.append(
                "ODA supported - using CAPK #" + capkIndexHex + "\n"
            );
        }

        return Math.max(odaOutcomeIndex,outcomeIndex);
    }

    private int checkUsageRestrictions(String ascKey, int outcomeIndex, StringBuilder capabilityNotes) {
        int usageOutcomeIndex = 0;

        // AUC = Application Usage Conditions
        byte[] aucValueBytes = getValueBytes(ascKey,"82");

        boolean visaAIDSelected = ascKey.startsWith("A000000003");

        if(aucValueBytes == null) {
            capabilityNotes.append("AUC not found => unable to check if CDA supported\n");
            usageOutcomeIndex = 1;
        } else if(aucValueBytes.length != 2) {
            capabilityNotes.append("AUC has unexpected length => unable to check usage restrictions\n");
            usageOutcomeIndex = 1;
        } else if ( visaAIDSelected == true ) {
            // None of the conditions checked below are relevant to Visa applications
            // byte 1 bit 1 'valid at ATMs' is RFU(0)
            // byte 1 bits 6-3 are not used for kernel 3
            // Ref EMVCo Book C-3 v2.11 page 87
        } else if( (aucValueBytes[0]&0x01) == 0x00 ) {
            capabilityNotes.append("AUC byte 1 bit 1 not set => medium only valid at ATMs\n");
            usageOutcomeIndex = 2;
        } else if( (aucValueBytes[0]&0x0C) == 0x00 ) {
            capabilityNotes.append("AUC byte 1 bits 3 and 4 not set => medium not valid for services anywhere\n");
            usageOutcomeIndex = 2;
        } else if( (aucValueBytes[0]&0x0C) == 0x00 ) {
            capabilityNotes.append(
                "AUC byte 1 bit 3 not set => medium not valid for services outside country of issue\n"
            );
            String countryISOCodeValueHex = getValueHex(ascKey, "9F1A");
            capabilityNotes.append("(ISO code for country of issue: " + countryISOCodeValueHex + ")\n");
            usageOutcomeIndex = 1;
        } 
        
        return Math.max(usageOutcomeIndex,outcomeIndex);
    }

    private int checkValidityPeriod(String ascKey, int outcomeIndex, StringBuilder capabilityNotes) {
        int vpOutcomeIndex = 0;
        String appExpiryHex = getValueHex(ascKey, "5F24");
        String appEffectiveHex = getValueHex(ascKey, "5F25");
        String currentDateString = new SimpleDateFormat("yyMMdd").format(new Date());

        if(appExpiryHex==null) {
            capabilityNotes.append("Application Expiry Date not found\n");
            vpOutcomeIndex = 2;
        } else if(currentDateString.compareTo(appExpiryHex) > 0) {
            capabilityNotes.append("Application validity period ended " + appExpiryHex + "\n");
            vpOutcomeIndex = 1;
        } else if(appEffectiveHex==null) {
            capabilityNotes.append("Application Effective Date not found\n");
            vpOutcomeIndex = 1;
        } else if(currentDateString.compareTo(appEffectiveHex) < 0) {
            capabilityNotes.append("Application validity period does not start until " + appExpiryHex + "\n");
            vpOutcomeIndex = 1;
        } 

        return Math.max(vpOutcomeIndex,outcomeIndex);
    }

    private int checkIADStatus(String ascKey, int outcomeIndex, StringBuilder capabilityNotes) {
        int thisOutcomeIndex = 0;

        // IAD = Issue Application Data
        // format varies between brands but may contain a brand-specific
        // substring called Card Validation Results (CVR)
        // ref: https://paymentcardtools.com/emv-tag-decoders/iad
        String iadValueHex = getValueHex(ascKey, "9F10");

        // For VCPS (Visa) media, if IAD  has prefix 06111203, 
        // the next 3 bytes are Card Validation Results CVR
        // VCPS CVR byte 2:
        //   bit 6: exceeded velocity checking counters
        //   bit 5: new card
        //   bit 2: application blocked by card because PIN try limit exceeded
        
        // For MChip (Mastercard) media, if IAD has prefix 0110,
        // the next 6 bytes are CVR
        // MChip CVR byte 5:
        //   bit 8: Lower consecutive offline limit exceeded
        //   bit 7: Upper consecutive offline limit exceeded
        //   bit 6: Lower cumulative offline limit exceeded
        //   bit 5: Upper cumulative offline limit exceeded
        //   bit 4: Go online on next transaction was set

        // TODO: Implement these if cards where any of these bits
        // are set are found

        return Math.max(thisOutcomeIndex,outcomeIndex);
    }

    private int checkCVMCapabilities(String ascKey, int outcomeIndex, StringBuilder capabilityNotes) {
        int thisOutcomeIndex = 0;
        // TODO: is there anything we can/should do here
        return Math.max(thisOutcomeIndex,outcomeIndex);
    }


    private int checkOtherCapabilities(String ascKey, int outcomeIndex, StringBuilder capabilityNotes) {
        int thisOutcomeIndex = 0;
        // TODO: preserve this as a template for new capability checks
        return Math.max(thisOutcomeIndex,outcomeIndex);
    }
}

