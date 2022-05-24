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

package com.tencent.bkrepo.common.artifact.manager

import com.tencent.bkrepo.common.artifact.api.toArtifactFile
import com.tencent.bkrepo.common.artifact.stream.StreamReadListener
import com.tencent.bkrepo.common.artifact.stream.closeQuietly
import com.tencent.bkrepo.common.artifact.stream.releaseQuietly
import com.tencent.bkrepo.common.storage.core.StorageService
import java.io.FileOutputStream
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Files
import java.nio.file.StandardOpenOption

/**
 * 代理拉取数据写入缓存
 */
class ProxyBlobCacheWriter(
    private val storageService: StorageService,
    val digest: String
) : StreamReadListener {

    private val receivedPath = storageService.getTempPath().resolve(digest.plus(LOCK_SUFFIX))
    private val channel = FileChannel.open(receivedPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
    private var outputStream: FileOutputStream? = null
    private var lock: FileLock? = null

    init {
        try {
            lock = channel.tryLock()
            outputStream = receivedPath.toFile().outputStream()
            check(lock != null)
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

    override fun data(buffer: ByteArray, off: Int, length: Int) {
        if (lock != null) {
            outputStream?.write(buffer, off, length)
        }
    }

    override fun finish() {
        if (lock != null) {
            outputStream?.flush()
            outputStream?.closeQuietly()
            channel.closeQuietly()
            storageService.store(digest, receivedPath.toFile().toArtifactFile(), null)
            lock?.releaseQuietly()
            lock = null
        }
    }

    override fun close() {
        if (lock != null) {
            outputStream?.flush()
            outputStream?.closeQuietly()
            Files.deleteIfExists(receivedPath)
            channel.closeQuietly()
            lock?.releaseQuietly()
            lock = null
        }
    }

    companion object {
        private const val LOCK_SUFFIX = ".lock"
    }
}
