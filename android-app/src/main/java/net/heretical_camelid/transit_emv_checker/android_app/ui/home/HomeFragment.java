package net.heretical_camelid.transit_emv_checker.android_app.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import net.heretical_camelid.transit_emv_checker.android_app.MainActivity;
import net.heretical_camelid.transit_emv_checker.android_app.R;
import net.heretical_camelid.transit_emv_checker.android_app.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel;
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        TextView m_log = binding.logHome;
        Button m_button = binding.buttonHome;

        m_button.setText(R.string.start_emv_media_detection);

        homeViewModel.getLog().observe(getViewLifecycleOwner(), m_log::setText);
        homeViewModel.getLog().setValue("Starting up...");

        MainActivity mainActivity = (MainActivity) getActivity();
        assert mainActivity != null;
        mainActivity.registerHomeViewModel(homeViewModel);
        m_button.setOnClickListener(v -> {
            mainActivity.setInitialState();
            mainActivity.tryToDetectMedia();
            v.setEnabled(false);
            ((Button) v).setText(R.string.waiting_for_emv_media);
        });

        return root;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}