package net.heretical_camelid.transit_emv_checker.android_app.ui.html;

import android.util.Base64;
import android.webkit.WebView;

public class ObservableWebViewWrapper {
    private final WebView m_webView;
    public ObservableWebViewWrapper(WebView webView) {
        m_webView = webView;
    }

    public void loadHtmlText(String htmlText) {
        String encodedHtml = Base64.encodeToString(
                htmlText.getBytes(),
                Base64.NO_PADDING
        );
        m_webView.loadData(encodedHtml, "text/html", "base64");
    }
}
