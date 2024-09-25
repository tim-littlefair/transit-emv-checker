package net.heretical_camelid.transit_emv_checker.library;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

import fr.devnied.bitlib.BytesUtils;
import com.github.devnied.emvnfccard.model.enums.CountryCodeEnum;

class ApplicationCapabilityCheckerBase {

    int checkValidityPeriod(
        int outcomeIndex, StringBuilder capabilityNotes,
        String appExpiryHex, String appEffectiveHex
        ) {
        int vpOutcomeIndex = 0;
        String currentDateString = new SimpleDateFormat(
            "yyMMdd", Locale.ENGLISH
        ).format(new Date());

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
            capabilityNotes.append("Application validity period does not start until " + appEffectiveHex + "\n");
            vpOutcomeIndex = 1;
        }

        return Math.max(vpOutcomeIndex,outcomeIndex);
    }

    int checkODACapability(
        int outcomeIndex, StringBuilder capabilityNotes,
        String capkIndexHex, byte[] aipValueBytes
    ) {
        int odaOutcomeIndex = 0;

        if (capkIndexHex == null) {
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

    int checkUsageRestrictions(
        int outcomeIndex, StringBuilder capabilityNotes,
        byte[] aipValueBytes, byte[] aucValueBytes, String countryISOCodeValueHex
    ) {
        int usageOutcomeIndex = 0;

        if (aucValueBytes == null) {
            capabilityNotes.append("AUC not found => unable to check usage restrictions\n");
            usageOutcomeIndex = 1;
        } else if (aucValueBytes.length != 2) {
            capabilityNotes.append("AUC has unexpected length => unable to check usage restrictions\n");
            usageOutcomeIndex = 1;
        }

        // AUC byte 1 bits 8, 7 and byte 2 bits 8, 7 are interpreted
        // consistently between Mastercard book C2 and Visa book C3,
        // but none of these bits affect transit capability.

        // There are no other bits in the Visa/C3 definition which need checking,
        // but this function will be overridden for Mastercard/C2

        return Math.max(usageOutcomeIndex,outcomeIndex);

    }
}

class VisaPaywaveApplicationCapabilityChecker extends ApplicationCapabilityCheckerBase {
    @Override
    int checkODACapability(
        int outcomeIndex, StringBuilder capabilityNotes,
        String capkIndexHex, byte[] aipValueBytes
    ) {
        int odaOutcomeIndex = 0;
        // AIP checking for ODA capability seems to be Visa-specific, not required for Mastercard,
        // TBD to determine if it is required for other brands
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
        // If we already know that ODA is disabled due to AIP absence or value there is
        // no point doing the generic check for whether there is an index for the CAPK
        if(odaOutcomeIndex<2) {
            odaOutcomeIndex = super.checkODACapability(
                odaOutcomeIndex, capabilityNotes, capkIndexHex, aipValueBytes
            );
        }
        return Math.max(outcomeIndex,odaOutcomeIndex);
    }

}

class MastercardPaypassApplicationCapabilityChecker extends ApplicationCapabilityCheckerBase {
    @Override
    int checkUsageRestrictions(
        int outcomeIndex, StringBuilder capabilityNotes,
        byte[] aipValueBytes, byte[] aucValueBytes,
        String countryISOCodeValueHex) {

        // The superclass only checks whether the AUC tag is present and the expected length
        int usageOutcomeIndex = super.checkUsageRestrictions(
            outcomeIndex, capabilityNotes, aipValueBytes, aucValueBytes, countryISOCodeValueHex
        );

        if(usageOutcomeIndex==1) {
            // The AUC EMV tag was either not found or unexpected length, so we can't check it
        }  else if ((aucValueBytes[0] & 0x01) == 0x00) {
            capabilityNotes.append("AUC byte 1 bit 1 not set => medium only valid at ATMs\n");
            usageOutcomeIndex = 2;
        } else if ((aucValueBytes[0] & 0x0C) == 0x00) {
            capabilityNotes.append("AUC byte 1 bits 3 and 4 not set => medium not valid for services anywhere\n");
            usageOutcomeIndex = 2;
        } else if ((aucValueBytes[0] & 0x0C) == 0x00) {
            capabilityNotes.append(
                "AUC byte 1 bit 3 not set => medium not valid for services outside country of issue\n"
            );
            capabilityNotes.append("(ISO code for country of issue: " + countryISOCodeValueHex + ")\n");
            usageOutcomeIndex = 1;
        }
        return Math.max(outcomeIndex,usageOutcomeIndex);
    }

}

public class TransitCapabilityChecker {
    TreeSet<String> m_appSelectionContexts;
    TreeMap<String,String> m_emvTagEntryIndex;

    public TransitCapabilityChecker(APDUObserver apduObserver) {
        m_appSelectionContexts = new TreeSet<>();
        for(AppAccountIdentifier aai: apduObserver.m_accountIdentifiers.navigableKeySet()) {
            for(AppSelectionContext asc: apduObserver.m_accountIdentifiers.get(aai)) {
                assert asc!=null;
                m_appSelectionContexts.add(asc.toString());
            }
        }
        m_emvTagEntryIndex = new TreeMap<>();
        for(EMVTagEntry ete: apduObserver.m_emvTagEntries) {
            String eteScope = ete.scope;
            if(eteScope == null) {
                // counters - maybe relevant for velocity checks?
            } else {
                String eteKey = ete.scope + "." + ete.tagHex;
                m_emvTagEntryIndex.put(eteKey, ete.valueHex);
            }
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

        final ApplicationCapabilityCheckerBase appCapabilityChecker;
        if(
            ascKey.startsWith("A000000003")
        ) {
            appCapabilityChecker = new VisaPaywaveApplicationCapabilityChecker();
        } else if (
            ascKey.startsWith("A000000004")
        ) {
            appCapabilityChecker = new MastercardPaypassApplicationCapabilityChecker();
        } else {
            appCapabilityChecker = new ApplicationCapabilityCheckerBase();
        }

        String appExpiryHex = getValueHex(ascKey, "5F24");
        String appEffectiveHex = getValueHex(ascKey, "5F25");
        // card issuer CA PK index (required for ODA, all apps)
        String capkIndexHex = getValueHex(ascKey, "8F");
        // Application Interchange Profile
        // (required for ODA and usage restrictions, Visa/Paywave)
        // (not required, Mastercard/Paypass)
        // (TBD, others)
        byte[] aipValueBytes = getValueBytes(ascKey,"82");
        // Application Usage Conditions
        // (TBD)
        byte[] aucValueBytes = getValueBytes(ascKey,"9F07");
        // Country of issue of card (relevant for cards with a domestic-only restriction)
        String countryISOCodeValueHex = getValueHex(ascKey, "9F1A");

        outcomeIndex = appCapabilityChecker.checkODACapability(
            outcomeIndex, capabilityNotes, capkIndexHex, aipValueBytes
        );
        outcomeIndex = appCapabilityChecker.checkUsageRestrictions(
            outcomeIndex, capabilityNotes, aipValueBytes, aucValueBytes, countryISOCodeValueHex
        );
        outcomeIndex = appCapabilityChecker.checkValidityPeriod(
            outcomeIndex, capabilityNotes, appExpiryHex, appEffectiveHex
        );

        capabilityNotes.append(_OVERALL_CHECK_OUTCOMES[outcomeIndex] + "\n");
    }

    private String getValueHex(String scope, String tagHexString) {
        AppSelectionContext scopeAsc = new AppSelectionContext(scope);
        for(String scopeToCheck: scopeAsc.getInheritedScopes()) {
            String tagValueHex = m_emvTagEntryIndex.get(scopeToCheck + "." + tagHexString);
            if(tagValueHex != null) {
                return tagValueHex;
            }
        }
        return null;
    }

    private byte[] getValueBytes(String scope, String tagHexString) {
        String valueHex = getValueHex(scope, tagHexString);
        if(valueHex != null) {
            return BytesUtils.fromString(valueHex);
        }
        return null;
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

