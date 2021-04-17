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

package com.tencent.bkrepo.common.storage.credentials

import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.storage.core.StorageProperties
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@EnableConfigurationProperties(value = [StorageProperties::class])
@TestPropertySource(
    locations = ["classpath:storage-test.properties"]
)
internal class StorageCredentialsTest {

    @Autowired
    private lateinit var storageProperties: StorageProperties

    @Test
    fun testConfiguration() {
        Assertions.assertNotNull(storageProperties)
        Assertions.assertEquals(StorageType.INNERCOS, storageProperties.type)
        Assertions.assertEquals("region", storageProperties.innercos.region)
        Assertions.assertEquals("bucket", storageProperties.innercos.bucket)
        Assertions.assertEquals("secretId", storageProperties.innercos.secretId)
        Assertions.assertEquals("secretKey", storageProperties.innercos.secretKey)
        Assertions.assertEquals(1001, storageProperties.innercos.modId)
        Assertions.assertEquals(2002, storageProperties.innercos.cmdId)
        Assertions.assertEquals(1.0F, storageProperties.innercos.timeout)
        Assertions.assertTrue(storageProperties.innercos.cache.enabled)
        Assertions.assertEquals("/data/cached", storageProperties.innercos.cache.path)
        Assertions.assertEquals(10, storageProperties.innercos.cache.expireDays)
        Assertions.assertEquals("/data/temp", storageProperties.innercos.upload.location)
    }

    @Test
    fun testSerialize() {
        val originalCredentials = FileSystemCredentials()
        originalCredentials.path = "test"
        originalCredentials.cache.enabled = true
        originalCredentials.cache.path = "cache-test"
        originalCredentials.cache.expireDays = 10
        val jsonString = originalCredentials.toJsonString()

        val serializedCredentials = jsonString.readJsonString<StorageCredentials>()
        Assertions.assertTrue(serializedCredentials is FileSystemCredentials)
        serializedCredentials as FileSystemCredentials
        Assertions.assertEquals(originalCredentials.path, serializedCredentials.path)
        Assertions.assertEquals(originalCredentials.cache.enabled, serializedCredentials.cache.enabled)
        Assertions.assertEquals(originalCredentials.cache.path, serializedCredentials.cache.path)
        Assertions.assertEquals(originalCredentials.cache.expireDays, serializedCredentials.cache.expireDays)
        Assertions.assertEquals(originalCredentials.upload.location, serializedCredentials.upload.location)
        Assertions.assertEquals(jsonString, serializedCredentials.toJsonString())
    }

    @Test
    fun testGetTempPathTwice() {
        val path1 = System.getProperty("java.io.tmpdir")
        val path2 = System.getProperty("java.io.tmpdir")
        // should be same
        Assertions.assertEquals(path1, path2)
    }
}
