package net.heretical_camelid.transit_emv_checker.android_app.ui.html;

import android.content.Context;
import android.util.Base64;
import android.webkit.WebView;
import androidx.annotation.NonNull;

public class ObservableWebViewWrapper {
    private WebView m_webView;
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
