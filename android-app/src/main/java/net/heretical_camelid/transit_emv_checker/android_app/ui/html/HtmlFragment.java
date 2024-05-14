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
import com.google.android.material.bottomnavigation.BottomNavigationView;
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
        View parent = container.getChildAt(container.getChildCount()-1);

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<p>root.id=" + root.getId() + " " + root.getTag() + "</p>");
        sb.append("<p>parent.id=" + parent.getId() + " " + parent.getTag() + "</p>");
        sb.append("</body></html>");
        m_htmlViewModel = new ViewModelProvider(this).get(HtmlViewModel.class);
        m_htmlViewModel.setText(sb.toString());

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
        NavController navController = Navigation.findNavController(
                (MainActivity) getActivity(),
                R.id.nav_host_fragment_activity_main
        );
        NavDestination currentDestination = navController.getCurrentDestination();
        assert currentDestination != null;

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<p>destination.id: " + currentDestination.getId() + "</p>");
        sb.append("<p>destination.label: " + currentDestination.getLabel() + "</p>");
        sb.append("</body></html>");
        m_htmlViewModel.setText(sb.toString());
    }
}