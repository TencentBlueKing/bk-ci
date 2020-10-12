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

package com.tencent.bkrepo.common.storage.core

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.api.constant.StringPool.uniqueId
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.toArtifactFile
import com.tencent.bkrepo.common.artifact.hash.md5
import com.tencent.bkrepo.common.artifact.hash.sha256
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.artifact.stream.EmptyInputStream
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.artifact.stream.ZeroInputStream
import com.tencent.bkrepo.common.storage.core.locator.FileLocator
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.common.storage.filesystem.FileSystemClient
import com.tencent.bkrepo.common.storage.filesystem.check.SynchronizeResult
import com.tencent.bkrepo.common.storage.filesystem.cleanup.CleanupResult
import com.tencent.bkrepo.common.storage.message.HealthCheckFailedException
import com.tencent.bkrepo.common.storage.message.StorageException
import com.tencent.bkrepo.common.storage.message.StorageMessageCode
import com.tencent.bkrepo.common.storage.monitor.StorageHealthMonitor
import com.tencent.bkrepo.common.storage.monitor.Throughput
import com.tencent.bkrepo.common.storage.pojo.FileInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.system.measureNanoTime

/**
 * 存储服务抽象实现
 */
@Suppress("TooGenericExceptionCaught")
abstract class AbstractStorageService : StorageService {

    @Autowired
    protected lateinit var fileLocator: FileLocator

    @Autowired
    protected lateinit var fileStorage: FileStorage

    @Autowired
    protected lateinit var storageProperties: StorageProperties

    @Autowired
    protected lateinit var monitor: StorageHealthMonitor

    private val healthCheckExecutor = Executors.newSingleThreadExecutor()

    override fun store(digest: String, artifactFile: ArtifactFile, storageCredentials: StorageCredentials?) {
        val path = fileLocator.locate(digest)
        val credentials = getCredentialsOrDefault(storageCredentials)
        try {
            if (doExist(path, digest, credentials)) {
                logger.info("Artifact file [$digest] exists, skip store.")
                return
            } else {
                val size = artifactFile.getSize()
                val nanoTime = measureNanoTime { doStore(path, digest, artifactFile, credentials) }
                val throughput = Throughput(size, nanoTime)
                logger.info("Success to store artifact file [$digest], $throughput.")
            }
        } catch (exception: Exception) {
            logger.error("Failed to store artifact file [$digest].", exception)
            throw StorageException(StorageMessageCode.STORE_ERROR, exception.message.toString())
        }
    }

    override fun load(digest: String, range: Range, storageCredentials: StorageCredentials?): ArtifactInputStream? {
        if (range.isEmpty()) return ArtifactInputStream(EmptyInputStream.INSTANCE, range)
        val path = fileLocator.locate(digest)
        val credentials = getCredentialsOrDefault(storageCredentials)
        try {
            return doLoad(path, digest, range, credentials) ?: run {
                if (credentials != storageProperties.defaultStorageCredentials()) {
                    logger.warn("Fallback to default storage [$digest].")
                    doLoad(path, digest, range, storageProperties.defaultStorageCredentials())
                } else null
            }
        } catch (exception: Exception) {
            logger.error("Failed to load file [$digest] on [$credentials].", exception)
            throw StorageException(StorageMessageCode.LOAD_ERROR, exception.message.toString())
        }
    }

    override fun delete(digest: String, storageCredentials: StorageCredentials?) {
        val path = fileLocator.locate(digest)
        val credentials = getCredentialsOrDefault(storageCredentials)
        try {
            doDelete(path, digest, credentials)
            logger.info("Success to delete file [$digest].")
        } catch (exception: Exception) {
            logger.error("Failed to delete file [$digest] on [$credentials].", exception)
            throw StorageException(StorageMessageCode.DELETE_ERROR, exception.message.toString())
        }
    }

    override fun exist(digest: String, storageCredentials: StorageCredentials?): Boolean {
        val path = fileLocator.locate(digest)
        val credentials = getCredentialsOrDefault(storageCredentials)
        try {
            return doExist(path, digest, credentials)
        } catch (exception: Exception) {
            logger.error("Failed to check file [$digest] exist on [$credentials].", exception)
            throw StorageException(StorageMessageCode.QUERY_ERROR, exception.message.toString())
        }
    }

    override fun copy(digest: String, fromCredentials: StorageCredentials?, toCredentials: StorageCredentials?) {
        val path = fileLocator.locate(digest)
        val from = getCredentialsOrDefault(fromCredentials)
        val to = getCredentialsOrDefault(toCredentials)
        try {
            if (from == to) {
                logger.info("Source and destination credentials are same, skip copy file [$digest].")
                return
            }
            if (doExist(path, digest, to)) {
                logger.info("File [$digest] exist on destination credentials, skip copy file.")
                return
            }
            fileStorage.copy(path, digest, from, to)
            logger.info("Success to copy file [$digest] from [$from] to [$to].")
        } catch (exception: Exception) {
            logger.error("Failed to copy file [$digest] from [$from] to [$to].", exception)
            throw StorageException(StorageMessageCode.COPY_ERROR, exception.message.toString())
        }
    }

