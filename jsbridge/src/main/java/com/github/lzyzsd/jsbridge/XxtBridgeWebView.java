package com.github.lzyzsd.jsbridge;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.widget.AbsoluteLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.lzyzsd.library.BuildConfig;
import com.github.lzyzsd.library.R;
import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebView;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressLint("SetJavaScriptEnabled")
public class XxtBridgeWebView extends WebView implements WebViewJavascriptBridge {

	private final String TAG = "BridgeWebView";

	public static final String toLoadJs = "WebViewJavascriptBridge.js";
	Map<String, CallBackFunction> responseCallbacks = new HashMap<String, CallBackFunction>();
	Map<String, BridgeHandler> messageHandlers = new HashMap<String, BridgeHandler>();
	BridgeHandler defaultHandler = new DefaultHandler();

	private List<Message> startupMessage = new ArrayList<Message>();

	private ProgressBar progressbar;  //进度条

	private int progressHeight = 3;  //进度条的高度，默认3px

	public List<Message> getStartupMessage() {
		return startupMessage;
	}

	private boolean progressShowFlag = false;

	private boolean isShowMessage;

	private Context context;

	public void setStartupMessage(List<Message> startupMessage) {
		this.startupMessage = startupMessage;
	}

	private long uniqueId = 0;

	public XxtBridgeWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public XxtBridgeWebView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public XxtBridgeWebView(Context context) {
		super(context);
		init(context);
	}

	/**
	 *
	 * @param handler
	 *            default handler,handle messages send by js without assigned handler name,
	 *            if js message has handler name, it will be handled by named handlers registered by native
	 */
	public void setDefaultHandler(BridgeHandler handler) {
		this.defaultHandler = handler;
	}

	@SuppressLint("ObsoleteSdkInt")
	private void init(Context context) {
		this.context = context;
		this.setVerticalScrollBarEnabled(false);
		this.setHorizontalScrollBarEnabled(false);
		this.getSettings().setJavaScriptEnabled(true);
		this.getSettings().setSavePassword(false);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			WebView.setWebContentsDebuggingEnabled(true);
		}
		//创建进度条
		progressbar = new ProgressBar(context, null,
				android.R.attr.progressBarStyleHorizontal);
		//设置加载进度条的高度
		progressbar.setLayoutParams(new AbsoluteLayout.LayoutParams(LayoutParams.MATCH_PARENT, progressHeight, 0, 0));

		Drawable drawable = context.getResources().getDrawable(R.drawable.progress_bar_web);
		progressbar.setProgressDrawable(drawable);

		//添加进度到WebView
		addView(progressbar);

		progressbar.setVisibility(GONE);

		this.setWebViewClient(generateBridgeWebViewClient());
		this.setWebChromeClient(new XxtBridgeChromeClient(this));

