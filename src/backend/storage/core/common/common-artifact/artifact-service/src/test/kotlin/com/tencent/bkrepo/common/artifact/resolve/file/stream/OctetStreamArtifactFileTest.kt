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

package com.tencent.bkrepo.common.artifact.resolve.file.stream

import com.tencent.bkrepo.common.api.constant.StringPool.randomString
import com.tencent.bkrepo.common.storage.config.UploadProperties
import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.common.storage.credentials.FileSystemCredentials
import com.tencent.bkrepo.common.storage.monitor.MonitorProperties
import com.tencent.bkrepo.common.storage.monitor.StorageHealthMonitor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.util.unit.DataSize
import java.io.InputStream
import java.nio.charset.Charset

class OctetStreamArtifactFileTest {

    private val tempDir = System.getProperty("java.io.tmpdir")

    private val uploadProperties = UploadProperties(location = tempDir)

    private val storageCredentials = FileSystemCredentials(upload = uploadProperties)

    private fun buildArtifactFile(source: InputStream, threshold: Long): OctetStreamArtifactFile {
        val storageProperties = StorageProperties(
            filesystem = storageCredentials,
            fileSizeThreshold = DataSize.ofBytes(threshold),
            monitor = MonitorProperties()
        )
        val monitor = StorageHealthMonitor(storageProperties)
        return OctetStreamArtifactFile(source, monitor, storageProperties, storageCredentials)
    }

    @Test
    fun testZeroThreshold() {
        val source = randomString(0).byteInputStream()
        val artifactFile = buildArtifactFile(source, 0)
        assertTrue(artifactFile.isInMemory())
    }

    @Test
    fun testInMemory() {
        val randomString = randomString(10)
        val source = randomString.byteInputStream()
        val artifactFile = buildArtifactFile(source, 11)
        assertTrue(artifactFile.isInMemory())
        val artifactFileString = artifactFile.getInputStream().readBytes().toString(Charset.defaultCharset())
        assertEquals(randomString, artifactFileString)
    }

    @Test
    fun testInFile() {
        val randomString = randomString(10)
        val source = randomString.byteInputStream()
        val artifactFile = buildArtifactFile(source, 9)
        assertFalse(artifactFile.isInMemory())
        assertTrue(artifactFile.getFile()!!.exists())
        val artifactFileString = artifactFile.getInputStream().readBytes().toString(Charset.defaultCharset())
        assertEquals(randomString, artifactFileString)
    }

    @Test
    fun testBigSizeInMemory() {
        val randomString = randomString(1024 * 1024)
        val source = randomString.byteInputStream()
        val artifactFile = buildArtifactFile(source, 1024 * 1024 + 1)
        assertTrue(artifactFile.isInMemory())
        val artifactFileString = artifactFile.getInputStream().readBytes().toString(Charset.defaultCharset())
        assertEquals(randomString, artifactFileString)
    }

    @Test
    fun testBigSizeInFile() {
        val randomString = randomString(1024 * 1024)
        val source = randomString.byteInputStream()
        val artifactFile = buildArtifactFile(source, 1024 * 1024 - 1)
        assertFalse(artifactFile.isInMemory())
        assertTrue(artifactFile.getFile()!!.exists())
        val artifactFileString = artifactFile.getInputStream().readBytes().toString(Charset.defaultCharset())
        assertEquals(randomString, artifactFileString)
    }

    @Test
    fun testDeleteInMemory() {
        val randomString = randomString(10)
        val source = randomString.byteInputStream()
        val artifactFile = buildArtifactFile(source, 10)
        assertTrue(artifactFile.isInMemory())
        assertNull(artifactFile.getFile())
        artifactFile.delete()
    }

    @Test
    fun testDeleteInFile() {
        val randomString = randomString(11)
        val source = randomString.byteInputStream()
        val artifactFile = buildArtifactFile(source, 10)
        assertFalse(artifactFile.isInMemory())
        assertTrue(artifactFile.getFile()!!.exists())
        artifactFile.delete()
        assertFalse(artifactFile.getFile()!!.exists())
    }

    @Test
    fun testFlushToFile() {
        val randomString = randomString(10)
        val source = randomString.byteInputStream()
        val artifactFile = buildArtifactFile(source, 10)
        assertTrue(artifactFile.isInMemory())
        assertNull(artifactFile.getFile())

        val file = artifactFile.flushToFile()
        assertFalse(artifactFile.isInMemory())
        assertTrue(file.exists())
        assertEquals(file.absolutePath, artifactFile.getFile()!!.absolutePath)
        assertEquals(10, file.length())
        file.delete()
        assertFalse(file.exists())
        assertFalse(artifactFile.getFile()!!.exists())
    }
}
