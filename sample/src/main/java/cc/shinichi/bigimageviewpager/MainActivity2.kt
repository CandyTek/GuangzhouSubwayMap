package cc.shinichi.bigimageviewpager

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import cc.shinichi.library.ImagePreview
import cc.shinichi.library.ImagePreview.Companion.instance
import cc.shinichi.library.bean.ImageInfo
import cc.shinichi.library.bean.Type
import cc.shinichi.library.view.listener.OnPageFinishListener
import com.bumptech.glide.Glide
import kotlin.system.exitProcess

class MainActivity2 : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		chooseImage()
	}

	private fun chooseImage() {
		val mediaList: MutableList<ImageInfo> = ArrayList()
		val imageInfo = ImageInfo()
		imageInfo.type = Type.IMAGE
		imageInfo.originUrl = FILE_PATH
		mediaList.add(imageInfo)
		
		instance
			.setShowDownButton(false)
			.with(this@MainActivity2)
			.setMediaInfoList(mediaList)
			.setEnableDragClose(false)
			.setEnableClickClose(false)
			.setLoadStrategy(ImagePreview.LoadStrategy.AlwaysOrigin)
			.setOnPageFinishListener(object : OnPageFinishListener() {
				override fun onFinish(activity: Activity) {
					finishAffinity()
					Thread {
						Glide.get(this@MainActivity2.applicationContext).clearDiskCache()
						exitProcess(0)
					}.start()
				}
			})
			.start()
	}

	companion object {
		const val FILE_PATH: String = "file:///android_asset/subway.png"
	}
}
