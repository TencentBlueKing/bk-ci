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
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.FileSystemArtifactFile
import com.tencent.bkrepo.common.artifact.hash.sha256
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.storage.StorageAutoConfiguration
import com.tencent.bkrepo.common.storage.core.FileStorage
import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.common.storage.core.locator.FileLocator
import com.tencent.bkrepo.common.storage.filesystem.FileSystemClient
import com.tencent.bkrepo.common.storage.filesystem.FileSystemStorage
import com.tencent.bkrepo.common.storage.monitor.StorageHealthMonitor
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.TestPropertySource
import java.io.File
import java.nio.charset.Charset
import java.util.concurrent.CyclicBarrier
import kotlin.concurrent.thread

@SpringBootTest
@ImportAutoConfiguration(StorageAutoConfiguration::class)
@TestPropertySource(locations = ["classpath:storage-cache-fs.properties"])
internal class CacheStorageServiceTest {

    @Autowired
    private lateinit var storageService: StorageService

    @Autowired
    private lateinit var fileStorage: FileStorage

    @Autowired
    private lateinit var fileLocator: FileLocator

    @Autowired
    private lateinit var storageProperties: StorageProperties

    private val cacheClient by lazy { FileSystemClient(storageProperties.filesystem.cache.path) }

    @BeforeEach
    fun beforeEach() {
        // before each
    }

    @AfterEach
    fun afterEach() {
        FileUtils.deleteDirectory(File(storageProperties.filesystem.cache.path))
        FileUtils.deleteDirectory(File(storageProperties.filesystem.upload.location))
        FileUtils.deleteDirectory(File(storageProperties.filesystem.path))
    }

    @Test
    fun `should create correct storage service`() {
        Assertions.assertTrue(storageService is CacheStorageService)
        Assertions.assertTrue(fileStorage is FileSystemStorage)
    }

    @Test
    fun `should save to cache when store`() {
        val size = 1024L
        val artifactFile = createTempArtifactFile(size)
        val sha256 = artifactFile.getFileSha256()
        val path = fileLocator.locate(sha256)
        storageService.store(sha256, artifactFile, null)

        // wait to async store
        Thread.sleep(500)

        // check persist
        Assertions.assertTrue(storageService.exist(sha256, null))

        // check cache
        Assertions.assertTrue(cacheClient.exist(path, sha256))

        // should load from cache
        val artifactInputStream = storageService.load(sha256, Range.full(size), null)
        Assertions.assertNotNull(artifactInputStream)
        Assertions.assertEquals(artifactInputStream!!.sha256(), sha256)
    }

    @Test
    fun `should save to cache when load`() {
        val size = 10240L
        val artifactFile = createTempArtifactFile(size)
        val sha256 = artifactFile.getFileSha256()
        val path = fileLocator.locate(sha256)
        storageService.store(sha256, artifactFile, null)

        // wait to async store
        Thread.sleep(500)

        // check persist
        Assertions.assertTrue(storageService.exist(sha256, null))

        // check cache
        Assertions.assertTrue(cacheClient.exist(path, sha256))

        // remove cache
        cacheClient.delete(path, sha256)
        Assertions.assertFalse(cacheClient.exist(path, sha256))

        // should load from persist
        val artifactInputStream = storageService.load(sha256, Range.full(size.toLong()), null)
        Assertions.assertNotNull(artifactInputStream)
        Assertions.assertEquals(artifactInputStream!!.sha256(), sha256)

        // check cache
        Assertions.assertTrue(cacheClient.exist(path, sha256))
        Assertions.assertEquals(sha256, cacheClient.load(path, sha256)?.sha256())
    }

    @Test
    fun `should cache once when loading concurrently`() {
        val size = 1024L
        val artifactFile = createTempArtifactFile(size)
        val sha256 = artifactFile.getFileSha256()
        val path = fileLocator.locate(sha256)
        storageService.store(sha256, artifactFile, null)

        // wait to async store
        Thread.sleep(500)

        // check cache
        Assertions.assertTrue(cacheClient.exist(path, sha256))

        // check persist
        Assertions.assertTrue(storageService.exist(sha256, null))

        // remove cache
        cacheClient.delete(path, sha256)
        Assertions.assertFalse(cacheClient.exist(path, sha256))

        val count = 10
        val cyclicBarrier = CyclicBarrier(count)
        val threadList = mutableListOf<Thread>()
        repeat(count) {
            val thread = thread {
                cyclicBarrier.await()
                val artifactInputStream = storageService.load(sha256, Range.full(size), null)
                Assertions.assertEquals(artifactInputStream!!.sha256(), sha256)
            }
            threadList.add(thread)
        }
        threadList.forEach { it.join() }
        // check cache
        Assertions.assertTrue(cacheClient.exist(path, sha256))
        Assertions.assertEquals(sha256, cacheClient.load(path, sha256)?.sha256())
    }

    @Test
    fun `should not cache when loading partial content`() {
        val size = 10240L
        val rangeSize = 10
        val artifactFile = createTempArtifactFile(size)
        val buffer = ByteArray(rangeSize)
        artifactFile.getInputStream().use { it.read(buffer, 0, rangeSize) }
        val partialSha256 = buffer.toString(Charset.defaultCharset()).sha256()
        val sha256 = artifactFile.getFileSha256()
        val path = fileLocator.locate(sha256)
        storageService.store(sha256, artifactFile, null)
        // wait to async store
        Thread.sleep(500)
        // remove cache
        cacheClient.delete(path, sha256)

        val count = 10
        val cyclicBarrier = CyclicBarrier(count)
        val threadList = mutableListOf<Thread>()
        repeat(count) {
            val thread = thread {
                cyclicBarrier.await()
                val range = Range(0, rangeSize.toLong() - 1, size)
                val artifactInputStream = storageService.load(sha256, range, null)
                Assertions.assertEquals(artifactInputStream!!.sha256(), partialSha256)
            }
            threadList.add(thread)
        }
        threadList.forEach { it.join() }

        // check cache
        Assertions.assertFalse(cacheClient.exist(path, sha256))
    }

    private fun createTempArtifactFile(size: Long): ArtifactFile {
        val tempFile = createTempFile()
        val content = StringPool.randomString(size.toInt())
        content.byteInputStream().use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return FileSystemArtifactFile(tempFile)
    }

    @TestConfiguration
    class CacheStorageTestConfig {

        @Bean
        fun storageHealthMonitor(storageProperties: StorageProperties): StorageHealthMonitor {
            return StorageHealthMonitor(storageProperties)
        }
    }
}
