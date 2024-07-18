package net.heretical_camelid.transit_emv_checker.android_app.ui.html;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;

import net.heretical_camelid.transit_emv_checker.android_app.MainActivity;
import net.heretical_camelid.transit_emv_checker.library.APDUObserver;

public class ObservableWebViewWrapper extends WebViewClient implements Handler.Callback {
    private static final String TAG = ObservableWebViewWrapper.class.getName();
    private final WebView m_webView;
    private final MainActivity m_mainActivity;

    @SuppressLint("SetJavaScriptEnabled")
    public ObservableWebViewWrapper(WebView webView, MainActivity activity) {
        m_mainActivity = activity;
        m_webView = webView;
        // I have no reason to want Javascript enabled,
        // but running with it disabled triggers large
        // stack traces in logcat.
        // ref: https://stackoverflow.com/q/74198645
        m_webView.getSettings().setJavaScriptEnabled(true);
        m_webView.addJavascriptInterface(this,"observer");
        m_webView.setWebViewClient(this);
    }

    public void loadHtmlText(String htmlText) {
        String encodedHtml = Base64.encodeToString(
                htmlText.getBytes(),
                Base64.NO_PADDING
        );
        m_webView.loadData(encodedHtml, "text/html", "base64");
    }

    @JavascriptInterface
    public void buttonClicked(String buttonCommand) {
        if(buttonCommand==null) {
            Log.w(TAG, "buttonClicked - null command");
            // do nothing
        } else if(buttonCommand.equals("save_xml")) {
            m_mainActivity.writeXmlCaptureFile();
        } else {
            Log.w(TAG, "buttonClicked - unexpected command " + buttonCommand);
        }
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView wv, WebResourceRequest wrr) {
        String urlString = wrr.getUrl().toString();
        if(urlString.contains("return_to_startup_alert")) {
            MainActivity.showStartupAlert();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        return false;
    }
}
