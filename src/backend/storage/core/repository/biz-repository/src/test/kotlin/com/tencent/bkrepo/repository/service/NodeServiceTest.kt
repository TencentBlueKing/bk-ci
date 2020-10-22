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
import com.tencent.bkrepo.repository.UT_PROJECT_ID
import com.tencent.bkrepo.repository.UT_REPO_DESC
import com.tencent.bkrepo.repository.UT_REPO_DISPLAY
import com.tencent.bkrepo.repository.UT_REPO_NAME
import com.tencent.bkrepo.repository.UT_USER
import com.tencent.bkrepo.repository.dao.FileReferenceDao
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.pojo.node.service.NodeCopyRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeMoveRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeRenameRequest
import com.tencent.bkrepo.repository.pojo.project.ProjectCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
import com.tencent.bkrepo.repository.util.NodeUtils
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
import org.springframework.context.annotation.Import

@DisplayName("节点服务测试")
@DataMongoTest
@Import(
    NodeDao::class,
    FileReferenceDao::class
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeServiceTest @Autowired constructor(
    private val projectService: ProjectService,
    private val repositoryService: RepositoryService,
    private val nodeService: NodeService
) : ServiceBaseTest() {

    @BeforeAll
    fun beforeAll() {
        initMock()

        if (!projectService.exist(UT_PROJECT_ID)) {
            val projectCreateRequest = ProjectCreateRequest(UT_PROJECT_ID, UT_REPO_NAME, UT_REPO_DISPLAY, UT_USER)
            projectService.create(projectCreateRequest)
        }
        if (!repositoryService.exist(UT_PROJECT_ID, UT_REPO_NAME)) {
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
            repositoryService.create(repoCreateRequest)
        }
    }

    @BeforeEach
    fun beforeEach() {
        initMock()
        nodeService.deleteByPath(UT_PROJECT_ID, UT_REPO_NAME, NodeUtils.ROOT_PATH, UT_USER, false)
    }

    @Test
    @DisplayName("根节点测试")
    fun testRootNode() {
        assertNull(nodeService.detail(UT_PROJECT_ID, UT_REPO_NAME, "/"))
        assertEquals(0, nodeService.list(UT_PROJECT_ID, UT_REPO_NAME, "/", includeFolder = true, deep = true).size)

        nodeService.create(createRequest("/1.txt", false))
        assertNotNull(nodeService.detail(UT_PROJECT_ID, UT_REPO_NAME, "/"))
        assertEquals(1, nodeService.list(UT_PROJECT_ID, UT_REPO_NAME, "/", includeFolder = false, deep = true).size)

        nodeService.create(createRequest("/a/b/1.txt", false))
        assertNotNull(nodeService.detail(UT_PROJECT_ID, UT_REPO_NAME, "/"))

        assertEquals(2, nodeService.list(UT_PROJECT_ID, UT_REPO_NAME, "/", includeFolder = false, deep = true).size)
    }

    @Test
    @DisplayName("测试元数据查询")
    fun testIncludeMetadata() {
        nodeService.create(createRequest("/a/b/1.txt", folder = false, metadata = mapOf("key" to "value")))
        nodeService.create(createRequest("/a/b/2.txt", folder = false))

        val node1 = nodeService.detail(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/1.txt")
        val node2 = nodeService.detail(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/2.txt")
        assertNotNull(node1!!.metadata)
        assertNotNull(node1.metadata["key"])
        assertNotNull(node2!!.metadata)

        val list1 = nodeService.list(UT_PROJECT_ID, UT_REPO_NAME, "/a/b", includeMetadata = true)
        assertNotNull(list1[0].metadata)
        assertNotNull(list1[1].metadata)

        val list2 = nodeService.list(UT_PROJECT_ID, UT_REPO_NAME, "/a/b", includeMetadata = false)
        assertNull(list2[0].metadata)
        assertNull(list2[1].metadata)
    }

    @Test
    @DisplayName("列表查询")
    fun testList() {
        nodeService.create(createRequest("/a/b"))
        nodeService.create(createRequest("/a/b/1.txt", false))
        val size = 20
        repeat(size) { i -> nodeService.create(createRequest("/a/b/c/$i.txt", false)) }
        repeat(size) { i -> nodeService.create(createRequest("/a/b/d/$i.txt", false)) }

        assertEquals(1, nodeService.list(UT_PROJECT_ID, UT_REPO_NAME, "/a/b", includeFolder = false, deep = false).size)
        assertEquals(3, nodeService.list(UT_PROJECT_ID, UT_REPO_NAME, "/a/b", includeFolder = true, deep = false).size)
        assertEquals(size * 2 + 1, nodeService.list(UT_PROJECT_ID, UT_REPO_NAME, "/a/b", includeFolder = false, deep = true).size)
        assertEquals(size * 2 + 3, nodeService.list(UT_PROJECT_ID, UT_REPO_NAME, "/a/b", includeFolder = true, deep = true).size)
        assertEquals(size, nodeService.list(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/c", includeFolder = true, deep = true).size)
    }

    @Test
    @DisplayName("列表查询")
    fun testList2() {
        nodeService.create(createRequest("/a/"))
        nodeService.create(createRequest("/b"))
        val size = 20
        repeat(size) { i -> nodeService.create(createRequest("/a/$i.txt", false)) }
        repeat(size) { i -> nodeService.create(createRequest("/b/$i.txt", false)) }

        assertEquals(0, nodeService.list(UT_PROJECT_ID, UT_REPO_NAME, "", includeFolder = false, deep = false).size)
        assertEquals(2, nodeService.list(UT_PROJECT_ID, UT_REPO_NAME, "", includeFolder = true, deep = false).size)
        assertEquals(42, nodeService.list(UT_PROJECT_ID, UT_REPO_NAME, "", includeFolder = true, deep = true).size)
    }

    @Test
    @DisplayName("分页查询")
    fun testPage() {
        nodeService.create(createRequest("/a/b/c"))

        val size = 51L
        repeat(size.toInt()) { i -> nodeService.create(createRequest("/a/b/c/$i.txt", false)) }

        var page = nodeService.page(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/c", 0, 10, includeFolder = false, deep = false)
        assertEquals(10, page.records.size)
        assertEquals(size, page.count)
        assertEquals(0, page.page)
        assertEquals(10, page.pageSize)

        page = nodeService.page(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/c", 5, 10, includeFolder = false, deep = false)
        assertEquals(1, page.records.size)
        page = nodeService.page(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/c", 6, 10, includeFolder = false, deep = false)
        assertEquals(0, page.records.size)

        page = nodeService.page(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/c", 0, 20, includeFolder = false, deep = false)
        assertEquals(20, page.records.size)
        assertEquals(size, page.count)
        assertEquals(0, page.page)
        assertEquals(20, page.pageSize)
    }

    @Test
    @DisplayName("判断节点是否存在")
    fun testExist() {
        assertFalse(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, ""))
        assertFalse(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/"))

        createRequest("  / a /   b /  1.txt   ", false)
        nodeService.create(createRequest("  / a /   b /  1.txt   ", false))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/1.txt"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b"))
    }

    @Test
    @DisplayName("创建节点，非法名称抛异常")
    fun testCreateThrow() {
        assertThrows<ErrorCodeException> { nodeService.create(createRequest(" a /   b /  1.txt   ", false)) }
        assertThrows<ErrorCodeException> { nodeService.create(createRequest(" a /   b /  1./txt   ", false)) }
        assertThrows<ErrorCodeException> { nodeService.create(createRequest("/a/b/..", true)) }
        assertThrows<ErrorCodeException> { nodeService.create(createRequest("/a/b/.", true)) }
        nodeService.create(createRequest("/a/b", true))
    }

    @Test
    @DisplayName("创建文件")
    fun testCreateFile() {
        nodeService.create(createRequest("  / a /   b /  1.txt  ", false))
        assertThrows<ErrorCodeException> { nodeService.create(createRequest("  / a /   b /  1.txt  ", false)) }
        val node = nodeService.detail(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/1.txt")!!

        assertEquals(UT_USER, node.createdBy)
        assertNotNull(node.createdDate)
        assertEquals(UT_USER, node.lastModifiedBy)
        assertNotNull(node.lastModifiedDate)

        assertEquals(false, node.folder)
        assertEquals("/a/b/", node.path)
        assertEquals("1.txt", node.name)
        assertEquals("/a/b/1.txt", node.fullPath)
        assertEquals(1L, node.size)
        assertEquals("sha256", node.sha256)
    }

    @Test
    @DisplayName("创建目录测试")
    fun testCreatePath() {
        nodeService.create(createRequest("  /// a /   c ////    中文.@_-`~...  "))
        val node = nodeService.detail(UT_PROJECT_ID, UT_REPO_NAME, "/a/c/中文.@_-`~...")!!

        assertEquals(UT_USER, node.createdBy)
        assertNotNull(node.createdDate)
        assertEquals(UT_USER, node.lastModifiedBy)
        assertNotNull(node.lastModifiedDate)

        assertEquals(true, node.folder)
        assertEquals("/a/c/", node.path)
        assertEquals("中文.@_-`~...", node.name)
        assertEquals("/a/c/中文.@_-`~...", node.fullPath)
        assertEquals(0L, node.size)
        assertEquals(null, node.sha256)
    }

    @Test
    @DisplayName("删除节点")
    fun testDelete() {
        nodeService.create(createRequest("/a/b/1.txt", false))
        nodeService.delete(
            NodeDeleteRequest(
                projectId = UT_PROJECT_ID,
                repoName = UT_REPO_NAME,
                fullPath = "/a/b/1.txt",
                operator = UT_USER
            )
        )

        assertFalse(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/1.txt"))

        nodeService.create(createRequest("/a/b/1.txt"))

        nodeService.create(createRequest("/a/b/c"))
        nodeService.create(createRequest("/a/b/c/1.txt", false))

        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/c/1.txt"))

        nodeService.delete(
            NodeDeleteRequest(
                projectId = UT_PROJECT_ID,
                repoName = UT_REPO_NAME,
                fullPath = "/a/b/c/1.txt",
                operator = UT_USER
            )
        )

        assertFalse(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/c/1.txt"))
    }

    @Test
    @DisplayName("正则转义")
    fun testEscape() {
        nodeService.create(createRequest("/.*|^/a/1.txt", false))
        nodeService.create(createRequest("/a/1.txt", false))

        assertEquals(1, nodeService.list(UT_PROJECT_ID, UT_REPO_NAME, "/.*|^/a", includeFolder = true, deep = true).size)
        nodeService.deleteByPath(UT_PROJECT_ID, UT_REPO_NAME, "/.*|^/a", UT_USER)
        assertEquals(0, nodeService.list(UT_PROJECT_ID, UT_REPO_NAME, "/.*|^/a", includeFolder = true, deep = true).size)
        assertEquals(1, nodeService.list(UT_PROJECT_ID, UT_REPO_NAME, "/a", includeFolder = true, deep = true).size)
    }

    @Test
    @DisplayName("计算节点大小")
    fun testComputeSize() {
        val size = 20
        repeat(size) { i -> nodeService.create(createRequest("/a/b/c/$i.txt", false)) }
        repeat(size) { i -> nodeService.create(createRequest("/a/b/d/$i.txt", false)) }

        val pathSizeInfo = nodeService.computeSize(UT_PROJECT_ID, UT_REPO_NAME, "/a/b")

        assertEquals(42, pathSizeInfo.subNodeCount)
        assertEquals(40, pathSizeInfo.size)

        val fileSizeInfo = nodeService.computeSize(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/c/1.txt")

        assertEquals(0, fileSizeInfo.subNodeCount)
        assertEquals(1, fileSizeInfo.size)
    }

    @Test
    @DisplayName("重命名目录")
    fun testRenamePath() {
        nodeService.create(createRequest("/a/b/1.txt", false))
        nodeService.create(createRequest("/a/b/2.txt", false))
        nodeService.create(createRequest("/a/b/c/1.txt", false))
        nodeService.create(createRequest("/a/b/c/2.txt", false))

        val renameRequest = NodeRenameRequest(
            projectId = UT_PROJECT_ID,
            repoName = UT_REPO_NAME,
            fullPath = "/a",
            newFullPath = "/aa",
            operator = UT_USER
        )
        nodeService.rename(renameRequest)

        assertFalse(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a"))
        assertFalse(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b"))
        assertFalse(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/c"))
        assertFalse(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/1.txt"))
        assertFalse(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/c/2.txt"))

        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/aa"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/aa/b"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/aa/b/c"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/aa/b/1.txt"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/aa/b/c/2.txt"))
    }

    @Test
    @DisplayName("重命名中间目录")
    fun testRenameSubPath() {
        nodeService.create(createRequest("/a/b/c", true))

        val renameRequest = NodeRenameRequest(
            projectId = UT_PROJECT_ID,
            repoName = UT_REPO_NAME,
            fullPath = "/a/b/c",
            newFullPath = "/a/d/c",
            operator = UT_USER
        )
        nodeService.rename(renameRequest)

        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b"))
        assertFalse(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/c"))

        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/d"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/d/c"))
    }

    @Test
    @DisplayName("重命名文件，遇同名文件抛异常")
    fun testRenameThrow() {
        nodeService.create(createRequest("/a/b/1.txt", false))
        nodeService.create(createRequest("/a/b/2.txt", false))
        nodeService.create(createRequest("/a/b/c/1.txt", false))
        nodeService.create(createRequest("/a/b/c/2.txt", false))

        nodeService.create(createRequest("/aa/b/c/2.txt", false))

        val renameRequest = NodeRenameRequest(
            projectId = UT_PROJECT_ID,
            repoName = UT_REPO_NAME,
            fullPath = "/a",
            newFullPath = "/aa",
            operator = UT_USER
        )
        assertThrows<ErrorCodeException> { nodeService.rename(renameRequest) }
    }

    @Test
    @DisplayName("移动文件，目录 -> 不存在的目录")
    fun testMovePathToNotExistPath() {
        nodeService.create(createRequest("/a/b/c/d/1.txt", false))
        nodeService.create(createRequest("/a/b/c/d/2.txt", false))
        nodeService.create(createRequest("/a/b/c/d/e/1.txt", false))
        nodeService.create(createRequest("/a/b/c/d/e/2.txt", false))
        nodeService.create(createRequest("/a/1.txt", false))

        val moveRequest = NodeMoveRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/a/b",
            destFullPath = "/ab",
            operator = UT_USER
        )
        nodeService.move(moveRequest)

        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/1.txt"))
        assertFalse(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b"))
        assertFalse(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/c"))
        assertFalse(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/c/d"))
        assertFalse(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/c/d/e"))
        assertFalse(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/c/d/1.txt"))
        assertFalse(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/c/d/e/2.txt"))

        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/ab"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/ab/c"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/ab/c/d"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/ab/c/d/e"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/ab/c/d/1.txt"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/ab/c/d/e/2.txt"))
    }

    @Test
    @DisplayName("移动文件，目录 -> 存在的目录")
    fun testMovePathToExistPath() {
        nodeService.create(createRequest("/a/b/c/d/1.txt", false))
        nodeService.create(createRequest("/a/b/c/d/2.txt", false))
        nodeService.create(createRequest("/a/b/c/d/e/1.txt", false))
        nodeService.create(createRequest("/a/b/c/d/e/2.txt", false))
        nodeService.create(createRequest("/a/1.txt", false))
        nodeService.create(createRequest("/ab", true))

        var moveRequest = NodeMoveRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/a/b",
            destFullPath = "/ab",
            operator = UT_USER
        )
        nodeService.move(moveRequest)

        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/1.txt"))
        assertFalse(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b"))
        assertFalse(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/c"))
        assertFalse(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/c/d"))
        assertFalse(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/c/d/e"))
        assertFalse(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/c/d/1.txt"))
        assertFalse(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/c/d/e/2.txt"))

        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/ab"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/ab/b"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/ab/b/c"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/ab/b/c/d"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/ab/b/c/d/1.txt"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/ab/b/c/d/e/2.txt"))

        nodeService.create(createRequest("/data/mkdir/aa.txt", false))
        nodeService.create(createRequest("/data/dir3", true))

        moveRequest = NodeMoveRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/data/mkdir/aa.txt",
            destFullPath = "/data/dir3",
            operator = UT_USER
        )
        nodeService.move(moveRequest)
        assertTrue(nodeService.list(UT_PROJECT_ID, UT_REPO_NAME, "/data/dir3", includeFolder = false, deep = false).size == 1)
    }

    @Test
    @DisplayName("移动文件 -> 存在的目录")
    fun testMoveFileToExistPath() {
        nodeService.create(createRequest("/a/1.txt", false))
        nodeService.create(createRequest("/ab", true))

        val moveRequest = NodeMoveRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/a/1.txt",
            destFullPath = "/ab",
            operator = UT_USER
        )
        nodeService.move(moveRequest)

        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a"))
        assertFalse(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/1.txt"))

        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/ab"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/ab/1.txt"))
    }

    @Test
    @DisplayName("移动文件，文件 -> 不存在的路径")
    fun testMoveFileToNotExistPath() {
        nodeService.create(createRequest("/a/1.txt", false))

        val moveRequest = NodeMoveRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/a/1.txt",
            destFullPath = "/ab",
            operator = UT_USER
        )
        nodeService.move(moveRequest)

        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a"))
        assertFalse(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/1.txt"))

        val destNode = nodeService.detail(UT_PROJECT_ID, UT_REPO_NAME, "/ab")
        assertNotNull(destNode)
        assertFalse(destNode!!.folder)
    }

    @Test
    @DisplayName("移动文件，文件 -> 存在的文件且覆盖")
    fun testMoveFileToExistFileAndOverwrite() {
        nodeService.create(createRequest("/a/1.txt", false, size = 1))
        nodeService.create(createRequest("/ab/a/1.txt", false, size = 2))
        nodeService.create(createRequest("/abc/a/1.txt", false, size = 2))

        // path -> path
        var moveRequest = NodeMoveRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/a",
            destFullPath = "/ab",
            operator = UT_USER,
            overwrite = true
        )
        nodeService.move(moveRequest)

        // file -> file
        var node = nodeService.detail(UT_PROJECT_ID, UT_REPO_NAME, "/ab/a/1.txt")!!
        assertEquals(1, node.size)

        moveRequest = NodeMoveRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/ab/a/1.txt",
            destFullPath = "/abc/a/1.txt",
            operator = UT_USER,
            overwrite = true
        )
        nodeService.move(moveRequest)

        node = nodeService.detail(UT_PROJECT_ID, UT_REPO_NAME, "/abc/a/1.txt")!!
        assertEquals(1, node.size)
    }

    @Test
    @DisplayName("移动文件，文件 -> 存在的文件且不覆盖")
    fun testMoveOverwriteThrow() {
        nodeService.create(createRequest("/a/1.txt", false))
        nodeService.create(createRequest("/ab/a/1.txt", false))

        val moveRequest = NodeMoveRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/a",
            destFullPath = "/ab",
            operator = UT_USER
        )
        assertThrows<ErrorCodeException> { nodeService.move(moveRequest) }
    }

    @Test
    @DisplayName("移动文件，目录 -> 自己")
    fun testMovePathToSelf() {
        nodeService.create(createRequest("/a/1.txt", false))
        nodeService.create(createRequest("/a/b/1.txt", false))

        val moveRequest = NodeMoveRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/a",
            destFullPath = "/a",
            operator = UT_USER
        )
        nodeService.move(moveRequest)
    }

    @Test
    @DisplayName("移动文件, 目录 -> 父目录")
    fun testMovePathToParentPath() {
        nodeService.create(createRequest("/a/b/1.txt", false))

        val moveRequest = NodeMoveRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/a/b",
            destFullPath = "/a",
            operator = UT_USER
        )
        nodeService.move(moveRequest)

        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/1.txt"))
    }

    @Test
    @DisplayName("移动文件, 目录 -> 根目录")
    fun testMoveToRootPath() {
        nodeService.create(createRequest("/a/b/1.txt", false))

        val moveRequest = NodeMoveRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/a/b",
            destFullPath = "",
            operator = UT_USER
        )
        nodeService.move(moveRequest)

        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/b/1.txt"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a"))
        assertFalse(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b"))
    }

    @Test
    @DisplayName("移动文件, 文件 -> 父目录")
    fun testMoveFileToRootPath() {
        nodeService.create(createRequest("/a/b/1.txt", false))

        val moveRequest = NodeMoveRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/a/b/1.txt",
            destFullPath = "/a/b",
            operator = UT_USER
        )
        nodeService.move(moveRequest)
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/1.txt"))
    }

    @Test
    @DisplayName("拷贝文件, 目录 -> 不存在的目录")
    fun testCopy() {
        nodeService.create(createRequest("/a/b/c/d/1.txt", false))
        nodeService.create(createRequest("/a/b/c/d/2.txt", false))
        nodeService.create(createRequest("/a/b/c/d/e/1.txt", false))
        nodeService.create(createRequest("/a/b/c/d/e/2.txt", false))
        nodeService.create(createRequest("/a/1.txt", false))

        val copyRequest = NodeCopyRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/a/b",
            destFullPath = "/ab",
            operator = UT_USER
        )
        nodeService.copy(copyRequest)

        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/1.txt"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/c"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/c/d"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/c/d/e"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/c/d/1.txt"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/a/b/c/d/e/2.txt"))

        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/ab"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/ab/c"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/ab/c/d"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/ab/c/d/e"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/ab/c/d/1.txt"))
        assertTrue(nodeService.exist(UT_PROJECT_ID, UT_REPO_NAME, "/ab/c/d/e/2.txt"))
    }

    @Test
    @DisplayName("拷贝文件 -> 存在的目录")
    fun testCopyFileToExistPath() {
        nodeService.create(createRequest("/a/1.txt", false))
        nodeService.create(createRequest("/b", true))

        val copyRequest = NodeCopyRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/a/1.txt",
            destFullPath = "/b/",
            operator = UT_USER,
            overwrite = true
        )
        nodeService.copy(copyRequest)

        assertTrue(nodeService.detail(UT_PROJECT_ID, UT_REPO_NAME, "/b")?.folder == true)
        assertTrue(nodeService.detail(UT_PROJECT_ID, UT_REPO_NAME, "/b/1.txt")?.folder == false)
        nodeService.list(UT_PROJECT_ID, UT_REPO_NAME, "/", true, deep = true).forEach { println(it) }
    }

    @Test
    @DisplayName("拷贝文件, 元数据一起拷贝")
    fun testCopyWithMetadata() {
        nodeService.create(createRequest("/a", false, metadata = mapOf("key" to "value")))

        val copyRequest = NodeCopyRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/a",
            destFullPath = "/b",
            operator = UT_USER
        )
        nodeService.copy(copyRequest)
        assertEquals("value", nodeService.detail(UT_PROJECT_ID, UT_REPO_NAME, "/b")!!.metadata["key"])
    }

    private fun createRequest(fullPath: String = "/a/b/c", folder: Boolean = true, size: Long = 1, metadata: Map<String, String>? = null): NodeCreateRequest {
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
