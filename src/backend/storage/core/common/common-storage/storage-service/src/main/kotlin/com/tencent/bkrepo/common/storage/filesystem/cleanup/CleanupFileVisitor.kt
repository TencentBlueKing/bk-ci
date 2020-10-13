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

package com.tencent.bkrepo.common.storage.filesystem.cleanup

import com.tencent.bkrepo.common.storage.filesystem.FileLockExecutor
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.time.Duration

class CleanupFileVisitor(
    private val rootPath: Path,
    private val expireDays: Int
) : SimpleFileVisitor<Path>() {

    val cleanupResult = CleanupResult()

    @Throws(IOException::class)
    override fun visitFile(filePath: Path, attributes: BasicFileAttributes): FileVisitResult {
        if (isExpired(attributes, expireDays)) {
            val size = attributes.size()
            FileLockExecutor.executeInLock(filePath.toFile()) {
                Files.delete(filePath)
            }
            cleanupResult.fileCount += 1
            cleanupResult.size += size
        }
        return FileVisitResult.CONTINUE
    }

    @Throws(IOException::class)
    override fun postVisitDirectory(dirPath: Path, exc: IOException?): FileVisitResult {
        if (!Files.isSameFile(rootPath, dirPath) && !Files.list(dirPath).iterator().hasNext()) {
            Files.delete(dirPath)
            cleanupResult.folderCount += 1
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
}
