package com.nozomi.gifbin.util;

import android.content.Context;
import android.widget.Toast;

public class CommUtil  {

	private static Toast toast = null;

	public static void makeToast(Context context, String text) {
		if (toast == null) {
			toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
		} else {
			toast.setText(text);
		}
		toast.show();
	}
}
