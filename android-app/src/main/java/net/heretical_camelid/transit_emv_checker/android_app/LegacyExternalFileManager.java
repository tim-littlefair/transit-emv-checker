package net.heretical_camelid.transit_emv_checker.android_app;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.TreeMap;

public class LegacyExternalFileManager extends ExternalFileManagerBase {
    static final Logger LOGGER = LoggerFactory.getLogger(LegacyExternalFileManager.class);

    final private MainActivity m_mainActivity;
    boolean m_permissionAcquired;
    Uri m_xmlSaveDirectoryUri = null;
    boolean m_saveDirectoryDisabled = false;
    private String m_xmlTextToSave = null;


    public LegacyExternalFileManager(MainActivity mainActivity) {
        m_mainActivity = mainActivity;
    }

    @Override
    public void configureSaveDirectory(TreeMap<String, String> permissionStatuses) {
        m_xmlSaveDirectoryUri = null;
        for(String permissionName: permissionStatuses.keySet()) {
            LOGGER.info(String.format(
                "Permission: %s status: %s",
                permissionName, permissionStatuses.get(permissionName)
            ));
        }
        String externalStoragePermissionStatus = permissionStatuses.get(Manifest.permission.READ_EXTERNAL_STORAGE);
        if(MainActivity.PERMISSION_STATE_GRANTED.equals(externalStoragePermissionStatus)) {
            File downloadDirFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if(downloadDirFile != null) {
                m_xmlSaveDirectoryUri = Uri.fromFile(downloadDirFile);
                LOGGER.info("Files can be saved in " + m_xmlSaveDirectoryUri.getPath());
            }
        } else {
            File[] appCacheDirs = m_mainActivity.getExternalCacheDirs();
            LOGGER.info("Number of cache dirs: " + appCacheDirs.length);
            File appCacheDir = appCacheDirs[appCacheDirs.length - 1];
            if (appCacheDir == null) {
                LOGGER.warn("Can't save files: cache directory not available");
            } else {
                boolean appCacheDirExists = appCacheDir.exists();
                if (appCacheDirExists == false) {
                    appCacheDirExists = appCacheDir.mkdirs();
                }
                if (appCacheDirExists == false) {
                    LOGGER.warn("Can't save files: cache directory did not exist and could not be created");
                    appCacheDirExists = appCacheDir.mkdirs();
                }
                if (appCacheDir.canWrite() == false) {
                    LOGGER.warn("Can't save files: cache directory not writeable");
                } else {
                    m_xmlSaveDirectoryUri = Uri.fromFile(appCacheDir);
                    LOGGER.info("Files can be saved in " + appCacheDir.getPath());
                }
            }
        }
    }


    @Override
    public @NotNull String saveFile(String xmlFilename, String xmlContent) {
        Uri.Builder documentUriBuilder = m_xmlSaveDirectoryUri.buildUpon();
        documentUriBuilder.appendPath(xmlFilename);
        Uri documentUri = documentUriBuilder.build();
        LOGGER.info("Attempting to save to " + documentUri.getPath());
        OutputStream outputStream;
        try {
            outputStream = m_mainActivity.getContentResolver().openOutputStream(documentUri);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
            writer.write(xmlContent);
            writer.flush();
            writer.close();
            LOGGER.error("Saved to " + documentUri.getPath());
        } catch (IOException e) {
            LOGGER.error("Failed to save with message: " + e.getMessage());
            throw new RuntimeException(e);
        }
        return documentUri.getPath();
    }

    public void setSaveDirectory(Uri uri) {
        m_xmlSaveDirectoryUri = uri;
        m_mainActivity.getContentResolver().takePersistableUriPermission(
            m_xmlSaveDirectoryUri,
      Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_READ_URI_PERMISSION
        );
        m_saveDirectoryDisabled = false;
    }

    public void storeFileContent(Uri documentUri) {
        OutputStream outputStream;
        try {
            outputStream = m_mainActivity.getContentResolver().openOutputStream(documentUri);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
            writer.write(m_xmlTextToSave);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        m_xmlTextToSave = null;
    }
}
