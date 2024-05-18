package net.heretical_camelid.transit_emv_checker.android_app.ui.html;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HtmlViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public HtmlViewModel() {
        mText = null;
    }

    public void setText(String s) {
        mText.postValue(s);
    }

    public LiveData<String> getText() {
        return mText;
    }

    public void setData(MutableLiveData<String> stringMutableLiveData) {
        mText = stringMutableLiveData;
    }
}