package com.tencent.devops.common.api.cache

import com.jakewharton.disklrucache.DiskLruCache
import com.tencent.devops.common.api.util.ShaUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * 蓝盾文件磁盘缓存
 */
class BkDiskLruFileCache(
    private val cacheDir: String,
    private val cacheSize: Long
) {

    private val diskCache: DiskLruCache = DiskLruCache.open(File(cacheDir), 1, 1, cacheSize)

    companion object {
        private const val BUFFER_SIZE = 1024
        private val logger = LoggerFactory.getLogger(BkDiskLruFileCache::class.java)
    }

    /**
     * 把文件放入磁盘缓存
     * @param key 磁盘缓存key
     * @param inputFile 缓存文件
     */
    @Throws(IOException::class)
    fun put(key: String, inputFile: File) {
        // 根据key获取缓存编辑器（为了保证key格式符合DiskLruCache规范，key需要用sha算法计算出散列值进行转换）
        val editor = diskCache.edit(ShaUtils.sha256(key)) ?: return
        // 如果编辑器不为空，把文件写入磁盘缓存
        FileInputStream(inputFile).use { inputStream ->
            editor.newOutputStream(0).use { outputStream ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
            }
        }
        editor.commit()
        // 手动触发淘汰策略
        diskCache.flush()
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
        snapshot?.getInputStream(0)?.use { inputStream ->
            // 将snapshot对象输出流写入输出文件
            if (!outputFile.exists()) {
                outputFile.parentFile.mkdirs()
            }
            FileOutputStream(outputFile).use { outputStream ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
            }
        }
        // 将文件设置为可执行文件
        if (outputFile.exists()) {
            val success = outputFile.setExecutable(true)
            if (success) {
                logger.info("file[${outputFile.absolutePath}] execution permission added successfully.")
            } else {
                logger.warn("file[${outputFile.absolutePath}] failed to add execution permission.")
            }
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
