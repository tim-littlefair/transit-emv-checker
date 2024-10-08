package net.heretical_camelid.transit_emv_checker.android_app;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.MutableLiveData;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.test.core.app.ApplicationProvider;

import com.github.devnied.emvnfccard.iso7816emv.ITerminal;

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
import net.heretical_camelid.transit_emv_checker.library.TapConductor;
import net.heretical_camelid.transit_emv_checker.library.TapReplayAgent;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLInputFactory;

public class MainActivity extends AppCompatActivity {
    static final Logger LOGGER = LoggerFactory.getLogger(MainActivity.class);


    private NavController m_navController;

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
    private HomeFragment m_homeFragment;

    private String m_xmlContent = null;
    private String m_xmlFileBaseName = null;

    public static void showStartupAlert() {
        if(s_userHasAgreed ==false) {
            s_startupAlert.show();
        }
    }

    // Ugly, will have to do until we have a better way
    static public MainActivity s_activeInstance = null;

    @NonNull
    TapConductor replayCapturedTap(String mediaAssetName, ITerminal terminal) {
        TapConductor trc;
        String assetFilename = String.format("media_captures/%s.xml", mediaAssetName);
        try {
            Context context = ApplicationProvider.getApplicationContext();
            InputStream captureXmlStream = context.getAssets().open(assetFilename);
            trc = TapConductor.createReplayTapConductor(
                terminal,
                XMLInputFactory.newInstance(),
                captureXmlStream
            );
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        setDisplayMediaDetailsState(
            trc.transitCapabilities(),
            trc.summary(),
            trc.diagnosticXml(),
            "replaying-" + mediaAssetName
        );
        return trc;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        s_userHasAgreed = false;
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
        NavHostFragment navHostFragment = (NavHostFragment) (
            getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main)
        );
        assert navHostFragment != null;
        m_navController = navHostFragment.getNavController();
        NavigationUI.setupActionBarWithNavController(this, m_navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, m_navController);

        populateAboutPage();
        buildStartupAlert();
        showStartupAlert();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(s_userHasAgreed ==false) {
            s_startupAlert.show();
        }

        assert s_activeInstance == null;
        s_activeInstance = this;
    }

    @Override
    public void onPause() {
        assert s_activeInstance != null;
        s_activeInstance = null;
        super.onPause();
    }

    private void buildStartupAlert() {
        StartupAlertListener startupAlertListener = new StartupAlertListener(this);
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
            R.string.i_understand_and_agree, startupAlertListener
        );
        startupAlertBuilder.setNeutralButton("more information", startupAlertListener);
        startupAlertBuilder.setNegativeButton("decline and close", startupAlertListener);
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

        if(versionString.contains("-dirty")) {
            // In developer builds from unmodified git source,
            // the version name contains the suffix '-dirty'
            // replace this with a string indicating that this is a
            // developer build
            versionString = versionString.replace(
                "-dirty",
                "-devbuild"
            );
            //noinspection
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Add a timestamp if the API makes it easy to do so
                versionString += "-" +
                    ZonedDateTime.now(ZoneOffset.UTC)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'"));
            }
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

    public void setDisplayMediaDetailsState(String transitCapabilities, String emvApplicationDetails, String diagnosticXml, String xmlFileBaseName) {
        setPageHtmlText(R.id.navigation_transit,
            "<html><body><pre style='white-space: pre-wrap;'>" +
            transitCapabilities +
            "</pre></body></html>"
        );
        String xmlButtonHtml;
        if(diagnosticXml != null && xmlFileBaseName != null) {
            xmlButtonHtml = (
                "<div width='100%'><button name='save_xml' width='100%' enabled='true' onclick='observer.buttonClicked(this.name)'>" +
                "Save Diagnostic XML" +
                "</button></div>"
            );
            m_xmlContent = diagnosticXml;
            m_xmlFileBaseName = xmlFileBaseName;
        } else {
            xmlButtonHtml = (
                "<div width='100%'><button width='100%' enabled='false'>" +
                    "Diagnostic XML not available to Save" +
                    "</button></div>"
            );
            m_xmlContent = null;
            m_xmlFileBaseName = null;
        }
        String emvDetailsHtml = (
            "<html><body>" +
            "<div><pre style='white-space: pre-wrap;'>" +
            emvApplicationDetails +
            "</pre><div>" +
            xmlButtonHtml +
            "</body></html>"
        );
        setPageHtmlText(R.id.navigation_emv_details, emvDetailsHtml);
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
            Toast.makeText(
                this,
                "Request approved but null data",
                Toast.LENGTH_LONG
            ).show();
        } else if(requestCode== REQUEST_CODE_DOCUMENT_DIRECTORY_ACCESS) {
            LOGGER.warn("Unexpected directory access result received at MainActivity");
        } else if(requestCode == REQUEST_CODE_CREATE_DOCUMENT) {
            m_externalFileManager.storeFileContent(resultData.getData());
        } else {
            LOGGER.warn(
                "Unexpected result received at MainActivity for request code {}",
                requestCode
            );
        }
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

    public void writeXmlCaptureFile() {
        m_externalFileManager.saveFile(m_xmlFileBaseName, "text/xml", m_xmlContent.getBytes(StandardCharsets.UTF_8));
    }
}