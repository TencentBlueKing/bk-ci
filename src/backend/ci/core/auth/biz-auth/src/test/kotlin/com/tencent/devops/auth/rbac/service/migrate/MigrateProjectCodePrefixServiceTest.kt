/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.tencent.devops.auth.rbac.service.migrate

import com.tencent.devops.auth.dao.AuthResourceDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.dao.AuthResourceGroupMemberDao
import com.tencent.devops.auth.pojo.AuthResourceGroup
import com.tencent.devops.auth.provider.rbac.service.migrate.MigrateProjectCodePrefixService
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.auth.service.iam.PermissionResourceService
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.test.BkCiAbstractTest
import com.tencent.devops.model.auth.tables.TAuthResource
import com.tencent.devops.model.auth.tables.records.TAuthResourceRecord
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@Suppress("LongMethod")
class MigrateProjectCodePrefixServiceTest : BkCiAbstractTest() {

    private val authResourceDao = mockk<AuthResourceDao>()
    private val authResourceGroupDao = mockk<AuthResourceGroupDao>()
    private val authResourceGroupMemberDao =
        mockk<AuthResourceGroupMemberDao>()
    private val permissionResourceService =
        mockk<PermissionResourceService>()
    private val permissionResourceMemberService =
        mockk<PermissionResourceMemberService>()

    private val service = MigrateProjectCodePrefixService(
        dslContext = dslContext,
        authResourceDao = authResourceDao,
        authResourceGroupDao = authResourceGroupDao,
        authResourceGroupMemberDao = authResourceGroupMemberDao,
        permissionResourceService = permissionResourceService,
        permissionResourceMemberService = permissionResourceMemberService
    )

    companion object {
        private const val OLD_PROJECT_CODE = "myproject"
        private const val NEW_PROJECT_CODE = "tencent.myproject"
        private const val PIPELINE_CODE = "p-001"
    }

    /**
     * 构造 TAuthResourceRecord mock
     */
    private fun mockResourceRecord(
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): TAuthResourceRecord {
        val record = mockk<TAuthResourceRecord>(relaxed = true)
        every { record.projectCode } returns projectCode
        every { record.resourceType } returns resourceType
        every { record.resourceCode } returns resourceCode
        return record
    }

    /**
     * 构造 AuthResourceGroup
     */
    private fun buildGroup(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        groupCode: String,
        groupName: String,
        relationId: Int
    ): AuthResourceGroup {
        return AuthResourceGroup(
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode,
            resourceName = "name-$resourceCode",
            iamResourceCode = resourceCode,
            groupCode = groupCode,
            groupName = groupName,
            defaultGroup = true,
            relationId = relationId
        )
    }

    /**
     * 设置 authResourceDao.list 返回指定资源列表
     */
    private fun mockResourceList(
        projectCode: String,
        records: List<TAuthResourceRecord>
    ) {
        val result = dslContext.mockResult(
            TAuthResource.T_AUTH_RESOURCE,
            *records.toTypedArray()
        )
        every {
            authResourceDao.list(
                dslContext = any(),
                projectCode = projectCode,
                resourceName = null,
                resourceType = null,
                offset = 0,
                limit = 10000
            )
        } returns result
    }

    /**
     * 设置 authResourceGroupDao.getByResourceCode 返回
     */
    private fun mockGroupsByResource(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        groups: List<AuthResourceGroup>
    ) {
        every {
            authResourceGroupDao.getByResourceCode(
                dslContext = any(),
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode
            )
        } returns groups
    }

