package com.nozomi.gifbin.util;

import java.io.File;

import android.content.Context;
import android.os.Environment;

public class FileUtil {
	public static File getHomeDirectory(Context context) {
		File homeDirectory = new File(Environment.getExternalStorageDirectory()
				+ "/gifbin");
		if (Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)) {
			if (homeDirectory.exists()) {
				if (homeDirectory.isDirectory()) {
					return homeDirectory;
				}
			} else {
				if (homeDirectory.mkdirs()) {
					return homeDirectory;
				}
			}
		}
		return context.getFilesDir();
	}

}
