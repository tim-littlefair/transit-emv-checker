package net.heretical_camelid.transit_emv_checker.android_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.MutableLiveData;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Objects;

import net.heretical_camelid.transit_emv_checker.android_app.databinding.ActivityMainBinding;
import net.heretical_camelid.transit_emv_checker.android_app.ui.home.HomeFragment;
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

    private StartupAlertListener m_startupAlertListener;

    // Model attributes driving fragment UI elements
    private final HashMap<Integer, MutableLiveData<String> > m_htmlPageRegistry = new HashMap<>();
    private MutableLiveData<String> m_homePageLog = null;

    // NFC/EMV operations controller
    private EMVMediaAgent m_emvMediaAgent;

    // Saving XML capture files depends on these
    private ExternalFileManagerBase m_externalFileManager;
    final static int REQUEST_CODE_DOCUMENT_DIRECTORY_ACCESS = 201;
    final static int REQUEST_CODE_CREATE_DOCUMENT = 202;

    static boolean s_userHasAgreed = false;
    private static AlertDialog s_startupAlert;
    private ActivityMainBinding m_binding;
    private HomeFragment m_homeFragment;

    public static void showStartupAlert() {
        if(s_userHasAgreed ==false) {
            s_startupAlert.show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        s_userHasAgreed = false;
        m_emvMediaAgent = new EMVMediaAgent(this);

        m_htmlPageRegistry.put(R.id.navigation_transit,new MutableLiveData<>());
        m_htmlPageRegistry.put(R.id.navigation_emv_details,new MutableLiveData<>());
        m_htmlPageRegistry.put(R.id.navigation_about,new MutableLiveData<>());

        m_binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(m_binding.getRoot());

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home,
                R.id.navigation_transit,
                R.id.navigation_emv_details,
                R.id.navigation_about
        ).build();
        NavHostFragment navHostFragment = (NavHostFragment) (
            getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main)
        );
        assert navHostFragment != null;
        m_navController = navHostFragment.getNavController();
        NavigationUI.setupActionBarWithNavController(this, m_navController, appBarConfiguration);
        NavigationUI.setupWithNavController(m_binding.navView, m_navController);
        m_navView = findViewById(R.id.nav_view);
        populateAboutPage();
        setInitialState();

        buildStartupAlert();
        showStartupAlert();
    }

    @Override public void onResume() {
        super.onResume();
        if(s_userHasAgreed ==false) {
            s_startupAlert.show();
        }
    }

    private void buildStartupAlert() {
        m_startupAlertListener = new StartupAlertListener(this);
        m_externalFileManager = new ModernExternalFileManager(this);
        AlertDialog.Builder startupAlertBuilder = new AlertDialog.Builder(MainActivity.this);
        startupAlertBuilder.setTitle("Transit EMV Checker");
        String disclaimerHtml;
        try {
            disclaimerHtml = MainActivity.this.getTextFromAsset("short_disclaimer.html");
        } catch (IOException e) {
            disclaimerHtml = "Please be aware of implications of using this app related to PCI and card issuer fraud detection";
        }
        Spanned disclaimerRichText = Html.fromHtml(
            disclaimerHtml, (
                Html.FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH |
                Html.FROM_HTML_SEPARATOR_LINE_BREAK_LIST |
                Html.FROM_HTML_SEPARATOR_LINE_BREAK_LIST_ITEM
            )
        );
        startupAlertBuilder.setMessage(disclaimerRichText);
        startupAlertBuilder.setPositiveButton(
            R.string.i_understand_and_agree, m_startupAlertListener
        );
        startupAlertBuilder.setNeutralButton("more information", m_startupAlertListener);
        startupAlertBuilder.setNegativeButton("decline and close", m_startupAlertListener);
        s_startupAlert = startupAlertBuilder.create();
    }

    private void setPageHtmlText(int pageNavigationId, String htmlText) {
        MutableLiveData<String> pageHtml = m_htmlPageRegistry.get(pageNavigationId);
        if(pageHtml != null) {
            pageHtml.postValue(htmlText);
        }
    }

    void populateAboutPage() {
        String longDisclaimerText;
        try {
            longDisclaimerText = getTextFromAsset("long_disclaimer.html");
        }
        catch(IOException e) {
            longDisclaimerText = "<html><body><p>ERROR: Long disclaimer text not found</p></body></html>";
        }

        if(s_userHasAgreed == false) {
            // Display only the long disclaimer and a button to trigger return
            // to the startup dialog
            String returnButtonText = (
                "<a href='data:return_to_startup_alert'><button " +
                "style='align: center; position: absolute; left: 50%; transform: translateX: 50%;' " +
                ">"
            );
            returnButtonText += "AGREE OR DECLINE";
            returnButtonText += "</button><br/><br/><br/></a>";
            setPageHtmlText(R.id.navigation_about,
                longDisclaimerText + returnButtonText
            );
        } else {
            // Display the full about text (which includes the long disclaimer as
            // a section.
            String aboutText = null;
            try {
                aboutText = getTextFromAsset("about.html");
            } catch (IOException e) {
                // Placeholders
                if (aboutText == null) {
                    aboutText = "<html><body><p>TEC by TJL</p></body></html>";
                }
            }

            String versionString = getVersionString();
            aboutText = aboutText.replace("%VERSION%", versionString);
            aboutText = aboutText.replace("%RESPONSIBLE_USE%", longDisclaimerText);
            setPageHtmlText(R.id.navigation_about, aboutText);
        }
    }

    @NonNull
    private static String getVersionString() {
        String versionString = BuildConfig.VERSION_NAME;
        int versionCode = BuildConfig.VERSION_CODE;

        // In CI builds, the build script devenv/gen_version_code.sh will
        // run before the app is built and will modify build.gradle so that
        // versionCode will contain an integer parsed from the 7-hex-digit
        // prefix of the git commit hash.
        // This integer is rendered back into hex to match the hash prefix
        // as shown in the git commit log.
        versionString += String.format("-%07x",versionCode);

        if(versionCode==1) {
            // In developer builds from unmodified git source,
            // the hexified versionCode will always be equal to 000001
            // so replace it with a timestamp.
            versionString = versionString.replace(
                "-0000001",
                "@" + ZonedDateTime.now(ZoneOffset.UTC)
                          .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'"))
            );
        }
        return versionString;
    }

    private @NotNull String getTextFromAsset(String assetFilename) throws IOException {
        int _BLOCK_SIZE = 10240;
        byte[] nextBlock = new byte[_BLOCK_SIZE];
        InputStream assetStream = getAssets().open(assetFilename);
        byte[] assetBytes = new byte[0];
        int bytesRead;
        while(true) {
            bytesRead = assetStream.read(nextBlock);
            if(bytesRead<=0) {
                break;
            }
            byte[] newAssetBytes = new byte[assetBytes.length + bytesRead];
            System.arraycopy(assetBytes,0, newAssetBytes, 0, assetBytes.length);
            System.arraycopy(nextBlock, 0, newAssetBytes, assetBytes.length, bytesRead);
            assetBytes = newAssetBytes;
        }
        assetStream.close();
        return new String(assetBytes, StandardCharsets.UTF_8);
    }

    public void setInitialState() {
        setPageHtmlText(R.id.navigation_transit,"<html><body><p>Card not read yet</p></body></html>");
        setPageHtmlText(R.id.navigation_emv_details,"<html><body><p>Card not read yet</p></body></html>");
        if(m_homeFragment != null) {
            m_homeFragment.resetButtonToPromptForDetection();
        }
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
    }

    public void registerHomeViewModel(HomeViewModel theModel) {
        m_homePageLog = theModel.getLog();
    }

    public void registerHtmlViewModel(int whichModel, HtmlViewModel theModel) {
        theModel.setData(m_htmlPageRegistry.get(whichModel));
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
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        LOGGER.info(String.format("onNewIntent action=%s uri=%s",intent.getAction(),intent.getData()));
        if(Objects.equals(intent.getAction(), Intent.ACTION_CREATE_DOCUMENT)) {
            m_externalFileManager.storeFileContent(intent.getData());
        }
    }

    public void navigateToPage(int pageId) {

        m_navController.navigate(pageId);
    }

    public void closeApplication() {
        MainActivity.this.finish();
        System.exit(0);
    }

    public void registerHomeFragment(HomeFragment homeFragment) {
        m_homeFragment = homeFragment;
    }
}