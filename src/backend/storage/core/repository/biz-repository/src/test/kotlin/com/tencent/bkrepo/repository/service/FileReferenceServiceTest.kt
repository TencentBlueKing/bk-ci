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

import com.tencent.bkrepo.common.api.constant.StringPool.uniqueId
import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.pojo.configuration.local.LocalConfiguration
import com.tencent.bkrepo.common.storage.credentials.FileSystemCredentials
import com.tencent.bkrepo.repository.UT_PROJECT_ID
import com.tencent.bkrepo.repository.UT_REPO_DESC
import com.tencent.bkrepo.repository.UT_REPO_NAME
import com.tencent.bkrepo.repository.UT_STORAGE_CREDENTIALS_KEY
import com.tencent.bkrepo.repository.UT_USER
import com.tencent.bkrepo.repository.dao.FileReferenceDao
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.pojo.credendials.StorageCredentialsCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
import com.tencent.bkrepo.repository.service.file.FileReferenceService
import com.tencent.bkrepo.repository.service.node.NodeService
import com.tencent.bkrepo.repository.service.repo.ProjectService
import com.tencent.bkrepo.repository.service.repo.RepositoryService
import com.tencent.bkrepo.repository.service.repo.StorageCredentialService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.context.annotation.Import

@DisplayName("文件引用计数测试")
@DataMongoTest
@Import(
    FileReferenceDao::class,
    NodeDao::class
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileReferenceServiceTest @Autowired constructor(
    private val projectService: ProjectService,
    private val repositoryService: RepositoryService,
    private val nodeService: NodeService,
    private val fileReferenceService: FileReferenceService,
    private val storageCredentialService: StorageCredentialService
) : ServiceBaseTest() {

    @BeforeAll
    fun init() {
        initMock()
        initRepoForUnitTest(projectService, repositoryService)
    }

    @Test
    fun testIncrementAndDecrement() {
        val sha256 = uniqueId()
        assertEquals(0, fileReferenceService.count(sha256, null))
        assertTrue(fileReferenceService.increment(sha256, null))
        assertEquals(1, fileReferenceService.count(sha256, null))
        assertTrue(fileReferenceService.increment(sha256, null))
        assertEquals(2, fileReferenceService.count(sha256, null))

        assertTrue(fileReferenceService.decrement(sha256, null))
        assertEquals(1, fileReferenceService.count(sha256, null))
        assertTrue(fileReferenceService.decrement(sha256, null))
        assertEquals(0, fileReferenceService.count(sha256, null))
        assertFalse(fileReferenceService.decrement(sha256, null))
        assertEquals(0, fileReferenceService.count(sha256, null))
    }

    @Test
    fun testGetFileReference() {
        assertThrows(NotFoundException::class.java) {
            fileReferenceService.get(null, "notExistsSha256")
        }

        // 测试获取默认存储中的引用
        createReference()
        fileReferenceService.get(null, TEST_SHA_256).apply {
            assertEquals(sha256, TEST_SHA_256)
            assertEquals(credentialsKey, null)
            assertEquals(count, 1L)
        }

        // 测试获取特定存储中的引用
        createRepoUseSpecificCredential()
        createReference(USED_SPECIFIC_CREDENTIAL_REPO_NAME)
        fileReferenceService.get(UT_STORAGE_CREDENTIALS_KEY, TEST_SHA_256).apply {
            assertEquals(sha256, TEST_SHA_256)
            assertEquals(credentialsKey, UT_STORAGE_CREDENTIALS_KEY)
            assertEquals(count, 1L)
        }
    }

    private fun createReference(repoName: String = UT_REPO_NAME) {
        val createRequest = NodeCreateRequest(
            projectId = UT_PROJECT_ID,
            repoName = repoName,
            folder = false,
            fullPath = "/1.txt",
            expires = 0,
            overwrite = false,
            size = 1,
            sha256 = TEST_SHA_256,
            md5 = "md5",
            metadata = emptyMap(),
            operator = UT_USER
        )
        nodeService.createNode(createRequest)
    }

    private fun createRepoUseSpecificCredential() {
        val createRequest = StorageCredentialsCreateRequest(UT_STORAGE_CREDENTIALS_KEY, FileSystemCredentials(), "")
        storageCredentialService.create(UT_USER, createRequest)
        val repoCreateRequest = RepoCreateRequest(
            projectId = UT_PROJECT_ID,
            name = USED_SPECIFIC_CREDENTIAL_REPO_NAME,
            type = RepositoryType.GENERIC,
            category = RepositoryCategory.LOCAL,
            public = false,
            description = UT_REPO_DESC,
            configuration = LocalConfiguration(),
            operator = UT_USER,
            storageCredentialsKey = UT_STORAGE_CREDENTIALS_KEY
        )
        repositoryService.createRepo(repoCreateRequest)
    }

    companion object {
        private const val USED_SPECIFIC_CREDENTIAL_REPO_NAME = "repoUsedSpecificCredential"
        private const val TEST_SHA_256 = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"
    }
}
