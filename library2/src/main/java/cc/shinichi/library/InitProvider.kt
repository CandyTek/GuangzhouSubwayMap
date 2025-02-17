package cc.shinichi.library

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import java.io.File

/**
 * 文件名: InitProvider.java
 * 作者: kirito
 * 描述: 初始化
 * 创建时间: 2024/11/27
 */
class InitProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        // 获取 Application 实例
        val application = context?.applicationContext as? Application
        if (application != null) {
            // 在这里进行初始化操作
            initializeLibrary(application)
        }
        return true // 返回 true 表示成功初始化
    }

    private fun initializeLibrary(application: Application) {
        // downloadDirectory
        val downloadDirectory = File(application.cacheDir, "media_cache")
        // maxBytes 500MB
        val maxBytes = 500 * 1024 * 1024L
        // Note: This should be a singleton in your app.
        GlobalContext.init(application)
    }

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?
    ): Int = 0

    override fun getType(uri: Uri): String? = null
}
