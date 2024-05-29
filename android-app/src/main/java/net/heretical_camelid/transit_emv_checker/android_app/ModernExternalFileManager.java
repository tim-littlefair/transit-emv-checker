package net.heretical_camelid.transit_emv_checker.android_app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import org.jetbrains.annotations.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.TreeMap;
import android.Manifest;

public class ModernExternalFileManager extends ExternalFileManagerBase {
    static final Logger LOGGER = LoggerFactory.getLogger(ModernExternalFileManager.class);

    final private MainActivity m_mainActivity;
    Uri m_xmlSaveDirectoryUri = null;
    boolean m_saveDirectoryDisabled = false;
    private String m_xmlTextToSave = null;


    public ModernExternalFileManager(MainActivity mainActivity) {
        m_mainActivity = mainActivity;
    }

    public void configureSaveDirectory(TreeMap<String, String> permissionStatuses) {
        m_xmlSaveDirectoryUri = null;

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
        String URI_SUFFIX = "Download%2F";
        Uri initialUri = Uri.parse(URI_PREFIX + URI_SUFFIX);
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);

        ActivityResultLauncher<Intent> directoryAccessRequestLauncher =
            m_mainActivity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if(result.getResultCode()== Activity.RESULT_OK ) {
                            m_xmlSaveDirectoryUri = result.getData().getData();
                            m_mainActivity.getContentResolver().takePersistableUriPermission(
                                m_xmlSaveDirectoryUri,
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            );
                        }
                    }
                }
            )
        ;
        directoryAccessRequestLauncher.launch(intent);

/*
        Uri applicationUri = Uri.parse("package:${BuildConfig.APPLICATION_ID}");
        Intent intent = new Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            applicationUri
        );
        m_mainActivity.startActivityForResult(intent, intentRequestCode);
 */
    }

    public @NotNull String saveFile(String xmlFilename, String xmlContent) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/xml");
        intent.putExtra(Intent.EXTRA_TITLE, xmlFilename);
        m_xmlTextToSave = xmlContent;

        final Uri[] userSelectedDocumentUriHolder = {null};
        ActivityResultLauncher<Intent> fileSaveRequestLauncher =
            m_mainActivity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if(result.getResultCode()== Activity.RESULT_OK ) {
                            userSelectedDocumentUriHolder[0] = result.getData().getData();
                            storeFileContent(userSelectedDocumentUriHolder[0]);
                        }
                    }
                }
            )
        ;
        fileSaveRequestLauncher.launch(intent);
        String userSelectedDocumentPath = null;
        if(userSelectedDocumentUriHolder[0]!=null) {
            userSelectedDocumentPath = userSelectedDocumentUriHolder[0].getPath();
        }
        return userSelectedDocumentPath;
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
