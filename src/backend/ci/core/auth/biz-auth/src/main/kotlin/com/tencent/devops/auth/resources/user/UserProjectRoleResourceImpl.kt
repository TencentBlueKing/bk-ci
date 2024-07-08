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

package com.tencent.devops.auth.resources.user

import com.tencent.devops.auth.api.user.UserProjectRoleResource
import com.tencent.devops.auth.pojo.DefaultGroup
import com.tencent.devops.auth.pojo.dto.ProjectRoleDTO
import com.tencent.devops.auth.pojo.vo.GroupInfoVo
import com.tencent.devops.auth.service.iam.PermissionGradeService
import com.tencent.devops.auth.service.iam.PermissionRoleService
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserProjectRoleResourceImpl @Autowired constructor(
    val permissionRoleService: PermissionRoleService,
    val permissionGradeService: PermissionGradeService
) : UserProjectRoleResource {
    override fun createProjectRole(
        userId: String,
        projectId: Int,
        projectCode: String,
        groupInfo: ProjectRoleDTO
    ): Result<String> {
        return Result(
            permissionRoleService.createPermissionRole(
                userId = userId,
                projectId = projectId,
                projectCode = projectCode,
                groupInfo = groupInfo
            ).toString()
        )
    }

    override fun updateProjectRole(
        userId: String,
        projectId: Int,
        roleId: Int,
        groupInfo: ProjectRoleDTO
    ): Result<Boolean> {
        permissionRoleService.renamePermissionRole(
            userId = userId,
            projectId = projectId,
            roleId = roleId,
            groupInfo = groupInfo
        )
        return Result(true)
    }

    override fun getProjectRoles(userId: String, projectId: Int): Result<List<GroupInfoVo>> {
        return Result(permissionRoleService.getPermissionRole(projectId))
    }

    override fun deleteProjectRole(userId: String, projectId: Int, roleId: Int): Result<Boolean> {
        permissionRoleService.deletePermissionRole(userId, projectId, roleId)
        return Result(true)
    }

    override fun hashPermission(userId: String, projectId: Int): Result<Boolean> {
        try {
            permissionGradeService.checkGradeManagerUser(userId, projectId)
        } catch (e: PermissionForbiddenException) {
            return Result(false)
        }
        return Result(true)
    }

    override fun getDefaultRole(userId: String): Result<List<DefaultGroup>> {
        return Result(permissionRoleService.getDefaultRole())
    }
}
