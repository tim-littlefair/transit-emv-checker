package net.heretical_camelid.transit_emv_checker.android_app;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import org.jetbrains.annotations.NotNull;

import java.io.*;

public class ExternalFileManager {
    final private MainActivity m_mainActivity;
    boolean m_permissionAcquired;
    Uri m_xmlSaveDirectoryUri = null;
    boolean m_saveDirectoryDisabled = false;
    private String m_xmlTextToSave = null;


    public ExternalFileManager(MainActivity mainActivity) {
        m_mainActivity = mainActivity;
        m_permissionAcquired = true; // checkPermission();
    }

    public void configureSaveDirectory(int intentRequestCode) {
        StorageManager sm = (StorageManager) m_mainActivity.getSystemService(Context.STORAGE_SERVICE);

        // ref https://stackoverflow.com/a/72404595
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.setFlags(
            Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED |
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );
        String URI_PREFIX = "content://com.android.externalstorage.documents/tree/primary%3A";
        String URI_SUFFIX = "Android%2F";
        Uri initialUri = Uri.parse(URI_PREFIX + URI_SUFFIX);
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);
        m_mainActivity.startActivityForResult(intent, intentRequestCode);
    }

    public @NotNull String saveViaIntent(String xmlFilename, String xmlContent, int intentRequestCode) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/xml");
        intent.putExtra(Intent.EXTRA_TITLE, xmlFilename);
        m_xmlTextToSave = xmlContent;
        m_mainActivity.startActivityForResult(intent, intentRequestCode, null);
        return m_xmlSaveDirectoryUri.getPath() + "/" + xmlFilename;
    }

    public @NotNull String saveDirectly(String xmlFilename, String xmlContent) {
        Uri.Builder documentUriBuilder = m_xmlSaveDirectoryUri.buildUpon();
        documentUriBuilder.appendPath(xmlFilename);
        Uri documentUri = documentUriBuilder.build();
        OutputStream outputStream;
        try {
            outputStream = m_mainActivity.getContentResolver().openOutputStream(documentUri);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
            writer.write("something, anything\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
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
