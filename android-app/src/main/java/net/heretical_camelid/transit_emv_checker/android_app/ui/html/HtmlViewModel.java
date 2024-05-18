package net.heretical_camelid.transit_emv_checker.android_app.ui.html;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HtmlViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public HtmlViewModel() {
        mText = new MutableLiveData<>();
        setText("<html><body><h1>HtmlViewModel</h1><p>Empty</p></body></html>");
    }

    public void setText(String s) {
        mText.postValue(s);
    }

    public LiveData<String> getText() {
        return mText;
    }
}