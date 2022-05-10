/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
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

import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.common.storage.filesystem.check.SynchronizeResult
import com.tencent.bkrepo.common.storage.message.StorageErrorException
import com.tencent.bkrepo.common.storage.message.StorageMessageCode
import com.tencent.bkrepo.common.storage.monitor.Throughput
import org.slf4j.LoggerFactory
import kotlin.system.measureNanoTime

/**
 * 存储服务抽象实现
 */
@Suppress("TooGenericExceptionCaught")
abstract class AbstractStorageService : FileBlockSupport() {

    override fun store(digest: String, artifactFile: ArtifactFile, storageCredentials: StorageCredentials?): Int {
        val path = fileLocator.locate(digest)
        val credentials = getCredentialsOrDefault(storageCredentials)
        try {
            return if (doExist(path, digest, credentials)) {
                logger.info("Artifact file [$digest] exists, skip store.")
                0
            } else {
                val size = artifactFile.getSize()
                val nanoTime = measureNanoTime { doStore(path, digest, artifactFile, credentials) }
                val throughput = Throughput(size, nanoTime)
                logger.info("Success to store artifact file [$digest], $throughput.")
                1
            }
        } catch (exception: Exception) {
            logger.error("Failed to store artifact file [$digest] on [${credentials.key}]", exception)
            throw StorageErrorException(StorageMessageCode.STORE_ERROR, cause = exception)
        }
    }

    override fun load(digest: String, range: Range, storageCredentials: StorageCredentials?): ArtifactInputStream? {
        val path = fileLocator.locate(digest)
        val credentials = getCredentialsOrDefault(storageCredentials)
        try {
            return doLoad(path, digest, range, credentials)
        } catch (exception: Exception) {
            logger.error("Failed to load file [$digest] on [${credentials.key}]", exception)
            throw StorageErrorException(StorageMessageCode.LOAD_ERROR)
        }
    }

    override fun delete(digest: String, storageCredentials: StorageCredentials?) {
        val path = fileLocator.locate(digest)
        val credentials = getCredentialsOrDefault(storageCredentials)
        try {
            doDelete(path, digest, credentials)
            logger.info("Success to delete file [$digest]")
        } catch (exception: Exception) {
            logger.error("Failed to delete file [$digest] on [${credentials.key}]", exception)
            throw StorageErrorException(StorageMessageCode.DELETE_ERROR)
        }
    }

    override fun exist(digest: String, storageCredentials: StorageCredentials?): Boolean {
        val path = fileLocator.locate(digest)
        val credentials = getCredentialsOrDefault(storageCredentials)
        try {
            return doExist(path, digest, credentials)
        } catch (exception: Exception) {
            logger.error("Failed to check file [$digest] exist on [${credentials.key}]", exception)
            throw StorageErrorException(StorageMessageCode.QUERY_ERROR)
        }
    }

    override fun copy(digest: String, fromCredentials: StorageCredentials?, toCredentials: StorageCredentials?) {
        val path = fileLocator.locate(digest)
        val from = getCredentialsOrDefault(fromCredentials)
        val to = getCredentialsOrDefault(toCredentials)
        try {
            if (from == to) {
                logger.info("Source and destination credentials are same, skip copy file [$digest]")
                return
            }
            if (doExist(path, digest, to)) {
                logger.info("File [$digest] exist on destination credentials, skip copy file.")
                return
            }
            fileStorage.copy(path, digest, from, to)
            logger.info("Success to copy file [$digest] from [${from.key}] to [${to.key}]")
        } catch (exception: Exception) {
            logger.error("Failed to copy file [$digest] from [${from.key}] to [${to.key}]", exception)
            throw StorageErrorException(StorageMessageCode.COPY_ERROR)
        }
    }

    override fun synchronizeFile(storageCredentials: StorageCredentials?) = SynchronizeResult()

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractStorageService::class.java)
    }
}