    /**
     * 创建可追加的文件, 返回文件追加Id
     * 追加文件组织格式: 在temp目录下创建一个具有唯一id的文件，文件名称即追加Id
     * 数据每次追加都写入到该文件中
     */
    override fun createAppendId(storageCredentials: StorageCredentials?): String {
        val appendId = uniqueId()
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        try {
            tempClient.touch(CURRENT_PATH, appendId)
            logger.info("Success to create append id [$appendId].")
            return appendId
        } catch (exception: Exception) {
            logger.error("Failed to create append id [$appendId] on [$credentials].", exception)
            throw StorageException(StorageMessageCode.STORE_ERROR, exception.message.toString())
        }
    }

    override fun append(appendId: String, artifactFile: ArtifactFile, storageCredentials: StorageCredentials?): Long {
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        try {
            val length = tempClient.append(CURRENT_PATH, appendId, artifactFile.getInputStream(), artifactFile.getSize())
            logger.info("Success to append file [$appendId].")
            return length
        } catch (exception: Exception) {
            logger.error("Failed to append file [$appendId] on [$credentials].", exception)
            throw StorageException(StorageMessageCode.STORE_ERROR, exception.message.toString())
        }
    }

    override fun finishAppend(appendId: String, storageCredentials: StorageCredentials?): FileInfo {
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        try {
            val fileInfo = tempClient.load(CURRENT_PATH, appendId)?.let { storeFile(it, credentials) }
                ?: throw IllegalArgumentException("Append file does not exist.")
            tempClient.delete(CURRENT_PATH, appendId)
            logger.info("Success to finish append file [$appendId], file info [$fileInfo].")
            return fileInfo
        } catch (exception: Exception) {
            logger.error("Failed to finish append file [$appendId] on [$credentials].", exception)
            throw StorageException(StorageMessageCode.STORE_ERROR, exception.message.toString())
        }
    }

    /**
     * 创建分块存储目录，返回分块存储Id
     * 组织格式: 在temp目录下创建一个名称唯一的目录，所有分块存储在该目录下，目录名称即blockId
     * 其中，每个分块对应两个文件，命名分别为$sequence.block和$sequence.sha256
     * $sequence.block文件保存其数据，
     * $sequence.sha256保存文件sha256，用于后续分块合并时校验
     */
    override fun createBlockId(storageCredentials: StorageCredentials?): String {
        val blockId = uniqueId()
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        try {
            tempClient.createDirectory(CURRENT_PATH, blockId)
            logger.info("Success to create block [$blockId].")
            return blockId
        } catch (exception: Exception) {
            logger.error("Failed to create block [$blockId] on [$credentials].", exception)
            throw StorageException(StorageMessageCode.STORE_ERROR, exception.message.toString())
        }
    }

    override fun checkBlockId(blockId: String, storageCredentials: StorageCredentials?): Boolean {
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        try {
            return tempClient.checkDirectory(blockId)
        } catch (exception: Exception) {
            logger.error("Failed to check block [$blockId] on [$credentials].", exception)
            throw StorageException(StorageMessageCode.STORE_ERROR, exception.message.toString())
        }
    }

    override fun storeBlock(
        blockId: String,
        sequence: Int,
        digest: String,
        artifactFile: ArtifactFile,
        overwrite: Boolean,
        storageCredentials: StorageCredentials?
    ) {
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        try {
            tempClient.store(blockId, "$sequence$BLOCK_SUFFIX", artifactFile.getInputStream(), artifactFile.getSize(), overwrite)
            tempClient.store(blockId, "$sequence$SHA256_SUFFIX", digest.byteInputStream(), digest.length.toLong(), overwrite)
            logger.info("Success to store block [$blockId/$sequence].")
        } catch (exception: Exception) {
            logger.error("Failed to store block [$blockId/$sequence] on [$credentials].", exception)
            throw StorageException(StorageMessageCode.STORE_ERROR, exception.message.toString())
        }
    }

    override fun mergeBlock(blockId: String, storageCredentials: StorageCredentials?): FileInfo {
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        try {
            val blockFileList = tempClient.listFiles(blockId, BLOCK_SUFFIX).sortedBy { it.name.removeSuffix(BLOCK_SUFFIX).toInt() }
            blockFileList.takeIf { it.isNotEmpty() } ?: throw StorageException(StorageMessageCode.BLOCK_EMPTY)
            for (index in blockFileList.indices) {
                val sequence = index + 1
                if (blockFileList[index].name.removeSuffix(BLOCK_SUFFIX).toInt() != sequence) {
                    throw StorageException(StorageMessageCode.BLOCK_MISSING, sequence.toString())
                }
            }
            val mergedFile = tempClient.mergeFiles(blockFileList, tempClient.touch(blockId, MERGED_FILENAME))
            val fileInfo = storeFile(mergedFile, credentials)
            tempClient.deleteDirectory(CURRENT_PATH, blockId)
            logger.info("Success to merge block [$blockId].")
            return fileInfo
        } catch (storageException: StorageException) {
            logger.error("Failed to merge block [$blockId] on [$credentials]: ${storageException.messageCode}")
            throw storageException
        } catch (exception: Exception) {
            logger.error("Failed to merge block [$blockId] on [$credentials].", exception)
            throw StorageException(StorageMessageCode.STORE_ERROR, exception.message.orEmpty())
        }
    }

