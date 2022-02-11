package com.github.lzyzsd.jsbridge;

import com.tencent.smtt.export.external.interfaces.GeolocationPermissionsCallback;
import com.tencent.smtt.export.external.interfaces.PermissionRequest;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebView;

/**
 * Created by Luke on 2017/6/17.
 */

public class XxtBridgeChromeClient extends WebChromeClient {

    private XxtBridgeWebView webView;

    public XxtBridgeChromeClient(XxtBridgeWebView webView) {
        this.webView = webView;
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        super.onProgressChanged(view, newProgress);

        webView.updateProgress(newProgress);
    }

    @Override
    public void onGeolocationPermissionsShowPrompt(String s, GeolocationPermissionsCallback geolocationPermissionsCallback) {
        geolocationPermissionsCallback.invoke(s, true, true);
    }

    @Override
    public void onPermissionRequest(PermissionRequest permissionRequest) {
        permissionRequest.grant(permissionRequest.getResources());
    }
}
