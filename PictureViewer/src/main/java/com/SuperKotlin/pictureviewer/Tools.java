package com.SuperKotlin.pictureviewer;

import android.content.Context;

import java.util.ArrayList;

public class Tools {

	public static void openSubwayMap(Context context){
		ArrayList<String> list = new ArrayList<>();
		list.add("");
		PictureConfig config = new PictureConfig.Builder()
				.setListData(list)//图片数据List<String> list
				.setPosition(0)//图片下标（从第position张图片开始浏览）
				.setDownloadPath("pictureviewer")//图片下载文件夹地址
				.setIsShowNumber(true)//是否显示数字下标
				.needDownload(true)//是否支持图片下载
				.build();
		ImagePagerActivity.startActivity(context,config);
	}
}
