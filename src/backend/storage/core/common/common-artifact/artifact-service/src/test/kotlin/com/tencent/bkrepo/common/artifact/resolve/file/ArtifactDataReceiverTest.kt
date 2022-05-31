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

import com.tencent.bkrepo.common.api.constant.StringPool.randomString
import com.tencent.bkrepo.common.artifact.stream.RateLimitInputStream
import com.tencent.bkrepo.common.storage.core.config.ReceiveProperties
import com.tencent.bkrepo.common.storage.monitor.MonitorProperties
import com.tencent.bkrepo.common.storage.util.toPath
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.util.unit.DataSize
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import kotlin.concurrent.thread
import org.junit.jupiter.api.DisplayName

internal class ArtifactDataReceiverTest {

    private val primaryPath = "temp".toPath()
    private val fallbackPath = "fallback".toPath()
    private val filename = "testfile"
    private val shortContent = randomString(DEFAULT_BUFFER_SIZE)
    private val longContent = randomString(DEFAULT_BUFFER_SIZE * 20)

    @BeforeEach
    fun initAndClean() {
        Files.createDirectories(primaryPath)
        Files.createDirectories(fallbackPath)
        Files.deleteIfExists(primaryPath.resolve(filename))
        Files.deleteIfExists(fallbackPath.resolve(filename))
    }

    @AfterEach
    fun clean() {
        Files.deleteIfExists(primaryPath.resolve(filename))
        Files.deleteIfExists(fallbackPath.resolve(filename))
    }

    /**
     * 测试正常情况下，接收小于阈值大小的数据，数据将存放到内存中
     */
    @Test
    fun testNormalInMemory() {
        val receiver = createReceiver(
            true,
            DataSize.ofBytes(DEFAULT_BUFFER_SIZE.toLong()).toBytes()
        )
        val source = shortContent.byteInputStream()
        receiver.receiveStream(source)

        Assertions.assertTrue(receiver.inMemory)
        Assertions.assertFalse(Files.exists(primaryPath.resolve(filename)))
        Assertions.assertFalse(Files.exists(fallbackPath.resolve(filename)))
        Assertions.assertFalse(receiver.fallback)

        val memoryContent = receiver.cachedByteArray.toString(Charset.defaultCharset())
        Assertions.assertEquals(shortContent, memoryContent)
    }

    /**
     * 测试正常情况下，接收超过阈值大小的数据，数据将存放到primaryPath中
     */
    @Test
    fun testNormalInFile() {
        val receiver = createReceiver(
            true,
            DataSize.ofBytes(DEFAULT_BUFFER_SIZE - 1L).toBytes()
        )
        val source = shortContent.byteInputStream()
        receiver.receiveStream(source)

        Assertions.assertFalse(receiver.inMemory)
        Assertions.assertTrue(Files.exists(primaryPath.resolve(filename)))
        Assertions.assertFalse(Files.exists(fallbackPath.resolve(filename)))
        Assertions.assertFalse(receiver.fallback)
        Assertions.assertEquals(shortContent, readText(primaryPath.resolve(filename)))
    }

    /**
     * 开启enableTransfer
     * 当数据在内存中触发fallback，数据应该也要写入fallbackPath
     */
    @Test
    fun testFallbackInMemory() {
        val receiver = createReceiver(true)
        val inputStream = createRateLimitInputStream(longContent)
        simulateIODelay(receiver, inputStream, 5 * 1000)
        assertFallbackResult(receiver, true)
    }

    /**
     * 开启enableTransfer
     * 当数据落入primaryPath时触发fallback，数据应该转移到fallbackPath
     */
    @Test
    fun testFallbackInFile() {
        val receiver = createReceiver(true)
        val inputStream = createRateLimitInputStream(longContent)
        simulateIODelay(receiver, inputStream, 15 * 1000)
        assertFallbackResult(receiver, true)
    }

    /**
     * 关闭enableTransfer
     * 当数据在内存中触发fallback，数据应该也要写入fallbackPath
     */
    @Test
    fun testFallbackInMemoryWithDisableTransfer() {
        val receiver = createReceiver(false)
        val inputStream = createRateLimitInputStream(longContent)
        simulateIODelay(receiver, inputStream, 5 * 1000)
        assertFallbackResult(receiver, true)
    }

