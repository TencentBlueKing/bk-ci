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
 */

package com.tencent.devops.auth.resources.service

import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceProjectAuthResourceImpl @Autowired constructor(
    val permissionProjectService: PermissionProjectService
) : ServiceProjectAuthResource {
    override fun getProjectUsers(
        token: String,
        type: String?,
        projectCode: String,
        group: BkAuthGroup?
    ): Result<List<String>> {
        return Result(
            permissionProjectService.getProjectUsers(
                projectCode = projectCode,
                group = group
            )
        )
    }

    override fun getProjectGroupAndUserList(
        token: String,
        projectCode: String
    ): Result<List<BkAuthGroupAndUserList>> {
        return Result(
            permissionProjectService.getProjectGroupAndUserList(
                projectCode = projectCode
            )
        )
    }

    override fun getUserProjects(token: String, userId: String): Result<List<String>> {
        return Result(permissionProjectService.getUserProjects(userId))
    }

    override fun getUserProjectsByPermission(token: String, userId: String, action: String): Result<List<String>> {
        return Result(
            permissionProjectService.getUserProjectsByPermission(
                userId = userId,
                action = action
            )
        )
    }

    override fun isProjectUser(
        token: String,
        type: String?,
        userId: String,
        projectCode: String,
        group: BkAuthGroup?
    ): Result<Boolean> {
        return Result(
            permissionProjectService.isProjectUser(
                userId = userId,
                projectCode = projectCode,
                group = group
            )
        )
    }

    override fun checkManager(token: String, userId: String, projectId: String): Result<Boolean> {
        val result = permissionProjectService.checkProjectManager(userId, projectId) ||
            permissionProjectService.isProjectUser(userId, projectId, BkAuthGroup.CI_MANAGER)
        return Result(result)
    }

    override fun checkProjectManager(
        token: String,
        type: String?,
        userId: String,
        projectCode: String
    ): Result<Boolean> {
        return Result(
            permissionProjectService.checkProjectManager(
                userId = userId,
                projectCode = projectCode
            )
        )
    }

    override fun createProjectUser(
        token: String,
        userId: String,
        projectCode: String,
        role: String
    ): Result<Boolean> {
        return Result(
            permissionProjectService.createProjectUser(
                userId = userId,
                projectCode = projectCode,
                roleCode = role
            )
        )
    }

    override fun getProjectRoles(
        token: String,
        projectCode: String,
        projectId: String
    ): Result<List<BKAuthProjectRolesResources>> {
        return Result(
            permissionProjectService.getProjectRoles(
                projectCode = projectCode,
                projectId = projectId
            )
        )
    }
}
