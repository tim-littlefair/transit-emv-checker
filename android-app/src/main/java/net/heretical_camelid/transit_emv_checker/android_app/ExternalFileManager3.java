package net.heretical_camelid.transit_emv_checker.android_app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.TreeMap;

/** @noinspection unused*/
public class ExternalFileManager3 extends ExternalFileManagerBase {
    static final Logger LOGGER = LoggerFactory.getLogger(ModernExternalFileManager.class);

    static final int SAVEDIR_FLAGS_READ_WRITE = (
        Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
        Intent.FLAG_GRANT_READ_URI_PERMISSION
    );

    final private MainActivity m_mainActivity;
    File m_saveDirectoryFile = null;
    Uri m_saveDirectoryUri = null;

    private byte[] m_bytesToSave = null;
    ActivityResultLauncher<Intent> m_createFileLauncher;

    public ExternalFileManager3(MainActivity mainActivity) {
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
        File externalDownloadsDirectory
            // = m_mainActivity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS
            );

        if( externalDownloadsDirectory == null ) {
            LOGGER.warn("Downloads directory on external storage not found");
        // } else if(externalDownloadsDirectory.canWrite() == false) {
        //    LOGGER.warn("Downloads directory on external storage not writeable");
        } else {
            // Files are stored under a parent directory "TransitEMVChecker" in
            // one subdirectory per local business day
            String currentDate = DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now());
            File cetDirectory = new File(externalDownloadsDirectory, "TransitEMVChecker");
            m_saveDirectoryFile = new File(cetDirectory, currentDate);
            try {
                if (m_saveDirectoryFile.exists()) {
                    LOGGER.info(
                        "Save directory already exists at " +
                            m_saveDirectoryFile.getCanonicalPath()
                    );
                } else if (m_saveDirectoryFile.mkdirs()) {
                    LOGGER.info(
                        "Save directory created at " +
                            m_saveDirectoryFile.getCanonicalPath()
                    );
                } else {
                    LOGGER.warn(
                        "Save directory could not be created at " +
                            m_saveDirectoryFile.getCanonicalPath()
                    );
                    m_saveDirectoryFile = null;
                }
            } catch (IOException | SecurityException e) {
                LOGGER.error(
                    "Exception caught during save directory creation " +
                        e.getMessage()
                );
                m_saveDirectoryFile = null;
            }
        }

        if(m_saveDirectoryFile != null) {
            m_saveDirectoryUri = Uri.fromFile(m_saveDirectoryFile);
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
        intent.putExtra(
                DocumentsContract.EXTRA_PROMPT,
                "Please select a directory to store EMV tap dumps in"
        );
        intent.putExtra(
                DocumentsContract.EXTRA_INFO,
                "More info"
        );

        ActivityResultLauncher<Intent> directoryAccessRequestLauncher =
            m_mainActivity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode()== Activity.RESULT_OK ) {
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
        m_bytesToSave = fileContent;
        if(m_saveDirectoryUri == null) {
            return;
        }
        Uri documentUri = Uri.withAppendedPath(m_saveDirectoryUri,fileBaseName + ".xml");
        storeFileContent(documentUri);
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
