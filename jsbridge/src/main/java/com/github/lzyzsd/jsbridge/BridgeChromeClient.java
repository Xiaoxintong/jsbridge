package com.github.lzyzsd.jsbridge;

import android.webkit.WebChromeClient;
import android.webkit.WebView;

/**
 * Created by Luke on 2017/6/17.
 */

public class BridgeChromeClient extends WebChromeClient {

    private BridgeWebView webView;

    public BridgeChromeClient(BridgeWebView webView) {
        this.webView = webView;
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        super.onProgressChanged(view, newProgress);

//        webView.updateProgress(newProgress);
    }
}
