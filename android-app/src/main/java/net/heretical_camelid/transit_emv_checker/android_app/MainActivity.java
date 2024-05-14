package net.heretical_camelid.transit_emv_checker.android_app;

import android.os.Bundle;
import android.util.Base64;
import android.webkit.WebView;
import androidx.fragment.app.FragmentContainerView;
import androidx.navigation.NavDestination;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import net.heretical_camelid.transit_emv_checker.android_app.databinding.ActivityMainBinding;
import net.heretical_camelid.transit_emv_checker.android_app.ui.html.HtmlViewModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements NavController.OnDestinationChangedListener {

    private ActivityMainBinding binding;

    static private HashMap<String, HtmlViewModel> s_viewModelRegistry = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home,
                R.id.navigation_transit,
                R.id.navigation_emv_details
                // R.id.navigation_about
        ).build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        BottomNavigationView navView = findViewById(R.id.nav_view);
        FragmentContainerView fcv = new FragmentContainerView(this);

        // navController.addOnDestinationChangedListener(this);
    }

    static public void registerHtmlViewModel(String whichModel, HtmlViewModel theModel) {
        s_viewModelRegistry.put(whichModel, theModel);
    }

    @Override
    public void onDestinationChanged(@NotNull NavController navController, @NotNull NavDestination navDestination, @Nullable Bundle bundle) {
        if(navDestination.getId()==R.id.navigation_transit) {
            WebView wv = findViewById(R.id.wv_html);
            String htmlText = "<html><body><h1>Transit</h1><p>Text</p></body></html>";
            String encodedHtml = Base64.encodeToString(
                    htmlText.getBytes(),
                    Base64.NO_PADDING
            );
            wv.loadData(encodedHtml, "text/html", "base64");
        }
    }
}