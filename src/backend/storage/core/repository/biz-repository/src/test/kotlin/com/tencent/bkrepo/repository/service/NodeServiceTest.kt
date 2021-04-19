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
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.api.DefaultArtifactInfo
import com.tencent.bkrepo.common.artifact.path.PathUtils.ROOT
import com.tencent.bkrepo.repository.UT_PROJECT_ID
import com.tencent.bkrepo.repository.UT_REPO_NAME
import com.tencent.bkrepo.repository.UT_USER
import com.tencent.bkrepo.repository.dao.FileReferenceDao
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.pojo.node.NodeListOption
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeMoveCopyRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeRenameRequest
import com.tencent.bkrepo.repository.service.node.NodeService
import com.tencent.bkrepo.repository.service.repo.ProjectService
import com.tencent.bkrepo.repository.service.repo.RepositoryService
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

    private val option = NodeListOption(includeFolder = false, deep = false)
    private val folderOption = NodeListOption(includeFolder = true, deep = false)
    private val deepOption = NodeListOption(includeFolder = false, deep = true)
    private val folderAndDeepOption = NodeListOption(includeFolder = true, deep = true)

    @BeforeAll
    fun beforeAll() {
        initMock()
        initRepoForUnitTest(projectService, repositoryService)
    }

    @BeforeEach
    fun beforeEach() {
        nodeService.deleteByPath(UT_PROJECT_ID, UT_REPO_NAME, ROOT, UT_USER)
    }

    @Test
    @DisplayName("测试根节点")
    fun testRootNode() {
        // 查询根节点，一直存在
        assertTrue(nodeService.checkExist(node("")))
        val nodeDetail = nodeService.getNodeDetail(node(""))
        assertNotNull(nodeDetail)
        assertEquals("", nodeDetail?.name)
        assertEquals(ROOT, nodeDetail?.path)
        assertEquals(ROOT, nodeDetail?.fullPath)
    }

    @Test
    @DisplayName("测试创建文件")
    fun testCreateFile() {
        val request = createRequest("/1/2/3.txt", folder = false, size = 100, metadata = mapOf("key" to "value"))
        nodeService.createNode(request)
        val node = nodeService.getNodeDetail(node("/1/2/3.txt"))!!
        assertEquals(UT_USER, node.createdBy)
        assertNotNull(node.createdDate)
        assertEquals(UT_USER, node.lastModifiedBy)
        assertNotNull(node.lastModifiedDate)
        assertFalse(node.folder)
        assertEquals("/1/2/", node.path)
        assertEquals("3.txt", node.name)
        assertEquals("/1/2/3.txt", node.fullPath)
        assertEquals(100, node.size)
        assertEquals("sha256", node.sha256)
        assertEquals("md5", node.md5)
        assertEquals("value", node.metadata["key"])
    }

    @Test
    @DisplayName("测试创建目录")
    fun testCreateDir() {
        val request = createRequest("/1/2/3.txt", folder = true, size = 100, metadata = mapOf("key" to "value"))
        nodeService.createNode(request)
        val node = nodeService.getNodeDetail(node("/1/2/3.txt"))!!
        assertEquals(UT_USER, node.createdBy)
        assertNotNull(node.createdDate)
        assertEquals(UT_USER, node.lastModifiedBy)
        assertNotNull(node.lastModifiedDate)
        assertTrue(node.folder)
        assertEquals("/1/2/", node.path)
        assertEquals("3.txt", node.name)
        assertEquals("/1/2/3.txt", node.fullPath)
        assertEquals(0, node.size)
        assertNull(node.sha256)
        assertNull(node.md5)
        assertEquals("value", node.metadata["key"])
    }

    @Test
    @DisplayName("测试自动创建父目录")
    fun testCreateParentPath() {
        nodeService.createNode(createRequest("/1/2/3.txt", folder = false))
        assertNotNull(nodeService.getNodeDetail(node("")))
        assertNotNull(nodeService.getNodeDetail(node("/1")))
        assertNotNull(nodeService.getNodeDetail(node("/1/2")))
        assertNotNull(nodeService.getNodeDetail(node("/1/2/3.txt")))

        nodeService.createNode(createRequest("/a/b/c", folder = true))
        assertNotNull(nodeService.getNodeDetail(node("")))
        assertNotNull(nodeService.getNodeDetail(node("/a")))
        assertNotNull(nodeService.getNodeDetail(node("/a/b")))
        assertNotNull(nodeService.getNodeDetail(node("/a/b/c")))
    }

    @Test
    @DisplayName("测试使用.路径创建节点")
    fun testCreateWithDot() {
        nodeService.createNode(createRequest("/ a / b / . / 2.txt", true))
        assertTrue(nodeService.checkExist(node("./a/b/./2.txt")))
        assertTrue(nodeService.checkExist(node("/a/b/")))
    }

    @Test
    @DisplayName("测试使用..路径创建节点")
    fun testCreateWithDoubleDot() {
        nodeService.createNode(createRequest("/a/b/ .. ", true))
        assertTrue(nodeService.checkExist(node("/a")))
        assertFalse(nodeService.checkExist(node("/a/b")))
        // /aa/bb/.. 应该存在，因为会格式化
        assertTrue(nodeService.checkExist(node("/c/.././a/b/..")))

        nodeService.createNode(createRequest("/aa/bb/ . . ", true))
        assertTrue(nodeService.checkExist(node("/aa")))
        assertTrue(nodeService.checkExist(node("/aa/bb")))
        assertTrue(nodeService.checkExist(node("/aa/bb/. .")))
    }

    @Test
    @DisplayName("测试元数据查询")
    fun testIncludeMetadata() {
        nodeService.createNode(createRequest("/a/b/1.txt", folder = false, metadata = mapOf("key" to "value")))
        nodeService.createNode(createRequest("/a/b/2.txt", folder = false))

        val node1 = nodeService.getNodeDetail(node("/a/b/1.txt"))
        val node2 = nodeService.getNodeDetail(node("/a/b/2.txt"))
        assertNotNull(node1!!.metadata)
        assertNotNull(node1.metadata["key"])
        assertNotNull(node2!!.metadata)
    }

    @Test
    @DisplayName("测试列表查询")
    fun testListNode() {
        assertEquals(0, nodeService.listNode(node(""), option).size)

        nodeService.createNode(createRequest("/a/b/1.txt", false))
        assertEquals(1, nodeService.listNode(node(""), folderOption).size)

        val size = 20
        repeat(size) { i -> nodeService.createNode(createRequest("/a/b/c/$i.txt", false)) }
        repeat(size) { i -> nodeService.createNode(createRequest("/a/b/d/$i.txt", false)) }

        assertEquals(1, nodeService.listNode(node("/a/b"), option).size)
        assertEquals(3, nodeService.listNode(node("/a/b"), folderOption).size)
        assertEquals(size, nodeService.listNode(node("/a/b/c"), folderAndDeepOption).size)
        assertEquals(size * 2 + 1, nodeService.listNode(node("/a/b"), deepOption).size)
        assertEquals(size * 2 + 1 + 2, nodeService.listNode(node("/a/b"), folderAndDeepOption).size)
        assertEquals(size * 2 + 1 + 2 + 2, nodeService.listNode(node("/"), folderAndDeepOption).size)
    }

    @Test
    @DisplayName("测试分页查询")
    fun testListNodePage() {
        val size = 51L
        repeat(size.toInt()) { i -> nodeService.createNode(createRequest("/a/b/c/$i.txt", false)) }

        // 测试从第0页开始，兼容性测试
        var page = nodeService.listNodePage(node("/a/b/c"), page(0))
        assertEquals(10, page.records.size)
        assertEquals(size, page.totalRecords)
        assertEquals(6, page.totalPages)
        assertEquals(10, page.pageSize)
        assertEquals(1, page.pageNumber)

        page = nodeService.listNodePage(node("/a/b/c"), page(1))
        assertEquals(10, page.records.size)
        assertEquals(size, page.totalRecords)
        assertEquals(6, page.totalPages)
        assertEquals(10, page.pageSize)
        assertEquals(1, page.pageNumber)

        page = nodeService.listNodePage(node("/a/b/c"), page(6))
        assertEquals(1, page.records.size)
        assertEquals(size, page.totalRecords)
        assertEquals(6, page.totalPages)
        assertEquals(10, page.pageSize)
        assertEquals(6, page.pageNumber)

        // 测试空页码
        page = nodeService.listNodePage(node("/a/b/c"), page(7))
        assertEquals(0, page.records.size)
        assertEquals(size, page.totalRecords)
        assertEquals(6, page.totalPages)
        assertEquals(10, page.pageSize)
        assertEquals(7, page.pageNumber)
    }

    @Test
    @DisplayName("测试删除文件")
    fun testDeleteFile() {
        nodeService.createNode(createRequest("/a/b/1.txt", false))
        nodeService.deleteNode(
            NodeDeleteRequest(
                projectId = UT_PROJECT_ID,
                repoName = UT_REPO_NAME,
                fullPath = "/a/b/1.txt",
                operator = UT_USER
            )
        )
        assertFalse(nodeService.checkExist(node("/a/b/1.txt")))
    }

    @Test
    @DisplayName("测试删除目录")
    fun testDeleteDir() {
        nodeService.createNode(createRequest("/a/b/1.txt", false))
        nodeService.deleteNode(
            NodeDeleteRequest(
                projectId = UT_PROJECT_ID,
                repoName = UT_REPO_NAME,
                fullPath = "/a",
                operator = UT_USER
            )
        )
        assertFalse(nodeService.checkExist(node("/a/b")))
        assertFalse(nodeService.checkExist(node("/a/b/1.txt")))
    }

    @Test
    @DisplayName("测试正则转义")
    fun testWindowsSeparator() {
        nodeService.createNode(createRequest("/a\\b\\\\c\\/\\d", false))
        assertTrue(nodeService.checkExist(node("/a/b/c/d")))
    }

    @Test
    @DisplayName("测试正则转义")
    fun testEscape() {
        nodeService.createNode(createRequest("/.*|^/a/1.txt", false))
        nodeService.createNode(createRequest("/a/1.txt", false))

        assertEquals(1, nodeService.listNode(node("/.*|^/a"), folderAndDeepOption).size)
        nodeService.deleteByPath(UT_PROJECT_ID, UT_REPO_NAME, "/.*|^/a", UT_USER)
        assertEquals(0, nodeService.listNode(node("/.*|^/a"), folderAndDeepOption).size)
        assertEquals(1, nodeService.listNode(node("/a"), folderAndDeepOption).size)
    }

    @Test
    @DisplayName("测试特殊字符")
    fun testSpecialCharacter() {
        nodeService.createNode(createRequest("/~`!@#$%^&*()_-+=<,>.?/:;\"'{[}]|"))
        val nodeDetail = nodeService.getNodeDetail(node("/~`!@#$%^&*()_-+=<,>.?/:;\"'{[}]|\\"))
        assertEquals("/~`!@#$%^&*()_-+=<,>.?/:;\"'{[}]|", nodeDetail?.fullPath)
    }

    @Test
    @DisplayName("测试计算目录大小")
    fun testComputeDirSize() {
        val size = 20
        repeat(size) { i -> nodeService.createNode(createRequest("/a/b/c/$i.txt", false)) }
        repeat(size) { i -> nodeService.createNode(createRequest("/a/b/d/$i.txt", false)) }

        val pathSizeInfo = nodeService.computeSize(node("/a/b"))

        assertEquals(42, pathSizeInfo.subNodeCount)
        assertEquals(40, pathSizeInfo.size)
    }

    @Test
    @DisplayName("测试计算文件大小")
    fun testComputeFileSize() {
        nodeService.createNode(createRequest("/a/b/c/1.txt", false))

        val fileSizeInfo = nodeService.computeSize(node("/a/b/c/1.txt"))
        assertEquals(0, fileSizeInfo.subNodeCount)
        assertEquals(1, fileSizeInfo.size)
    }

    @Test
    @DisplayName("测试计算根节点大小")
    fun testComputeRootNodeSize() {
        val size = 20
        repeat(size) { i -> nodeService.createNode(createRequest("/a/$i.txt", false)) }
        repeat(size) { i -> nodeService.createNode(createRequest("/b/$i.txt", false)) }

        val pathSizeInfo = nodeService.computeSize(node("/"))

        assertEquals(42, pathSizeInfo.subNodeCount)
        assertEquals(40, pathSizeInfo.size)
    }

    @Test
    @DisplayName("重命名目录")
    fun testRenamePath() {
        nodeService.createNode(createRequest("/a/b/1.txt", false))
        nodeService.createNode(createRequest("/a/b/2.txt", false))
        nodeService.createNode(createRequest("/a/b/c/1.txt", false))
        nodeService.createNode(createRequest("/a/b/c/2.txt", false))

        val renameRequest = NodeRenameRequest(
            projectId = UT_PROJECT_ID,
            repoName = UT_REPO_NAME,
            fullPath = "/a",
            newFullPath = "/aa",
            operator = UT_USER
        )
        nodeService.renameNode(renameRequest)

        assertFalse(nodeService.checkExist(node("/a")))
        assertFalse(nodeService.checkExist(node("/a/b")))
        assertFalse(nodeService.checkExist(node("/a/b/c")))
        assertFalse(nodeService.checkExist(node("/a/b/1.txt")))
        assertFalse(nodeService.checkExist(node("/a/b/c/2.txt")))

        assertTrue(nodeService.checkExist(node("/aa")))
        assertTrue(nodeService.checkExist(node("/aa/b")))
        assertTrue(nodeService.checkExist(node("/aa/b/c")))
        assertTrue(nodeService.checkExist(node("/aa/b/1.txt")))
        assertTrue(nodeService.checkExist(node("/aa/b/c/2.txt")))
    }

    @Test
    @DisplayName("重命名中间目录")
    fun testRenameSubPath() {
        nodeService.createNode(createRequest("/a/b/c", true))

        val renameRequest = NodeRenameRequest(
            projectId = UT_PROJECT_ID,
            repoName = UT_REPO_NAME,
            fullPath = "/a/b/c",
            newFullPath = "/a/d/c",
            operator = UT_USER
        )
        nodeService.renameNode(renameRequest)

        assertTrue(nodeService.checkExist(node("/a")))
        assertTrue(nodeService.checkExist(node("/a/b")))
        assertFalse(nodeService.checkExist(node("/a/b/c")))

        assertTrue(nodeService.checkExist(node("/a/d")))
        assertTrue(nodeService.checkExist(node("/a/d/c")))
    }

    @Test
    @DisplayName("重命名文件，遇同名文件抛异常")
    fun testRenameThrow() {
        nodeService.createNode(createRequest("/a/b/1.txt", false))
        nodeService.createNode(createRequest("/a/b/2.txt", false))
        nodeService.createNode(createRequest("/a/b/c/1.txt", false))
        nodeService.createNode(createRequest("/a/b/c/2.txt", false))

        nodeService.createNode(createRequest("/aa/b/c/2.txt", false))

        val renameRequest = NodeRenameRequest(
            projectId = UT_PROJECT_ID,
            repoName = UT_REPO_NAME,
            fullPath = "/a",
            newFullPath = "/aa",
            operator = UT_USER
        )
        assertThrows<ErrorCodeException> { nodeService.renameNode(renameRequest) }
    }

    @Test
    @DisplayName("移动文件，目录 -> 不存在的目录")
    fun testMovePathToNotExistPath() {
        nodeService.createNode(createRequest("/a/b/c/d/1.txt", false))
        nodeService.createNode(createRequest("/a/b/c/d/2.txt", false))
        nodeService.createNode(createRequest("/a/b/c/d/e/1.txt", false))
        nodeService.createNode(createRequest("/a/b/c/d/e/2.txt", false))
        nodeService.createNode(createRequest("/a/1.txt", false))

        val moveRequest = NodeMoveCopyRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/a/b",
            destFullPath = "/ab",
            operator = UT_USER
        )
        nodeService.moveNode(moveRequest)

        assertTrue(nodeService.checkExist(node("/a")))
        assertTrue(nodeService.checkExist(node("/a/1.txt")))
        assertFalse(nodeService.checkExist(node("/a/b")))
        assertFalse(nodeService.checkExist(node("/a/b/c")))
        assertFalse(nodeService.checkExist(node("/a/b/c/d")))
        assertFalse(nodeService.checkExist(node("/a/b/c/d/e")))
        assertFalse(nodeService.checkExist(node("/a/b/c/d/1.txt")))
        assertFalse(nodeService.checkExist(node("/a/b/c/d/e/2.txt")))

        assertTrue(nodeService.checkExist(node("/ab")))
        assertTrue(nodeService.checkExist(node("/ab/c")))
        assertTrue(nodeService.checkExist(node("/ab/c/d")))
        assertTrue(nodeService.checkExist(node("/ab/c/d/e")))
        assertTrue(nodeService.checkExist(node("/ab/c/d/1.txt")))
        assertTrue(nodeService.checkExist(node("/ab/c/d/e/2.txt")))
    }

    @Test
    @DisplayName("移动文件，目录 -> 存在的目录")
    fun testMovePathToExistPath() {
        nodeService.createNode(createRequest("/a/b/c/d/1.txt", false))
        nodeService.createNode(createRequest("/a/b/c/d/2.txt", false))
        nodeService.createNode(createRequest("/a/b/c/d/e/1.txt", false))
        nodeService.createNode(createRequest("/a/b/c/d/e/2.txt", false))
        nodeService.createNode(createRequest("/a/1.txt", false))
        nodeService.createNode(createRequest("/ab", true))

        var moveRequest = NodeMoveCopyRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/a/b",
            destFullPath = "/ab",
            operator = UT_USER
        )
        nodeService.moveNode(moveRequest)

        assertTrue(nodeService.checkExist(node("/a")))
        assertTrue(nodeService.checkExist(node("/a/1.txt")))
        assertFalse(nodeService.checkExist(node("/a/b")))
        assertFalse(nodeService.checkExist(node("/a/b/c")))
        assertFalse(nodeService.checkExist(node("/a/b/c/d")))
        assertFalse(nodeService.checkExist(node("/a/b/c/d/e")))
        assertFalse(nodeService.checkExist(node("/a/b/c/d/1.txt")))
        assertFalse(nodeService.checkExist(node("/a/b/c/d/e/2.txt")))

        assertTrue(nodeService.checkExist(node("/ab")))
        assertTrue(nodeService.checkExist(node("/ab/b")))
        assertTrue(nodeService.checkExist(node("/ab/b/c")))
        assertTrue(nodeService.checkExist(node("/ab/b/c/d")))
        assertTrue(nodeService.checkExist(node("/ab/b/c/d/1.txt")))
        assertTrue(nodeService.checkExist(node("/ab/b/c/d/e/2.txt")))

        nodeService.createNode(createRequest("/data/mkdir/aa.txt", false))
        nodeService.createNode(createRequest("/data/dir3", true))

        moveRequest = NodeMoveCopyRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/data/mkdir/aa.txt",
            destFullPath = "/data/dir3",
            operator = UT_USER
        )
        nodeService.moveNode(moveRequest)
        assertTrue(nodeService.listNode(node("/data/dir3"), option).size == 1)
    }

    @Test
    @DisplayName("移动文件 -> 存在的目录")
    fun testMoveFileToExistPath() {
        nodeService.createNode(createRequest("/a/1.txt", false))
        nodeService.createNode(createRequest("/ab", true))

        val moveRequest = NodeMoveCopyRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/a/1.txt",
            destFullPath = "/ab",
            operator = UT_USER
        )
        nodeService.moveNode(moveRequest)

        assertTrue(nodeService.checkExist(node("/a")))
        assertFalse(nodeService.checkExist(node("/a/1.txt")))

        assertTrue(nodeService.checkExist(node("/ab")))
        assertTrue(nodeService.checkExist(node("/ab/1.txt")))
    }

    @Test
    @DisplayName("移动文件，文件 -> 不存在的路径")
    fun testMoveFileToNotExistPath() {
        nodeService.createNode(createRequest("/a/1.txt", false))

        val moveRequest = NodeMoveCopyRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/a/1.txt",
            destFullPath = "/ab",
            operator = UT_USER
        )
        nodeService.moveNode(moveRequest)

        assertTrue(nodeService.checkExist(node("/a")))
        assertFalse(nodeService.checkExist(node("/a/1.txt")))

        val destNode = nodeService.getNodeDetail(node("/ab"))
        assertNotNull(destNode)
        assertFalse(destNode!!.folder)
    }

    @Test
    @DisplayName("移动文件，文件 -> 存在的文件且覆盖")
    fun testMoveFileToExistFileAndOverwrite() {
        nodeService.createNode(createRequest("/a/1.txt", false, size = 1))
        nodeService.createNode(createRequest("/ab/a/1.txt", false, size = 2))
        nodeService.createNode(createRequest("/abc/a/1.txt", false, size = 2))

        // path -> path
        var moveRequest = NodeMoveCopyRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/a",
            destFullPath = "/ab",
            operator = UT_USER,
            overwrite = true
        )
        nodeService.moveNode(moveRequest)

        // file -> file
        var node = nodeService.getNodeDetail(node("/ab/a/1.txt"))!!
        assertEquals(1, node.size)

        moveRequest = NodeMoveCopyRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/ab/a/1.txt",
            destFullPath = "/abc/a/1.txt",
            operator = UT_USER,
            overwrite = true
        )
        nodeService.moveNode(moveRequest)

        node = nodeService.getNodeDetail(node("/abc/a/1.txt"))!!
        assertEquals(1, node.size)
    }

    @Test
    @DisplayName("移动文件，文件 -> 存在的文件且不覆盖")
    fun testMoveOverwriteThrow() {
        nodeService.createNode(createRequest("/a/1.txt", false))
        nodeService.createNode(createRequest("/ab/a/1.txt", false))

        val moveRequest = NodeMoveCopyRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/a",
            destFullPath = "/ab",
            operator = UT_USER
        )
        assertThrows<ErrorCodeException> { nodeService.moveNode(moveRequest) }
    }

    @Test
    @DisplayName("移动文件，目录 -> 自己")
    fun testMovePathToSelf() {
        nodeService.createNode(createRequest("/a/1.txt", false))
        nodeService.createNode(createRequest("/a/b/1.txt", false))

        val moveRequest = NodeMoveCopyRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/a",
            destFullPath = "/a",
            operator = UT_USER
        )
        nodeService.moveNode(moveRequest)
    }

    @Test
    @DisplayName("移动文件, 目录 -> 父目录")
    fun testMovePathToParentPath() {
        nodeService.createNode(createRequest("/a/b/1.txt", false))

        val moveRequest = NodeMoveCopyRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/a/b",
            destFullPath = "/a",
            operator = UT_USER
        )
        nodeService.moveNode(moveRequest)

        assertTrue(nodeService.checkExist(node("/a/b")))
        assertTrue(nodeService.checkExist(node("/a/b/1.txt")))
    }

    @Test
    @DisplayName("移动文件, 目录 -> 根目录")
    fun testMoveToRootPath() {
        nodeService.createNode(createRequest("/a/b/1.txt", false))

        val moveRequest = NodeMoveCopyRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/a/b",
            destFullPath = "",
            operator = UT_USER
        )
        nodeService.moveNode(moveRequest)
        assertTrue(nodeService.checkExist(node("/b/1.txt")))
        assertTrue(nodeService.checkExist(node("/a")))
        assertFalse(nodeService.checkExist(node("/a/b")))
    }

    @Test
    @DisplayName("移动文件, 文件 -> 父目录")
    fun testMoveFileToRootPath() {
        nodeService.createNode(createRequest("/a/b/1.txt", false))

        val moveRequest = NodeMoveCopyRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/a/b/1.txt",
            destFullPath = "/a/b",
            operator = UT_USER
        )
        nodeService.moveNode(moveRequest)
        assertTrue(nodeService.checkExist(node("/a/b")))
        assertTrue(nodeService.checkExist(node("/a/b/1.txt")))
    }

    @Test
    @DisplayName("拷贝文件, 目录 -> 不存在的目录")
    fun testCopy() {
        nodeService.createNode(createRequest("/a/b/c/d/1.txt", false))
        nodeService.createNode(createRequest("/a/b/c/d/2.txt", false))
        nodeService.createNode(createRequest("/a/b/c/d/e/1.txt", false))
        nodeService.createNode(createRequest("/a/b/c/d/e/2.txt", false))
        nodeService.createNode(createRequest("/a/1.txt", false))

        val copyRequest = NodeMoveCopyRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/a/b",
            destFullPath = "/ab",
            operator = UT_USER
        )
        nodeService.copyNode(copyRequest)

        assertTrue(nodeService.checkExist(node("/a")))
        assertTrue(nodeService.checkExist(node("/a/1.txt")))
        assertTrue(nodeService.checkExist(node("/a/b")))
        assertTrue(nodeService.checkExist(node("/a/b/c")))
        assertTrue(nodeService.checkExist(node("/a/b/c/d")))
        assertTrue(nodeService.checkExist(node("/a/b/c/d/e")))
        assertTrue(nodeService.checkExist(node("/a/b/c/d/1.txt")))
        assertTrue(nodeService.checkExist(node("/a/b/c/d/e/2.txt")))

        assertTrue(nodeService.checkExist(node("/ab")))
        assertTrue(nodeService.checkExist(node("/ab/c")))
        assertTrue(nodeService.checkExist(node("/ab/c/d")))
        assertTrue(nodeService.checkExist(node("/ab/c/d/e")))
        assertTrue(nodeService.checkExist(node("/ab/c/d/1.txt")))
        assertTrue(nodeService.checkExist(node("/ab/c/d/e/2.txt")))
    }

    @Test
    @DisplayName("拷贝文件 -> 存在的目录")
    fun testCopyFileToExistPath() {
        nodeService.createNode(createRequest("/a/1.txt", false))
        nodeService.createNode(createRequest("/b", true))

        val copyRequest = NodeMoveCopyRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/a/1.txt",
            destFullPath = "/b/",
            operator = UT_USER,
            overwrite = true
        )
        nodeService.copyNode(copyRequest)

        assertTrue(nodeService.getNodeDetail(node("/b"))?.folder == true)
        assertTrue(nodeService.getNodeDetail(node("/b/1.txt"))?.folder == false)
    }

    @Test
    @DisplayName("拷贝文件, 元数据一起拷贝")
    fun testCopyWithMetadata() {
        nodeService.createNode(createRequest("/a", false, metadata = mapOf("key" to "value")))

        val copyRequest = NodeMoveCopyRequest(
            srcProjectId = UT_PROJECT_ID,
            srcRepoName = UT_REPO_NAME,
            srcFullPath = "/a",
            destFullPath = "/b",
            operator = UT_USER
        )
        nodeService.copyNode(copyRequest)
        assertEquals("value", nodeService.getNodeDetail(node("/b"))!!.metadata["key"])
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

    private fun node(fullPath: String = ROOT): ArtifactInfo {
        return DefaultArtifactInfo(UT_PROJECT_ID, UT_REPO_NAME, fullPath)
    }

    private fun page(pageNumber: Int, pageSize: Int = 10): NodeListOption {
        return NodeListOption(
            pageNumber = pageNumber,
            pageSize = pageSize,
            includeFolder = false,
            includeMetadata = false,
            deep = false,
            sort = false
        )
    }
}
