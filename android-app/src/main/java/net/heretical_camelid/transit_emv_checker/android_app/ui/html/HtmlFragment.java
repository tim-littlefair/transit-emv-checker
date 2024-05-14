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
import net.heretical_camelid.transit_emv_checker.android_app.databinding.FragmentHomeBinding;
import net.heretical_camelid.transit_emv_checker.android_app.databinding.FragmentHtmlBinding;

public class HtmlFragment extends Fragment {

    private FragmentHtmlBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HtmlViewModel htmlViewModel =
                new ViewModelProvider(this).get(HtmlViewModel.class);

        binding = FragmentHtmlBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final ObservableWebViewWrapper observableWebViewWrapper =
                new ObservableWebViewWrapper(binding.wvHtml)
        ;

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