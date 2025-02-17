package cc.shinichi.library.glide.progress

import android.text.TextUtils
import cc.shinichi.library.glide.SSLSocketClient
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 */
object ProgressManager {

    private val listenersMap = Collections.synchronizedMap(HashMap<String, OnProgressListener>())

 
    @JvmStatic
    fun addListener(url: String?, listener: OnProgressListener?) {
        if (!TextUtils.isEmpty(url)) {
            listenersMap[url] = listener
            listener?.onProgress(url, false, 1, 0, 0)
        }
    }

    private fun removeListener(url: String?) {
        if (!TextUtils.isEmpty(url)) {
            listenersMap.remove(url)
        }
    }
}
