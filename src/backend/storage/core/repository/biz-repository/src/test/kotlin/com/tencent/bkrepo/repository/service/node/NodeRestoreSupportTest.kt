/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.repository.service.node

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.api.DefaultArtifactInfo
import com.tencent.bkrepo.repository.UT_PROJECT_ID
import com.tencent.bkrepo.repository.UT_REPO_NAME
import com.tencent.bkrepo.repository.constant.SYSTEM_USER
import com.tencent.bkrepo.repository.dao.FileReferenceDao
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.pojo.node.ConflictStrategy
import com.tencent.bkrepo.repository.pojo.node.NodeRestoreOption
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.service.ServiceBaseTest
import com.tencent.bkrepo.repository.service.repo.ProjectService
import com.tencent.bkrepo.repository.service.repo.RepositoryService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import java.time.LocalDateTime

@DisplayName("节点恢复测试")
@DataMongoTest
@Import(
    NodeDao::class,
    FileReferenceDao::class
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeRestoreSupportTest @Autowired constructor(
    private val projectService: ProjectService,
    private val repositoryService: RepositoryService,
    private val nodeService: NodeService,
    private val nodeDao: NodeDao
) : ServiceBaseTest() {

    @BeforeAll
    fun beforeAll() {
        initMock()
        initRepoForUnitTest(projectService, repositoryService)
    }

    @BeforeEach
    fun beforeEach() {
        clearDatabase()
    }

    @Test
    @DisplayName("测试恢复无冲突的单个文件")
    fun testRestoreFile1() {
        // 创建
        createNode("/a/1", size = 1)
        val artifactInfo = artifactInfo("/a/1")
        Assertions.assertTrue(nodeService.checkExist(artifactInfo))
        Assertions.assertEquals(0, nodeService.listDeletedPoint(artifactInfo).size)
        // 删除
        deleteNode("/a/1")
        Assertions.assertFalse(nodeService.checkExist(artifactInfo))
        val list1 = nodeService.listDeletedPoint(artifactInfo)
        Assertions.assertEquals(1, list1.size)
        // 再次创建
        createNode("/a/1", size = 2)
        // 再次删除
        deleteNode("/a/1")
        Assertions.assertFalse(nodeService.checkExist(artifactInfo))
        val list2 = nodeService.listDeletedPoint(artifactInfo)
        Assertions.assertEquals(2, list2.size)
        // 恢复
        val option = NodeRestoreOption(deletedTime = list2[0].deletedTime)
        val result = nodeService.restoreNode(artifactInfo, option)
        Assertions.assertEquals("/a/1", result.fullPath)
        Assertions.assertEquals(1, result.restoreCount)
        Assertions.assertEquals(0, result.skipCount)
        Assertions.assertEquals(0, result.conflictCount)
        // 判断恢复的最近一次
        Assertions.assertEquals(2, nodeService.getNodeDetail(artifactInfo)!!.size)
        Assertions.assertEquals(1, nodeService.listDeletedPoint(artifactInfo).size)
    }

    @Test
    @DisplayName("测试恢复存在冲突的单个文件")
    fun testRestoreFile2() {
        // 创建
        createNode("/a/1", size = 1)
        val artifactInfo = artifactInfo("/a/1")
        // 再次创建
        createNode("/a/1", size = 2)
        val list = nodeService.listDeletedPoint(artifactInfo)
        Assertions.assertEquals(1, list.size)

        // FAILED 模式恢复
        val option1 = NodeRestoreOption(deletedTime = list[0].deletedTime, conflictStrategy = ConflictStrategy.FAILED)
        assertThrows<ErrorCodeException> { nodeService.restoreNode(artifactInfo, option1) }
        // 数据不会有变化
        Assertions.assertEquals(2, nodeService.getNodeDetail(artifactInfo)!!.size)
        Assertions.assertEquals(1, nodeService.listDeletedPoint(artifactInfo)[0].size)

        // SKIP 模式恢复
        val option2 = option1.copy(conflictStrategy = ConflictStrategy.SKIP)
        val result2 = nodeService.restoreNode(artifactInfo, option2)
        // 数据不会有变化
        Assertions.assertEquals(0, result2.restoreCount)
        Assertions.assertEquals(1, result2.skipCount)
        Assertions.assertEquals(0, result2.conflictCount)
        Assertions.assertEquals(2, nodeService.getNodeDetail(artifactInfo)!!.size)
        Assertions.assertEquals(1, nodeService.listDeletedPoint(artifactInfo)[0].size)

        // OVERWRITE 模式恢复
        val option3 = option1.copy(conflictStrategy = ConflictStrategy.OVERWRITE)
        val result3 = nodeService.restoreNode(artifactInfo, option3)
        // 数据发生变化
        Assertions.assertEquals(1, result3.restoreCount)
        Assertions.assertEquals(0, result3.skipCount)
        Assertions.assertEquals(1, result3.conflictCount)
        Assertions.assertEquals(1, nodeService.getNodeDetail(artifactInfo)!!.size)
        Assertions.assertEquals(2, nodeService.listDeletedPoint(artifactInfo)[0].size)
    }

    @Test
    @DisplayName("测试恢复根目录")
    fun testRestoreRoot() {
        // 创建
        createNode("/1")
        createNode("/a/1")
        val rootArtifactInfo = artifactInfo("/")
        val subArtifactInfo = artifactInfo("/a")
        // 删除
        deleteNode("/")
        // 恢复
        // 因为数据库本身没有记录根目录，所以无法查询到根目录的删除点
        Assertions.assertEquals(0, nodeService.listDeletedPoint(rootArtifactInfo).size)
        // 使用子节点的删除点来进行恢复
        val deletedTime = nodeService.listDeletedPoint(subArtifactInfo)[0].deletedTime
        val option = NodeRestoreOption(deletedTime = deletedTime)
        val result = nodeService.restoreNode(rootArtifactInfo, option)
        Assertions.assertEquals("/", result.fullPath)
        Assertions.assertEquals(3, result.restoreCount)
        Assertions.assertEquals(0, result.skipCount)
        Assertions.assertEquals(0, result.conflictCount)
    }

    @Test
    @DisplayName("测试恢复无冲突的目录")
    fun testRestoreFolder1() {
        val deletedTime = initTestFolder(false)
        val option = NodeRestoreOption(deletedTime = deletedTime)
        val result = nodeService.restoreNode(artifactInfo("/a"), option)

        Assertions.assertEquals(5, result.restoreCount)
        Assertions.assertEquals(0, result.skipCount)
        Assertions.assertEquals(0, result.conflictCount)
        assertFolderData(false)
    }

    @Test
    @DisplayName("测试FAILED模式恢复存在冲突的目录")
    fun testRestoreFolder2() {
        val deletedTime = initTestFolder(true)
        val option = NodeRestoreOption(deletedTime = deletedTime, conflictStrategy = ConflictStrategy.FAILED)
        assertThrows<ErrorCodeException> { nodeService.restoreNode(artifactInfo("/"), option) }
    }

    @Test
    @DisplayName("测试SKIP模式恢复存在冲突的目录")
    fun testRestoreFolder3() {
        val deletedTime = initTestFolder(true)
        val option = NodeRestoreOption(deletedTime = deletedTime, conflictStrategy = ConflictStrategy.SKIP)
        val result = nodeService.restoreNode(artifactInfo("/a"), option)

        Assertions.assertEquals(2, result.restoreCount)
        Assertions.assertEquals(3, result.skipCount)
        Assertions.assertEquals(0, result.conflictCount)
        assertFolderData(true)
    }

    @Test
    @DisplayName("测试OVERWRITE模式恢复存在冲突的目录")
    fun testRestoreFolder4() {
        val deletedTime = initTestFolder(true)
        val option = NodeRestoreOption(deletedTime = deletedTime, conflictStrategy = ConflictStrategy.OVERWRITE)
        val result = nodeService.restoreNode(artifactInfo("/a"), option)

        Assertions.assertEquals(5, result.restoreCount)
        Assertions.assertEquals(0, result.skipCount)
        Assertions.assertEquals(3, result.conflictCount)
        assertFolderData(true)
    }

    @Test
    @DisplayName("测试查询删除点列表")
    fun listDeletedPoint() {
        createNode("/a/1", size = 1)
        createNode("/a/2")
        createNode("/a/1", size = 2)
        createNode("/a/1", size = 3)

        val list = nodeService.listDeletedPoint(artifactInfo("/a/1"))
        Assertions.assertEquals(2, list.size)
        Assertions.assertTrue(list[0].deletedTime.isAfter(list[1].deletedTime))
        Assertions.assertEquals(2, list[0].size)
        Assertions.assertEquals(1, list[1].size)
        Assertions.assertEquals(0, nodeService.listDeletedPoint(artifactInfo("/a/2")).size)
        Assertions.assertEquals(0, nodeService.listDeletedPoint(artifactInfo("/a/3")).size)
    }

    private fun initTestFolder(conflict: Boolean): LocalDateTime {
        // 创建
        createNode("/a/1")
        createNode("/a/2")
        createNode("/a/b/1")
        createNode("/b/1")
        // 删除
        deleteNode("/a")
        if (conflict) {
            // 创建
            createNode("/a/1")
            createNode("/a/b/2")
        }
        return nodeService.listDeletedPoint(artifactInfo("/a"))[0].deletedTime
    }

    private fun assertFolderData(conflict: Boolean) {
        Assertions.assertTrue(nodeService.checkExist(artifactInfo("/a")))
        Assertions.assertTrue(nodeService.checkExist(artifactInfo("/a/1")))
        Assertions.assertTrue(nodeService.checkExist(artifactInfo("/a/2")))
        Assertions.assertTrue(nodeService.checkExist(artifactInfo("/a/b")))
        Assertions.assertTrue(nodeService.checkExist(artifactInfo("/a/b/1")))
        Assertions.assertTrue(nodeService.checkExist(artifactInfo("/b/1")))
        if (conflict) {
            Assertions.assertTrue(nodeService.checkExist(artifactInfo("/a/b/2")))
        }
    }

    private fun createNode(fullPath: String, size: Long = 1L) {
        val request = NodeCreateRequest(
            projectId = UT_PROJECT_ID,
            repoName = UT_REPO_NAME,
            fullPath = fullPath,
            folder = false,
            overwrite = true,
            size = size,
            sha256 = "sha256",
            md5 = "md5"
        )
        nodeService.createNode(request)
    }

    private fun deleteNode(fullPath: String) {
        nodeService.deleteByPath(
            projectId = UT_PROJECT_ID,
            repoName = UT_REPO_NAME,
            fullPath = fullPath,
            operator = SYSTEM_USER
        )
    }

    private fun clearDatabase() {
        nodeDao.remove(Query(where(TNode::projectId).isEqualTo(UT_PROJECT_ID)))
    }

    private fun artifactInfo(fullPath: String): ArtifactInfo {
        return DefaultArtifactInfo(
            projectId = UT_PROJECT_ID,
            repoName = UT_REPO_NAME,
            artifactUri = fullPath
        )
    }
}
