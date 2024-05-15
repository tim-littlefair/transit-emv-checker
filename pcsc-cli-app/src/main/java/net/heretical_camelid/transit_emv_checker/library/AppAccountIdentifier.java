package net.heretical_camelid.transit_emv_checker.library;

/**
 * The medium is typically identified by a three part tuple 
 * containing mandatory PAN, mandatory expiry month, and 
 * optional PAN sequence number (PSN)
 * All of these are from EMV tags which are received in responses
 * after a specific AID is selected, so in theory it would be possible
 * for different applications on the card to respond with different
 * values and the ApduObserver stores these in a 
 */
class AppAccountIdentifier implements Comparable<AppAccountIdentifier> {
    String applicationPAN = null;
    String applicationExpiryMonth = null;
    // If the PSN is not explicitly set we implicitly set it to 
    // the empty string (as null would not be comparable)
    String applicationPSN = "";

    public String toString() {
        if(applicationPSN.length()>0) {
            return String.format("%s.%s.%s",applicationPAN, applicationExpiryMonth,applicationPSN);
        } else {
            return String.format("%s.%s",applicationPAN, applicationExpiryMonth);
        }
    }

    public int compareTo(AppAccountIdentifier other) {
        int compareResult = applicationPAN.compareTo(other.applicationPAN);
        if(compareResult == 0) {
            compareResult = applicationPSN.compareTo(other.applicationPSN);
        }
        if(compareResult == 0) {
            compareResult = applicationExpiryMonth.compareTo(other.applicationExpiryMonth);
        }
        return compareResult;        
    }
}