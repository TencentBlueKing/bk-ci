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

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.artifact.hash.sha256
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.artifact.stream.artifactStream
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CyclicBarrier
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

internal class CachedFileWriterTest {

    private val cachePath = Paths.get(System.getProperty("java.io.tmpdir"), "cache")
    private val tempPath = Paths.get(System.getProperty("java.io.tmpdir"), "temp")
    private val filename = "test"

    @AfterEach
    fun afterEach() {
        Files.deleteIfExists(cachePath.resolve(filename))
        Files.deleteIfExists(tempPath.resolve(filename))
    }

    @Test
    fun `test read concurrently`() {
        Assertions.assertFalse { cachePath.resolve(filename).toFile().exists() }

        val size = 1024 * 1024 * 1L
        val randomString = StringPool.randomString(size.toInt())
        val expectedSha256 = randomString.sha256()
        val count = 10
        val cyclicBarrier = CyclicBarrier(count)
        val threadList = mutableListOf<Thread>()
        measureTimeMillis {
            repeat(count) {
                val thread = thread {
                    cyclicBarrier.await()
                    val inputStream = randomString.byteInputStream().artifactStream(Range.full(size))
                    val out = ByteArrayOutputStream()

                    val listener = CachedFileWriter(cachePath, filename, tempPath)
                    inputStream.addListener(listener)
                    inputStream.use { it.copyTo(out) }
                    val toString = out.toString(Charset.defaultCharset().name())
                    Assertions.assertEquals(expectedSha256, toString.sha256())
                }
                threadList.add(thread)
            }
            threadList.forEach { it.join() }
        }.apply { println("duration: $this ms") }

        val sha256 = cachePath.resolve(filename).toFile().sha256()
        Assertions.assertEquals(expectedSha256, sha256)
    }

    @Test
    fun `test close when transferring part of content`() {
        Assertions.assertFalse { Files.exists(cachePath.resolve(filename)) }

        val size = 1024 * 1024 * 1L
        val randomString = StringPool.randomString(size.toInt())
        val inputStream = randomString.byteInputStream().artifactStream(Range.full(size))
        val listener = CachedFileWriter(cachePath, filename, tempPath)
        inputStream.addListener(listener)
        try {
            throwExceptionWhenIOCopy(inputStream, size)
        } catch (ignored: IOException) {
        }
        // should not exist
        Assertions.assertFalse { Files.exists(cachePath.resolve(filename)) }
    }

    @Test
    fun `test part range`() {
        Assertions.assertFalse { Files.exists(cachePath.resolve(filename)) }

        val size = 1024 * 1024 * 1L
        val randomString = StringPool.randomString(size.toInt())
        val range = Range(0, size / 2, size)
        val inputStream = randomString.byteInputStream().artifactStream(range)
        val listener = CachedFileWriter(cachePath, filename, tempPath)
        assertThrows<IllegalArgumentException> { inputStream.addListener(listener) }
    }

    @Test
    fun test() {
        Assertions.assertFalse { Files.exists(cachePath.resolve(filename)) }

        val size = 1024 * 1024 * 1L
        val randomString = StringPool.randomString(size.toInt())
        val expectedSha256 = randomString.sha256()
        val inputStream = randomString.byteInputStream().artifactStream(Range.full(size))
        val out = ByteArrayOutputStream()
        val listener = CachedFileWriter(cachePath, filename, tempPath)
        inputStream.addListener(listener)
        inputStream.use { it.copyTo(out) }
        val toString = out.toString(Charset.defaultCharset().name())
        Assertions.assertEquals(expectedSha256, toString.sha256())
        // should exist
        val cacheFile = cachePath.resolve(filename).toFile()
        Assertions.assertTrue { cacheFile.exists() }
        Assertions.assertEquals(expectedSha256, cacheFile.readText().sha256())
    }

    private fun throwExceptionWhenIOCopy(inputStream: InputStream, size: Long) {
        val out = ByteArrayOutputStream()
        inputStream.use {
            var bytesCopied: Long = 0
            val buffer = ByteArray(1024 * 8)
            var bytes = it.read(buffer)
            while (bytes >= 0) {
                out.write(buffer, 0, bytes)
                bytesCopied += bytes
                bytes = it.read(buffer)
                if (bytesCopied > size / 2) {
                    throw IOException()
                }
            }
        }
    }
}
