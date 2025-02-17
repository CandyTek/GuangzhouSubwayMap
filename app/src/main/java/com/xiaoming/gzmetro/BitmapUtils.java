package com.xiaoming.gzmetro;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.AnyRes;

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



	public static Bitmap decodeSampledBitmapFromResource(Context context,@AnyRes int filePath,int reqWidth,int reqHeight) {
		// 首先获取图片的原始尺寸
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;  // 只获取图片的尺寸，不加载图片到内存中
		BitmapFactory.decodeResource(context.getResources(), filePath,options);

		// 计算 inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// 通过设置 inSampleSize 来加载图片，避免一次性加载过大的图片
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeResource(context.getResources(), filePath, options);
	}

	public static Bitmap decodeSampledBitmapFromFile(String filePath,int reqWidth,int reqHeight) {
		// 首先获取图片的原始尺寸
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;  // 只获取图片的尺寸，不加载图片到内存中
		BitmapFactory.decodeFile(filePath, options);

		// 计算 inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// 通过设置 inSampleSize 来加载图片，避免一次性加载过大的图片
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeFile(filePath, options);
	}

	private static int calculateInSampleSize(BitmapFactory.Options options,int reqWidth,int reqHeight) {
		// 原始的图片宽和高
		int height = options.outHeight;
		int width = options.outWidth;

		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// 计算缩小的比例
			while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
				inSampleSize *= 2;
			}
		}
		return inSampleSize;
	}

}