		try {
			SharedPreferences pref = context.getSharedPreferences("application_shared_prefs", Context.MODE_PRIVATE);

			boolean isDebug = pref.getBoolean("commons_sp_key_can_debug",false) || BuildConfig.DEBUG;
			if (isDebug) {
				isShowMessage = pref.getBoolean("commons_sp_key_show_web_load_log_in_pandora", false);
			}
		} catch (Exception e) {

		}
	}

	protected XxtBridgeWebViewClient generateBridgeWebViewClient() {
		return new XxtBridgeWebViewClient(this, context);
	}

	/**
	 * 获取到CallBackFunction data执行调用并且从数据集移除
	 * @param url
	 */
	void handlerReturnData(String url) {
		String functionName = XxtBridgeUtil.getFunctionFromReturnUrl(url);
		CallBackFunction f = responseCallbacks.get(functionName);
		String data = XxtBridgeUtil.getDataFromReturnUrl(url);
		if (f != null) {
			f.onCallBack(data);
			responseCallbacks.remove(functionName);
			return;
		}
	}

	@Override
	public void send(String data) {
		send(data, null);
	}

	@Override
	public void send(String data, CallBackFunction responseCallback) {
		doSend(null, data, responseCallback);
	}

	/**
	 * 保存message到消息队列
	 * @param handlerName handlerName
	 * @param data data
	 * @param responseCallback CallBackFunction
	 */
	private void doSend(String handlerName, String data, CallBackFunction responseCallback) {
		Message m = new Message();
		if (!TextUtils.isEmpty(data)) {
			m.setData(data);
		}
		if (responseCallback != null) {
			String callbackStr = String.format(XxtBridgeUtil.CALLBACK_ID_FORMAT, ++uniqueId + (XxtBridgeUtil.UNDERLINE_STR + SystemClock.currentThreadTimeMillis()));
			responseCallbacks.put(callbackStr, responseCallback);
			m.setCallbackId(callbackStr);
		}
		if (!TextUtils.isEmpty(handlerName)) {
			m.setHandlerName(handlerName);
		}
		queueMessage(m);
	}

	/**
	 * list<message> != null 添加到消息集合否则分发消息
	 * @param m Message
	 */
	private void queueMessage(Message m) {
		if (startupMessage != null) {
			startupMessage.add(m);
		} else {
			dispatchMessage(m);
		}
	}

	/**
	 * 分发message 必须在主线程才分发成功
	 * @param m Message
	 */
	void dispatchMessage(Message m) {
		String messageJson = m.toJson();
		//escape special characters for json string  为json字符串转义特殊字符
		messageJson = messageJson.replaceAll("(\\\\)([^utrn])", "\\\\\\\\$1$2");
		messageJson = messageJson.replaceAll("(?<=[^\\\\])(\")", "\\\\\"");
		messageJson = messageJson.replaceAll("(?<=[^\\\\])(\')", "\\\\\'");
		messageJson = messageJson.replaceAll("%7B", URLEncoder.encode("%7B"));
		messageJson = messageJson.replaceAll("%7D", URLEncoder.encode("%7D"));
		messageJson = messageJson.replaceAll("%22", URLEncoder.encode("%22"));
		String javascriptCommand = String.format(XxtBridgeUtil.JS_HANDLE_MESSAGE_FROM_JAVA, messageJson);
		// 必须要找主线程才会将数据传递出去 --- 划重点
		if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
//			this.loadUrl(javascriptCommand);
			// loadUrl执行时，会自动urldecode,H5使用上会有一些问题，所以修改成了evaluateJavascript，
			// 支持4.4+, 而我们5.0+，可以这样做
			this.evaluateJavascript(javascriptCommand, new ValueCallback<String>() {
				@Override
				public void onReceiveValue(String s) {

				}
			});
		}
	}

	/**
	 * 刷新消息队列
	 */
	void flushMessageQueue() {
		if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
			loadUrl(XxtBridgeUtil.JS_FETCH_QUEUE_FROM_JAVA, new CallBackFunction() {

				@Override
				public void onCallBack(String data) {
					// deserializeMessage 反序列化消息
					List<Message> list = null;
					try {
						list = Message.toArrayList(data);
					} catch (Exception e) {
						e.printStackTrace();
						return;
					}
					if (list == null || list.size() == 0) {
						return;
					}
					for (int i = 0; i < list.size(); i++) {
						Message m = list.get(i);
						String responseId = m.getResponseId();
						// 是否是response  CallBackFunction
						if (!TextUtils.isEmpty(responseId)) {
							CallBackFunction function = responseCallbacks.get(responseId);
							String responseData = m.getResponseData();
							function.onCallBack(responseData);
							responseCallbacks.remove(responseId);
						} else {
							CallBackFunction responseFunction = null;
							// if had callbackId 如果有回调Id
							final String callbackId = m.getCallbackId();
							if (!TextUtils.isEmpty(callbackId)) {
								responseFunction = new CallBackFunction() {
									@Override
									public void onCallBack(String data) {
										Message responseMsg = new Message();
										responseMsg.setResponseId(callbackId);
										responseMsg.setResponseData(data);
										queueMessage(responseMsg);
									}
								};
							} else {
								responseFunction = new CallBackFunction() {
									@Override
									public void onCallBack(String data) {
										// do nothing
									}
								};
							}
							// BridgeHandler执行
							BridgeHandler handler;
							if (!TextUtils.isEmpty(m.getHandlerName())) {
								handler = messageHandlers.get(m.getHandlerName());
							} else {
								handler = defaultHandler;
							}
							Log.d("jsbridge", "接收到桥接："+m.getHandlerName()+"\n参数："+String.valueOf(m.getData()));
							if (handler != null){
								handler.handler(m.getData(), responseFunction);
								try {
									if (isShowMessage) {
										Toast toast = Toast.makeText(context, "接收到桥接："+m.getHandlerName()+"\n参数："+String.valueOf(m.getData()), Toast.LENGTH_SHORT);
										toast.setGravity(Gravity.CENTER, 0, 0);
										toast.show();
									}
								} catch (Exception e) {

								}
							}
						}
					}
				}
			});
		}
	}


	public void loadUrl(String jsUrl, CallBackFunction returnCallback) {
		this.loadUrl(jsUrl);
		// 添加至 Map<String, CallBackFunction>
		responseCallbacks.put(XxtBridgeUtil.parseFunctionName(jsUrl), returnCallback);
	}

	/**
	 * register handler,so that javascript can call it
	 * 注册处理程序,以便javascript调用它
	 * @param handlerName handlerName
	 * @param handler BridgeHandler
	 */
	public void registerHandler(String handlerName, BridgeHandler handler) {
		if (handler != null) {
			// 添加至 Map<String, BridgeHandler>
			messageHandlers.put(handlerName, handler);
		}
	}

	/**
	 * unregister handler
	 *
	 * @param handlerName
	 */
	public void unregisterHandler(String handlerName) {
		if (handlerName != null) {
			messageHandlers.remove(handlerName);
		}
	}

	public boolean hasHandler(String handlerName) {
		if (handlerName != null) {
			return messageHandlers.containsKey(handlerName);
		} else {
			return false;
		}
	}

	/**
	 * call javascript registered handler
	 * 调用javascript处理程序注册
	 * @param handlerName handlerName
	 * @param data data
	 * @param callBack CallBackFunction
	 */
	public void callHandler(String handlerName, String data, CallBackFunction callBack) {
		doSend(handlerName, data, callBack);
	}

	public void updateProgress(int newProgress) {
		if (progressShowFlag) {
			if (newProgress == 100) {
				progressbar.setVisibility(GONE);
			} else {
				if (progressbar.getVisibility() == GONE) {
					progressbar.setVisibility(VISIBLE);
				}
				progressbar.setProgress(newProgress);
			}
		}
	}
}