    @Nested
    @DisplayName("buildNewGroupMap 测试")
    inner class BuildNewGroupMapTests {

        @Test
        @DisplayName("项目资源组 - resourceCode 加了前缀后能正确匹配")
        fun `project resource groups match with prefix`() {
            // Arrange: 旧项目有一个项目资源
            val oldProjectRecord = mockResourceRecord(
                projectCode = OLD_PROJECT_CODE,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = OLD_PROJECT_CODE
            )
            mockResourceList(OLD_PROJECT_CODE, listOf(oldProjectRecord))
            // 旧项目的项目资源下有一个管理员组
            val oldGroup = buildGroup(
                projectCode = OLD_PROJECT_CODE,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = OLD_PROJECT_CODE,
                groupCode = "manager",
                groupName = "管理员",
                relationId = 100
            )
            mockGroupsByResource(
                projectCode = OLD_PROJECT_CODE,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = OLD_PROJECT_CODE,
                groups = listOf(oldGroup)
            )

            // 新项目有一个项目资源（resourceCode 带前缀）
            val newProjectRecord = mockResourceRecord(
                projectCode = NEW_PROJECT_CODE,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = NEW_PROJECT_CODE
            )
            mockResourceList(NEW_PROJECT_CODE, listOf(newProjectRecord))
            // 新项目的项目资源下有一个管理员组
            val newGroup = buildGroup(
                projectCode = NEW_PROJECT_CODE,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = NEW_PROJECT_CODE,
                groupCode = "manager",
                groupName = "管理员",
                relationId = 200
            )
            mockGroupsByResource(
                projectCode = NEW_PROJECT_CODE,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = NEW_PROJECT_CODE,
                groups = listOf(newGroup)
            )

            // Act
            @Suppress("UNCHECKED_CAST")
            val result = service.invokePrivate<
                Map<Int, AuthResourceGroup>
            >(
                "buildNewGroupMap",
                OLD_PROJECT_CODE,
                NEW_PROJECT_CODE
            )!!

            // Assert
            Assertions.assertEquals(1, result.size)
            Assertions.assertTrue(result.containsKey(100))
            Assertions.assertEquals(200, result[100]!!.relationId)
            Assertions.assertEquals(
                NEW_PROJECT_CODE,
                result[100]!!.resourceCode
            )
        }

        @Test
        @DisplayName("非项目资源组 - resourceCode 不变能正确匹配")
        fun `non-project resource groups match without prefix`() {
            // Arrange: 旧项目有一个流水线资源
            val oldPipelineRecord = mockResourceRecord(
                projectCode = OLD_PROJECT_CODE,
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceCode = PIPELINE_CODE
            )
            mockResourceList(
                OLD_PROJECT_CODE, listOf(oldPipelineRecord)
            )
            val oldGroup = buildGroup(
                projectCode = OLD_PROJECT_CODE,
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceCode = PIPELINE_CODE,
                groupCode = "viewer",
                groupName = "查看者",
                relationId = 101
            )
            mockGroupsByResource(
                projectCode = OLD_PROJECT_CODE,
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceCode = PIPELINE_CODE,
                groups = listOf(oldGroup)
            )

            // 新项目有同一流水线资源（resourceCode 不变）
            val newPipelineRecord = mockResourceRecord(
                projectCode = NEW_PROJECT_CODE,
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceCode = PIPELINE_CODE
            )
            mockResourceList(
                NEW_PROJECT_CODE, listOf(newPipelineRecord)
            )
            val newGroup = buildGroup(
                projectCode = NEW_PROJECT_CODE,
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceCode = PIPELINE_CODE,
                groupCode = "viewer",
                groupName = "查看者",
                relationId = 201
            )
            mockGroupsByResource(
                projectCode = NEW_PROJECT_CODE,
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceCode = PIPELINE_CODE,
                groups = listOf(newGroup)
            )

            // Act
            @Suppress("UNCHECKED_CAST")
            val result = service.invokePrivate<
                Map<Int, AuthResourceGroup>
            >(
                "buildNewGroupMap",
                OLD_PROJECT_CODE,
                NEW_PROJECT_CODE
            )!!

            // Assert
            Assertions.assertEquals(1, result.size)
            Assertions.assertTrue(result.containsKey(101))
            Assertions.assertEquals(201, result[101]!!.relationId)
        }

        @Test
        @DisplayName("混合资源 - 项目资源和非项目资源同时匹配")
        fun `mixed resources match correctly`() {
            // Arrange: 旧项目有项目资源 + 流水线资源
            val oldProjectRecord = mockResourceRecord(
                projectCode = OLD_PROJECT_CODE,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = OLD_PROJECT_CODE
            )
            val oldPipelineRecord = mockResourceRecord(
                projectCode = OLD_PROJECT_CODE,
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceCode = PIPELINE_CODE
            )
            mockResourceList(
                OLD_PROJECT_CODE,
                listOf(oldProjectRecord, oldPipelineRecord)
            )

            val oldProjectGroup = buildGroup(
                projectCode = OLD_PROJECT_CODE,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = OLD_PROJECT_CODE,
                groupCode = "manager",
                groupName = "管理员",
                relationId = 100
            )
            val oldPipelineGroup = buildGroup(
                projectCode = OLD_PROJECT_CODE,
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceCode = PIPELINE_CODE,
                groupCode = "editor",
                groupName = "编辑者",
                relationId = 101
            )
            mockGroupsByResource(
                projectCode = OLD_PROJECT_CODE,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = OLD_PROJECT_CODE,
                groups = listOf(oldProjectGroup)
            )
            mockGroupsByResource(
                projectCode = OLD_PROJECT_CODE,
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceCode = PIPELINE_CODE,
                groups = listOf(oldPipelineGroup)
            )

            // 新项目
            val newProjectRecord = mockResourceRecord(
                projectCode = NEW_PROJECT_CODE,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = NEW_PROJECT_CODE
            )
            val newPipelineRecord = mockResourceRecord(
                projectCode = NEW_PROJECT_CODE,
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceCode = PIPELINE_CODE
            )
            mockResourceList(
                NEW_PROJECT_CODE,
                listOf(newProjectRecord, newPipelineRecord)
            )

            val newProjectGroup = buildGroup(
                projectCode = NEW_PROJECT_CODE,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = NEW_PROJECT_CODE,
                groupCode = "manager",
                groupName = "管理员",
                relationId = 200
            )
            val newPipelineGroup = buildGroup(
                projectCode = NEW_PROJECT_CODE,
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceCode = PIPELINE_CODE,
                groupCode = "editor",
                groupName = "编辑者",
                relationId = 201
            )
            mockGroupsByResource(
                projectCode = NEW_PROJECT_CODE,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = NEW_PROJECT_CODE,
                groups = listOf(newProjectGroup)
            )
            mockGroupsByResource(
                projectCode = NEW_PROJECT_CODE,
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceCode = PIPELINE_CODE,
                groups = listOf(newPipelineGroup)
            )

            // Act
            @Suppress("UNCHECKED_CAST")
            val result = service.invokePrivate<
                Map<Int, AuthResourceGroup>
            >(
                "buildNewGroupMap",
                OLD_PROJECT_CODE,
                NEW_PROJECT_CODE
            )!!

            // Assert
            Assertions.assertEquals(2, result.size)
            Assertions.assertEquals(200, result[100]!!.relationId)
            Assertions.assertEquals(201, result[101]!!.relationId)
        }

        @Test
        @DisplayName("无匹配组 - 旧组在新项目中无对应组时映射为空")
        fun `unmatched groups are skipped`() {
            // Arrange: 旧项目有一个组
            val oldProjectRecord = mockResourceRecord(
                projectCode = OLD_PROJECT_CODE,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = OLD_PROJECT_CODE
            )
            mockResourceList(
                OLD_PROJECT_CODE, listOf(oldProjectRecord)
            )
            val oldGroup = buildGroup(
                projectCode = OLD_PROJECT_CODE,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = OLD_PROJECT_CODE,
                groupCode = "custom",
                groupName = "自定义组A",
                relationId = 100
            )
            mockGroupsByResource(
                projectCode = OLD_PROJECT_CODE,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = OLD_PROJECT_CODE,
                groups = listOf(oldGroup)
            )

            // 新项目没有任何资源
            mockResourceList(NEW_PROJECT_CODE, emptyList())

            // Act
            @Suppress("UNCHECKED_CAST")
            val result = service.invokePrivate<
                Map<Int, AuthResourceGroup>
            >(
                "buildNewGroupMap",
                OLD_PROJECT_CODE,
                NEW_PROJECT_CODE
            )!!

            // Assert
            Assertions.assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("旧项目无组 - 返回空映射")
        fun `empty old groups returns empty map`() {
            // Arrange: 旧项目无资源
            mockResourceList(OLD_PROJECT_CODE, emptyList())

            // 新项目有组
            val newProjectRecord = mockResourceRecord(
                projectCode = NEW_PROJECT_CODE,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = NEW_PROJECT_CODE
            )
            mockResourceList(
                NEW_PROJECT_CODE, listOf(newProjectRecord)
            )
            mockGroupsByResource(
                projectCode = NEW_PROJECT_CODE,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = NEW_PROJECT_CODE,
                groups = listOf(
                    buildGroup(
                        projectCode = NEW_PROJECT_CODE,
                        resourceType = AuthResourceType.PROJECT.value,
                        resourceCode = NEW_PROJECT_CODE,
                        groupCode = "manager",
                        groupName = "管理员",
                        relationId = 200
                    )
                )
            )

            // Act
            @Suppress("UNCHECKED_CAST")
            val result = service.invokePrivate<
                Map<Int, AuthResourceGroup>
            >(
                "buildNewGroupMap",
                OLD_PROJECT_CODE,
                NEW_PROJECT_CODE
            )!!

            // Assert
            Assertions.assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName(
            "同名组不同resourceType - 不会错误匹配"
        )
        fun `same groupName different resourceType no match`() {
            // Arrange: 旧项目有项目资源下的"查看者"组
            val oldProjectRecord = mockResourceRecord(
                projectCode = OLD_PROJECT_CODE,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = OLD_PROJECT_CODE
            )
            mockResourceList(
                OLD_PROJECT_CODE, listOf(oldProjectRecord)
            )
            val oldGroup = buildGroup(
                projectCode = OLD_PROJECT_CODE,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = OLD_PROJECT_CODE,
                groupCode = "viewer",
                groupName = "查看者",
                relationId = 100
            )
            mockGroupsByResource(
                projectCode = OLD_PROJECT_CODE,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = OLD_PROJECT_CODE,
                groups = listOf(oldGroup)
            )

            // 新项目只有流水线资源下的"查看者"组（类型不同）
            val newPipelineRecord = mockResourceRecord(
                projectCode = NEW_PROJECT_CODE,
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceCode = PIPELINE_CODE
            )
            mockResourceList(
                NEW_PROJECT_CODE, listOf(newPipelineRecord)
            )
            val newGroup = buildGroup(
                projectCode = NEW_PROJECT_CODE,
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceCode = PIPELINE_CODE,
                groupCode = "viewer",
                groupName = "查看者",
                relationId = 200
            )
            mockGroupsByResource(
                projectCode = NEW_PROJECT_CODE,
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceCode = PIPELINE_CODE,
                groups = listOf(newGroup)
            )

            // Act
            @Suppress("UNCHECKED_CAST")
            val result = service.invokePrivate<
                Map<Int, AuthResourceGroup>
            >(
                "buildNewGroupMap",
                OLD_PROJECT_CODE,
                NEW_PROJECT_CODE
            )!!

            // Assert: 类型不同，不应匹配
            Assertions.assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName(
            "同名组同resourceType不同resourceCode - 不会错误匹配"
        )
        fun `same groupName same type different code no match`() {
            // Arrange: 旧项目有流水线 p-001 的"查看者"组
            val oldPipelineRecord = mockResourceRecord(
                projectCode = OLD_PROJECT_CODE,
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceCode = PIPELINE_CODE
            )
            mockResourceList(
                OLD_PROJECT_CODE, listOf(oldPipelineRecord)
            )
            val oldGroup = buildGroup(
                projectCode = OLD_PROJECT_CODE,
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceCode = PIPELINE_CODE,
                groupCode = "viewer",
                groupName = "查看者",
                relationId = 100
            )
            mockGroupsByResource(
                projectCode = OLD_PROJECT_CODE,
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceCode = PIPELINE_CODE,
                groups = listOf(oldGroup)
            )

            // 新项目只有流水线 p-002 的"查看者"组
            val newPipelineRecord = mockResourceRecord(
                projectCode = NEW_PROJECT_CODE,
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceCode = "p-002"
            )
            mockResourceList(
                NEW_PROJECT_CODE, listOf(newPipelineRecord)
            )
            val newGroup = buildGroup(
                projectCode = NEW_PROJECT_CODE,
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceCode = "p-002",
                groupCode = "viewer",
                groupName = "查看者",
                relationId = 200
            )
            mockGroupsByResource(
                projectCode = NEW_PROJECT_CODE,
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceCode = "p-002",
                groups = listOf(newGroup)
            )

            // Act
            @Suppress("UNCHECKED_CAST")
            val result = service.invokePrivate<
                Map<Int, AuthResourceGroup>
            >(
                "buildNewGroupMap",
                OLD_PROJECT_CODE,
                NEW_PROJECT_CODE
            )!!

            // Assert: resourceCode 不同，不应匹配
            Assertions.assertTrue(result.isEmpty())
        }
    }
}
