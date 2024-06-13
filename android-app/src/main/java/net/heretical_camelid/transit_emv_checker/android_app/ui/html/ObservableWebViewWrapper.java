package net.heretical_camelid.transit_emv_checker.android_app.ui.html;

import android.annotation.SuppressLint;
import android.util.Base64;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import net.heretical_camelid.transit_emv_checker.android_app.MainActivity;

public class ObservableWebViewWrapper extends WebViewClient {
    private final WebView m_webView;
    @SuppressLint("SetJavaScriptEnabled")
    public ObservableWebViewWrapper(WebView webView) {
        m_webView = webView;
        // I have no reason to want Javascript enabled,
        // but running with it disabled triggers large
        // stack traces in logcat.
        // ref: https://stackoverflow.com/q/74198645
        m_webView.getSettings().setJavaScriptEnabled(true);
        m_webView.setWebViewClient(this);
    }

    public void loadHtmlText(String htmlText) {
        String encodedHtml = Base64.encodeToString(
                htmlText.getBytes(),
                Base64.NO_PADDING
        );
        m_webView.loadData(encodedHtml, "text/html", "base64");
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
}
