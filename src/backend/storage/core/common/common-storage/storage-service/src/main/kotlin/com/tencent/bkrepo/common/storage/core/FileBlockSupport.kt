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
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.storage.core

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.toArtifactFile
import com.tencent.bkrepo.common.artifact.hash.md5
import com.tencent.bkrepo.common.artifact.hash.sha256
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.common.storage.message.StorageErrorException
import com.tencent.bkrepo.common.storage.message.StorageMessageCode
import com.tencent.bkrepo.common.storage.pojo.FileInfo
import org.slf4j.LoggerFactory
import java.io.File

/**
 * 分块操作实现类
 */
@Suppress("TooGenericExceptionCaught")
abstract class FileBlockSupport : CleanupSupport() {

    override fun createAppendId(storageCredentials: StorageCredentials?): String {
        val appendId = StringPool.uniqueId()
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        try {
            tempClient.touch(CURRENT_PATH, appendId)
            logger.info("Success to create append id [$appendId]")
            return appendId
        } catch (exception: Exception) {
            logger.error("Failed to create append id [$appendId] on [${credentials.key}]", exception)
            throw StorageErrorException(StorageMessageCode.STORE_ERROR)
        }
    }

    override fun append(appendId: String, artifactFile: ArtifactFile, storageCredentials: StorageCredentials?): Long {
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        val inputStream = artifactFile.getInputStream()
        val size = artifactFile.getSize()
        try {
            val length = tempClient.append(CURRENT_PATH, appendId, inputStream, size)
            logger.info("Success to append file [$appendId]")
            return length
        } catch (exception: Exception) {
            logger.error("Failed to append file [$appendId] on [${credentials.key}]", exception)
            throw StorageErrorException(StorageMessageCode.STORE_ERROR)
        }
    }

    override fun finishAppend(appendId: String, storageCredentials: StorageCredentials?): FileInfo {
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        try {
            val fileInfo = tempClient.load(CURRENT_PATH, appendId)?.let { storeMergedFile(it, credentials) }
                ?: throw IllegalArgumentException("Append file does not exist.")
            tempClient.delete(CURRENT_PATH, appendId)
            logger.info("Success to finish append file [$appendId], file info [$fileInfo]")
            return fileInfo
        } catch (exception: Exception) {
            logger.error("Failed to finish append file [$appendId] on [${credentials.key}]", exception)
            throw StorageErrorException(StorageMessageCode.STORE_ERROR)
        }
    }

    override fun createBlockId(storageCredentials: StorageCredentials?): String {
        val blockId = StringPool.uniqueId()
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        try {
            tempClient.createDirectory(CURRENT_PATH, blockId)
            logger.info("Success to create block [$blockId]")
            return blockId
        } catch (exception: Exception) {
            logger.error("Failed to create block [$blockId] on [${credentials.key}]", exception)
            throw StorageErrorException(StorageMessageCode.STORE_ERROR)
        }
    }

    override fun checkBlockId(blockId: String, storageCredentials: StorageCredentials?): Boolean {
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        try {
            return tempClient.checkDirectory(blockId)
        } catch (exception: Exception) {
            logger.error("Failed to check block [$blockId] on [${credentials.key}]", exception)
            throw StorageErrorException(StorageMessageCode.STORE_ERROR)
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
        val blockInputStream = artifactFile.getInputStream()
        val blockSize = artifactFile.getSize()
        val digestInputStream = digest.byteInputStream()
        val digestSize = digest.length.toLong()
        try {
            tempClient.store(blockId, "$sequence$BLOCK_SUFFIX", blockInputStream, blockSize, overwrite)
            tempClient.store(blockId, "$sequence$SHA256_SUFFIX", digestInputStream, digestSize, overwrite)
            logger.info("Success to store block [$blockId/$sequence]")
        } catch (exception: Exception) {
            logger.error("Failed to store block [$blockId/$sequence] on [${credentials.key}]", exception)
            throw StorageErrorException(StorageMessageCode.STORE_ERROR)
        }
    }

    override fun mergeBlock(blockId: String, storageCredentials: StorageCredentials?): FileInfo {
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        try {
            val blockFileList = tempClient.listFiles(blockId, BLOCK_SUFFIX).sortedBy {
                it.name.removeSuffix(BLOCK_SUFFIX).toInt()
            }
            blockFileList.takeIf { it.isNotEmpty() } ?: throw StorageErrorException(StorageMessageCode.BLOCK_EMPTY)
            for (index in blockFileList.indices) {
                val sequence = index + 1
                if (blockFileList[index].name.removeSuffix(BLOCK_SUFFIX).toInt() != sequence) {
                    throw StorageErrorException(StorageMessageCode.BLOCK_MISSING, sequence.toString())
                }
            }
            val mergedFile = tempClient.mergeFiles(
                blockFileList, tempClient.touch(
                    blockId,
                    MERGED_FILENAME
                )
            )
            val fileInfo = storeMergedFile(mergedFile, credentials)
            tempClient.deleteDirectory(CURRENT_PATH, blockId)
            logger.info("Success to merge block [$blockId]")
            return fileInfo
        } catch (storageException: StorageErrorException) {
            logger.error("Failed to merge block [$blockId] on [${credentials.key}]: ${storageException.messageCode}")
            throw storageException
        } catch (exception: Exception) {
            logger.error("Failed to merge block [$blockId] on [${credentials.key}]", exception)
            throw StorageErrorException(StorageMessageCode.STORE_ERROR)
        }
    }

    override fun deleteBlockId(blockId: String, storageCredentials: StorageCredentials?) {
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        try {
            tempClient.deleteDirectory(CURRENT_PATH, blockId)
            logger.info("Success to delete block id [$blockId]")
        } catch (exception: Exception) {
            logger.error("Failed to delete block id [$blockId] on [${credentials.key}]", exception)
            throw StorageErrorException(StorageMessageCode.STORE_ERROR)
        }
    }

    override fun listBlock(blockId: String, storageCredentials: StorageCredentials?): List<Pair<Long, String>> {
        val credentials = getCredentialsOrDefault(storageCredentials)
        val tempClient = getTempClient(credentials)
        try {
            val blockFileList = tempClient.listFiles(blockId, BLOCK_SUFFIX).sortedBy {
                it.name.removeSuffix(BLOCK_SUFFIX).toInt()
            }
            return blockFileList.map {
                val size = it.length()
                val name = it.name.replace(BLOCK_SUFFIX, SHA256_SUFFIX)
                val sha256 = tempClient.load(blockId, name)?.readText().orEmpty()
                Pair(size, sha256)
            }
        } catch (exception: Exception) {
            logger.error("Failed to list block [$blockId] on [${credentials.key}]", exception)
            throw StorageErrorException(StorageMessageCode.STORE_ERROR)
        }
    }

    private fun storeMergedFile(file: File, credentials: StorageCredentials): FileInfo {
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

    companion object {
        private val logger = LoggerFactory.getLogger(FileBlockSupport::class.java)
        private const val CURRENT_PATH = StringPool.EMPTY
        private const val BLOCK_SUFFIX = ".block"
        private const val SHA256_SUFFIX = ".sha256"
        private const val MERGED_FILENAME = "merged.data"
    }
}
