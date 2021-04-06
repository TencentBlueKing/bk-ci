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

package com.tencent.bkrepo.repository.service

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.storage.credentials.FileSystemCredentials
import com.tencent.bkrepo.common.storage.credentials.InnerCosCredentials
import com.tencent.bkrepo.repository.UT_STORAGE_CREDENTIALS_KEY
import com.tencent.bkrepo.repository.UT_USER
import com.tencent.bkrepo.repository.pojo.credendials.StorageCredentialsCreateRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest

@DisplayName("存储身份凭证服务测试")
@DataMongoTest
internal class StorageCredentialServiceTest @Autowired constructor(
    private val storageCredentialService: StorageCredentialService
) : ServiceBaseTest() {

    @BeforeEach
    fun beforeEach() {
        storageCredentialService.delete(UT_STORAGE_CREDENTIALS_KEY)
    }

    @Test
    fun testCreate() {
        val credential = FileSystemCredentials()
        credential.path = "test"
        credential.cache.enabled = true
        credential.cache.path = "cache-test"
        credential.cache.expireDays = 10

        val createRequest = StorageCredentialsCreateRequest(UT_STORAGE_CREDENTIALS_KEY, credential)
        storageCredentialService.create(UT_USER, createRequest)

        val dbCredentials = storageCredentialService.findByKey(UT_STORAGE_CREDENTIALS_KEY)
        Assertions.assertNotNull(dbCredentials)
        Assertions.assertTrue(dbCredentials is FileSystemCredentials)
        dbCredentials as FileSystemCredentials
        Assertions.assertEquals(credential.path, dbCredentials.path)
        Assertions.assertEquals(credential.cache.enabled, dbCredentials.cache.enabled)
        Assertions.assertEquals(credential.cache.path, dbCredentials.cache.path)
        Assertions.assertEquals(credential.cache.expireDays, dbCredentials.cache.expireDays)

        assertThrows<ErrorCodeException> {
            storageCredentialService.create(UT_USER, createRequest)
        }

        assertThrows<ErrorCodeException> {
            val createRequest1 = StorageCredentialsCreateRequest("   ", credential)
            storageCredentialService.create(UT_USER, createRequest1)
        }
    }

    @Test
    fun testCreateDifferentTypeCredential() {
        val credential = InnerCosCredentials()
        val createRequest = StorageCredentialsCreateRequest(UT_STORAGE_CREDENTIALS_KEY, credential)
        assertThrows<ErrorCodeException> {
            storageCredentialService.create(UT_USER, createRequest)
        }
    }

    @Test
    fun testList() {
        var list = storageCredentialService.list()
        Assertions.assertEquals(0, list.size)

        val credential1 = FileSystemCredentials()
        credential1.path = "test"
        credential1.cache.enabled = true
        credential1.cache.path = "cache-test"
        credential1.cache.expireDays = 10

        val createRequest1 = StorageCredentialsCreateRequest(UT_STORAGE_CREDENTIALS_KEY, credential1)
        storageCredentialService.create(UT_USER, createRequest1)

        list = storageCredentialService.list()
        Assertions.assertEquals(1, list.size)

        val credential2 = FileSystemCredentials()
        credential1.path = "test2"
        credential1.cache.enabled = true
        credential1.cache.path = "cache-test2"
        credential1.cache.expireDays = 10

        val createRequest2 = StorageCredentialsCreateRequest(UT_STORAGE_CREDENTIALS_KEY + "2", credential2)
        storageCredentialService.create(UT_USER, createRequest2)

        list = storageCredentialService.list()
        Assertions.assertEquals(2, list.size)

        storageCredentialService.delete(UT_STORAGE_CREDENTIALS_KEY + "2")

        list = storageCredentialService.list()
        Assertions.assertEquals(1, list.size)
    }
}
