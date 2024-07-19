package net.heretical_camelid.transit_emv_checker.android_app.ui.html;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import net.heretical_camelid.transit_emv_checker.android_app.MainActivity;
import net.heretical_camelid.transit_emv_checker.android_app.R;
import net.heretical_camelid.transit_emv_checker.android_app.databinding.FragmentHtmlBinding;

public class HtmlFragment extends Fragment {
    private FragmentHtmlBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // There is an exception thrown and recovered from
        // within FragmentHtmlBinding.inflate which generates a long
        // stack trace at logging level INFO.
        // I haven't yet found a way to suppress the logging of this
        // exception.
        // Could probably do it by switching the log4j implementation
        // away from sl4j-simple, and doing a pair of run-time set-level's
        // on the  implementation bracketing this call, but not going to
        // do that right now.
        binding = FragmentHtmlBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        MainActivity activity = (MainActivity) getActivity();
        assert activity != null;
        NavController navController = Navigation.findNavController(
                activity,
                R.id.nav_host_fragment_activity_main
        );
        NavDestination currentDestination = navController.getCurrentDestination();

        assert currentDestination != null;

        ObservableWebViewWrapper m_observableWebViewWrapper = new ObservableWebViewWrapper(binding.wvHtml, activity);
        HtmlViewModel m_htmlViewModel = new ViewModelProvider(this).get(HtmlViewModel.class);
        activity.registerHtmlViewModel(currentDestination.getId(), m_htmlViewModel);
        m_htmlViewModel.getText().observe(
            getViewLifecycleOwner(),
            m_observableWebViewWrapper::loadHtmlText
        );
    }
}