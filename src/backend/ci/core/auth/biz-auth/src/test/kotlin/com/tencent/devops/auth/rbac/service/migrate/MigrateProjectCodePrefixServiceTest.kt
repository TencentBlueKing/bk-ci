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
            val oldProjectRecord = mockResourceRecord(
                projectCode = OLD_PROJECT_CODE,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = OLD_PROJECT_CODE
            )
            mockResourceList(OLD_PROJECT_CODE, listOf(oldProjectRecord))
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

            val newProjectRecord = mockResourceRecord(
                projectCode = NEW_PROJECT_CODE,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = NEW_PROJECT_CODE
            )
            mockResourceList(NEW_PROJECT_CODE, listOf(newProjectRecord))
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

            @Suppress("UNCHECKED_CAST")
            val result = service.invokePrivate<
                Map<Int, AuthResourceGroup>
                >(
                "buildNewGroupMap",
                OLD_PROJECT_CODE,
                NEW_PROJECT_CODE
            )!!

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

            @Suppress("UNCHECKED_CAST")
            val result = service.invokePrivate<
                Map<Int, AuthResourceGroup>
                >(
                "buildNewGroupMap",
                OLD_PROJECT_CODE,
                NEW_PROJECT_CODE
            )!!

            Assertions.assertEquals(1, result.size)
            Assertions.assertTrue(result.containsKey(101))
            Assertions.assertEquals(201, result[101]!!.relationId)
        }

        @Test
        @DisplayName("无匹配组 - 旧组在新项目中无对应组时映射为空")
        fun `unmatched groups are skipped`() {
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

            mockResourceList(NEW_PROJECT_CODE, emptyList())

            @Suppress("UNCHECKED_CAST")
            val result = service.invokePrivate<
                Map<Int, AuthResourceGroup>
                >(
                "buildNewGroupMap",
                OLD_PROJECT_CODE,
                NEW_PROJECT_CODE
            )!!

            Assertions.assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName(
            "新项目下同匹配键多条组时 associateBy 保留后者"
        )
        fun `duplicate new group match keys keep last group`() {
            val oldProjectRecord = mockResourceRecord(
                projectCode = OLD_PROJECT_CODE,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = OLD_PROJECT_CODE
            )
            mockResourceList(OLD_PROJECT_CODE, listOf(oldProjectRecord))
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

            val newProjectRecord = mockResourceRecord(
                projectCode = NEW_PROJECT_CODE,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = NEW_PROJECT_CODE
            )
            mockResourceList(NEW_PROJECT_CODE, listOf(newProjectRecord))
            val firstNew = buildGroup(
                projectCode = NEW_PROJECT_CODE,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = NEW_PROJECT_CODE,
                groupCode = "manager",
                groupName = "管理员",
                relationId = 200
            )
            val secondNew = firstNew.copy(relationId = 300)
            mockGroupsByResource(
                projectCode = NEW_PROJECT_CODE,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = NEW_PROJECT_CODE,
                groups = listOf(firstNew, secondNew)
            )

            @Suppress("UNCHECKED_CAST")
            val result = service.invokePrivate<
                Map<Int, AuthResourceGroup>
                >(
                "buildNewGroupMap",
                OLD_PROJECT_CODE,
                NEW_PROJECT_CODE
            )!!

            Assertions.assertEquals(1, result.size)
            Assertions.assertEquals(300, result[100]!!.relationId)
        }
    }
}
