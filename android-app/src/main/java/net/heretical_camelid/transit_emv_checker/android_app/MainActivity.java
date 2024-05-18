package net.heretical_camelid.transit_emv_checker.android_app;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.lifecycle.MutableLiveData;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import net.heretical_camelid.transit_emv_checker.android_app.databinding.ActivityMainBinding;
import net.heretical_camelid.transit_emv_checker.android_app.ui.home.HomeViewModel;
import net.heretical_camelid.transit_emv_checker.android_app.ui.html.HtmlViewModel;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private MutableLiveData<String> m_homePageStatus;
    private MutableLiveData<String> m_homePageLog;
    private MutableLiveData<String> m_transitPageHTML;
    private MutableLiveData<String> m_emvPageHTML;
    private MutableLiveData<String> m_aboutPageHTML;
    private final HashMap<Integer, HtmlViewModel> m_viewModelRegistry = new HashMap<>();
    private BottomNavigationView m_navView;
    private NavController m_navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        setInitialState();
    }

    private void setPageHtmlText(int pageNavigationId, String htmlText) {
        HtmlViewModel hvm = m_viewModelRegistry.get(pageNavigationId);
        assert hvm != null;
        hvm.setText(htmlText);
    }

    private void populateAboutPage() {
        setPageHtmlText(R.id.navigation_about,"<html><body><p>TEC by TJL</p></body></html>");
    }

    private void setInitialState() {
        setItemState(R.id.navigation_transit,false);
        setItemState(R.id.navigation_emv_details,false);
    }

    public void registerHomeViewModel(HomeViewModel theModel) {
        m_homePageLog = theModel.getLog();
    }
    public void registerHtmlViewModel(int whichModel, HtmlViewModel theModel) {
        m_viewModelRegistry.put(whichModel, theModel);

        if(whichModel==R.id.navigation_about) {
            populateAboutPage();
        }
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
        m_homePageLog.setValue(
            m_homePageLog.getValue() + "\n" + s
        );
    }
}