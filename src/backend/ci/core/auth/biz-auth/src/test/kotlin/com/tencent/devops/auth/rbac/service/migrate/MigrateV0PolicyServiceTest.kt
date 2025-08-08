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

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.sdk.iam.dto.manager.AuthorizationScopes
import com.tencent.devops.auth.provider.rbac.service.migrate.MigrateV0PolicyService
import com.tencent.devops.auth.provider.rbac.pojo.migrate.MigrateTaskDataResult
import com.tencent.devops.common.api.util.JsonUtil
import io.mockk.every
import io.mockk.spyk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import java.nio.charset.Charset

class MigrateV0PolicyServiceTest : AbMigratePolicyServiceTest() {

    private val projectCode = "bkdevops"
    private val self: MigrateV0PolicyService = spyk(
        MigrateV0PolicyService(
            v2ManagerService = v2ManagerService,
            iamConfiguration = iamConfiguration,
            dslContext = dslContext,
            authResourceGroupDao = authResourceGroupDao,
            authResourceGroupConfigDao = authResourceGroupConfigDao,
            migrateResourceCodeConverter = migrateResourceCodeConverter,
            migrateIamApiService = migrateIamApiService,
            authResourceCodeConverter = authResourceCodeConverter,
            permissionService = permissionService,
            rbacCommonService = rbacCommonService,
            authMigrationDao = authMigrationDao,
            deptService = deptService,
            permissionResourceGroupPermissionService = permissionResourceGroupPermissionService,
            permissionResourceMemberService = permissionResourceMemberService
        ),
        recordPrivateCalls = true
    )

    @Nested
    inner class BuildRbacAuthorizationScopeList {
        @Test
        @DisplayName("迁移默认组-管理员组")
        fun test_1() {
            buildRbacAuthorizationScopeListTest(
                self = self,
                projectCode = projectCode,
                actualFilePath = "v0/group_web_policy_admin.json",
                expectedFilePath = "v0/expected/group_web_policy_admin.json"
            )
        }

        @Test
        @DisplayName("迁移默认组-开发人员组")
        fun test_2() {
            val classPathResource = ClassPathResource("v0/group_web_policy_developer.json")
            val taskDataResult = JsonUtil.to(
                json = classPathResource.inputStream.readBytes().toString(Charset.defaultCharset()),
                type = MigrateTaskDataResult::class.java
            )
            val (rbacAuthorizationScopeList, groupIdListOfPipelineActionGroup) = self.buildRbacAuthorizationScopeList(
                projectCode = projectCode,
                projectName = projectCode,
                managerGroupId = 1,
                result = taskDataResult
            )
            val expectedClassPathResource = ClassPathResource("v0/expected/group_web_policy_developer.json")
            val expectedRbacAuthorizationScopes =
                JsonUtil.to(
                    json = expectedClassPathResource.inputStream.readBytes().toString(Charset.defaultCharset()),
                    typeReference = object : TypeReference<List<AuthorizationScopes>>() {}
                )

            Assertions.assertEquals(
                JsonUtil.toJson(expectedRbacAuthorizationScopes),
                JsonUtil.toJson(rbacAuthorizationScopeList)
            )
            Assertions.assertEquals(
                groupIdListOfPipelineActionGroup,
                listOf("1", "1")
            )
        }

        @Test
        @DisplayName("迁移用户自定义组")
        fun test_3() {
            buildRbacAuthorizationScopeListTest(
                self = self,
                projectCode = projectCode,
                actualFilePath = "v0/group_web_policy_custom.json",
                expectedFilePath = "v0/expected/group_web_policy_custom.json"
            )
        }

        @Test
        @DisplayName("迁移用户自定义组-资源转换为空")
        fun test_4() {
            every {
                authResourceCodeConverter.code2IamCode(
                    projectCode = projectCode,
                    resourceType = "pipeline",
                    resourceCode = "p-665e332fa3bf4df29037de2de0a29f81"
                )
            } returns null
            buildRbacAuthorizationScopeListTest(
                self = self,
                projectCode = projectCode,
                actualFilePath = "v0/group_web_policy_custom.json",
                expectedFilePath = "v0/expected/group_web_policy_custom.json"
            )
        }
    }
}
