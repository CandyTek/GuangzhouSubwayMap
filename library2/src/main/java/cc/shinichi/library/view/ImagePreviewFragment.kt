package cc.shinichi.library.view

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import cc.shinichi.library.ImagePreview
import cc.shinichi.library.ImagePreview.LoadStrategy
import cc.shinichi.library.R
import cc.shinichi.library.bean.ImageInfo
import cc.shinichi.library.bean.Type
import cc.shinichi.library.glide.FileTarget
import cc.shinichi.library.glide.ImageLoader.getGlideCacheFile
import cc.shinichi.library.tool.common.HttpUtil.downloadFile
import cc.shinichi.library.tool.common.NetworkUtil.isWiFi
import cc.shinichi.library.tool.common.PhoneUtil
import cc.shinichi.library.tool.common.PhoneUtil.getPhoneHei
import cc.shinichi.library.tool.common.SLog
import cc.shinichi.library.tool.common.ToastUtil
import cc.shinichi.library.tool.common.UIUtil
import cc.shinichi.library.tool.file.FileUtil.Companion.getAvailableCacheDir
import cc.shinichi.library.tool.image.ImageUtil
import cc.shinichi.library.view.helper.DragCloseView
import cc.shinichi.library.view.listener.SimpleOnImageEventListener
import cc.shinichi.library.view.photoview.PhotoView
import cc.shinichi.library.view.subsampling.ImageSource
import cc.shinichi.library.view.subsampling.SubsamplingScaleImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import java.io.File
import java.util.Locale
import kotlin.compareTo
import kotlin.div
import kotlin.math.abs

/**
 * 文件名: ImagePreviewFragment.java
 * 作者: kirito
 * 描述: 单个页面
 * 创建时间: 2024/11/25
 */
class ImagePreviewFragment : Fragment() {

    /**
     * 当前是否加载过
     */
    private var mLoading = false

    private lateinit var imagePreviewActivity: ImagePreviewActivity
    private lateinit var imageInfo: ImageInfo
    private var position: Int = 0

    private lateinit var dragCloseView: DragCloseView
    private lateinit var imageStatic: SubsamplingScaleImageView
    private lateinit var imageAnim: PhotoView
    private lateinit var progressBar: ProgressBar


    // private lateinit var ivPlayButton: ImageView
    private lateinit var tvPlayTime: TextView
    private lateinit var seekBar: SeekBar

    private var progressHandler: Handler? = null
    private var progressRunnable: Runnable? = null
    private var isDragging = false
    private var onPausePlaying = false

