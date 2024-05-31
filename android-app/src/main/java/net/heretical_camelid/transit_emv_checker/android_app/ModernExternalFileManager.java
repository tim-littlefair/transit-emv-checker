package net.heretical_camelid.transit_emv_checker.android_app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.TreeMap;

public class ModernExternalFileManager extends ExternalFileManagerBase {
    static final Logger LOGGER = LoggerFactory.getLogger(ModernExternalFileManager.class);

    final private MainActivity m_mainActivity;
    Uri m_xmlSaveDirectoryUri = null;
    private byte[] m_bytesToSave = null;
    ActivityResultLauncher<Intent> m_createFileLauncher = null;

    public ModernExternalFileManager(MainActivity mainActivity) {
        m_mainActivity = mainActivity;
        m_createFileLauncher = m_mainActivity.registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Uri userSelectedUri = result.getData().getData();
                        storeFileContent(userSelectedUri);
                    }
                }
            }
        );
    }

    @Override
    public void requestPermissions() {
        // Nothing to do for this implementation
        configureSaveDirectory(null);
    }

    public void configureSaveDirectory(TreeMap<String, String> permissionStatuses) {
        m_xmlSaveDirectoryUri = null;

        // ref https://stackoverflow.com/a/72404595
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.setFlags(
            // Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED |
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );
        String URI_PREFIX = "content://com.android.externalstorage.documents/tree/primary%3A";
        String URI_SUFFIX = "Documents%2F";
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
    }

    public void saveFile(String fileBaseName, String fileMimeType, byte[] fileContent) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(fileMimeType);
        intent.putExtra(Intent.EXTRA_TITLE, fileBaseName);
        m_bytesToSave = fileContent;
        //m_mainActivity.startActivityForResult(intent,MainActivity.REQUEST_CODE_CREATE_DOCUMENT);

        m_createFileLauncher.launch(intent);
    }

    public void storeFileContent(Uri documentUri) {
        OutputStream outputStream;
        try {
            LOGGER.info("Before call to openOutputStream for URI " + documentUri);
            outputStream = m_mainActivity.getContentResolver().openOutputStream(documentUri,"wt");
            LOGGER.info("Before write");
            outputStream.write(m_bytesToSave);
            LOGGER.info("Before flush");
            outputStream.flush();
            LOGGER.info("Before close");
            outputStream.close();
            LOGGER.info("All done");
        } catch (IOException e) {
            LOGGER.error("Exception: " + e.getMessage());
        }
        m_bytesToSave = null;
    }
}
