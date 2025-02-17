package cc.shinichi.library

import android.app.Application
import android.content.Context

/**
 * 文件名: GlobalContext.java
 * 作者: kirito
 * 描述: 全局
 * 创建时间: 2024/11/27
 */
object GlobalContext {
    private var application: Application? = null

    fun init(app: Application) {
        if (application == null) {
            application = app
        }
    }

    fun getApplication(): Application {
        return application ?: throw IllegalStateException("Application is not initialized")
    }



    fun getContext(): Context {
        return getApplication().applicationContext
    }
}