    companion object {
        private const val TAG = "ImagePreviewFragment"
        fun newInstance(
            imagePreviewActivity: ImagePreviewActivity,
            position: Int,
            imageInfo: ImageInfo
        ): ImagePreviewFragment {
            val fragment = ImagePreviewFragment()
            fragment.imagePreviewActivity = imagePreviewActivity
            fragment.position = position
            fragment.imageInfo = imageInfo
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mLoading = false
        val view = inflater.inflate(R.layout.sh_item_photoview, container, false)
        initView(view)
        return view
    }

    private fun initData() {
        SLog.d(TAG, "initData: position = $position")
        val type = imageInfo.type
        if (type == Type.IMAGE) {
            initImageType()
        } else if (type == Type.VIDEO) {
            initVideoType()
        }
    }

    private fun initView(view: View) {
        progressBar = view.findViewById(R.id.progress_view)
        dragCloseView = view.findViewById(R.id.fingerDragHelper)
        imageStatic = view.findViewById(R.id.static_view)
        imageAnim = view.findViewById(R.id.anim_view)
        val phoneHei = getPhoneHei(imagePreviewActivity.applicationContext)
        // 手势拖拽事件
        if (ImagePreview.instance.isEnableDragClose) {
            dragCloseView.setOnAlphaChangeListener(object : DragCloseView.onAlphaChangedListener {
                override fun onTranslationYChanged(event: MotionEvent?, translationY: Float) {
                    if (translationY > 0) {
                        ImagePreview.instance.onPageDragListener?.onDrag(
                            imagePreviewActivity,
                            imagePreviewActivity.parentView,
                            event,
                            translationY
                        )
                    } else {
                        ImagePreview.instance.onPageDragListener?.onDragEnd(
                            imagePreviewActivity,
                            imagePreviewActivity.parentView
                        )
                    }
                    val yAbs = abs(translationY)
                    val percent = yAbs / phoneHei
                    val number = 1.0f - percent
                    imagePreviewActivity.setAlpha(number)
                    if (imageAnim.visibility == View.VISIBLE) {
                        imageAnim.scaleY = number
                        imageAnim.scaleX = number
                    }
                    if (imageStatic.visibility == View.VISIBLE) {
                        imageStatic.scaleY = number
                        imageStatic.scaleX = number
                    }
                }

                override fun onExit() {
                    imagePreviewActivity.setAlpha(0f)
                }
            })
        }
        // 点击事件(视频类型不支持点击关闭)
        imageStatic.setOnClickListener { v ->
            if (ImagePreview.instance.isEnableClickClose) {
                imagePreviewActivity.onBackPressed()
            }
            ImagePreview.instance.bigImageClickListener?.onClick(imagePreviewActivity, v, position)
        }
        imageAnim.setOnClickListener { v ->
            if (ImagePreview.instance.isEnableClickClose) {
                imagePreviewActivity.onBackPressed()
            }
            ImagePreview.instance.bigImageClickListener?.onClick(imagePreviewActivity, v, position)
        }
        // ivPlayButton.setOnClickListener {
            // 控制播放和暂停
        // }
        // 长按事件
        ImagePreview.instance.bigImageLongClickListener?.let {
            imageStatic.setOnLongClickListener { v ->
                ImagePreview.instance.bigImageLongClickListener?.onLongClick(
                    imagePreviewActivity,
                    v,
                    position
                )
                true
            }
            imageAnim.setOnLongClickListener { v ->
                ImagePreview.instance.bigImageLongClickListener?.onLongClick(
                    imagePreviewActivity,
                    v,
                    position
                )
                true
            }
        }
    }

    private fun initImageType() {
        // 图片类型，隐藏视频

        val originPathUrl = imageInfo.originUrl
        val thumbPathUrl = imageInfo.thumbnailUrl

        imageStatic.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
        imageStatic.setDoubleTapZoomStyle(SubsamplingScaleImageView.ZOOM_FOCUS_CENTER)
        imageStatic.setDoubleTapZoomDuration(ImagePreview.instance.zoomTransitionDuration)

        imageAnim.setZoomTransitionDuration(ImagePreview.instance.zoomTransitionDuration)
        imageAnim.minimumScale = ImagePreview.instance.minScale
        imageAnim.maximumScale = ImagePreview.instance.maxScale
        imageAnim.scaleType = ImageView.ScaleType.FIT_CENTER

        // 根据当前加载策略判断，需要加载的url是哪一个
        var finalLoadUrl: String = ""
        when (ImagePreview.instance.loadStrategy) {
            LoadStrategy.Default -> {
                finalLoadUrl = thumbPathUrl
            }

            LoadStrategy.AlwaysOrigin -> {
                finalLoadUrl = originPathUrl
            }

            LoadStrategy.AlwaysThumb -> {
                finalLoadUrl = thumbPathUrl
            }

            LoadStrategy.NetworkAuto -> {
                finalLoadUrl = if (isWiFi(imagePreviewActivity)) {
                    originPathUrl
                } else {
                    thumbPathUrl
                }
            }

            LoadStrategy.Auto -> {
                finalLoadUrl = if (isWiFi(imagePreviewActivity)) {
                    originPathUrl
                } else {
                    thumbPathUrl
                }
            }
        }
        finalLoadUrl = finalLoadUrl.trim()
        val url: String = finalLoadUrl

        // 显示加载圈圈
        progressBar.visibility = View.VISIBLE

        // 判断原图缓存是否存在，存在的话，直接显示原图缓存，优先保证清晰。
        val cacheFile = getGlideCacheFile(imagePreviewActivity, originPathUrl)
        if (cacheFile != null && cacheFile.exists()) {
            SLog.d(TAG, "initImageType: 原图缓存存在，直接显示 originPathUrl = $originPathUrl")
            loadSuccess(
                originPathUrl,
                cacheFile
            )
        } else {
            SLog.d(TAG, "initImageType: 原图缓存不存在，开始加载 url = $url")
            // 判断url是否是res资源 R.mipmap.xxx
            if (url.startsWith("res://")) {
                SLog.d(TAG, "initImageType: res资源")
                loadResImage(url, originPathUrl)
            } else {
                SLog.d(TAG, "initImageType: url资源")
                loadUrlImage(
                    url,
                    originPathUrl
                )
            }
        }
    }

    private fun initVideoType() {
        // 视频类型，隐藏图片
        imageStatic.visibility = View.GONE
        imageAnim.visibility = View.GONE
        progressBar.visibility = View.GONE

        // 自定义控制
        refreshUIMargin()

        // 初始化播放器

    }

    private fun formatTimestamp(timestampInSeconds: Long): String {
        val minutes = timestampInSeconds / 60
        val seconds = timestampInSeconds % 60
        return String.format(locale = Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    fun onOriginal() {
        if (imageInfo.type == Type.IMAGE) {
            SLog.d(TAG, "onOriginal: 加载原图")
            loadOriginal()
        } else {
            SLog.d(TAG, "onOriginal: 视频类型，不做处理")
        }
    }

    private fun loadOriginal() {
        val originalUrl = imageInfo.originUrl
        val cacheFile = getGlideCacheFile(imagePreviewActivity, imageInfo.originUrl)
        if (cacheFile != null && cacheFile.exists()) {
            val isStatic = ImageUtil.isStaticImage(originalUrl, cacheFile.absolutePath)
            if (isStatic) {
                SLog.d(TAG, "loadOriginal: 静态图")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    SubsamplingScaleImageView.setPreferredBitmapConfig(Bitmap.Config.ARGB_4444)
                } else {
                    SubsamplingScaleImageView.setPreferredBitmapConfig(Bitmap.Config.ARGB_8888)
                }
                imageAnim.visibility = View.GONE
                imageStatic.visibility = View.VISIBLE
                imageStatic.let {
                    val thumbnailUrl = imageInfo.thumbnailUrl
                    val smallCacheFile = getGlideCacheFile(imagePreviewActivity, thumbnailUrl)
                    var small: ImageSource? = null
                    if (smallCacheFile != null && smallCacheFile.exists()) {
                        val smallImagePath = smallCacheFile.absolutePath
                        small = ImageUtil.getImageBitmap(
                            smallImagePath,
                            ImageUtil.getBitmapDegree(smallImagePath)
                        )?.let {
                            ImageSource.bitmap(it)
                        }
                        val widSmall = ImageUtil.getWidthHeight(smallImagePath)[0]
                        val heiSmall = ImageUtil.getWidthHeight(smallImagePath)[1]
                        if (ImageUtil.isBmpImageWithMime(originalUrl, cacheFile.absolutePath)) {
                            small?.tilingDisabled()
                        }
                        small?.dimensions(widSmall, heiSmall)
                    }
                    val imagePath = cacheFile.absolutePath
                    val origin = ImageSource.uri(imagePath)
                    val widOrigin = ImageUtil.getWidthHeight(imagePath)[0]
                    val heiOrigin = ImageUtil.getWidthHeight(imagePath)[1]
                    if (ImageUtil.isBmpImageWithMime(originalUrl, cacheFile.absolutePath)) {
                        origin.tilingDisabled()
                    }
                    origin.dimensions(widOrigin, heiOrigin)
                    imageStatic.setImage(origin, small)
                    // 缩放适配
                    setImageStatic(imagePath, imageStatic)
                }
            } else {
                SLog.d(TAG, "loadOriginal: 动态图")
                imageStatic.visibility = View.GONE
                imageAnim.visibility = View.VISIBLE
                imageAnim.let {
                    Glide.with(imagePreviewActivity)
                        .asGif()
                        .load(cacheFile)
                        .apply(
                            RequestOptions()
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .error(ImagePreview.instance.errorPlaceHolder)
                        )
                        .into(imageAnim)
                }
            }
        }
    }

    fun onSelected() {
        if (imageInfo.type == Type.VIDEO) {
        }
    }

    fun onUnSelected() {
        if (imageInfo.type == Type.VIDEO) {
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshUIMargin()
    }

    private fun refreshUIMargin() {
        // 获取当前屏幕方向
    }

    fun updateItem(imageInfo: ImageInfo) {
        this.imageInfo = imageInfo
        initData()
    }

    override fun onResume() {
        super.onResume()
        if (!mLoading) {
            mLoading = true
            // 初始化时调用一次
            initData()
        } else {
            // 已经初始化过，如果当前是视频，就执行播放
            if (imageInfo.type == Type.VIDEO) {
                // 后台前是播放的才恢复播放
                if (onPausePlaying == true) {
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        SLog.d(TAG, "onPause: position = $position")
        if (imageInfo.type == Type.VIDEO) {
            // 只特殊处理视频类型
            onUnSelected()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mLoading = false
        onRelease()
    }

    private fun loadResImage(
        url: String,
        originPathUrl: String
    ) {
        Glide.with(imagePreviewActivity).load(R.drawable.icon_download_new)
            .addListener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable?>,
                    isFirstResource: Boolean
                ): Boolean {
                    loadFailed(e)
                    return true
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    progressBar.visibility = View.GONE
                    imageAnim.visibility = View.GONE
                    imageStatic.visibility = View.VISIBLE
                    imageStatic.setImage(ImageSource.resource(R.drawable.icon_download_new))
                    return true
                }
            })
            .into(imageAnim)
    }

    private fun loadUrlImage(
        url: String,
        originPathUrl: String
    ) {
        Glide.with(imagePreviewActivity).downloadOnly().load(url)
            .addListener(object : RequestListener<File> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<File?>,
                    isFirstResource: Boolean
                ): Boolean {
                    // glide加载失败，使用http下载后再次加载
                    Thread {
                        val fileFullName = System.currentTimeMillis().toString()
                        val saveDir =
                            getAvailableCacheDir(imagePreviewActivity)?.absolutePath + File.separator + "image/"
                        val downloadFile = downloadFile(url, fileFullName, saveDir)
                        Handler(Looper.getMainLooper()).post {
                            if (downloadFile != null && downloadFile.exists() && downloadFile.length() > 0) {
                                // 通过urlConn下载完成
                                loadSuccess(
                                    originPathUrl,
                                    downloadFile
                                )
                            } else {
                                loadFailed(e)
                            }
                        }
                    }.start()
                    return true
                }

                override fun onResourceReady(
                    resource: File,
                    model: Any,
                    target: Target<File>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    loadSuccess(
                        url,
                        resource
                    )
                    return true
                }
            }).into(object : FileTarget() {
            })
    }

    private fun loadSuccess(
        imageUrl: String,
        resource: File
    ) {
        val imagePath = resource.absolutePath
        val isStatic = ImageUtil.isStaticImage(imageUrl, imagePath)
        if (isStatic) {
            SLog.d(TAG, "loadSuccess: 动静判断: 静态图")
            loadImageStatic(imagePath)
        } else {
            SLog.d(TAG, "loadSuccess: 动静判断: 动态图")
            loadImageAnim(
                imageUrl,
                imagePath
            )
        }
    }

    /**
     * 加载失败的处理
     */
    private fun loadFailed(
        e: GlideException?
    ) {
        progressBar.visibility = View.GONE
        imageAnim.visibility = View.GONE
        imageStatic.visibility = View.VISIBLE
        imageStatic.isZoomEnabled = false
        imageStatic.setImage(ImageSource.resource(ImagePreview.instance.errorPlaceHolder))
        if (ImagePreview.instance.isShowErrorToast) {
            var errorMsg = imagePreviewActivity.getString(R.string.toast_load_failed)
            if (e != null) {
                errorMsg = e.localizedMessage as String
            }
            if (errorMsg.length > 200) {
                errorMsg = errorMsg.substring(0, 199)
            }
            ToastUtil.instance.showShort(imagePreviewActivity.applicationContext, errorMsg)
        }
    }

    private fun setImageStatic(imagePath: String, imageStatic: SubsamplingScaleImageView) {
        imageStatic.orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            SubsamplingScaleImageView.setPreferredBitmapConfig(Bitmap.Config.ARGB_4444)
        } else {
            SubsamplingScaleImageView.setPreferredBitmapConfig(Bitmap.Config.ARGB_8888)
        }
        val tabletOrLandscape = ImageUtil.isTabletOrLandscape(imagePreviewActivity)
        if (tabletOrLandscape) {
            // Tablet
            imageStatic.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
            imageStatic.minScale = ImagePreview.instance.minScale
            imageStatic.maxScale = ImagePreview.instance.maxScale
            imageStatic.setDoubleTapZoomScale(ImagePreview.instance.mediumScale)
        } else {
            // Phone
            imageStatic.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
            imageStatic.minScale = 1f
            val isLongImage = ImageUtil.isLongImage(imagePath)
            val isWideImage = ImageUtil.isWideImage(imagePath)
            if (isLongImage) {
                // 长图，高/宽>=3
                imageStatic.maxScale =
                    ImageUtil.getLongImageMaxZoomScale(imagePreviewActivity, imagePath)
                imageStatic.setDoubleTapZoomScale(
                    ImageUtil.getLongImageDoubleZoomScale(
                        imagePreviewActivity,
                        imagePath
                    )
                )
                // 设置长图的默认展示模式：宽度拉满/居中显示
                when (ImagePreview.instance.longPicDisplayMode) {
                    ImagePreview.LongPicDisplayMode.Default -> {
                    }

                    ImagePreview.LongPicDisplayMode.FillWidth -> {
                        imageStatic.setScaleAndCenter(
                            ImageUtil.getLongImageFillWidthScale(
                                this.imagePreviewActivity,
                                imagePath
                            ), PointF(0f, 0f)
                        )
                    }
                }
            } else if (isWideImage) {
                // 宽图，宽/高>=3
                imageStatic.maxScale =
                    ImageUtil.getWideImageMaxZoomScale(imagePreviewActivity, imagePath)
                imageStatic.setDoubleTapZoomScale(
                    ImageUtil.getWideImageDoubleScale(
                        imagePreviewActivity,
                        imagePath
                    )
                )
            } else {
                // 普通图片，其他
                imageStatic.maxScale =
                    ImageUtil.getStandardImageMaxZoomScale(imagePreviewActivity, imagePath)
                imageStatic.setDoubleTapZoomScale(
                    ImageUtil.getStandardImageDoubleScale(
                        imagePreviewActivity,
                        imagePath
                    )
                )
            }
        }
    }

    /**
     * 加载静态图片
     */
    private fun loadImageStatic(
        imagePath: String
    ) {
        imageAnim.visibility = View.GONE
        imageStatic.visibility = View.VISIBLE
        val imageSource = ImageSource.uri(Uri.fromFile(File(imagePath)))
        if (ImageUtil.isBmpImageWithMime(imagePath, imagePath)) {
            imageSource.tilingDisabled()
        }
        imageStatic.setImage(imageSource)
        imageStatic.setOnImageEventListener(object : SimpleOnImageEventListener() {
            override fun onReady() {
                progressBar.visibility = View.GONE
            }
        })
        // 缩放适配
        setImageStatic(imagePath, imageStatic)
    }

    /**
     * 加载动图
     */
    private fun loadImageAnim(
        imageUrl: String, imagePath: String
    ) {
        imageStatic.visibility = View.GONE
        imageAnim.visibility = View.VISIBLE
        if (ImageUtil.isAnimWebp(imageUrl, imagePath)) {
            val fitCenter: Transformation<Bitmap> = FitCenter()
            Glide.with(imagePreviewActivity)
                .load(imagePath)
                .apply(
                    RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL)
                        .error(ImagePreview.instance.errorPlaceHolder)
                )
                // .optionalTransform(fitCenter)
                .addListener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable?>,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressBar.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable?>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressBar.visibility = View.GONE
                        return false
                    }
                })
                .into(imageAnim)
        } else {
            // Glide.with(imagePreviewActivity)
            //     .asGif()
            //     .load(imagePath)
            //     .apply(
            //         RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL)
            //             .error(ImagePreview.instance.errorPlaceHolder)
            //     )
            //     .listener(object : RequestListener<GifDrawable?> {
            //         override fun onLoadFailed(
            //             e: GlideException?,
            //             model: Any?,
            //             target: Target<GifDrawable?>,
            //             isFirstResource: Boolean
            //         ): Boolean {
            //             progressBar.visibility = View.GONE
            //             imageStatic.setImage(ImageSource.resource(ImagePreview.instance.errorPlaceHolder))
            //             return false
            //         }
            //
            //         override fun onResourceReady(
            //             resource: GifDrawable,
            //             model: Any,
            //             target: Target<GifDrawable?>?,
            //             dataSource: DataSource,
            //             isFirstResource: Boolean
            //         ): Boolean {
            //             progressBar.visibility = View.GONE
            //             return false
            //         }
            //     })
            //     .into(imageAnim)
        }
    }

    fun onRelease() {
        if (::imageStatic.isInitialized) {
            imageStatic.destroyDrawingCache()
            imageStatic.recycle()
        }
        if (::imageAnim.isInitialized) {
            imageAnim.destroyDrawingCache()
            imageAnim.setImageBitmap(null)
        }
    }
}
