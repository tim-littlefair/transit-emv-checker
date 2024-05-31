package net.heretical_camelid.transit_emv_checker.android_app;

import android.net.Uri;

import java.util.TreeMap;

abstract public class ExternalFileManagerBase {
    abstract void requestPermissions();
    abstract void configureSaveDirectory(TreeMap<String, String> mPermissionStatuses);
    abstract void saveFile(String fileBaseName, String fileMimeType, byte[] fileContent);
    abstract void storeFileContent(Uri documentUri);
}
