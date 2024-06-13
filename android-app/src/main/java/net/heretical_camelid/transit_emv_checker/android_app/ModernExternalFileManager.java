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
