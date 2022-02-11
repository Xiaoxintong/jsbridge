package com.github.lzyzsd.jsbridge;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;

import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import java.net.URLDecoder;

/**
 * 如果要自定义WebViewClient必须要集成此类
 * Created by bruce on 10/28/15.
 */
public class XxtBridgeWebViewClient extends WebViewClient {
    private XxtBridgeWebView webView;
    private Context bwvcContext;

    public XxtBridgeWebViewClient(XxtBridgeWebView webView, Context context) {
        this.webView = webView;
        this.bwvcContext = context;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        try {
            url = URLDecoder.decode(url, "UTF-8");

            if (url.startsWith(BridgeUtil.YY_RETURN_DATA)) { // 如果是返回数据
                webView.handlerReturnData(url);
                return true;
            } else if (url.startsWith(BridgeUtil.YY_OVERRIDE_SCHEMA)) { //
                webView.flushMessageQueue();
                return true;
            } else if (url.startsWith("tel:")) {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                bwvcContext.startActivity(intent);
                return true;
            } else if (url.startsWith("sms:")||url.startsWith("smsto:")||url.startsWith("mms:")
                    ||url.startsWith("mmsto:")||url.startsWith("mailto:")) {
                Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                bwvcContext.startActivity(intent);
                return true;
            } else if(url.startsWith("intent:")){
                Intent intent = Intent.parseUri(url, 0);
                bwvcContext.startActivity(intent);
                return true;
            } else if (url.startsWith("xxt:")||url.startsWith("zxq:")||url.startsWith("jxt:")
                    ||url.startsWith("zxjy:")||url.startsWith("zxkc:")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                bwvcContext.startActivity(intent);
            } else {
                return this.onCustomShouldOverrideUrlLoading(url)?true:super.shouldOverrideUrlLoading(view, url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this.onCustomShouldOverrideUrlLoading(url)?true:super.shouldOverrideUrlLoading(view, url);
    }

    // 增加shouldOverrideUrlLoading在api》=24时
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        try {
            String url = request.getUrl().toString();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                url = URLDecoder.decode(url, "UTF-8");

                if (url.startsWith(BridgeUtil.YY_RETURN_DATA)) { // 如果是返回数据
                    webView.handlerReturnData(url);
                    return true;
                } else if (url.startsWith(BridgeUtil.YY_OVERRIDE_SCHEMA)) { //
                    webView.flushMessageQueue();
                    return true;
                } else if (url.startsWith("tel:")) {
                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    bwvcContext.startActivity(intent);
                    return true;
                } else if (url.startsWith("sms:")||url.startsWith("smsto:")||url.startsWith("mms:")
                        ||url.startsWith("mmsto:")||url.startsWith("mailto:")) {
                    Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    bwvcContext.startActivity(intent);
                    return true;
                } else if(url.startsWith("intent:")){
                    Intent intent = Intent.parseUri(url, 0);
                    bwvcContext.startActivity(intent);
                    return true;
                } else if (url.startsWith("xxt:")||url.startsWith("zxq:")||url.startsWith("jxt:")
                        ||url.startsWith("zxjy:")||url.startsWith("zxkc:")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    bwvcContext.startActivity(intent);
                } else {
                    return this.onCustomShouldOverrideUrlLoading(url)?true:super.shouldOverrideUrlLoading(view, request);
                }
            }else {
                return this.onCustomShouldOverrideUrlLoading(url)?true:super.shouldOverrideUrlLoading(view, request);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return super.shouldOverrideUrlLoading(view, request);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);

        if (XxtBridgeWebView.toLoadJs != null) {
            XxtBridgeUtil.webViewLoadLocalJs(view, XxtBridgeWebView.toLoadJs);
        }

        //
        if (webView.getStartupMessage() != null) {
            for (Message m : webView.getStartupMessage()) {
                webView.dispatchMessage(m);
            }
            webView.setStartupMessage(null);
        }

        //
        onCustomPageFinishd(view,url);

    }

    protected boolean onCustomShouldOverrideUrlLoading(String url) {
        return false;
    }


    protected void onCustomPageFinishd(WebView view, String url){

    }




}