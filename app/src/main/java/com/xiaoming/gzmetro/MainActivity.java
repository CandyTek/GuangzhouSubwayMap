package com.xiaoming.gzmetro;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// import com.bumptech.glide.Glide;
// import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.xiaoming.gzmetro.databinding.ActivityMainBinding;

/** {@link R.layout#activity_main} */
public class MainActivity extends Activity {
	private static final String TAG = MainActivity.class.getSimpleName();

	private ActivityMainBinding binding;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityMainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());
		transparentStatusBar(this);
		transparentNavBar(this);
		// Glide.with(binding.photoview)
		// Glide.with(this)
		// 		.load(R.drawable.pic)
		// 		.skipMemoryCache(true)
		// 		.diskCacheStrategy(DiskCacheStrategy.NONE)
		// 		.override(5396,5394)
		// 		.into(binding.photoview);

		// binding.photoview.setImageDrawable(getResources().getDrawable(R.drawable.pic));
		// binding.photoview.enable();
		// binding.photoview.setMaxScale(8);
	}

	@Override
	public void onBackPressed() {
		// super.onBackPressed();
		finishAffinity();
		System.exit(0);
	}

	public static void transparentStatusBar(@NonNull final Activity activity) {
		transparentStatusBar(activity.getWindow());
	}

	public static void transparentStatusBar(@NonNull final Window window) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
			window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			int option = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
			int vis = window.getDecorView().getSystemUiVisibility();
			window.getDecorView().setSystemUiVisibility(option | vis);
			window.setStatusBarColor(Color.TRANSPARENT);
		} else {
			window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
		}
	}
	public static void transparentNavBar(@NonNull final Activity activity) {
		transparentNavBar(activity.getWindow());
	}

	public static void transparentNavBar(@NonNull final Window window) {
		if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) return;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			window.setNavigationBarContrastEnforced(false);
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			window.setNavigationBarColor(Color.TRANSPARENT);
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			if ((window.getAttributes().flags & WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION) == 0) {
				window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
			}
		}
		View decorView = window.getDecorView();
		int vis = decorView.getSystemUiVisibility();
		int option = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
		decorView.setSystemUiVisibility(vis | option);
	}
}
