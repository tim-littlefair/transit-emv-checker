package net.heretical_camelid.transit_emv_checker.android_app;

import android.app.Activity;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.provider.DocumentsContract;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.TreeMap;

public class ModernExternalFileManager extends ExternalFileManagerBase {
    static final Logger LOGGER = LoggerFactory.getLogger(ModernExternalFileManager.class);

    static final int SAVEDIR_FLAGS_READ_WRITE = (
        Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
        Intent.FLAG_GRANT_READ_URI_PERMISSION
    );

    final private MainActivity m_mainActivity;
    Uri m_saveDirectoryUri = null;
    private byte[] m_bytesToSave = null;
    ActivityResultLauncher<Intent> m_createFileLauncher;

    public ModernExternalFileManager(MainActivity mainActivity) {
        m_mainActivity = mainActivity;
        m_createFileLauncher = m_mainActivity.registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    assert result.getData() != null;
                    Uri userSelectedUri = result.getData().getData();
                    storeFileContent(userSelectedUri);
                }
            }
        );
    }

    @Override
    public void requestPermissions() {
        m_saveDirectoryUri = null;
        List<UriPermission> persistedPermissions =
            m_mainActivity.getContentResolver().getPersistedUriPermissions()
        ;

        if(persistedPermissions==null || persistedPermissions.isEmpty()) {
            promptUserForSaveDir();
        } else if(persistedPermissions.size()==1) {
            UriPermission currentSaveDirPermission = persistedPermissions.get(0);
            if (currentSaveDirPermission.isWritePermission()) {
                m_saveDirectoryUri = currentSaveDirPermission.getUri();
            } else {
                m_mainActivity.getContentResolver().releasePersistableUriPermission(
                    currentSaveDirPermission.getUri(),
                    SAVEDIR_FLAGS_READ_WRITE
                );
                promptUserForSaveDir();
            }
        } else {
            // More than one permission => something weird has happened
            // Delete all the persistable permissions found and start again
            for(UriPermission uriPermission: persistedPermissions) {
                m_mainActivity.getContentResolver().releasePersistableUriPermission(
                    uriPermission.getUri(),
                    SAVEDIR_FLAGS_READ_WRITE
                );
            }
            promptUserForSaveDir();
        }
    }

    private void promptUserForSaveDir() {
        // ref https://stackoverflow.com/a/72404595
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.setFlags(
            // Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED |
            SAVEDIR_FLAGS_READ_WRITE |
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION

        );
        String URI_PREFIX = "content://com.android.externalstorage.documents/tree/primary";
        Uri initialUri = Uri.parse(URI_PREFIX );
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);

        ActivityResultLauncher<Intent> directoryAccessRequestLauncher =
            m_mainActivity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        assert result.getData() != null;
                        m_saveDirectoryUri = result.getData().getData();
                        m_mainActivity.getContentResolver().takePersistableUriPermission(
                            m_saveDirectoryUri,
                            SAVEDIR_FLAGS_READ_WRITE
                        );
                    }
                }
            )
        ;
        directoryAccessRequestLauncher.launch(intent);
    }

    public void configureSaveDirectory(TreeMap<String, String> permissionStatuses) {
        // no longer required
    }

    public void saveFile(String fileBaseName, String fileMimeType, byte[] fileContent) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(fileMimeType);
        intent.putExtra(Intent.EXTRA_TITLE, fileBaseName);
        m_bytesToSave = fileContent;
        m_createFileLauncher.launch(intent);
    }

    public void storeFileContent(Uri documentUri) {
        OutputStream outputStream;
        try {
            LOGGER.info("Before call to openOutputStream for URI " + documentUri);
            outputStream = m_mainActivity.getContentResolver().openOutputStream(documentUri,"wt");
            assert outputStream != null;
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
