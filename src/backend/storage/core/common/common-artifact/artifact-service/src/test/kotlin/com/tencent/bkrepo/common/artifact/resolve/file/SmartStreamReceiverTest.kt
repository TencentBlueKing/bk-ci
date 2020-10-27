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

import com.google.common.util.concurrent.RateLimiter
import com.tencent.bkrepo.common.api.constant.StringPool.randomString
import com.tencent.bkrepo.common.artifact.stream.DigestCalculateListener
import com.tencent.bkrepo.common.storage.util.toPath
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.util.unit.DataSize
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Files
import kotlin.concurrent.thread

internal class SmartStreamReceiverTest {

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
        val receiver = SmartStreamReceiver(
            DataSize.ofBytes(DEFAULT_BUFFER_SIZE.toLong()).toBytes(),
            filename,
            primaryPath,
            true
        )
        val source = shortContent.byteInputStream()
        receiver.receive(source, DigestCalculateListener())

        Assertions.assertTrue(receiver.isInMemory)
        Assertions.assertFalse(Files.exists(primaryPath.resolve(filename)))
        Assertions.assertFalse(Files.exists(fallbackPath.resolve(filename)))
        Assertions.assertFalse(receiver.fallback)

        val memoryContent = receiver.getCachedByteArray().toString(Charset.defaultCharset())
        Assertions.assertEquals(shortContent, memoryContent)
    }

    /**
     * 测试正常情况下，接收超过阈值大小的数据，数据将存放到primaryPath中
     */
    @Test
    fun testNormalInFile() {
        val receiver = SmartStreamReceiver(
            DataSize.ofBytes(DEFAULT_BUFFER_SIZE - 1L).toBytes(),
            filename,
            primaryPath,
            true
        )
        val source = shortContent.byteInputStream()
        receiver.receive(source, DigestCalculateListener())

        Assertions.assertFalse(receiver.isInMemory)
        Assertions.assertTrue(Files.exists(primaryPath.resolve(filename)))
        Assertions.assertFalse(Files.exists(fallbackPath.resolve(filename)))
        Assertions.assertFalse(receiver.fallback)

        val fileContent = primaryPath.resolve(filename).toFile().readText(Charset.defaultCharset())
        Assertions.assertEquals(shortContent, fileContent)
    }

    /**
     * 开启enableTransfer
     * 当数据在内存中触发fallback，数据应该也要写入fallbackPath
     */
    @Test
    fun testFallbackInMemory() {
        val receiver = SmartStreamReceiver(
            DataSize.ofBytes(DEFAULT_BUFFER_SIZE * 10L).toBytes(),
            filename,
            primaryPath,
            true
        )
        val source = longContent.byteInputStream()
        // 限速的inputStream，平均一秒只能读取DEFAULT_BUFFER_SIZE大小的数据
        val rateLimitInputStream = RateLimitInputStream(source, DEFAULT_BUFFER_SIZE)
        thread {
            // 此时还未超过内存阈值
            Thread.sleep(5 * 1000)
            // 手动模拟unhealthy
            receiver.unhealthy(fallbackPath, "IO Delay")
        }
        receiver.receive(rateLimitInputStream, DigestCalculateListener())

        Assertions.assertFalse(receiver.isInMemory)
        Assertions.assertFalse(Files.exists(primaryPath.resolve(filename)))
        Assertions.assertTrue(Files.exists(fallbackPath.resolve(filename)))
        Assertions.assertTrue(receiver.fallback)

        val fileContent = fallbackPath.resolve(filename).toFile().readText(Charset.defaultCharset())
        Assertions.assertEquals(longContent, fileContent)
    }

    /**
     * 开启enableTransfer
     * 当数据落入primaryPath时触发fallback，数据应该转移到fallbackPath
     */
    @Test
    fun testFallbackInFile() {
        val receiver = SmartStreamReceiver(
            DataSize.ofBytes(DEFAULT_BUFFER_SIZE * 10L).toBytes(),
            filename,
            primaryPath,
            true
        )
        val source = longContent.byteInputStream()
        val rateLimitInputStream = RateLimitInputStream(source, DEFAULT_BUFFER_SIZE)
        thread {
            // 确保已经超过内存阈值
            Thread.sleep(15 * 1000)
            receiver.unhealthy(fallbackPath, "IO Delay")
        }
        receiver.receive(rateLimitInputStream, DigestCalculateListener())

        Assertions.assertFalse(receiver.isInMemory)
        Assertions.assertFalse(Files.exists(primaryPath.resolve(filename)))
        Assertions.assertTrue(Files.exists(fallbackPath.resolve(filename)))
        Assertions.assertTrue(receiver.fallback)

        val fileContent = fallbackPath.resolve(filename).toFile().readText(Charset.defaultCharset())
        Assertions.assertEquals(longContent, fileContent)
    }

    /**
     * 关闭enableTransfer
     * 当数据在内存中触发fallback，数据应该也要写入fallbackPath
     */
    @Test
    fun testFallbackInMemoryWithDisableTransfer() {
        val receiver = SmartStreamReceiver(
            DataSize.ofBytes(DEFAULT_BUFFER_SIZE * 10L).toBytes(),
            filename,
            primaryPath,
            false
        )
        val source = longContent.byteInputStream()
        val rateLimitInputStream = RateLimitInputStream(source, DEFAULT_BUFFER_SIZE)
        thread {
            // 此时还未超过内存阈值
            Thread.sleep(5 * 1000)
            receiver.unhealthy(fallbackPath, "IO Delay")
        }
        receiver.receive(rateLimitInputStream, DigestCalculateListener())

        Assertions.assertFalse(receiver.isInMemory)

        Assertions.assertFalse(Files.exists(primaryPath.resolve(filename)))
        Assertions.assertTrue(Files.exists(fallbackPath.resolve(filename)))
        Assertions.assertTrue(receiver.fallback)

        val fileContent = fallbackPath.resolve(filename).toFile().readText(Charset.defaultCharset())
        Assertions.assertEquals(longContent, fileContent)
    }

    /**
     * 关闭enableTransfer
     * 当数据落入primaryPath时触发fallback，数据应该继续写入primaryPath
     */
    @Test
    fun testFallbackInFileWithDisableTransfer() {
        val receiver = SmartStreamReceiver(
            DataSize.ofBytes(DEFAULT_BUFFER_SIZE * 10L).toBytes(),
            filename,
            primaryPath,
            false
        )
        val source = longContent.byteInputStream()
        val rateLimitInputStream = RateLimitInputStream(source, DEFAULT_BUFFER_SIZE)
        thread {
            // 确保已经超过内存阈值
            Thread.sleep(15 * 1000)
            receiver.unhealthy(fallbackPath, "IO Delay")
        }
        receiver.receive(rateLimitInputStream, DigestCalculateListener())

        Assertions.assertFalse(receiver.isInMemory)
        Assertions.assertTrue(Files.exists(primaryPath.resolve(filename)))
        Assertions.assertFalse(Files.exists(fallbackPath.resolve(filename)))
        Assertions.assertTrue(receiver.fallback)

        val fileContent = primaryPath.resolve(filename).toFile().readText(Charset.defaultCharset())
        Assertions.assertEquals(longContent, fileContent)
    }
}

private class RateLimitInputStream(
    private val source: InputStream,
    speed: Int
) : InputStream() {

    private val rateLimiter = RateLimiter.create(speed.toDouble())

    override fun skip(n: Long): Long {
        return source.skip(n)
    }

    override fun available(): Int {
        return source.available()
    }

    override fun reset() {
        source.reset()
    }

    override fun close() {
        source.close()
    }

    override fun mark(readlimit: Int) {
        source.mark(readlimit)
    }

    override fun markSupported(): Boolean {
        return source.markSupported()
    }

    override fun read(): Int {
        rateLimiter.acquire(1)
        return source.read()
    }

    override fun read(b: ByteArray): Int {
        rateLimiter.acquire(b.size)
        return source.read(b)
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        rateLimiter.acquire(len)
        return source.read(b, off, len)
    }
}
