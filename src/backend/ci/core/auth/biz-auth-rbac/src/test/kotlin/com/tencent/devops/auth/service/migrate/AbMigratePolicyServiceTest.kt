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

package com.tencent.devops.auth.service.migrate

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.dto.manager.AuthorizationScopes
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.dao.AuthMigrationDao
import com.tencent.devops.auth.dao.AuthResourceGroupConfigDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.pojo.migrate.MigrateTaskDataResult
import com.tencent.devops.auth.service.AuthResourceCodeConverter
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.PermissionGroupPoliciesService
import com.tencent.devops.auth.service.RbacCacheService
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.test.BkCiAbstractTest
import io.mockk.every
import io.mockk.mockk
import java.nio.charset.Charset
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.springframework.core.io.ClassPathResource

open class AbMigratePolicyServiceTest : BkCiAbstractTest() {
    val v2ManagerService: V2ManagerService = mockk()
    val iamConfiguration: IamConfiguration = mockk()
    val authResourceGroupDao: AuthResourceGroupDao = mockk()
    val authResourceGroupConfigDao: AuthResourceGroupConfigDao = mockk()
    val migrateIamApiService: MigrateIamApiService = mockk()
    val authMigrationDao: AuthMigrationDao = mockk()
    val permissionService: PermissionService = mockk()
    val rbacCacheService: RbacCacheService = mockk()
    val migrateResourceCodeConverter: MigrateResourceCodeConverter = mockk()
    val authResourceCodeConverter: AuthResourceCodeConverter = mockk()
    val deptService: DeptService = mockk()
    val permissionGroupPoliciesService: PermissionGroupPoliciesService = mockk()

    @BeforeEach
    fun before() {
        every {
            authResourceCodeConverter.code2IamCode(
                projectCode = any(),
                resourceType = any(),
                resourceCode = any()
            )
        } returnsArgument 2

        every {
            migrateResourceCodeConverter.getRbacResourceCode(
                projectCode = any(),
                resourceType = any(),
                migrateResourceCode = any()
            )
        } returnsArgument 2

        every {
            iamConfiguration.systemId
        } returns "bk_ci_rbac"
    }

    fun buildRbacAuthorizationScopeListTest(
        self: AbMigratePolicyService,
        projectCode: String,
        actualFilePath: String,
        expectedFilePath: String
    ) {
        val classPathResource = ClassPathResource(actualFilePath)
        val taskDataResult = JsonUtil.to(
            json = classPathResource.inputStream.readBytes().toString(Charset.defaultCharset()),
            type = MigrateTaskDataResult::class.java
        )

        val rbacAuthorizationScopeList = self.buildRbacAuthorizationScopeList(
            projectCode = projectCode,
            projectName = projectCode,
            managerGroupId = 1,
            result = taskDataResult
        )

        val expectedClassPathResource = ClassPathResource(expectedFilePath)
        val expectedRbacAuthorizationScopes =
            JsonUtil.to(
                json = expectedClassPathResource.inputStream.readBytes().toString(Charset.defaultCharset()),
                typeReference = object : TypeReference<List<AuthorizationScopes>>() {}
            )

        Assertions.assertEquals(
            JsonUtil.toJson(expectedRbacAuthorizationScopes),
            JsonUtil.toJson(rbacAuthorizationScopeList)
        )
    }
}
