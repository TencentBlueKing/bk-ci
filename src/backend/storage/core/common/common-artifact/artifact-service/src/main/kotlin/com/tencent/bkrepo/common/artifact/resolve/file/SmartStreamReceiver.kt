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

package com.tencent.bkrepo.common.artifact.resolve.file

import com.tencent.bkrepo.common.artifact.exception.ArtifactReceiveException
import com.tencent.bkrepo.common.artifact.stream.StreamReceiveListener
import com.tencent.bkrepo.common.storage.monitor.StorageHealthMonitor
import com.tencent.bkrepo.common.storage.monitor.Throughput
import com.tencent.bkrepo.common.storage.util.createFile
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.channels.ClosedChannelException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.measureNanoTime

class SmartStreamReceiver(
    private val fileSizeThreshold: Long,
    private val filename: String,
    private var path: Path,
    private val enableTransfer: Boolean
) : StorageHealthMonitor.Observer {
    private val contentBytes = ByteArrayOutputStream(DEFAULT_BUFFER_SIZE)
    private var outputStream: OutputStream = contentBytes
    private var hasTransferred: Boolean = false
    private var fallBackPath: Path? = null

    var isInMemory: Boolean = true
    var totalSize: Long = 0
    var fallback: Boolean = false

    fun receive(source: InputStream, listener: StreamReceiveListener): Throughput {
        try {
            var bytesCopied: Long = 0
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            val nanoTime = measureNanoTime {
                source.use {
                    var bytes = source.read(buffer)
                    while (bytes >= 0) {
                        checkFallback()
                        outputStream.write(buffer, 0, bytes)
                        listener.data(buffer, 0, bytes)
                        bytesCopied += bytes
                        checkThreshold(bytesCopied)
                        bytes = source.read(buffer)
                    }
                }
            }
            totalSize = bytesCopied
            checkSize()
            listener.finished()
            return Throughput(bytesCopied, nanoTime)
        } catch (exception: IOException) {
            cleanTempFile()
            val message = exception.message.orEmpty()
            when {
                message.contains("Connection reset by peer") -> {
                    throw ArtifactReceiveException(message)
                }
                message.contains("Remote peer closed connection") -> {
                    throw ArtifactReceiveException(message)
                }
                exception is ClosedChannelException -> {
                    throw ArtifactReceiveException("Channel closed")
                }
                else -> throw exception
            }
        } finally {
            cleanOriginalOutputStream()
        }
    }

    fun getCachedByteArray(): ByteArray = contentBytes.toByteArray()

    fun getFilePath(): Path = path.resolve(filename)

    @Synchronized
    fun flushToFile(closeStream: Boolean = true) {
        if (isInMemory) {
            val filePath = path.resolve(filename).apply { this.createFile() }
            val fileOutputStream = Files.newOutputStream(filePath)
            contentBytes.writeTo(fileOutputStream)
            outputStream = fileOutputStream
            isInMemory = false

            if (closeStream) {
                cleanOriginalOutputStream()
            }
        }
    }

    override fun unhealthy(fallbackPath: Path?, reason: String?) {
        if (!fallback) {
            fallBackPath = fallbackPath
            fallback = true
            logger.warn("Path[$path] is unhealthy, fallback to use [$fallBackPath], reason: $reason")
        }
    }

    /**
     * 检查是否需要fall back操作
     */
    private fun checkFallback() {
        if (fallback && !hasTransferred) {
            if (fallBackPath != null && fallBackPath != path) {
                // originalPath表示NFS位置， fallBackPath表示本地磁盘位置
                val originalPath = path
                // 更新当前path为本地磁盘
                path = fallBackPath!!
                // transfer date
                if (!isInMemory) {
                    // 当文件已经落到NFS
                    if (enableTransfer) {
                        // 开Transfer功能时，从NFS转移到本地盘
                        cleanOriginalOutputStream()
                        val originalFile = originalPath.resolve(filename)
                        val filePath = path.resolve(filename).apply { this.createFile() }
                        originalFile.toFile().inputStream().use {
                            outputStream = filePath.toFile().outputStream()
                            it.copyTo(outputStream)
                        }
                        Files.deleteIfExists(originalFile)
                        logger.info("Success to transfer data from [$originalPath] to [$path]")
                    } else {
                        // 禁用Transfer功能时，忽略操作，继续使用NFS
                        path = originalPath
                    }
                }
            } else {
                logger.info("Fallback path is null or equals to primary path, ignore transfer data")
            }
            hasTransferred = true
        }
    }

    /**
     * 检查是否超出内存阈值，超过则将数据写入文件中，并保持outputStream开启
     */
    private fun checkThreshold(bytesCopied: Long) {
        if (isInMemory && bytesCopied > fileSizeThreshold) {
            flushToFile(false)
        }
    }

    private fun checkSize() {
        if (isInMemory) {
            val actualSize = contentBytes.size().toLong()
            require(totalSize == actualSize) {
                "$totalSize bytes received, but $actualSize bytes saved in memory."
            }
        } else {
            val actualSize = Files.size(path.resolve(filename))
            require(totalSize == actualSize) {
                "$totalSize bytes received, but $actualSize bytes saved in file."
            }
        }
    }

    private fun cleanOriginalOutputStream() {
        try {
            outputStream.flush()
        } catch (ignored: IOException) { }

        try {
            outputStream.close()
        } catch (ignored: IOException) { }
    }

    private fun cleanTempFile() {
        if (!isInMemory) {
            try {
                Files.deleteIfExists(path.resolve(filename))
            } catch (ignored: IOException) { }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SmartStreamReceiver::class.java)
    }
}
