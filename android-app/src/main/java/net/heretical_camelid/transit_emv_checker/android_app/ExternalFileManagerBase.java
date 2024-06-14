package net.heretical_camelid.transit_emv_checker.android_app;

import android.net.Uri;

abstract public class ExternalFileManagerBase {
    abstract void saveFile(String fileBaseName, String fileMimeType, byte[] fileContent);
    abstract void storeFileContent(Uri documentUri);
}
