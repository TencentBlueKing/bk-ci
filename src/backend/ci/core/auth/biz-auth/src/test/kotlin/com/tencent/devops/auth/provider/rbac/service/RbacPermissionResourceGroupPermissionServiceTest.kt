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

package com.tencent.devops.auth.provider.rbac.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bk.sdk.iam.dto.manager.AuthorizationScopes
import io.mockk.mockk
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class RbacPermissionResourceGroupPermissionServiceTest {

    private val objectMapper = ObjectMapper()
    private val service = RbacPermissionResourceGroupPermissionService(
        v2ManagerService = mockk(relaxed = true),
        rbacCommonService = mockk(relaxed = true),
        monitorSpaceService = mockk(relaxed = true),
        authResourceGroupDao = mockk(relaxed = true),
        dslContext = mockk<DSLContext>(relaxed = true),
        resourceGroupPermissionDao = mockk(relaxed = true),
        converter = mockk(relaxed = true),
        client = mockk(relaxed = true),
        iamV2ManagerService = mockk(relaxed = true),
        authAuthorizationScopesService = mockk(relaxed = true),
        authActionDao = mockk(relaxed = true),
        authResourceGroupConfigDao = mockk(relaxed = true),
        objectMapper = objectMapper,
        authResourceDao = mockk(relaxed = true),
        authUserProjectPermissionDao = mockk(relaxed = true),
        authResourceMemberDao = mockk(relaxed = true),
        traceEventDispatcher = mockk(relaxed = true),
        syncDataTaskDao = mockk(relaxed = true),
        redisOperation = mockk(relaxed = true),
        authResourceGroupFactory = mockk(relaxed = true)
    )

    @Test
    @DisplayName("buildProjectPermissions should scope create actions to project")
    fun buildProjectPermissionsShouldScopeCreateActionsToProject() {
        val authorizationScopes = objectMapper.readValue(
            service.buildProjectPermissions(
                projectCode = "demo",
                projectName = "demo",
                actions = listOf("cert_create", "cert_view", "project_visit")
            ),
            object : TypeReference<List<AuthorizationScopes>>() {}
        )

        val projectScope = authorizationScopes.firstOrNull { it.resources.firstOrNull()?.type == "project" }
        val certScope = authorizationScopes.firstOrNull { it.resources.firstOrNull()?.type == "cert" }

        assertNotNull(projectScope)
        assertNotNull(certScope)
        assertEquals(
            listOf("cert_create", "project_visit"),
            projectScope!!.actions.map { it.id }.sorted()
        )
        assertEquals(listOf("cert_view"), certScope!!.actions.map { it.id })
    }
}
