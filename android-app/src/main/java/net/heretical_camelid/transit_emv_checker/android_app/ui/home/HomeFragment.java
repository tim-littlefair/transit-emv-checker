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
import net.heretical_camelid.transit_emv_checker.android_app.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private TextView m_status;
    private TextView m_log;
    private Button m_button;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel;
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        m_log = binding.logHome;
        m_button = binding.buttonHome;

        m_button.setText("Start EMV Media Detection");

        homeViewModel.getLog().observe(getViewLifecycleOwner(), m_log::setText);
        homeViewModel.getLog().setValue("Starting up...");

        MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.registerHomeViewModel(homeViewModel);
        m_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.setInitialState();
                mainActivity.tryToDetectMedia();
                v.setEnabled(false);
                ((Button) v).setText("Waiting for EMV media");
            }
        });

        return root;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}