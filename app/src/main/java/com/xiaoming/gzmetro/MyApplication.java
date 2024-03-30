package com.xiaoming.gzmetro;

import android.annotation.SuppressLint;
import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import androidx.annotation.NonNull;

@SuppressLint("CheckResult")
@SuppressWarnings({"unused"})
public class MyApplication extends Application {
	private static final String TAG = MyApplication.class.getSimpleName();
	private static MyApplication instance;

	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;
	}

	public static MyApplication getInstance() {
		return instance;
	}
	
	private static final Handler mHandler = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(@NonNull Message msg) {

		}
	};

	private static void toast(String s) {
		Toast.makeText(instance,s,Toast.LENGTH_SHORT).show();
	}

}
