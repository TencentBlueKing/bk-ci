package com.tencent.devops.common.api.cache

import com.jakewharton.disklrucache.DiskLruCache
import com.tencent.devops.common.api.util.ShaUtils
import org.springframework.util.FileCopyUtils
import java.io.File
import java.io.IOException

/**
 * 蓝盾文件磁盘缓存
 */
class BkDiskLruFileCache(
    private val cacheDir: String,
    private val cacheSize: Long
) {

    private val diskCache: DiskLruCache = DiskLruCache.open(File(cacheDir), 1, 1, cacheSize)

    /**
     * 把文件放入磁盘缓存
     * @param key 磁盘缓存key
     * @param inputFile 缓存文件
     */
    @Throws(IOException::class)
    fun put(key: String, inputFile: File) {
        // 根据key获取缓存编辑器（为了保证key格式符合DiskLruCache规范，key需要用sha算法计算出散列值进行转换）
        val editor = diskCache.edit(ShaUtils.sha256(key))
        if (editor != null) {
            // 如果编辑器不为空，把文件写入磁盘缓存
            FileCopyUtils.copy(inputFile.inputStream(), editor.newOutputStream(0))
            editor.commit()
        }
    }

    /**
     * 从磁盘缓存中获取文件到指定位置
     * @param key 磁盘缓存key
     * @param outputFile 输出文件
     */
    @Throws(IOException::class)
    fun get(key: String, outputFile: File) {
        // 根据key从磁盘缓存获取snapshot对象
        val snapshot = diskCache[ShaUtils.sha256(key)]
        snapshot?.getInputStream(0)?.use { input ->
            // 将snapshot对象输出流写入输出文件
            FileCopyUtils.copy(input, outputFile.outputStream())
        }
    }

    /**
     * 从磁盘缓存中移除缓存对象
     * @param key 磁盘缓存key
     */
    @Throws(IOException::class)
    fun remove(key: String) {
        diskCache.remove(ShaUtils.sha256(key))
    }

    /**
     * 关闭缓存
     */
    @Throws(IOException::class)
    fun close() {
        diskCache.close()
    }
}
