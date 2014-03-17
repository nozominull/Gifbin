package com.nozomi.gifbin.activity;

import org.apache.http.Header;

import java.io.File;
import java.util.ArrayList;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.TextHttpResponseHandler;
import com.nozomi.gifbin.R;
import com.nozomi.gifbin.util.CommUtil;
import com.nozomi.gifbin.util.FileUtil;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.app.Activity;

public class MainActivity extends Activity {
	private File homeDirectory = null;
	private GifImageView imageView = null;
	private AsyncHttpClient asyncHttpClient = null;
	private ArrayList<File> fileArray = new ArrayList<File>();
	private int index = -1;// TODO 显示出来才修改
	private boolean isCancel = false;// TODO 只处理显示
	private boolean isLoading = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		homeDirectory = FileUtil.getHomeDirectory(this);
		asyncHttpClient = new AsyncHttpClient();

		for (File file : homeDirectory.listFiles()) {
			if (file.isFile()
					&& (file.getName().endsWith(".gif") || file.getName()
							.endsWith(".GIF"))) {
				fileArray.add(file);
			}
		}

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

	}

	private void loadDataFromServer() {
		if (isLoading) {
			return;
		}
		CommUtil.makeToast(MainActivity.this, "正在读取图片网址");
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

}