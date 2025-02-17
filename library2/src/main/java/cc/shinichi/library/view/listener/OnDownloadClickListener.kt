package cc.shinichi.library.view.listener

import android.app.Activity
import android.view.View

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 * cc.shinichi.library.view.listener
 * create at 2018/12/19  16:23
 * description:
 */
abstract class OnDownloadClickListener {
    /**
     * 点击事件
     * 是否拦截下载行为
     */
    abstract fun onClick(activity: Activity, view: View, position: Int)

    /**
     * 是否拦截下载
     *
     * @return
     */
    abstract val isInterceptDownload: Boolean
}