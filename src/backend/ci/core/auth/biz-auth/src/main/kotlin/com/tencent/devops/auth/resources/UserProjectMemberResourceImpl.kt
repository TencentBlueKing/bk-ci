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

package com.tencent.devops.auth.resources

import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.bk.sdk.iam.dto.manager.ManagerRoleGroupInfo
import com.tencent.bk.sdk.iam.dto.manager.vo.ManagerGroupMemberVo
import com.tencent.devops.auth.api.user.UserProjectMemberResource
import com.tencent.devops.auth.pojo.dto.RoleMemberDTO
import com.tencent.devops.auth.pojo.vo.ProjectMembersVO
import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.auth.service.iam.PermissionRoleMemberService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserProjectMemberResourceImpl @Autowired constructor(
    val permissionRoleMemberService: PermissionRoleMemberService,
    val permissionProjectService: PermissionProjectService
) : UserProjectMemberResource {
    override fun createRoleMember(
        userId: String,
        projectId: Int,
        roleId: Int,
        managerGroup: Boolean,
        members: List<RoleMemberDTO>
    ): Result<Boolean> {
        permissionRoleMemberService.createRoleMember(
            userId = userId,
            projectId = projectId,
            roleId = roleId,
            members = members,
            managerGroup = managerGroup,
            checkAGradeManager = true
        )
        return Result(true)
    }

    override fun getRoleMember(
        projectId: Int,
        roleId: Int,
        page: Int?,
        pageSize: Int?
    ): Result<ManagerGroupMemberVo> {
        return Result(
            permissionRoleMemberService.getRoleMember(
                projectId = projectId,
                roleId = roleId,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun getProjectAllMember(projectId: Int, page: Int?, pageSize: Int?): Result<ProjectMembersVO?> {
        return Result(permissionRoleMemberService.getProjectAllMember(projectId, page, pageSize))
    }

    override fun deleteRoleMember(
        userId: String,
        projectId: Int,
        roleId: Int,
        managerGroup: Boolean,
        members: String,
        type: ManagerScopesEnum
    ): Result<Boolean> {
        Result(
            permissionRoleMemberService.deleteRoleMember(
                userId = userId,
                projectId = projectId,
                roleId = roleId,
                id = members,
                type = type,
                managerGroup = managerGroup
            )
        )
        return Result(true)
    }

    override fun getUserAllGroup(
        userId: String,
        projectId: Int,
        searchUserId: String
    ): Result<List<ManagerRoleGroupInfo>?> {
        return Result(permissionRoleMemberService.getUserGroups(projectId, searchUserId))
    }

    override fun checkManager(userId: String, projectId: String): Result<Boolean> {
        val result = permissionProjectService.checkProjectManager(userId, projectId) ||
                permissionProjectService.isProjectUser(userId, projectId, BkAuthGroup.CI_MANAGER)
        return Result(result)
    }
}
