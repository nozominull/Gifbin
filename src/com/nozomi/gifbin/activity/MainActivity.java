package com.nozomi.gifbin.activity;

import org.apache.http.Header;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.TextHttpResponseHandler;
import com.nozomi.gifbin.R;
import com.nozomi.gifbin.util.CommUtil;
import com.nozomi.gifbin.util.FileUtil;
import com.umeng.analytics.MobclickAgent;
import com.umeng.socialize.bean.SHARE_MEDIA;
import com.umeng.socialize.bean.SocializeEntity;
import com.umeng.socialize.controller.RequestType;
import com.umeng.socialize.controller.UMServiceFactory;
import com.umeng.socialize.controller.UMSocialService;
import com.umeng.socialize.controller.UMSsoHandler;
import com.umeng.socialize.controller.listener.SocializeListeners.SnsPostListener;
import com.umeng.socialize.media.UMImage;
import com.umeng.socialize.sso.SinaSsoHandler;
import com.umeng.update.UmengUpdateAgent;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.app.Activity;
import android.content.Intent;

public class MainActivity extends Activity {
	private File homeDirectory = null;
	private GifImageView imageView = null;
	private AsyncHttpClient asyncHttpClient = null;
	private ArrayList<File> fileArray = new ArrayList<File>();
	private int index = -1;// TODO 显示出来才修改
	private boolean isCancel = false;// TODO 只处理显示
	private boolean isLoading = false;
	private UMSocialService mController = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		UmengUpdateAgent.setUpdateOnlyWifi(false);
		UmengUpdateAgent.update(this);

		mController = UMServiceFactory.getUMSocialService("com.umeng.share",
				RequestType.SOCIAL);
		mController.getConfig().setSsoHandler(new SinaSsoHandler());

		homeDirectory = FileUtil.getHomeDirectory(this);
		asyncHttpClient = new AsyncHttpClient();
		for (File file : homeDirectory.listFiles()) {
			if (file.isFile()
					&& (file.getName().endsWith(".gif") || file.getName()
							.endsWith(".GIF"))) {
				fileArray.add(file);
			}
		}
		Collections.sort(fileArray);

		initView();
		if (index == -1) {
			loadDataFromServer();
		}
	}

	private void initView() {
		imageView = (GifImageView) findViewById(R.id.image);
		for (int i = fileArray.size() - 1; i >= 0; i--) {
			try {
				GifDrawable gifFromFile = new GifDrawable(fileArray.get(i));
				imageView.setImageDrawable(gifFromFile);
				index = i;
				break;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		imageView.setKeepScreenOn(true);
		ImageButton nextView = (ImageButton) findViewById(R.id.next);
		nextView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				for (int i = index + 1; i < fileArray.size(); i++) {
					try {
						GifDrawable gifFromFile = new GifDrawable(fileArray
								.get(i));
						imageView.setImageDrawable(gifFromFile);
						index = i;
						return;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				isCancel = false;
				loadDataFromServer();
			}
		});
		ImageButton previousView = (ImageButton) findViewById(R.id.previous);
		previousView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				isCancel = true;
				for (int i = index - 1; i >= 0; i--) {
					try {
						GifDrawable gifFromFile = new GifDrawable(fileArray
								.get(i));
						imageView.setImageDrawable(gifFromFile);
						index = i;
						return;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				CommUtil.makeToast(MainActivity.this, "已经是第一张");
			}
		});

		ImageButton shareView = (ImageButton) findViewById(R.id.share);
		shareView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (index >= 0) {
					mController.setShareContent("#GIF看片#");
					File file = fileArray.get(index);
					if (file.exists()) {
						UMImage uMImage = new UMImage(MainActivity.this, file);
						mController.setShareImage(uMImage);
						mController.postShare(MainActivity.this,
								SHARE_MEDIA.SINA, new SnsPostListener() {
									@Override
									public void onStart() {

									}

									@Override
									public void onComplete(
											SHARE_MEDIA platform, int eCode,
											SocializeEntity entity) {

									}
								});
					}
				}
			}

		});
	}

	private void loadDataFromServer() {
		if (isLoading) {
			return;
		}
		CommUtil.makeToast(MainActivity.this, "正在读取图片");
		isLoading = true;
		asyncHttpClient.get("http://m.gifbin.com/random",
				new TextHttpResponseHandler() {
					@Override
					public void onFailure(int statusCode, Header[] headers,
							String responseString, Throwable throwable) {
						CommUtil.makeToast(MainActivity.this, "网络连接失败");
						isLoading = false;
					}

					@Override
					public void onSuccess(int statusCode, Header[] headers,
							String responseString) {
						int beginIndex = responseString
								.indexOf("<div id=\"main\"");
						beginIndex = responseString.indexOf("<img src=\"",
								beginIndex) + "<img src=\"".length();
						int endIndex = responseString.indexOf("\"", beginIndex);
						String url = responseString.substring(beginIndex,
								endIndex);
						loadFileFromServer(url);
					}

				});
	}

	private void loadFileFromServer(String url) {
		File gifFile = new File(homeDirectory, System.currentTimeMillis()
				+ ".gif");
		asyncHttpClient.get(url, new FileAsyncHttpResponseHandler(gifFile) {
			@Override
			public void onSuccess(int statusCode, Header[] headers, File file) {
				try {
					if (file.exists()) {
						fileArray.add(file);
						if (!isCancel) {
							GifDrawable gifFromFile = new GifDrawable(file);
							imageView.setImageDrawable(gifFromFile);
							index = fileArray.size() - 1;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					CommUtil.makeToast(MainActivity.this, "打开失败");
				}
			}

			@Override
			public void onFailure(int statusCode, Header[] headers,
					Throwable throwable, File file) {
				CommUtil.makeToast(MainActivity.this, "网络连接失败");
			}

			@Override
			public void onFinish() {
				isLoading = false;
			}

			@Override
			public void onProgress(int bytesWritten, int totalSize) {
				if (!isCancel) {
					CommUtil.makeToast(
							MainActivity.this,
							String.format("下载中 %3d%%", 100 * bytesWritten
									/ totalSize));
				}
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		/** 使用SSO授权必须添加如下代码 */
		UMSsoHandler ssoHandler = mController.getConfig().getSsoHandler(
				requestCode);
		if (ssoHandler != null) {
			ssoHandler.authorizeCallBack(requestCode, resultCode, data);
		}
	}

}