    /**
     * 关闭enableTransfer
     * 当数据落入primaryPath时触发fallback，数据应该继续写入primaryPath
     */
    @Test
    fun testFallbackInFileWithDisableTransfer() {
        val receiver = createReceiver(false)
        val inputStream = createRateLimitInputStream(longContent)
        simulateIODelay(receiver, inputStream, 15 * 1000)
        assertFallbackResult(receiver, false)
    }

    @Test
    fun testChunkWrite() {
        val receiver = createReceiver(false)
        val source = shortContent.byteInputStream()
        receiver.receiveStream(source)
        val byteArray = shortContent.toByteArray()
        receiver.receiveChunk(shortContent.toByteArray(), 0, byteArray.size)

        Assertions.assertTrue(receiver.inMemory)
        Assertions.assertFalse(Files.exists(primaryPath.resolve(filename)))
        Assertions.assertFalse(Files.exists(fallbackPath.resolve(filename)))
        Assertions.assertFalse(receiver.fallback)

        val memoryContent = receiver.cachedByteArray.toString(Charset.defaultCharset())
        Assertions.assertEquals(shortContent + shortContent, memoryContent)
    }

    @DisplayName("测试文件接受到随机路径下面")
    @Test
    fun testRandomFilePath() {
        val receiver = createReceiver(
            true,
            DataSize.ofBytes(DEFAULT_BUFFER_SIZE.toLong() - 1).toBytes(),
            true
        )
        val source = shortContent.byteInputStream()
        receiver.receiveStream(source)
        receiver.finish()
        Assertions.assertFalse(Files.exists(primaryPath.resolve(filename)))
        Assertions.assertTrue(receiver.filePath.startsWith(primaryPath))
    }

    private fun createRateLimitInputStream(content: String): InputStream {
        return RateLimitInputStream(content.byteInputStream(), DEFAULT_BUFFER_SIZE.toLong())
    }

    private fun readText(path: Path): String {
        return path.toFile().readText()
    }

    private fun simulateIODelay(receiver: ArtifactDataReceiver, inputStream: InputStream, millis: Long) {
        thread {
            // 确保已经超过内存阈值
            Thread.sleep(millis)
            receiver.unhealthy(fallbackPath, "Simulated IO Delay")
        }
        receiver.receiveStream(inputStream)
    }

    /**
     * 对fallback结果进行断言
     * @param transferred 文件数据是否发生转移
     */
    private fun assertFallbackResult(receiver: ArtifactDataReceiver, transferred: Boolean) {
        // 文件数据应该落入文件中，inMemory=false
        Assertions.assertFalse(receiver.inMemory)
        // 是否发生fallback，fallback=true
        Assertions.assertTrue(receiver.fallback)
        // 数据发生转移
        if (transferred) {
            // primaryPath 文件不存在
            Assertions.assertFalse(Files.exists(primaryPath.resolve(filename)))
            // primaryPath 文件存在
            Assertions.assertTrue(Files.exists(fallbackPath.resolve(filename)))
            // 文件数据一直
            Assertions.assertEquals(longContent, readText(fallbackPath.resolve(filename)))
        } else {
            // primaryPath 文件不存在
            Assertions.assertTrue(Files.exists(primaryPath.resolve(filename)))
            // primaryPath 文件存在
            Assertions.assertFalse(Files.exists(fallbackPath.resolve(filename)))
            // 文件数据一直
            Assertions.assertEquals(longContent, readText(primaryPath.resolve(filename)))
        }
    }

    private fun createReceiver(
        enableTransfer: Boolean,
        fileSizeThreshold: Long = DataSize.ofBytes(DEFAULT_BUFFER_SIZE * 10L).toBytes(),
        randomPath: Boolean = false
    ): ArtifactDataReceiver {
        val receive = ReceiveProperties(
            fileSizeThreshold = DataSize.ofBytes(fileSizeThreshold),
            rateLimit = DataSize.ofBytes(-1)
        )
        val monitor = MonitorProperties(
            enableTransfer = enableTransfer
        )
        return ArtifactDataReceiver(receive, monitor, primaryPath, filename, randomPath)
    }
}
