package net.heretical_camelid.transit_emv_checker.android_app.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import net.heretical_camelid.transit_emv_checker.android_app.MainActivity;
import net.heretical_camelid.transit_emv_checker.android_app.R;
import net.heretical_camelid.transit_emv_checker.android_app.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private Button m_button;
    private HomeViewModel m_homeViewModel;
    private TextView m_log;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        m_homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        m_log = binding.logHome;
        m_button = binding.buttonHome;

        m_homeViewModel.getLog().observe(getViewLifecycleOwner(), m_log::setText);
        m_homeViewModel.getLog().setValue("Starting up...");

        MainActivity mainActivity = getMainActivity();
        mainActivity.registerHomeViewModel(m_homeViewModel);
        mainActivity.registerHomeFragment(this);
        getMainActivity().setInitialState();
        resetButtonToPromptForDetection();
        return root;
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        m_homeViewModel.getLog().setValue("Clearing details of previous card ...");
        m_homeViewModel.getLog().setValue("Restarting ...");
        getMainActivity().setInitialState();
        resetButtonToPromptForDetection();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MainActivity mainActivity = getMainActivity();
        mainActivity.registerHomeFragment(null);
        binding = null;
    }

    @NonNull
    private MainActivity getMainActivity() {
        MainActivity mainActivity = (MainActivity) getActivity();
        assert mainActivity != null;
        return mainActivity;
    }


    public void resetButtonToPromptForDetection() {
        m_button.setText(R.string.start_emv_media_detection);
        m_button.setEnabled(true);
        MainActivity mainActivity = getMainActivity();
        m_button.setOnClickListener(v -> {
            mainActivity.setInitialState();
            mainActivity.tryToDetectMedia();
            v.setEnabled(false);
            ((Button) v).setText(R.string.waiting_for_emv_media);
        });
    }
}

