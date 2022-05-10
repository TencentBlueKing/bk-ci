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

package com.tencent.bkrepo.common.artifact.resolve.file

import com.tencent.bkrepo.common.artifact.exception.ArtifactReceiveException
import com.tencent.bkrepo.common.artifact.hash.sha256
import com.tencent.bkrepo.common.artifact.stream.DigestCalculateListener
import com.tencent.bkrepo.common.artifact.stream.rateLimit
import com.tencent.bkrepo.common.artifact.util.http.IOExceptionUtils
import com.tencent.bkrepo.common.storage.core.config.ReceiveProperties
import com.tencent.bkrepo.common.storage.core.locator.HashFileLocator
import com.tencent.bkrepo.common.storage.innercos.retry
import com.tencent.bkrepo.common.storage.monitor.MonitorProperties
import com.tencent.bkrepo.common.storage.monitor.StorageHealthMonitor
import com.tencent.bkrepo.common.storage.monitor.Throughput
import com.tencent.bkrepo.common.storage.util.createFile
import com.tencent.bkrepo.common.storage.util.delete
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths
import java.security.SecureRandom
import kotlin.math.abs

/**
 * artifact数据接收类，作用：
 * 1. 负责接收输入数据
 * 2. 根据动态阈值将不同大小文件的存储链路进行分离，小文件内存缓存，大文件落磁盘
 * 3. 利用观察者模式，当存储降级时，自动切换到本地磁盘
 * 4. 接收数据同时计算md5和sha256，节省io操作
 * 5. 统计接收速率
 *
 */
