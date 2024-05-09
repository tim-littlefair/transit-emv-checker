package net.heretical_camelid.transit_emv_checker.library;

import fr.devnied.bitlib.BytesUtils;

/**
 * Normal processing of an EMV payment media consists of 
 * exchange between the terminal and the media of a sequence of 
 * commands (from the terminal) and responses (from the media).
 * The following class contains raw and interpreted data about
 * a single command/response pair.
 */
class CommandAndResponse {
    int stepNumber = 0;
    String stepName = null;
    byte[] rawCommand = null;
    byte[] rawResponse = null;
    String interpretedCommand = null;
    String interpretedResponseStatus = null;
    String interpretedResponseBody = null;

    private void appendIndentedHexNode(
        StringBuffer fragmentBuffer, String nodeKey, String hexValueString, 
        int indentDepth, String indentString
    ) {
        for(int i=0; i<indentDepth;++i) {
            fragmentBuffer.append(indentString);
        }
        fragmentBuffer.append(String.format("<%s>\n",nodeKey));

        for(int i=0; i<indentDepth+1;++i) {
            fragmentBuffer.append(indentString);
        }
        fragmentBuffer.append(hexValueString);
        fragmentBuffer.append("\n");

        for(int i=0; i<indentDepth;++i) {
            fragmentBuffer.append(indentString);
        }
        fragmentBuffer.append(String.format("</%s>\n",nodeKey));
    }

    String toXmlFragment(String indentString, boolean captureOnly) {
        StringBuffer xmlFragment = new StringBuffer();

        xmlFragment.append(String.format(
            "%s<command_and_response step_number=\"%d\" step_name=\"%s\">\n",
            indentString, stepNumber, stepName
        ));

        if(rawCommand!=null) {
            appendIndentedHexNode(
                xmlFragment, "raw_command", 
                BytesUtils.bytesToString(rawCommand),2,indentString
            );
        }

        if(captureOnly==false && interpretedCommand!=null) {
            xmlFragment.append(String.format(
                "%s<interpreted_command>\n%s\n%s</interpreted_command>\n",
                indentString + indentString,
                interpretedCommand.strip(),
                indentString + indentString
            ));
        }
        if(rawResponse!=null) {
            xmlFragment.append(String.format(
                "%s<raw_response>\n%s\n%s</raw_response>\n",
                indentString + indentString, 
                BytesUtils.bytesToString(rawResponse),
                indentString + indentString
            ));
        }  

        if(captureOnly==false && interpretedResponseStatus!=null) {
            xmlFragment.append(String.format(
                "%s<interpreted_response_status>%s</interpreted_response_status>\n",
                indentString+indentString, interpretedResponseStatus
            ));
        }
        if(captureOnly==false && interpretedResponseBody!=null) {
            xmlFragment.append(String.format(
                "%s<interpreted_response_body>\n%s\n%s</interpreted_response_body>\n",
                indentString + indentString, 
                interpretedResponseBody.strip(), 
                indentString + indentString
            ));
        }
        xmlFragment.append(indentString + "</command_and_response>\n");

        return xmlFragment.toString();
    }
}