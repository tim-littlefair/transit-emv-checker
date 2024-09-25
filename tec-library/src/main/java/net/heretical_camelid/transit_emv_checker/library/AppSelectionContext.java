package net.heretical_camelid.transit_emv_checker.library;

import java.util.ArrayList;
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
    String appVersionNumber = null; // optional
    String appKernelId = null;      // optional
    String label = "??????";
    List<TagAndLength> pdol = null; // optional - used to interpret terminal tags attached to GPO command

    AppSelectionContext(String ascString) {
        int priorityIndex = ascString.indexOf("p");
        if(priorityIndex!=-1) {
            assert ascString.length() == priorityIndex + 3;
            priority = ascString.substring(priorityIndex+1);
            ascString = ascString.substring(0, priorityIndex);
        }
        int kernelIndex = ascString.indexOf("k");
        if(kernelIndex!=-1) {
            assert ascString.length() == kernelIndex + 3;
            appKernelId = ascString.substring(kernelIndex+1);
            ascString = ascString.substring(0, kernelIndex);
        }
        int versionIndex = ascString.indexOf("v");
        if(versionIndex!=-1) {
            assert ascString.length() == versionIndex + 5;
            appVersionNumber = ascString.substring(versionIndex+1);
            ascString = ascString.substring(0, versionIndex);
        }
        aid = ascString;
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

    public ArrayList<String> getInheritedScopes() {
        ArrayList<String> retval = new ArrayList<>();
        retval.add(this.toString());
        AppSelectionContext copyAsc = this;

        // We could implement a comprehensive inheritance for every
        // variation on removing the priority, version, kernel
        // where they are present, but for the moment, the following
        // three cases seem to cover the majority of needs.
        if(copyAsc.priority.length()>0 && copyAsc.appVersionNumber!=null
        ) {
            copyAsc.priority = "";
            retval.add(copyAsc.toString());
            copyAsc.priority = this.priority;
            copyAsc.appVersionNumber = null;
            retval.add(copyAsc.toString());
            copyAsc.priority = "";
            retval.add(copyAsc.toString());
        } else if(copyAsc.priority.length()>0) {
            copyAsc.priority = "";
            retval.add(copyAsc.toString());
        } else if(copyAsc.appVersionNumber!=null) {
            copyAsc.appVersionNumber = null;
            retval.add(copyAsc.toString());
        }
        return retval;
    }
}