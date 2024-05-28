package net.heretical_camelid.transit_emv_checker.android_app;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.*;

public class FileSaver {
    final private MainActivity m_mainActivity;
    boolean m_permissionAcquired;

    public FileSaver(MainActivity mainActivity) {
        m_mainActivity = mainActivity;
        m_permissionAcquired = true; // checkPermission();
    }

/*
    private boolean checkPermission() {
        // https://stackoverflow.com/a/49378201
        int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
        if (ContextCompat.checkSelfPermission(m_mainActivity, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Request for permission
            ActivityCompat.requestPermissions(m_mainActivity,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            return false;
        } else {
            return true;
        }
    }
*/

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
}
