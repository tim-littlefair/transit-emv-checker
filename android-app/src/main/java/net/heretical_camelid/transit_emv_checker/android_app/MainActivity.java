package net.heretical_camelid.transit_emv_checker.android_app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.MutableLiveData;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.io.*;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

import net.heretical_camelid.transit_emv_checker.android_app.databinding.ActivityMainBinding;
import net.heretical_camelid.transit_emv_checker.android_app.ui.home.HomeViewModel;
import net.heretical_camelid.transit_emv_checker.android_app.ui.html.HtmlViewModel;

import org.jetbrains.annotations.NotNull;

public class MainActivity extends AppCompatActivity {

    // Activity wide UI elements
    private BottomNavigationView m_navView;
    private NavController m_navController;

    // Model attributes driving fragment UI elements
    private final HashMap<Integer, MutableLiveData<String> > m_htmlPageRegistry = new HashMap<>();
    private MutableLiveData<String> m_homePageStatus;
    private MutableLiveData<String> m_homePageLog = null;

    // NFC/EMV operations controller
    private EMVMediaAgent m_emvMediaAgent;

    // Saving XML capture files depends on these
    private FileSaver m_fileSaver;
    private final int REQUEST_CODE_DOCUMENT_DIRECTORY_ACCESS = 101;
    private final int REQUEST_CODE_CREATE_DOCUMENT = 102;

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
        m_fileSaver = new FileSaver(this);
        m_fileSaver.configureXmlSaveDirectory(REQUEST_CODE_DOCUMENT_DIRECTORY_ACCESS);
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                versionString +=
                    "@" + ZonedDateTime.now( ZoneOffset.UTC )
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'"))
                    ;
            }
        } else {
            // In CI builds, versionCode will contain an integer
            // parsed from the 7-hex-digit prefix of the git
            // commit hash.
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
            m_fileSaver.setSaveDirectory(resultData.getData());
        } else if(requestCode == REQUEST_CODE_CREATE_DOCUMENT) {
            // String xmlFilename = resultData.getParcelableExtra(Intent.EXTRA_TITLE);
            Uri documentUri = resultData.getData();
            m_fileSaver.storeFileContent(documentUri);
        }
    }

    public String saveXmlCaptureFile(String xmlFilename, String xmlContent) {
        return m_fileSaver.saveViaIntent(xmlFilename, xmlContent, REQUEST_CODE_DOCUMENT_DIRECTORY_ACCESS);
    }
}