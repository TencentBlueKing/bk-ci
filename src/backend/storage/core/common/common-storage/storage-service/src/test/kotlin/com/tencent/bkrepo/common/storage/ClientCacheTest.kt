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

package com.tencent.bkrepo.common.storage

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.tencent.bkrepo.common.storage.credentials.FileSystemCredentials
import com.tencent.bkrepo.common.storage.credentials.InnerCosCredentials
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ClientCacheTest {

    private val clientCache: LoadingCache<FileSystemCredentials, String> by lazy {
        val cacheLoader = object : CacheLoader<FileSystemCredentials, String>() {
            override fun load(credentials: FileSystemCredentials): String = onCreateClient(credentials)
        }
        CacheBuilder.newBuilder().maximumSize(3).build(cacheLoader)
    }

    private fun onCreateClient(credentials: FileSystemCredentials): String {
        return credentials.toString()
    }

    @Test
    fun test() {
        Assertions.assertEquals(0, clientCache.size())

        val credentials = FileSystemCredentials(path = "data")
        clientCache.get(credentials)
        Assertions.assertEquals(1, clientCache.size())
        clientCache.get(credentials)
        Assertions.assertEquals(1, clientCache.size())

        val sameCredentials = FileSystemCredentials(path = "data")
        clientCache.get(sameCredentials)
        Assertions.assertEquals(1, clientCache.size())

        val anotherCredential = FileSystemCredentials(path = "data2")
        clientCache.get(anotherCredential)
        Assertions.assertEquals(2, clientCache.size())

        val credentials3 = FileSystemCredentials(path = "data").apply { upload.location = "123" }
        clientCache.get(credentials3)
        Assertions.assertEquals(3, clientCache.size())

        val credentials4 = FileSystemCredentials(path = "data").apply { upload.location = "1231" }
        clientCache.get(credentials4)
        Assertions.assertEquals(3, clientCache.size())
    }

    @Test
    fun testHashCode() {
        val credentials1 = FileSystemCredentials(path = "data")
        val hashCode1 = credentials1.hashCode()

        val credentials2 = FileSystemCredentials(path = "data")
        val hashCode2 = credentials2.hashCode()
        Assertions.assertEquals(hashCode1, hashCode2)

        val credentials3 = FileSystemCredentials(path = "data2")
        val hashCode3 = credentials3.hashCode()
        Assertions.assertNotEquals(hashCode1, hashCode3)

        val credentials4 = FileSystemCredentials(path = "data")
        credentials4.upload.location = "123"
        val hashCode4 = credentials4.hashCode()
        Assertions.assertNotEquals(hashCode1, hashCode4)
    }

    @Test
    fun testClassEquals() {
        val fsCredentials = FileSystemCredentials()
        val fsCredentials2 = FileSystemCredentials(path = "data")
        val innerCredentials = InnerCosCredentials()

        Assertions.assertEquals(fsCredentials::class, fsCredentials2::class)
        Assertions.assertNotEquals(fsCredentials::class, innerCredentials::class)
    }
}
