package net.heretical_camelid.transit_emv_checker.android_app.ui.html;

import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import net.heretical_camelid.transit_emv_checker.android_app.R;
import net.heretical_camelid.transit_emv_checker.android_app.databinding.FragmentHomeBinding;
import net.heretical_camelid.transit_emv_checker.android_app.databinding.FragmentHtmlBinding;

public class HtmlFragment extends Fragment {

    private FragmentHtmlBinding binding;

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
        HtmlViewModel htmlViewModel =
                new ViewModelProvider(this).get(HtmlViewModel.class);
        htmlViewModel.setText(sb.toString());




        htmlViewModel.getText().observe(
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
}