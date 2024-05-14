package net.heretical_camelid.transit_emv_checker.android_app.ui.html;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HtmlViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public HtmlViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("<html><body><h1>HTML</h1><p>Text</p></body></html>");
    }

    public LiveData<String> getText() {
        return mText;
    }
}