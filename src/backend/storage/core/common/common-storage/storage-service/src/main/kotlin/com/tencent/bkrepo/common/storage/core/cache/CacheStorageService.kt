/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.common.storage.core.cache

import com.tencent.bkrepo.common.api.constant.StringPool.TEMP
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.artifact.stream.artifactStream
import com.tencent.bkrepo.common.storage.core.AbstractStorageService
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.common.storage.filesystem.FileSystemClient
import com.tencent.bkrepo.common.storage.filesystem.check.FileSynchronizeVisitor
import com.tencent.bkrepo.common.storage.filesystem.check.SynchronizeResult
import com.tencent.bkrepo.common.storage.filesystem.cleanup.CleanupFileVisitor
import com.tencent.bkrepo.common.storage.filesystem.cleanup.CleanupResult
import com.tencent.bkrepo.common.storage.monitor.StorageHealthMonitor
import org.slf4j.LoggerFactory
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.nio.file.Paths

/**
 * 支持缓存的存储服务
 */
class CacheStorageService(
    private val threadPoolTaskExecutor: ThreadPoolTaskExecutor
) : AbstractStorageService() {

    override fun doStore(path: String, filename: String, artifactFile: ArtifactFile, credentials: StorageCredentials) {
        when {
            artifactFile.isInMemory() -> {
                fileStorage.store(path, filename, artifactFile.getInputStream(), artifactFile.getSize(), credentials)
            }
            artifactFile.isFallback() || artifactFile.isInLocalDisk() -> {
                fileStorage.store(path, filename, artifactFile.flushToFile(), credentials)
            }
            else -> {
                val cachedFile = getCacheClient(credentials).move(path, filename, artifactFile.flushToFile())
                threadPoolTaskExecutor.execute {
                    try {
                        fileStorage.store(path, filename, cachedFile, credentials)
                    } catch (ignored: Exception) {
                        // 此处为异步上传，失败后异常不会被外层捕获，所以单独捕获打印error日志
                        logger.error("Failed to async store file [$filename] on [${credentials.key}]", ignored)
                    }
                }
            }
        }
    }

    override fun doLoad(
        path: String,
        filename: String,
        range: Range,
        credentials: StorageCredentials
    ): ArtifactInputStream? {
        val cacheClient = getCacheClient(credentials)
        val loadCacheFirst = isLoadCacheFirst(range, credentials)
        if (loadCacheFirst) {
            cacheClient.load(path, filename)?.artifactStream(range)?.let { return it }
        }
        val artifactInputStream = fileStorage.load(path, filename, range, credentials)?.artifactStream(range)
        if (artifactInputStream != null && loadCacheFirst && range.isFullContent()) {
            val cachePath = Paths.get(credentials.cache.path, path)
            val tempPath = Paths.get(credentials.cache.path, TEMP)
            val readListener = CachedFileWriter(cachePath, filename, tempPath)
            artifactInputStream.addListener(readListener)
        }
        return if (artifactInputStream == null && !loadCacheFirst) {
            cacheClient.load(path, filename)?.artifactStream(range)
        } else {
            artifactInputStream
        }
    }

    override fun doDelete(path: String, filename: String, credentials: StorageCredentials) {
        fileStorage.delete(path, filename, credentials)
        getCacheClient(credentials).delete(path, filename)
    }

    override fun doExist(path: String, filename: String, credentials: StorageCredentials): Boolean {
        return fileStorage.exist(path, filename, credentials)
    }

    /**
     * 覆盖父类cleanUp逻辑，还包括清理缓存的文件内容
     */
    override fun cleanUp(storageCredentials: StorageCredentials?): CleanupResult {
        val credentials = getCredentialsOrDefault(storageCredentials)
        val rootPath = Paths.get(credentials.cache.path)
        val tempPath = getTempPath(credentials)
        val visitor = CleanupFileVisitor(rootPath, tempPath, fileStorage, fileLocator, credentials)
        getCacheClient(credentials).walk(visitor)
        return visitor.result
    }

    override fun synchronizeFile(storageCredentials: StorageCredentials?): SynchronizeResult {
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempPath = Paths.get(credentials.cache.path, TEMP)
        val visitor = FileSynchronizeVisitor(tempPath, fileLocator, fileStorage, credentials)
        getCacheClient(credentials).walk(visitor)
        return visitor.result
    }

    override fun doCheckHealth(credentials: StorageCredentials) {
        val monitor = getMonitor(credentials)
        if (!monitor.healthy.get()) {
            throw IllegalStateException("Cache storage is unhealthy: ${monitor.fallBackReason}")
        }
        super.doCheckHealth(credentials)
    }

    /**
     * 判断是否优先从缓存加载数据
     * 判断规则:
     * 当cacheFirst开启，并且cache磁盘健康，并且当前文件未超过内存阈值大小
     */
    private fun isLoadCacheFirst(range: Range, credentials: StorageCredentials): Boolean {
        val isExceedThreshold = range.total > storageProperties.receive.fileSizeThreshold.toBytes()
        val isHealth = getMonitor(credentials).healthy.get()
        val cacheFirst = credentials.cache.loadCacheFirst
        return cacheFirst && isHealth && isExceedThreshold
    }

    private fun getMonitor(credentials: StorageCredentials): StorageHealthMonitor {
        return monitorHelper.getMonitor(storageProperties, credentials)
    }

    private fun getCacheClient(credentials: StorageCredentials): FileSystemClient {
        return FileSystemClient(credentials.cache.path)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CacheStorageService::class.java)
    }
}
