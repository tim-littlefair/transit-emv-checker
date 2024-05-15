package net.heretical_camelid.transit_emv_checker.library;

import java.util.List;

import com.github.devnied.emvnfccard.iso7816emv.TagAndLength;

// The medium (card, phone, watch, ring ...) can contain multiple applications
// with different AIDs, and different EMV tags may be present or common EMV 
// tags may have different values according to which entry in the PPSE has
// been selected (NB multiple PPSE entries may contain the same AID, for 
// example, for applications which are capable of being processed by more than one 
// kernel).
// The following members attempt to track which PPSE AID entry is presently being considered
// so that tag value differences between different selects.
class AppSelectionContext implements Comparable<AppSelectionContext> {
    final String aid;               // mandatory on creation
    String priority = "";           // optional - empty string will be treated as highest priority if not populated
    String label = "??????";
    String appVersionNumber = null; // optional
    String appKernelId = null;      // optional
    List<TagAndLength> pdol = null; // optional - used to interpret terminal tags attached to GPO command

    AppSelectionContext(String aid) {
        this.aid = aid;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(aid);
        if(appVersionNumber!=null) {
            sb.append("v" + appVersionNumber);
        }
        if(appKernelId!=null) {
            sb.append("k" + appKernelId);
        }
        if(priority.length()>0) {
            sb.append("p" + priority);
        }
        return sb.toString();
    }

    public int compareTo(AppSelectionContext other) {
        // In theory, within a single PPSE, priority should be
        // unique, so it should not be necessary to sort on 
        // anything else.
        return priority.compareTo(other.priority);
    }
}