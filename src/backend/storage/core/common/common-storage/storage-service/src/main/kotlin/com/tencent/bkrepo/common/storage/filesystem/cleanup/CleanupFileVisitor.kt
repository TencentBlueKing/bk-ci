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

package com.tencent.bkrepo.common.storage.filesystem.cleanup

import com.google.common.util.concurrent.RateLimiter
import com.tencent.bkrepo.common.api.constant.JOB_LOGGER_NAME
import com.tencent.bkrepo.common.storage.filesystem.FileLockExecutor
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.time.Duration

@Suppress("UnstableApiUsage")
class CleanupFileVisitor(
    private val rootPath: Path,
    private val expireDays: Int
) : SimpleFileVisitor<Path>() {

    val result = CleanupResult()
    private val rateLimiter = RateLimiter.create(permitsPerSecond)

    @Throws(IOException::class)
    override fun visitFile(filePath: Path, attributes: BasicFileAttributes): FileVisitResult {
        val size = attributes.size()
        result.totalFile += 1
        result.totalSize += 1
        if (isExpired(attributes, expireDays)) {
            rateLimiter.acquire()
            FileLockExecutor.executeInLock(filePath.toFile()) {
                Files.delete(filePath)
            }
            result.cleanupFile += 1
            result.cleanupSize += size
            logger.info("Clean up file[$filePath], size[$size], summary: $result")
        }
        return FileVisitResult.CONTINUE
    }

    @Throws(IOException::class)
    override fun postVisitDirectory(dirPath: Path, exc: IOException?): FileVisitResult {
        result.totalFolder += 1
        if (!Files.isSameFile(rootPath, dirPath) && !Files.list(dirPath).iterator().hasNext()) {
            Files.delete(dirPath)
            logger.info("Clean up folder[$dirPath].")
            result.cleanupFolder += 1
        }
        return FileVisitResult.CONTINUE
    }

    /**
     * 判断文件是否过期
     * 根据上次访问时间和上次修改时间判断
     */
    private fun isExpired(attributes: BasicFileAttributes, expireDays: Int): Boolean {
        val lastAccessTime = attributes.lastAccessTime().toMillis()
        val lastModifiedTime = attributes.lastModifiedTime().toMillis()
        val expiredTime = System.currentTimeMillis() - Duration.ofDays(expireDays.toLong()).toMillis()
        return lastAccessTime < expiredTime && lastModifiedTime < expiredTime
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JOB_LOGGER_NAME)
        private const val permitsPerSecond = 30.0
    }
}
