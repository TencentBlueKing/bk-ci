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

import com.tencent.bkrepo.common.api.exception.BadRequestException
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.pojo.configuration.local.LocalConfiguration
import com.tencent.bkrepo.common.storage.credentials.FileSystemCredentials
import com.tencent.bkrepo.common.storage.credentials.InnerCosCredentials
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.repository.UT_PROJECT_ID
import com.tencent.bkrepo.repository.UT_REGION
import com.tencent.bkrepo.repository.UT_REPO_DESC
import com.tencent.bkrepo.repository.UT_REPO_DISPLAY
import com.tencent.bkrepo.repository.UT_REPO_NAME
import com.tencent.bkrepo.repository.UT_STORAGE_CREDENTIALS_KEY
import com.tencent.bkrepo.repository.UT_USER
import com.tencent.bkrepo.repository.dao.FileReferenceDao
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.pojo.credendials.StorageCredentialsCreateRequest
import com.tencent.bkrepo.repository.pojo.credendials.StorageCredentialsUpdateRequest
import com.tencent.bkrepo.repository.pojo.project.ProjectCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import com.tencent.bkrepo.repository.service.repo.ProjectService
import com.tencent.bkrepo.repository.service.repo.RepositoryService
import com.tencent.bkrepo.repository.service.repo.StorageCredentialService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.context.annotation.Import

