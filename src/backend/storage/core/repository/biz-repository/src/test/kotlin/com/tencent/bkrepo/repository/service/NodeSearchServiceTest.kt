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
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.common.query.model.Sort
import com.tencent.bkrepo.repository.UT_PROJECT_ID
import com.tencent.bkrepo.repository.UT_REPO_DESC
import com.tencent.bkrepo.repository.UT_REPO_DISPLAY
import com.tencent.bkrepo.repository.UT_REPO_NAME
import com.tencent.bkrepo.repository.UT_USER
import com.tencent.bkrepo.repository.dao.FileReferenceDao
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.project.ProjectCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
import com.tencent.bkrepo.repository.search.common.RepoNameRuleInterceptor
import com.tencent.bkrepo.repository.search.common.RepoTypeRuleInterceptor
import com.tencent.bkrepo.repository.search.node.NodeQueryInterpreter
import com.tencent.bkrepo.repository.service.node.NodeSearchService
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

@DisplayName("节点自定义查询测试")
@DataMongoTest
@Import(
    NodeDao::class,
    FileReferenceDao::class,
    NodeQueryInterpreter::class,
    RepoNameRuleInterceptor::class,
    RepoTypeRuleInterceptor::class
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeSearchServiceTest @Autowired constructor(
    private val projectService: ProjectService,
    private val repositoryService: RepositoryService,
    private val nodeService: NodeService,
    private val nodeSearchService: NodeSearchService
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
    @DisplayName("完整路径前缀匹配查询")
    fun testFullPathSearch() {
        nodeService.createNode(createRequest("/a/b"))
        nodeService.createNode(createRequest("/a/b/1.txt", false))
        val size = 21
        repeat(size) { i -> nodeService.createNode(createRequest("/a/b/c/$i.txt", false)) }
        repeat(size) { i -> nodeService.createNode(createRequest("/a/b/d/$i.txt", false)) }

        val projectId = Rule.QueryRule("projectId", UT_PROJECT_ID)
        val repoName = Rule.QueryRule("repoName", UT_REPO_NAME)
        val path = Rule.QueryRule("fullPath", "/a/b/d", OperationType.PREFIX)
        val folder = Rule.QueryRule("folder", false)
        val rule = Rule.NestedRule(mutableListOf(projectId, repoName, path, folder))

        val queryModel = QueryModel(
            page = PageLimit(0, 11),
            sort = Sort(listOf("fullPath"), Sort.Direction.ASC),
            select = mutableListOf("projectId", "repoName", "fullPath", "metadata"),
            rule = rule
        )

        val result = nodeSearchService.search(queryModel)
        Assertions.assertEquals(21, result.totalRecords)
        Assertions.assertEquals(11, result.records.size)
    }

    @Test
    @DisplayName("元数据精确查询")
    fun testMetadataUserSearch() {
        nodeService.createNode(createRequest("/a/b/1.txt", false, metadata = mapOf("key1" to "1", "key2" to "2")))
        nodeService.createNode(createRequest("/a/b/2.txt", false, metadata = mapOf("key1" to "11", "key2" to "2")))
        nodeService.createNode(createRequest("/a/b/3.txt", false, metadata = mapOf("key1" to "22")))
        nodeService.createNode(createRequest("/a/b/4.txt", false, metadata = mapOf("key1" to "2", "key2" to "1")))

        val projectId = Rule.QueryRule("projectId", UT_PROJECT_ID)
        val repoName = Rule.QueryRule("repoName", UT_REPO_NAME)
        val metadata = Rule.QueryRule("metadata.key1", "1")
        val rule = Rule.NestedRule(mutableListOf(projectId, repoName, metadata))

        val queryModel = QueryModel(
            page = PageLimit(0, 10),
            sort = Sort(listOf("fullPath"), Sort.Direction.ASC),
            select = mutableListOf("projectId", "repoName", "fullPath", "metadata"),
            rule = rule
        )

        val result = nodeSearchService.search(queryModel)
        Assertions.assertEquals(1, result.totalRecords)
        Assertions.assertEquals(1, result.records.size)
        val node = result.records[0]
        val metadataMap = node["metadata"] as Map<*, *>
        Assertions.assertEquals("1", metadataMap["key1"])
        Assertions.assertEquals("/a/b/1.txt", node["fullPath"])
    }

    @Test
    @DisplayName("元数据前缀匹配查询")
    fun testMetadataPrefixSearch() {
        nodeService.createNode(createRequest("/a/b/1.txt", false, metadata = mapOf("key" to "1")))
        nodeService.createNode(createRequest("/a/b/2.txt", false, metadata = mapOf("key" to "11")))
        nodeService.createNode(createRequest("/a/b/3.txt", false, metadata = mapOf("key" to "22")))
        nodeService.createNode(createRequest("/a/b/4.txt", false, metadata = mapOf("key" to "22", "key1" to "1")))

        val projectId = Rule.QueryRule("projectId", UT_PROJECT_ID)
        val repoName = Rule.QueryRule("repoName", UT_REPO_NAME)
        val metadata = Rule.QueryRule("metadata.key", "1", OperationType.PREFIX) // 前缀
        val rule = Rule.NestedRule(mutableListOf(projectId, repoName, metadata))

        val queryModel = QueryModel(
            page = PageLimit(0, 10),
            sort = Sort(listOf("fullPath"), Sort.Direction.ASC),
            select = mutableListOf("projectId", "repoName", "fullPath", "metadata"),
            rule = rule
        )

        val result = nodeSearchService.search(queryModel)
        Assertions.assertEquals(2, result.totalRecords)
        Assertions.assertEquals(2, result.records.size)
    }

    @Test
    @DisplayName("元数据模糊匹配查询")
    fun testMetadataFuzzySearch() {
        nodeService.createNode(createRequest("/a/b/1.txt", false, metadata = mapOf("key" to "121")))
        nodeService.createNode(createRequest("/a/b/2.txt", false, metadata = mapOf("key" to "131")))
        nodeService.createNode(createRequest("/a/b/3.txt", false, metadata = mapOf("key" to "144")))

        val projectId = Rule.QueryRule("projectId", UT_PROJECT_ID)
        val repoName = Rule.QueryRule("repoName", UT_REPO_NAME)
        val metadata = Rule.QueryRule("metadata.key", "1*1", OperationType.MATCH) // 前缀
        val rule = Rule.NestedRule(mutableListOf(projectId, repoName, metadata))

        val queryModel = QueryModel(
            page = PageLimit(0, 10),
            sort = Sort(listOf("fullPath"), Sort.Direction.ASC),
            select = mutableListOf("projectId", "repoName", "fullPath", "metadata"),
            rule = rule
        )

        val result = nodeSearchService.search(queryModel)
        Assertions.assertEquals(2, result.totalRecords)
        Assertions.assertEquals(2, result.records.size)
    }

    private fun createRequest(
        fullPath: String = "/a/b/c",
        folder: Boolean = true,
        size: Long = 1,
        metadata: Map<String, String>? = null
    ): NodeCreateRequest {
        return NodeCreateRequest(
            projectId = UT_PROJECT_ID,
            repoName = UT_REPO_NAME,
            folder = folder,
            fullPath = fullPath,
            expires = 0,
            overwrite = false,
            size = size,
            sha256 = "sha256",
            md5 = "md5",
            operator = UT_USER,
            metadata = metadata
        )
    }
}
