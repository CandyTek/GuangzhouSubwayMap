package com.xiaoming.gzmetro;

import android.app.Activity;
import android.os.Bundle;

import com.SuperKotlin.pictureviewer.Tools;

/** 第二种库，图片查看器 */
public class MainActivity2 extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Tools.openSubwayMap(this);
		finish();
	}
}
