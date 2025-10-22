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

import com.tencent.devops.auth.provider.rbac.pojo.migrate.MigrateTaskDataResult
import com.tencent.devops.auth.provider.rbac.service.migrate.MigrateV3PolicyService
import com.tencent.devops.common.api.util.JsonUtil
import io.mockk.justRun
import io.mockk.spyk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import java.nio.charset.Charset

class MigrateV3PolicyServiceTest : AbMigratePolicyServiceTest() {

    private val projectCode = "bkdevops"
    private val self: MigrateV3PolicyService = spyk(
        MigrateV3PolicyService(
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

    @BeforeEach
    fun v3Before() {
        justRun {
            self.batchAddGroupMember(
                projectCode = any(),
                groupId = any(),
                defaultGroup = any(),
                members = any()
            )
        }
    }

    @Nested
    inner class BuildRbacAuthorizationScopeList {
        @Test
        @DisplayName("迁移api创建-管理员用户组")
        fun test_1() {
            val classPathResource = ClassPathResource("v3/group_api_policy_admin.json")
            val taskDataResult = JsonUtil.to(
                json = classPathResource.inputStream.readBytes().toString(Charset.defaultCharset()),
                type = MigrateTaskDataResult::class.java
            )

            val rbacAuthorizationScopeList = self.buildRbacAuthorizationScopeList(
                projectCode = projectCode,
                projectName = projectCode,
                managerGroupId = 1,
                result = taskDataResult
            ).first

            Assertions.assertTrue(rbacAuthorizationScopeList.isEmpty())
        }

        @Test
        @DisplayName("迁移api创建-用户自定义用户组")
        fun test_2() {
            buildRbacAuthorizationScopeListTest(
                self = self,
                projectCode = projectCode,
                actualFilePath = "v3/group_api_policy_custom.json",
                expectedFilePath = "v3/expected/group_api_policy_custom.json"
            )
        }

        @Test
        @DisplayName("迁移web创建-用户自定义用户组")
        fun test_3() {
            buildRbacAuthorizationScopeListTest(
                self = self,
                projectCode = projectCode,
                actualFilePath = "v3/group_web_policy_custom.json",
                expectedFilePath = "v3/expected/group_web_policy_custom.json"
            )
        }
    }
}
