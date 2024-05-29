package net.heretical_camelid.transit_emv_checker.android_app;

import android.net.Uri;

import java.util.TreeMap;

abstract public class ExternalFileManagerBase {
    abstract void configureSaveDirectory(TreeMap<String, String> mPermissionStatuses);
    abstract String saveFile(String xmlFilename, String xmlContent);
    abstract void storeFileContent(Uri documentUri);
}
