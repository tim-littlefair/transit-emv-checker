package net.heretical_camelid.transit_emv_checker.android_app.ui.html;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import net.heretical_camelid.transit_emv_checker.android_app.MainActivity;
import net.heretical_camelid.transit_emv_checker.android_app.R;
import net.heretical_camelid.transit_emv_checker.android_app.databinding.FragmentHtmlBinding;

public class HtmlFragment extends Fragment {

    private FragmentHtmlBinding binding;
    private HtmlViewModel m_htmlViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHtmlBinding.inflate(inflater, container, false);
        final ObservableWebViewWrapper observableWebViewWrapper =
                new ObservableWebViewWrapper(binding.wvHtml)
        ;

        View root = binding.getRoot();
        m_htmlViewModel = new ViewModelProvider(this).get(HtmlViewModel.class);

        m_htmlViewModel.getText().observe(
                getViewLifecycleOwner(),
                observableWebViewWrapper::loadHtmlText
        );

        return root;
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
        activity.registerHtmlViewModel(currentDestination.getId(),m_htmlViewModel);
    }
}