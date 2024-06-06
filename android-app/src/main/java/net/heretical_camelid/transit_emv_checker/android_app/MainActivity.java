package net.heretical_camelid.transit_emv_checker.android_app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import net.heretical_camelid.transit_emv_checker.android_app.databinding.ActivityMainBinding;
import net.heretical_camelid.transit_emv_checker.android_app.ui.home.HomeViewModel;
import net.heretical_camelid.transit_emv_checker.android_app.ui.html.HtmlViewModel;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainActivity extends AppCompatActivity {
    static final Logger LOGGER = LoggerFactory.getLogger(MainActivity.class);


    // Activity wide UI elements
    private BottomNavigationView m_navView;
    private NavController m_navController;

    // Model attributes driving fragment UI elements
    private final HashMap<Integer, MutableLiveData<String> > m_htmlPageRegistry = new HashMap<>();
    private MutableLiveData<String> m_homePageStatus;
    private MutableLiveData<String> m_homePageLog = null;

    // NFC/EMV operations controller
    private EMVMediaAgent m_emvMediaAgent;

    // Permission management
    final static int REQUEST_CODE_REQUEST_PERMISSIONS = 101;
    static final String PERMISSION_STATE_GRANTED = "granted";
    static final String PERMISSION_STATE_DENIED = "denied";
    TreeMap<String,String> m_permissionStatuses;

    // Saving XML capture files depends on these
    private ExternalFileManagerBase m_externalFileManager;
    final static int REQUEST_CODE_DOCUMENT_DIRECTORY_ACCESS = 201;
    final static int REQUEST_CODE_CREATE_DOCUMENT = 202;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_emvMediaAgent = new EMVMediaAgent(this);

        m_htmlPageRegistry.put(R.id.navigation_transit,new MutableLiveData<>());
        m_htmlPageRegistry.put(R.id.navigation_emv_details,new MutableLiveData<>());
        m_htmlPageRegistry.put(R.id.navigation_about,new MutableLiveData<>());

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home,
                R.id.navigation_transit,
                R.id.navigation_emv_details,
                R.id.navigation_about
        ).build();
        m_navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, m_navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, m_navController);
        m_navView = findViewById(R.id.nav_view);
        populateAboutPage();
        setInitialState();
        m_externalFileManager = new ModernExternalFileManager(this);
        m_externalFileManager.requestPermissions();
    }

    private void setPageHtmlText(int pageNavigationId, String htmlText) {
        MutableLiveData<String> pageHtml = m_htmlPageRegistry.get(pageNavigationId);
        if(pageHtml != null) {
            pageHtml.postValue(htmlText);
        }
    }

    private void populateAboutPage() {
        String aboutText = null;
        String openSourceLicensesText;
        try {
            aboutText = getTextFromAsset("about.html");
            openSourceLicensesText = getTextFromAsset("open_source_licenses.html");
        }
        catch(IOException e) {
            // Placeholders
            if(aboutText == null) {
                aboutText = "<html><body><p>TEC by TJL</p></body></html>";
            }
            openSourceLicensesText = "";
        }

        String versionString = BuildConfig.VERSION_NAME;
        int versionCode = BuildConfig.VERSION_CODE;
        if(versionCode==1) {
            // In developer builds from unmodified git source,
            // versionCode will be equal to 1 and is not interesting.
            versionString += "-dev";
            versionString +=
                "@" + ZonedDateTime.now(ZoneOffset.UTC)
                          .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'"))
            ;
        } else {
            // In CI builds, versionCode will contain an integer
            // parsed from the 7-hex-digit prefix of the git
            // commit hash so we can render it back to hex
            // to match the hash prefix.
            versionString += String.format(".%07x",versionCode);
        }
        aboutText = aboutText.replace("%VERSION%",versionString);
        aboutText = aboutText.replace(
            "%OPEN_SOURCE_LICENSES_HTML_BASE64%",
            Base64.encodeToString(openSourceLicensesText.getBytes(),Base64.NO_PADDING)
        );
        setPageHtmlText(R.id.navigation_about,aboutText);
    }

    private @NotNull String getTextFromAsset(String assetFilename) throws IOException {
        InputStream assetStream = getAssets().open(assetFilename);
        int lengthInBytes = assetStream.available();
        byte[] buffer = new byte[lengthInBytes];
        assetStream.read(buffer);
        assetStream.close();
        return new String(buffer, "UTF-8");
    }

    public void setInitialState() {
        setPageHtmlText(R.id.navigation_transit,"<html><body><p>Card not read yet</p></body></html>");
        setPageHtmlText(R.id.navigation_emv_details,"<html><body><p>Card not read yet</p></body></html>");
        //setItemState(R.id.navigation_transit,false);
        //setItemState(R.id.navigation_emv_details,false);
    }

    public void setDisplayMediaDetailsState(String transitCapabilities, String emvApplicationDetails) {
        setPageHtmlText(R.id.navigation_transit,
            "<html><body><pre style='white-space: pre-wrap;'>" +
            transitCapabilities +
            "</pre></body></html>"
        );
        setPageHtmlText(R.id.navigation_emv_details,
            "<html><body><pre style='white-space: pre-wrap;'>" +
                emvApplicationDetails +
                "</pre></body></html>"
        );
        //setItemState(R.id.navigation_transit,true);
        //setItemState(R.id.navigation_emv_details,true);
        // m_navController.navigate(R.id.navigation_transit);
    }

    public void registerHomeViewModel(HomeViewModel theModel) {
        m_homePageLog = theModel.getLog();
    }

    public void registerHtmlViewModel(int whichModel, HtmlViewModel theModel) {
        theModel.setData(m_htmlPageRegistry.get(whichModel));
    }

    private void setItemState(int itemId, boolean isEnabled) {
        View itemView = m_navView.findViewById(itemId);
        assert itemView != null;
        if(isEnabled == false) {
            itemView.setEnabled(false);
            itemView.setOnClickListener(v -> {
                m_navController.navigate(R.id.navigation_home);
                Toast.makeText(
                        MainActivity.this,
                        "No current EMV media",
                        Toast.LENGTH_SHORT
                ).show();
            });
            // itemView.setClickable(true);
        } else {
            itemView.setOnClickListener(null);
            itemView.setEnabled(true);
        }
    }


    public void homePageLogAppend(String s) {
        if(m_homePageLog != null) {
            m_homePageLog.postValue(
                m_homePageLog.getValue() + "\n" + s
            );
        }
    }

    public void tryToDetectMedia() {
        m_homePageLog.setValue("About to try to detect EMV media");
        m_emvMediaAgent.enableDetection();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if(resultCode != RESULT_OK) {
            Toast.makeText(this,"Request declined",Toast.LENGTH_LONG).show();
        } else if(resultData==null) {
            Toast.makeText(this, "Request approved but null data", Toast.LENGTH_LONG).show();
        } else if(requestCode== REQUEST_CODE_DOCUMENT_DIRECTORY_ACCESS) {
            LOGGER.warn("Unexpected directory access result received at MainActivity");
        } else if(requestCode == REQUEST_CODE_CREATE_DOCUMENT) {
            m_externalFileManager.storeFileContent(resultData.getData());
        } else {
            LOGGER.warn("Unexpected result received at MainActivity for request code " + requestCode);
        }
    }

    public void saveXmlCaptureFile(String xmlFilename, String xmlContent) {
        //return m_fileSaver.saveViaIntent(xmlFilename, xmlContent, REQUEST_CODE_DOCUMENT_DIRECTORY_ACCESS);
        m_externalFileManager.saveFile(xmlFilename, "text/xml", xmlContent.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void onRequestPermissionsResult(
        final int requestCode,
        final String[] permissions,
        final int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for(int i=0; i<permissions.length; ++i) {
            String permissionStatus =
                (PackageManager.PERMISSION_GRANTED==grantResults[i]) ?
                PERMISSION_STATE_GRANTED: PERMISSION_STATE_DENIED
            ;
            m_permissionStatuses.put(permissions[i], permissionStatus);
        }
        m_externalFileManager.configureSaveDirectory(m_permissionStatuses);
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        LOGGER.info(String.format("onNewIntent action=%s uri=%s",intent.getAction(),intent.getData()));
        if(intent.getAction() == Intent.ACTION_CREATE_DOCUMENT) {
            m_externalFileManager.storeFileContent(intent.getData());
        }
    }

    /*
    @Override
    public void startActivity(Intent intent, Bundle bundle) {
        super.startActivity(intent, bundle);
        LOGGER.info(String.format("startActivity action=%s uri=%s",intent.getAction(),intent.getData()));
        if(intent.getAction() == Intent.ACTION_CREATE_DOCUMENT) {
            m_externalFileManager.storeFileContent(intent.getData());
        }
    }
    */
}