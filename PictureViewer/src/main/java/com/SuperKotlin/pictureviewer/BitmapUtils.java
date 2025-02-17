package com.SuperKotlin.pictureviewer;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;

public class BitmapUtils {

	/**
	 * 获取设备允许的最大正方形Bitmap尺寸，单位是像素
	 * 计算方法基于设备最大内存堆栈和ARGB_8888的每像素4字节
	 *
	 * @param context 上下文
	 * @return 最大正方形Bitmap的边长（像素）
	 */
	public static int getMaxBitmapSize(Context context) {
		// 获取ActivityManager，获取最大可用内存
		ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		if (activityManager == null) {
			throw new IllegalStateException("ActivityManager is null");
		}

		// 获取当前设备的最大可用内存（单位字节）
		long maxMemory = activityManager.getMemoryClass() * 1024 * 1024;

		// ARGB_8888格式每个像素占4字节
		int bytesPerPixel = 4;

		// 计算允许的最大像素数量
		long maxPixels = maxMemory / bytesPerPixel;

		// 计算最大正方形的边长，最大正方形面积应为 maxPixels
		int maxSideLength = (int) Math.sqrt(maxPixels);

		// 返回最大正方形的边长
		return maxSideLength;
	}
}
