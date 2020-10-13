/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.  
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.common.storage.core.cache

import com.tencent.bkrepo.common.api.constant.StringPool.TEMP
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.artifact.stream.bound
import com.tencent.bkrepo.common.artifact.stream.toArtifactStream
import com.tencent.bkrepo.common.storage.core.AbstractStorageService
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.common.storage.filesystem.FileSystemClient
import com.tencent.bkrepo.common.storage.filesystem.check.FileSynchronizeVisitor
import com.tencent.bkrepo.common.storage.filesystem.check.SynchronizeResult
import com.tencent.bkrepo.common.storage.filesystem.cleanup.CleanupResult
import java.nio.file.Paths
import java.util.concurrent.Executor
import javax.annotation.Resource

/**
 * 支持缓存的存储服务
 */
class CacheStorageService : AbstractStorageService() {

    @Resource
    private lateinit var taskAsyncExecutor: Executor

    override fun doStore(path: String, filename: String, artifactFile: ArtifactFile, credentials: StorageCredentials) {
        when {
            artifactFile.isInMemory() -> {
                fileStorage.store(path, filename, artifactFile.getInputStream(), artifactFile.getSize(), credentials)
            }
            artifactFile.isFallback() -> {
                fileStorage.store(path, filename, artifactFile.flushToFile(), credentials)
            }
            else -> {
                val cachedFile = getCacheClient(credentials).move(path, filename, artifactFile.flushToFile())
                taskAsyncExecutor.execute {
                    fileStorage.store(path, filename, cachedFile, credentials)
                }
            }
        }
    }

    override fun doLoad(path: String, filename: String, range: Range, credentials: StorageCredentials): ArtifactInputStream? {
        val cacheClient = getCacheClient(credentials)
        val loadCacheFirst = isLoadCacheFirst(range, credentials)
        if (loadCacheFirst) {
            cacheClient.load(path, filename)?.bound(range)?.toArtifactStream(range)?.let { return it }
        }

        val artifactInputStream = fileStorage.load(path, filename, range, credentials)?.toArtifactStream(range)
        if (range.isFullContent() && loadCacheFirst && artifactInputStream != null) {
            val cachePath = Paths.get(credentials.cache.path, path)
            val tempPath = Paths.get(credentials.cache.path, TEMP)
            val readListener = CachedFileWriter(cachePath, filename, tempPath)
            artifactInputStream.addListener(readListener)
        }
        return artifactInputStream
    }

    override fun doDelete(path: String, filename: String, credentials: StorageCredentials) {
        fileStorage.delete(path, filename, credentials)
        getCacheClient(credentials).delete(path, filename)
    }

    override fun doExist(path: String, filename: String, credentials: StorageCredentials): Boolean {
        return fileStorage.exist(path, filename, credentials)
    }

    override fun getTempPath(credentials: StorageCredentials): String {
        return Paths.get(credentials.cache.path, TEMP).toString()
    }

    /**
     * 覆盖父类cleanUp逻辑，还包括清理缓存的文件内容
     */
    override fun cleanUp(storageCredentials: StorageCredentials?): CleanupResult {
        val credentials = getCredentialsOrDefault(storageCredentials)
        return getCacheClient(credentials).cleanUp(credentials.cache.expireDays)
    }

    override fun synchronizeFile(storageCredentials: StorageCredentials?): SynchronizeResult {
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempPath = Paths.get(credentials.cache.path, TEMP)
        val visitor = FileSynchronizeVisitor(tempPath, fileLocator, fileStorage, credentials)
        getCacheClient(credentials).walk(visitor)
        return visitor.checkResult
    }

    override fun doCheckHealth(credentials: StorageCredentials) {
        if (!monitor.health.get()) {
            throw IllegalStateException("Cache storage is unhealthy: ${monitor.reason}")
        }
        super.doCheckHealth(credentials)
    }

    /**
     * 判断是否优先从缓存加载数据
     * 判断规则:
     * 当cacheFirst开启，并且cache磁盘健康，并且当前文件未超过内存阈值大小
     */
    private fun isLoadCacheFirst(range: Range, credentials: StorageCredentials): Boolean {
        val isExceedThreshold = range.total > storageProperties.fileSizeThreshold.toBytes()
        val isHealth = if (credentials == storageProperties.defaultStorageCredentials()) {
            monitor.health.get()
        } else {
            true
        }
        val cacheFirst = credentials.cache.loadCacheFirst
        return cacheFirst && isHealth && isExceedThreshold
    }

    private fun getCacheClient(credentials: StorageCredentials): FileSystemClient {
        return FileSystemClient(credentials.cache.path)
    }
}
