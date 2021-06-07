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

package com.tencent.bkrepo.common.storage.core.cache

import com.tencent.bkrepo.common.artifact.stream.StreamReadListener
import com.tencent.bkrepo.common.artifact.stream.closeQuietly
import com.tencent.bkrepo.common.artifact.stream.releaseQuietly
import com.tencent.bkrepo.common.storage.util.createFile
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

class CachedFileWriter(
    private val cachePath: Path,
    private val filename: String,
    tempPath: Path
) : StreamReadListener {

    private val lockFilePath = tempPath.resolve(filename.plus(LOCK_SUFFIX))
    private val channel: FileChannel
    private var outputStream: FileOutputStream? = null
    private var lock: FileLock? = null

    init {
        Files.createDirectories(tempPath)
        channel = FileChannel.open(lockFilePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        try {
            lock = channel.tryLock()
            outputStream = lockFilePath.toFile().outputStream()
            assert(lock != null)
        } catch (ignored: Exception) {
            outputStream?.closeQuietly()
            channel.closeQuietly()
            lock = null
        }
    }

    override fun data(i: Int) {
        if (lock != null) {
            outputStream?.write(i)
        }
    }

    override fun data(buffer: ByteArray, length: Int) {
        if (lock != null) {
            outputStream?.write(buffer, 0, length)
        }
    }

    override fun finish() {
        if (lock != null) {
            outputStream?.flush()
            outputStream?.closeQuietly()
            channel.closeQuietly()
            val cacheFilePath = cachePath.resolve(filename).apply { createFile() }
            Files.move(lockFilePath, cacheFilePath, StandardCopyOption.REPLACE_EXISTING)
            lock?.releaseQuietly()
            lock = null
        }
    }

    override fun close() {
        if (lock != null) {
            outputStream?.flush()
            outputStream?.closeQuietly()
            channel.closeQuietly()
            lock?.releaseQuietly()
            lock = null
        }
    }

    companion object {
        private const val LOCK_SUFFIX = ".lock"
    }
}