    override fun deleteBlockId(blockId: String, storageCredentials: StorageCredentials?) {
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        try {
            tempClient.deleteDirectory(CURRENT_PATH, blockId)
            logger.info("Success to delete block id [$blockId].")
        } catch (exception: Exception) {
            logger.error("Failed to delete block id [$blockId] on [$credentials].", exception)
            throw StorageException(StorageMessageCode.STORE_ERROR, exception.message.toString())
        }
    }

    override fun listBlock(blockId: String, storageCredentials: StorageCredentials?): List<Pair<Long, String>> {
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        try {
            val blockFileList = tempClient.listFiles(blockId, BLOCK_SUFFIX).sortedBy { it.name.removeSuffix(BLOCK_SUFFIX).toInt() }
            return blockFileList.map {
                val size = it.length()
                val sha256 = tempClient.load(blockId, it.name.replace(BLOCK_SUFFIX, SHA256_SUFFIX))?.readText().orEmpty()
                Pair(size, sha256)
            }
        } catch (exception: Exception) {
            logger.error("Failed to list block [$blockId] on [$credentials].", exception)
            throw StorageException(StorageMessageCode.STORE_ERROR, exception.message.toString())
        }
    }

    /**
     * 清理temp目录文件，包括分块上传产生和追加上传产生的脏数据
     */
    override fun cleanUp(storageCredentials: StorageCredentials?): CleanupResult {
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        return tempClient.cleanUp(credentials.cache.expireDays)
    }

    override fun synchronizeFile(storageCredentials: StorageCredentials?) = SynchronizeResult()

    override fun checkHealth(storageCredentials: StorageCredentials?) {
        val credentials = getCredentialsOrDefault(storageCredentials)
        val future = healthCheckExecutor.submit(
            Callable {
                doCheckHealth(credentials)
            }
        )
        try {
            future.get(storageProperties.monitor.timeout.seconds, TimeUnit.SECONDS)
        } catch (timeoutException: TimeoutException) {
            throw HealthCheckFailedException(StorageHealthMonitor.IO_TIMEOUT_MESSAGE)
        } catch (exception: Exception) {
            throw HealthCheckFailedException(exception.message.orEmpty())
        }
    }

    protected fun getCredentialsOrDefault(storageCredentials: StorageCredentials?): StorageCredentials {
        return storageCredentials ?: storageProperties.defaultStorageCredentials()
    }

    private fun storeFile(file: File, credentials: StorageCredentials): FileInfo {
        val sha256 = file.sha256()
        val md5 = file.md5()
        val size = file.length()
        val fileInfo = FileInfo(sha256, md5, size)
        val path = fileLocator.locate(sha256)
        if (!doExist(path, sha256, credentials)) {
            doStore(path, sha256, file.toArtifactFile(), credentials)
        } else {
            logger.info("File [$sha256] exist, skip store.")
        }
        return fileInfo
    }

    /**
     * 获取fs client用于操作临时文件，
     * 临时文件用于分块上传、文件追加上传
     * cache: /data/cached/temp
     * simple:
     *   default: io.temp
     *   fs: /data/store/temp
     */
    private fun getTempClient(credentials: StorageCredentials): FileSystemClient {
        val tempPath = getTempPath(credentials) ?: fileStorage.getTempPath(credentials)
        return FileSystemClient(tempPath)
    }

    protected abstract fun doStore(path: String, filename: String, artifactFile: ArtifactFile, credentials: StorageCredentials)
    protected abstract fun doLoad(path: String, filename: String, range: Range, credentials: StorageCredentials): ArtifactInputStream?
    protected abstract fun doDelete(path: String, filename: String, credentials: StorageCredentials)
    protected abstract fun doExist(path: String, filename: String, credentials: StorageCredentials): Boolean

    open fun doCheckHealth(credentials: StorageCredentials) {
        val filename = System.nanoTime().toString()
        val size = storageProperties.monitor.dataSize.toBytes()
        val inputStream = ZeroInputStream(size)
        fileStorage.store(HEALTH_CHECK_PATH, filename, inputStream, size, credentials)
        fileStorage.delete(HEALTH_CHECK_PATH, filename, credentials)
    }

    open fun getTempPath(credentials: StorageCredentials): String? = null

    companion object {
        private const val CURRENT_PATH = StringPool.EMPTY
        private const val HEALTH_CHECK_PATH = "/health-check"
        private const val BLOCK_SUFFIX = ".block"
        private const val SHA256_SUFFIX = ".sha256"
        private const val MERGED_FILENAME = "merged.data"
        private val logger = LoggerFactory.getLogger(AbstractStorageService::class.java)
    }
}
