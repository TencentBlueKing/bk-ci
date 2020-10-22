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

package com.tencent.bkrepo.repository.service

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.pojo.configuration.local.LocalConfiguration
import com.tencent.bkrepo.common.storage.credentials.FileSystemCredentials
import com.tencent.bkrepo.repository.UT_PROJECT_ID
import com.tencent.bkrepo.repository.UT_REPO_DISPLAY
import com.tencent.bkrepo.repository.UT_REPO_NAME
import com.tencent.bkrepo.repository.UT_STORAGE_CREDENTIALS_KEY
import com.tencent.bkrepo.repository.UT_USER
import com.tencent.bkrepo.repository.config.RepositoryProperties
import com.tencent.bkrepo.repository.constant.SYSTEM_USER
import com.tencent.bkrepo.repository.pojo.credendial.StorageCredentialsCreateRequest
import com.tencent.bkrepo.repository.pojo.project.ProjectCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoDeleteRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoUpdateRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.boot.test.mock.mockito.MockBean

@DisplayName("仓库服务测试")
@DataMongoTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RepositoryServiceTest @Autowired constructor(
    private val projectService: ProjectService,
    private val repositoryService: RepositoryService,
    private val storageCredentialService: StorageCredentialService,
    private val repositoryProperties: RepositoryProperties
) : ServiceBaseTest() {

    @MockBean
    private lateinit var nodeService: NodeService

    private val storageCredentials = FileSystemCredentials().apply {
        path = "test"
        cache.enabled = true
        cache.path = "cache-test"
        cache.expireDays = 10
    }

    @BeforeAll
    fun beforeAll() {
        initMock()
        if (!projectService.exist(UT_PROJECT_ID)) {
            val projectCreateRequest = ProjectCreateRequest(UT_PROJECT_ID, UT_REPO_NAME, UT_REPO_DISPLAY, UT_USER)
            projectService.create(projectCreateRequest)
        }
        val storageCreateRequest = StorageCredentialsCreateRequest(UT_STORAGE_CREDENTIALS_KEY, storageCredentials)
        storageCredentialService.create(UT_USER, storageCreateRequest)
    }

    @BeforeEach
    fun beforeEach() {
        initMock()
        repositoryService.list(UT_PROJECT_ID).forEach {
            repositoryService.delete(RepoDeleteRequest(UT_PROJECT_ID, it.name, UT_USER))
        }
    }

    @Test
    @DisplayName("测试列表查询")
    fun `test list query`() {
        assertEquals(0, repositoryService.list(UT_PROJECT_ID).size)
        val size = 20
        repeat(size) { repositoryService.create(createRequest("repo$it")) }
        assertEquals(size, repositoryService.list(UT_PROJECT_ID).size)
    }

    @Test
    @DisplayName("测试分页查询")
    fun `test page query`() {
        assertEquals(0, repositoryService.list(UT_PROJECT_ID).size)
        val size = 51L
        repeat(size.toInt()) { repositoryService.create(createRequest("repo$it")) }
        var page = repositoryService.page(UT_PROJECT_ID, 0, 10)
        assertEquals(10, page.records.size)
        assertEquals(size, page.count)
        assertEquals(0, page.page)
        assertEquals(10, page.pageSize)

        page = repositoryService.page(UT_PROJECT_ID, 5, 10)
        assertEquals(1, page.records.size)
        page = repositoryService.page(UT_PROJECT_ID, 6, 10)
        assertEquals(0, page.records.size)

        page = repositoryService.page(UT_PROJECT_ID, 0, 20)
        assertEquals(20, page.records.size)
        assertEquals(size, page.count)
        assertEquals(0, page.page)
        assertEquals(20, page.pageSize)
    }

    @Test
    @DisplayName("测试判断仓库是否存在")
    fun `test check exist`() {
        repositoryService.create(createRequest())
        assertTrue(repositoryService.exist(UT_PROJECT_ID, UT_REPO_NAME))
        assertTrue(repositoryService.exist(UT_PROJECT_ID, UT_REPO_NAME, RepositoryType.GENERIC.name))
        assertFalse(repositoryService.exist("", ""))
        assertFalse(repositoryService.exist(UT_PROJECT_ID, ""))
        assertFalse(repositoryService.exist("", UT_REPO_NAME))

        repositoryService.delete(RepoDeleteRequest(UT_PROJECT_ID, UT_REPO_NAME, SYSTEM_USER))
        assertFalse(repositoryService.exist(UT_PROJECT_ID, UT_REPO_NAME))
    }

    @Test
    @DisplayName("测试创建同名仓库")
    fun `should throw exception when repo name exists`() {
        repositoryService.create(createRequest())
        assertThrows<ErrorCodeException> { repositoryService.create(createRequest()) }
    }

    @Test
    @DisplayName("测试使用指定storage key创建仓库")
    fun `test create with specific storage key`() {
        val request = createRequest(storageCredentialsKey = UT_STORAGE_CREDENTIALS_KEY)
        repositoryService.create(request)
        val repository = repositoryService.detail(UT_PROJECT_ID, UT_REPO_NAME, RepositoryType.GENERIC.name)!!
        assertEquals(UT_REPO_NAME, repository.name)
        assertEquals(RepositoryType.GENERIC, repository.type)
        assertEquals(RepositoryCategory.LOCAL, repository.category)
        assertEquals(true, repository.public)
        assertEquals(UT_PROJECT_ID, repository.projectId)
        assertEquals("simple description", repository.description)
        assertEquals(storageCredentials, repository.storageCredentials)

        assertThrows<ErrorCodeException> { repositoryService.create(createRequest()) }
    }

    @Test
    @DisplayName("测试使用空storage key创建仓库")
    fun `test create with null storage key`() {
        assertNull(repositoryProperties.defaultStorageCredentialsKey)
        repositoryService.create(createRequest())
        val repository = repositoryService.detail(UT_PROJECT_ID, UT_REPO_NAME, RepositoryType.GENERIC.name)!!
        assertNull(repository.storageCredentials)
    }

    @Test
    @DisplayName("测试使用默认storage key创建仓库")
    fun `test create with default storage key`() {
        repositoryProperties.defaultStorageCredentialsKey = UT_STORAGE_CREDENTIALS_KEY
        repositoryService.create(createRequest())
        val repository = repositoryService.detail(UT_PROJECT_ID, UT_REPO_NAME, RepositoryType.GENERIC.name)!!

        val dbCredential = repository.storageCredentials
        assertEquals(storageCredentials, dbCredential)
    }

    @Test
    @DisplayName("测试使用不存在的storage key创建仓库")
    fun `should throw exception when storage key nonexistent`() {
        val request = createRequest(storageCredentialsKey = "non-exist-credentials-key")
        assertThrows<ErrorCodeException> { repositoryService.create(request) }
    }

    @Test
    @DisplayName("测试更新仓库信息")
    fun `test update repository info`() {
        repositoryService.create(createRequest())
        repositoryService.update(
            RepoUpdateRequest(
                projectId = UT_PROJECT_ID,
                name = UT_REPO_NAME,
                public = false,
                description = "updated description",
                operator = UT_USER
            )
        )
        val repository = repositoryService.detail(UT_PROJECT_ID, UT_REPO_NAME)!!
        assertEquals(false, repository.public)
        assertEquals("updated description", repository.description)
    }

    @Test
    @DisplayName("测试删除仓库")
    fun `test delete repository`() {
        repositoryService.create(createRequest("test1"))
        repositoryService.create(createRequest("test2"))
        repositoryService.delete(RepoDeleteRequest(UT_PROJECT_ID, "test1", SYSTEM_USER))
        assertNull(repositoryService.detail(UT_PROJECT_ID, "test1"))

        assertThrows<ErrorCodeException> { repositoryService.delete(RepoDeleteRequest(UT_PROJECT_ID, "", SYSTEM_USER)) }
        assertThrows<ErrorCodeException> { repositoryService.delete(RepoDeleteRequest(UT_PROJECT_ID, "test1", SYSTEM_USER)) }

        assertNotNull(repositoryService.detail(UT_PROJECT_ID, "test2"))
    }

    private fun createRequest(name: String = UT_REPO_NAME, storageCredentialsKey: String? = null): RepoCreateRequest {
        return RepoCreateRequest(
            projectId = UT_PROJECT_ID,
            name = name,
            type = RepositoryType.GENERIC,
            category = RepositoryCategory.LOCAL,
            public = true,
            description = "simple description",
            configuration = LocalConfiguration(),
            storageCredentialsKey = storageCredentialsKey,
            operator = UT_USER
        )
    }
}
