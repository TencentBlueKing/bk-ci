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

import com.tencent.bkrepo.common.artifact.path.PathUtils.ROOT
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.pojo.configuration.local.LocalConfiguration
import com.tencent.bkrepo.repository.UT_PROJECT_ID
import com.tencent.bkrepo.repository.UT_REPO_DESC
import com.tencent.bkrepo.repository.UT_REPO_DISPLAY
import com.tencent.bkrepo.repository.UT_REPO_NAME
import com.tencent.bkrepo.repository.UT_USER
import com.tencent.bkrepo.repository.dao.FileReferenceDao
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.pojo.metadata.MetadataDeleteRequest
import com.tencent.bkrepo.repository.pojo.metadata.MetadataSaveRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.project.ProjectCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
import com.tencent.bkrepo.repository.service.node.NodeService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.context.annotation.Import

@DisplayName("元数据服务测试")
@DataMongoTest
@Import(
    NodeDao::class,
    FileReferenceDao::class
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MetadataServiceTest @Autowired constructor(
    private val projectService: ProjectService,
    private val repositoryService: RepositoryService,
    private val nodeService: NodeService,
    private val metadataService: MetadataService
) : ServiceBaseTest() {

    @BeforeAll
    fun beforeAll() {
        initMock()

        if (!projectService.checkExist(UT_PROJECT_ID)) {
            val projectCreateRequest = ProjectCreateRequest(UT_PROJECT_ID, UT_REPO_NAME, UT_REPO_DISPLAY, UT_USER)
            projectService.createProject(projectCreateRequest)
        }
        if (!repositoryService.checkExist(UT_PROJECT_ID, UT_REPO_NAME)) {
            val repoCreateRequest = RepoCreateRequest(
                projectId = UT_PROJECT_ID,
                name = UT_REPO_NAME,
                type = RepositoryType.GENERIC,
                category = RepositoryCategory.LOCAL,
                public = false,
                description = UT_REPO_DESC,
                configuration = LocalConfiguration(),
                operator = UT_USER
            )
            repositoryService.createRepo(repoCreateRequest)
        }
    }

    @BeforeEach
    fun beforeEach() {
        initMock()
        nodeService.deleteByPath(UT_PROJECT_ID, UT_REPO_NAME, ROOT, UT_USER)
    }

    @Test
    fun testCreate() {
        val node = nodeService.createNode(createRequest())
        Assertions.assertEquals(0, metadataService.listMetadata(UT_PROJECT_ID, UT_REPO_NAME, node.fullPath).size)
        metadataService.saveMetadata(MetadataSaveRequest(UT_PROJECT_ID, UT_REPO_NAME, node.fullPath, DEFAULT_METADATA))
        val dbMetadata = metadataService.listMetadata(UT_PROJECT_ID, UT_REPO_NAME, node.fullPath)
        Assertions.assertEquals(2, dbMetadata.size)
        Assertions.assertEquals("value1", dbMetadata["key1"])
        Assertions.assertEquals("value2", dbMetadata["key2"])
    }

    @Test
    fun testSaveEmpty() {
        val node = nodeService.createNode(createRequest(DEFAULT_METADATA))
        // update with empty key list
        metadataService.saveMetadata(MetadataSaveRequest(UT_PROJECT_ID, UT_REPO_NAME, node.fullPath, mutableMapOf()))

        val dbMetadata = metadataService.listMetadata(UT_PROJECT_ID, UT_REPO_NAME, node.fullPath)
        Assertions.assertEquals("value1", dbMetadata["key1"])
        Assertions.assertEquals("value2", dbMetadata["key2"])
    }

    @Test
    fun testUpdate() {
        val node = nodeService.createNode(createRequest(DEFAULT_METADATA))
        // update
        val newMetadata = mapOf("key1" to "value1", "key2" to "value22", "key3" to "value3")
        metadataService.saveMetadata(MetadataSaveRequest(UT_PROJECT_ID, UT_REPO_NAME, node.fullPath, newMetadata))

        val dbMetadata = metadataService.listMetadata(UT_PROJECT_ID, UT_REPO_NAME, node.fullPath)
        Assertions.assertEquals(3, dbMetadata.size)
        Assertions.assertEquals("value1", dbMetadata["key1"])
        Assertions.assertEquals("value22", dbMetadata["key2"])
        Assertions.assertEquals("value3", dbMetadata["key3"])
    }

    @Test
    fun testDelete() {
        val metadata = mapOf("key1" to "value1", "key2" to "value2", "key3" to "value3")
        val node = nodeService.createNode(createRequest(metadata))
        // delete
        metadataService.deleteMetadata(
            MetadataDeleteRequest(
                UT_PROJECT_ID,
                UT_REPO_NAME,
                node.fullPath,
                setOf("key1", "key2", "key0")
            )
        )

        val dbMetadata = metadataService.listMetadata(UT_PROJECT_ID, UT_REPO_NAME, node.fullPath)
        Assertions.assertEquals(1, dbMetadata.size)
        Assertions.assertEquals("value3", dbMetadata["key3"])
    }

    private fun createRequest(metadata: Map<String, String> = emptyMap()): NodeCreateRequest {
        return NodeCreateRequest(
            projectId = UT_PROJECT_ID,
            repoName = UT_REPO_NAME,
            folder = false,
            fullPath = "/1.txt",
            expires = 0,
            overwrite = false,
            size = 1,
            sha256 = "sha256",
            md5 = "md5",
            metadata = metadata,
            operator = UT_USER
        )
    }

    companion object {
        private val DEFAULT_METADATA = mapOf("key1" to "value1", "key2" to "value2")
    }
}