@Import(NodeDao::class, FileReferenceDao::class)
@DisplayName("存储身份凭证服务测试")
@DataMongoTest
internal class StorageCredentialServiceTest @Autowired constructor(
    private val storageCredentialService: StorageCredentialService,
    private val projectService: ProjectService,
    private val repositoryService: RepositoryService
) : ServiceBaseTest() {

    @BeforeEach
    fun beforeEach() {
        storageCredentialService.forceDelete(UT_STORAGE_CREDENTIALS_KEY)
    }

    @Test
    fun testCreate() {
        val credential = createCredential(type = FileSystemCredentials.type) as FileSystemCredentials
        val dbCredentials = storageCredentialService.findByKey(UT_STORAGE_CREDENTIALS_KEY)
        Assertions.assertNotNull(dbCredentials)
        Assertions.assertTrue(dbCredentials is FileSystemCredentials)
        dbCredentials as FileSystemCredentials
        assertEquals(credential.path, dbCredentials.path)
        assertEquals(credential.cache.enabled, dbCredentials.cache.enabled)
        assertEquals(credential.cache.path, dbCredentials.cache.path)
        assertEquals(credential.cache.expireDays, dbCredentials.cache.expireDays)

        assertThrows<ErrorCodeException> {
            createCredential()
        }

        assertThrows<ErrorCodeException> {
            val createRequest1 = StorageCredentialsCreateRequest("   ", credential, UT_REGION)
            storageCredentialService.create(UT_USER, createRequest1)
        }
    }

    @Test
    fun testCreateDifferentTypeCredential() {
        val credential = InnerCosCredentials()
        val createRequest = StorageCredentialsCreateRequest(UT_STORAGE_CREDENTIALS_KEY, credential, UT_REGION)
        assertThrows<ErrorCodeException> {
            storageCredentialService.create(UT_USER, createRequest)
        }
    }

    @Test
    fun testUpdateCredential() {
        val storageCredentials = createCredential()
        assertEquals(true, storageCredentials.cache.loadCacheFirst)
        assertEquals(10, storageCredentials.cache.expireDays)
        assertEquals(UT_STORAGE_CREDENTIALS_KEY, storageCredentials.key)

        var updateCredentialsPayload = storageCredentials.apply {
            cache = cache.copy(loadCacheFirst = false, expireDays = -1)
        }
        var updateReq = StorageCredentialsUpdateRequest(updateCredentialsPayload, UT_STORAGE_CREDENTIALS_KEY)
        var updatedStorageCredentials = storageCredentialService.update(UT_USER, updateReq)
        assertEquals(false, updatedStorageCredentials.cache.loadCacheFirst)
        assertEquals(-1, updatedStorageCredentials.cache.expireDays)
        assertEquals(storageCredentials.upload.localPath, updatedStorageCredentials.upload.localPath)
        assertEquals(UT_STORAGE_CREDENTIALS_KEY, updatedStorageCredentials.key)

        val localPath = "/test"
        updateCredentialsPayload = storageCredentials.apply {
            cache = cache.copy(loadCacheFirst = true, expireDays = 10)
            upload = upload.copy(localPath = localPath)
        }
        updateReq = StorageCredentialsUpdateRequest(updateCredentialsPayload, UT_STORAGE_CREDENTIALS_KEY)
        updatedStorageCredentials = storageCredentialService.update(UT_USER, updateReq)
        assertEquals(localPath, updatedStorageCredentials.upload.localPath)
        assertEquals(true, updatedStorageCredentials.cache.loadCacheFirst)
        assertEquals(10, updatedStorageCredentials.cache.expireDays)
        assertEquals(localPath, updatedStorageCredentials.upload.localPath)
        assertEquals(UT_STORAGE_CREDENTIALS_KEY, updatedStorageCredentials.key)
    }

    @Test
    fun testDelete() {
        val credential = createCredential()
        storageCredentialService.delete(credential.key!!)
        assertEquals(null, storageCredentialService.findByKey(credential.key!!))
    }

    @Test
    fun testDeleteNotExistsCredential() {
        assertThrows<NotFoundException> {
            storageCredentialService.delete("KeyOfNotExistsCredential")
        }
    }

    @Test
    fun testDeleteUsedCredential() {
        val credential = createCredential()
        createRepository(credential.key!!)
        assertThrows<BadRequestException> {
            storageCredentialService.delete(credential.key!!)
        }
    }

    @Test
    fun testList() {
        var list = storageCredentialService.list()
        assertEquals(0, list.size)

        createCredential()
        list = storageCredentialService.list()
        assertEquals(1, list.size)

        createCredential("${UT_STORAGE_CREDENTIALS_KEY}2")
        list = storageCredentialService.list()
        assertEquals(2, list.size)

        createCredential("${UT_STORAGE_CREDENTIALS_KEY}3", "${UT_REGION}2")
        list = storageCredentialService.list()
        assertEquals(3, list.size)

        list = storageCredentialService.list(region = UT_REGION)
        assertEquals(2, list.size)

        list = storageCredentialService.list(region = UT_REGION + "2")
        assertEquals(1, list.size)

        storageCredentialService.forceDelete(UT_STORAGE_CREDENTIALS_KEY + "2")

        list = storageCredentialService.list(region = UT_REGION)
        assertEquals(1, list.size)
    }

    private fun createCredential(
        key: String = UT_STORAGE_CREDENTIALS_KEY,
        region: String = UT_REGION,
        type: String = FileSystemCredentials.type
    ): StorageCredentials {
        val credential = when (type) {
            FileSystemCredentials.type -> {
                FileSystemCredentials().apply {
                    path = "test"
                }
            }
            else -> throw RuntimeException("Unknown credential type: $type")
        }.apply { configCredential(this) }

        val createRequest = StorageCredentialsCreateRequest(key, credential, region)
        return storageCredentialService.create(UT_USER, createRequest)
    }

    private fun configCredential(credential: StorageCredentials) {
        credential.apply {
            cache.enabled = true
            cache.path = "cache-test"
            cache.expireDays = 10
            cache.loadCacheFirst = true
        }
    }

    private fun createRepository(credentialKey: String): RepositoryDetail {
        val projectCreateRequest = ProjectCreateRequest(UT_PROJECT_ID, UT_REPO_NAME, UT_REPO_DISPLAY, UT_USER)
        projectService.createProject(projectCreateRequest)
        val repoCreateRequest = RepoCreateRequest(
            projectId = UT_PROJECT_ID,
            name = UT_REPO_NAME,
            type = RepositoryType.GENERIC,
            category = RepositoryCategory.LOCAL,
            public = false,
            description = UT_REPO_DESC,
            configuration = LocalConfiguration(),
            operator = UT_USER,
            storageCredentialsKey = credentialKey
        )
        return repositoryService.createRepo(repoCreateRequest)
    }
}
