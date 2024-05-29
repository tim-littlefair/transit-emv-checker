package net.heretical_camelid.transit_emv_checker.android_app;

import android.net.Uri;

import java.util.TreeMap;

public interface ExternalFileManagerInterface {
    String saveDirectly(String xmlFilename, String xmlContent);

    void setSaveDirectory(Uri data);

    void storeFileContent(Uri documentUri);

    void configureSaveDirectory(TreeMap<String, String> mPermissionStatuses);
}
