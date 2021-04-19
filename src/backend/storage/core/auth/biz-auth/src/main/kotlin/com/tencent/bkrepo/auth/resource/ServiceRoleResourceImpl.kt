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

package com.tencent.bkrepo.auth.resource

import com.tencent.bkrepo.auth.api.ServiceRoleResource
import com.tencent.bkrepo.auth.constant.PROJECT_MANAGE_ID
import com.tencent.bkrepo.auth.constant.PROJECT_MANAGE_NAME
import com.tencent.bkrepo.auth.constant.REPO_MANAGE_ID
import com.tencent.bkrepo.auth.constant.REPO_MANAGE_NAME
import com.tencent.bkrepo.auth.pojo.role.CreateRoleRequest
import com.tencent.bkrepo.auth.pojo.role.Role
import com.tencent.bkrepo.auth.pojo.enums.RoleType
import com.tencent.bkrepo.auth.service.RoleService
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@RestController
class ServiceRoleResourceImpl @Autowired constructor(
    private val roleService: RoleService
) : ServiceRoleResource {

    override fun createRole(request: CreateRoleRequest): Response<String?> {
        // todo check request
        val id = roleService.createRole(request)
        return ResponseBuilder.success(id)
    }

    override fun createProjectManage(projectId: String): Response<String?> {
        val request = CreateRoleRequest(
            PROJECT_MANAGE_ID,
            PROJECT_MANAGE_NAME,
            RoleType.PROJECT,
            projectId,
            null,
            true
        )
        val id = roleService.createRole(request)
        return ResponseBuilder.success(id)
    }

    override fun createRepoManage(projectId: String, repoName: String): Response<String?> {
        val request = CreateRoleRequest(
            REPO_MANAGE_ID,
            REPO_MANAGE_NAME,
            RoleType.REPO,
            projectId,
            repoName,
            true
        )
        val id = roleService.createRole(request)
        return ResponseBuilder.success(id)
    }

    override fun deleteRole(id: String): Response<Boolean> {
        // todo check request
        roleService.deleteRoleByid(id)
        return ResponseBuilder.success(true)
    }

    override fun detail(id: String): Response<Role?> {
        return ResponseBuilder.success(roleService.detail(id))
    }

    override fun detailByRidAndProjectId(rid: String, projectId: String): Response<Role?> {
        val result = roleService.detail(rid, projectId)
        return ResponseBuilder.success(result)
    }

    override fun detailByRidAndProjectIdAndRepoName(rid: String, projectId: String, repoName: String): Response<Role?> {
        val result = roleService.detail(rid, projectId, repoName)
        return ResponseBuilder.success(result)
    }

    override fun listRole(
        projectId: String,
        repoName: String?
    ): Response<List<Role>> {
        return ResponseBuilder.success(roleService.listRoleByProject(projectId, repoName))
    }
}