class ArtifactDataReceiver(
    private val receiveProperties: ReceiveProperties,
    private val monitorProperties: MonitorProperties,
    private var path: Path,
    private val filename: String = generateRandomName(),
    private val randomPath: Boolean = false,
    private val originPath: Path = path
) : StorageHealthMonitor.Observer, AutoCloseable {

    /**
     * 传输过程中发生存储降级时，是否将数据转移到本地磁盘
     */
    private val enableTransfer = monitorProperties.enableTransfer

    /**
     * 数据传输buffer大小
     */
    private val bufferSize = receiveProperties.bufferSize.toBytes().toInt()

    /**
     * 动态阈值，超过该阈值将数据落磁盘
     */
    private val fileSizeThreshold = receiveProperties.fileSizeThreshold.toBytes()

    /**
     * 内存缓存数组
     */
    private val contentBytes = ByteArrayOutputStream(bufferSize)

    /**
     * outputStream，初始化指向内存缓存数组
     */
    private var outputStream: OutputStream = contentBytes

    /**
     * 数据是否转移到本地磁盘
     */
    private var hasTransferred: Boolean = false

    /**
     * 降级存储路径，通常是本地磁盘路径
     */
    private var fallBackPath: Path? = null

    /**
     * 缓存数组
     */
    val cachedByteArray: ByteArray
        get() = contentBytes.toByteArray()

    /**
     * 接收文件路径
     */
    val filePath: Path
        get() = path.resolve(filename)

    /**
     * 数据摘要计算监听类
     */
    val listener = DigestCalculateListener()

    /**
     * 文件数据是否在内存缓存
     */
    var inMemory: Boolean = true

    /**
     * 是否发生降级
     */
    var fallback: Boolean = false

    /**
     * 接收开始时间
     */
    var startTime = 0L

    /**
     * 接收结束时间
     */
    var endTime = 0L

    /**
     * 接收字节数
     */
    var received = 0L

    /**
     * 接收是否完成
     */
    var finished = false

    init {
        initPath()
    }

    override fun unhealthy(fallbackPath: Path?, reason: String?) {
        if (!finished && !fallback) {
            fallBackPath = fallbackPath
            fallback = true
            logger.warn("Path[$path] is unhealthy, fallback to use [$fallBackPath], reason: $reason")
        }
    }

    /**
     * 接收数据块
     * @param chunk 字节数组
     * @param offset 偏移量
     * @param length 数据长度
     */
    fun receiveChunk(chunk: ByteArray, offset: Int, length: Int) {
        require(!finished) { "Receiver is close" }
        if (startTime == 0L) {
            startTime = System.nanoTime()
        }
        try {
            writeData(chunk, offset, length)
        } catch (exception: IOException) {
            handleIOException(exception)
        }
    }

    /**
     * 接受单个字节数据
     * @param b 字节数据
     * */
    fun receive(b: Int) {
        require(!finished) { "Receiver is close" }
        if (startTime == 0L) {
            startTime = System.nanoTime()
        }
        try {
            checkFallback()
            outputStream.write(b)
            listener.data(b)
            received += 1
            checkThreshold()
        } catch (exception: IOException) {
            handleIOException(exception)
        }
    }

    /**
     * 接收数据流
     * @param source 数据流
     */
    fun receiveStream(source: InputStream) {
        require(!finished) { "Receiver is close" }
        if (startTime == 0L) {
            startTime = System.nanoTime()
        }
        try {
            val input = source.rateLimit(receiveProperties.rateLimit.toBytes())
            val buffer = ByteArray(bufferSize)
            input.use {
                var bytes = input.read(buffer)
                while (bytes >= 0) {
                    writeData(buffer, 0, bytes)
                    bytes = input.read(buffer)
                }
            }
        } catch (exception: IOException) {
            handleIOException(exception)
        }
    }

    /**
     * 数据接收完成,当数据传输完毕后需要调用该函数
     */
    fun finish(): Throughput {
        if (!finished) {
            try {
                finished = true
                endTime = System.nanoTime()
                checkSize()
                listener.finished()
            } finally {
                cleanOriginalOutputStream()
            }
        }
        return Throughput(received, endTime - startTime)
    }

    /**
     * 将内存数据写入到磁盘中
     * @param closeStream 写入后是否关闭原始output stream, 当用户主动触发时，需要设置为true
     */
    @Synchronized
    fun flushToFile(closeStream: Boolean = true) {
        if (inMemory) {
            val filePath = this.filePath.apply { this.createFile() }
            val fileOutputStream = Files.newOutputStream(filePath)
            contentBytes.writeTo(fileOutputStream)
            outputStream = fileOutputStream
            inMemory = false

            if (closeStream) {
                cleanOriginalOutputStream()
            }
        }
    }

    /**
     * 获取文件流，使用完需要手动关闭
     */
    fun getInputStream(): InputStream {
        require(finished) { "Receiver is not finished" }
        return if (!inMemory) {
            Files.newInputStream(filePath)
        } else {
            ByteArrayInputStream(contentBytes.toByteArray())
        }
    }

    /**
     * 关闭原始输出流
     */
    private fun cleanOriginalOutputStream() {
        try {
            outputStream.flush()
        } catch (ignored: IOException) {
        }

        try {
            outputStream.close()
        } catch (ignored: IOException) {
        }
    }

    /**
     * 写入数据
     * @param buffer 字节数组
     * @param offset 偏移量
     * @param length 数据长度
     */
    private fun writeData(buffer: ByteArray, offset: Int, length: Int) {
        checkFallback()
        outputStream.write(buffer, offset, length)
        listener.data(buffer, offset, length)
        received += length
        checkThreshold()
    }

    /**
     * 处理IO异常
     */
    private fun handleIOException(exception: IOException) {
        finished = true
        endTime = System.nanoTime()
        close()
        if (IOExceptionUtils.isClientBroken(exception)) {
            throw ArtifactReceiveException(exception.message.orEmpty())
        } else throw exception
    }

    /**
     * 检查是否需要fall back操作
     */
    private fun checkFallback() {
        if (!fallback || hasTransferred) {
            return
        }
        if (fallBackPath == null || fallBackPath == path) {
            logger.info("Fallback path is null or equals to primary path, skip transfer data")
            hasTransferred = true
            return
        }
        // originalPath表示NFS位置， fallBackPath表示本地磁盘位置
        val originalPath = path
        // 更新当前path为本地磁盘
        path = fallBackPath!!
        // transfer date
        if (!inMemory) {
            // 当文件已经落到NFS
            if (enableTransfer) {
                // 开Transfer功能时，从NFS转移到本地盘
                cleanOriginalOutputStream()
                val originalFile = originalPath.resolve(filename)
                val filePath = this.filePath.apply { this.createFile() }
                originalFile.toFile().inputStream().use {
                    outputStream = filePath.toFile().outputStream()
                    it.copyTo(outputStream, bufferSize)
                }
                Files.deleteIfExists(originalFile)
                logger.info("Success to transfer data from [$originalPath] to [$path]")
            } else {
                // 禁用Transfer功能时，忽略操作，继续使用NFS
                path = originalPath
            }
        }
        hasTransferred = true
    }

    /**
     * 检查文件接受阈值，超过内存阈值时将写入文件中，
     * 同时检查是否超过本地上传阈值，如果未超过，则使用本地磁盘
     */
    private fun checkThreshold() {
        if (inMemory && received > fileSizeThreshold) {
            flushToFile(false)
        }
    }

    /**
     * 接收完毕后，检查接收到的字节数和实际的字节数是否一致
     * 生产环境中出现过不一致的情况，所以加此校验
     */
    private fun checkSize() {
        if (inMemory) {
            val actualSize = contentBytes.size().toLong()
            require(received == actualSize) {
                "$received bytes received, but $actualSize bytes saved in memory."
            }
        } else {
            retry(times = RETRY_CHECK_TIMES, delayInSeconds = 1) {
                val actualSize = Files.size(this.filePath)
                require(received == actualSize) {
                    "$received bytes received, but $actualSize bytes saved in file."
                }
            }
        }
    }

    /**
     * 删除临时文件，如果使用了随机目录，则会删除生成的随机目录
     * */
    private fun deleteTempFile() {
        if (!inMemory) {
            var tempPath = filePath
            while (tempPath != originPath) {
                if (!tempPath.delete()) {
                    // 说明当前目录下还有目录或者文件，则不继续清理
                    return
                }
                logger.info("Delete path $tempPath")
                tempPath = tempPath.parent
            }
        }
    }

    /**
     * 关闭接收器，清理资源
     * */
    override fun close() {
        try {
            cleanOriginalOutputStream()
            deleteTempFile()
        } catch (ignored: NoSuchFileException) {
            // already deleted
        }
    }

    /**
     * 生成随机文件路径
     * */
    private fun generateRandomPath(root: Path, filename: String): Path {
        val fileLocator = HashFileLocator()
        val dir = fileLocator.locate(filename.sha256())
        return Paths.get(root.toFile().path, dir)
    }

    /**
     * 如果开启了随机路径，则进行初始化path
     * */
    private fun initPath() {
        if (randomPath) {
            path = generateRandomPath(path, filename)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArtifactDataReceiver::class.java)
        private val random = SecureRandom()
        private const val RETRY_CHECK_TIMES = 3
        private const val ARTIFACT_PREFIX = "artifact_"
        private const val ARTIFACT_SUFFIX = ".temp"

        /**
         * 生成随机文件名
         */
        private fun generateRandomName(): String {
            var randomLong = random.nextLong()
            randomLong = if (randomLong == Long.MIN_VALUE) 0 else abs(randomLong)
            return ARTIFACT_PREFIX + randomLong.toString() + ARTIFACT_SUFFIX
        }
    }
}
