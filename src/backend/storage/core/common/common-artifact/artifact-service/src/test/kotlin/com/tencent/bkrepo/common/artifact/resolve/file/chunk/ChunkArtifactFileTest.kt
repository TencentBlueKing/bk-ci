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

package com.tencent.bkrepo.common.artifact.resolve.file.chunk

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.storage.config.UploadProperties
import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.common.storage.core.config.ReceiveProperties
import com.tencent.bkrepo.common.storage.credentials.FileSystemCredentials
import com.tencent.bkrepo.common.storage.monitor.StorageHealthMonitor
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.util.unit.DataSize
import java.nio.charset.Charset

// TODO 现在Mockito版本不能mock静态方法，需要升级框架版本
class ChunkArtifactFileTest {

    private val tempDir = System.getProperty("java.io.tmpdir")

    private val uploadProperties = UploadProperties(location = tempDir)

    private val storageCredentials = FileSystemCredentials(upload = uploadProperties)

    private fun buildArtifactFile(): ChunkedArtifactFile {
        val storageProperties = StorageProperties(
            filesystem = storageCredentials,
            receive = ReceiveProperties(fileSizeThreshold = DataSize.ofBytes(DEFAULT_BUFFER_SIZE.toLong()))
        )
        val monitor = StorageHealthMonitor(storageProperties, storageCredentials.upload.location)
        return ChunkedArtifactFile(monitor, storageProperties, storageCredentials)
    }

    @Test
    fun testWriteChunk() {
        val artifactFile = buildArtifactFile()
        val source1 = StringPool.randomString(0)
        val source2 = StringPool.randomString(100)
        val source3 = StringPool.randomString(1024)
        val source4 = StringPool.randomString(1024)
        artifactFile.write(source1.toByteArray(), 0, 0)
        artifactFile.write(source2.toByteArray(), 0, 100)
        artifactFile.write(source3.toByteArray(), 0, 1024)
        artifactFile.write(source4.toByteArray(), 0, 0)
        artifactFile.finish()
        Assertions.assertTrue(artifactFile.isInMemory())
        Assertions.assertEquals(
            source1 + source2 + source3,
            artifactFile.getInputStream().readBytes().toString(Charset.defaultCharset())
        )
    }

    @Test
    fun testReadBeforeFinish() {
        val artifactFile = buildArtifactFile()
        val source = StringPool.randomString(100)
        artifactFile.write(source.toByteArray(), 0, 100)

        assertDoesNotThrow { artifactFile.isInMemory() }
        assertThrows<IllegalArgumentException> { artifactFile.getInputStream() }
    }
}
