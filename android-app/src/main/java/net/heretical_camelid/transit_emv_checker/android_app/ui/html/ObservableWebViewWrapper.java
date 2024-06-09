package net.heretical_camelid.transit_emv_checker.android_app.ui.html;

import android.annotation.SuppressLint;
import android.util.Base64;
import android.webkit.WebView;

public class ObservableWebViewWrapper {
    private final WebView m_webView;
    @SuppressLint("SetJavaScriptEnabled")
    public ObservableWebViewWrapper(WebView webView) {
        m_webView = webView;
        // I have no reason to want Javascript enabled,
        // but running with it disabled triggers large
        // stack traces in logcat.
        // ref: https://stackoverflow.com/q/74198645
        m_webView.getSettings().setJavaScriptEnabled(true);
    }

    public void loadHtmlText(String htmlText) {
        String encodedHtml = Base64.encodeToString(
                htmlText.getBytes(),
                Base64.NO_PADDING
        );
        m_webView.loadData(encodedHtml, "text/html", "base64");
    }
}
