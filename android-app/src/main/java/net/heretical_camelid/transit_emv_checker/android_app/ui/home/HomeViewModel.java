package net.heretical_camelid.transit_emv_checker.android_app.ui.home;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> m_log;

    public HomeViewModel() {
        m_log = new MutableLiveData<>();
    }

    public MutableLiveData<String> getLog() {
        return m_log;
    }
}