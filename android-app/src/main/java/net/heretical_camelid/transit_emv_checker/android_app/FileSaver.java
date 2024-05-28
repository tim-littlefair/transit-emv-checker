package net.heretical_camelid.transit_emv_checker.android_app;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.jetbrains.annotations.NotNull;

import java.io.*;

public class FileSaver {
    final private MainActivity m_mainActivity;
    boolean m_permissionAcquired;
    Uri m_xmlSaveDirectoryUri = null;
    boolean m_saveDirectoryDisabled = false;
    private String m_xmlTextToSave = null;


    public FileSaver(MainActivity mainActivity) {
        m_mainActivity = mainActivity;
        m_permissionAcquired = true; // checkPermission();
    }

    public void configureXmlSaveDirectory(int intentRequestCode) {
        StorageManager sm = (StorageManager) m_mainActivity.getSystemService(Context.STORAGE_SERVICE);

        // ref https://stackoverflow.com/a/72404595
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.setFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
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

    private @NotNull String saveDirectly(String xmlFilename, String xmlContent) {
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


    public int saveFile(String fileBaseName, byte[] fileContent) {
        if(m_permissionAcquired == false) {
            return -1;
        }
        byte[] imageBytes = fileContent;
        String filename = fileBaseName;

        // The block below pasted from https://stackoverflow.com/a/77338179
        // presently as nearly verbatim as possible without compile errors.
        // The local variables 'imageBytes' and 'filename' defined directly before this comment
        // and the public method getApplicationContext() defined after the function
        // are glue code to enable the snippet to compile.
        // In addition to the glue code, changes were required to remove
        // the nested declaration of 'filename' in the Android 8/9 block
        // and fix exception throwing syntax at the end of that block.

        // Future checkins of this class will modify this
        // to integrate more economically with the surrounding class

        // VERBATIM_STARTS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 and above
            Context context = getApplicationContext();
            ContentResolver contentResolver = context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Downloads.DISPLAY_NAME, filename);
            contentValues.put(MediaStore.Downloads.MIME_TYPE, "image/png");
            contentValues.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri contentUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
            Uri itemUri = contentResolver.insert(contentUri, contentValues);

            if (itemUri != null) {
                try {
                    OutputStream outputStream = contentResolver.openOutputStream(itemUri);
                    if (outputStream != null) {
                        outputStream.write(imageBytes);
                        outputStream.close();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException("Failed to create a file in the Downloads directory.");
            }
        } else {
            // Android 8 and 9
            File downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            if (!downloadsDirectory.exists()) {
                downloadsDirectory.mkdirs();
            }

            long currentTimestamp = System.currentTimeMillis();
            /* String */ filename = "downloaded_image_" + currentTimestamp + ".png";

            File imageFile = new File(downloadsDirectory, filename);

            try {
                FileOutputStream outputStream = new FileOutputStream(imageFile);
                outputStream.write(imageBytes);
                outputStream.close();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw /*a*/ new RuntimeException(e);
            }
        }
        // VERBATIM_ENDS

        return 0;
    }

    private Context getApplicationContext() { return m_mainActivity;  }

    public void setSaveDirectory(Uri uri) {
        m_xmlSaveDirectoryUri = uri;
        m_mainActivity.getContentResolver().takePersistableUriPermission(
            m_xmlSaveDirectoryUri,
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
