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

import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.devops.auth.api.user.UserProjectMemberResource
import com.tencent.devops.auth.pojo.dto.GroupMemberDTO
import com.tencent.devops.auth.pojo.dto.RoleMemberDTO
import com.tencent.devops.auth.pojo.dto.UserGroupInfoDTO
import com.tencent.devops.auth.pojo.vo.ProjectMembersVO
import com.tencent.devops.auth.service.ci.PermissionRoleMemberService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserProjectMemberResourceImpl @Autowired constructor(
    val permissionRoleMemberService: PermissionRoleMemberService
) : UserProjectMemberResource {
    override fun createRoleMember(
        userId: String,
        projectId: String,
        roleId: Int,
        managerGroup: Boolean,
        members: List<RoleMemberDTO>,
        expiredDay: Long
    ): Result<Boolean> {
        permissionRoleMemberService.createRoleMember(
            userId = userId,
            projectId = projectId,
            roleId = roleId,
            members = members,
            managerGroup = managerGroup,
            checkAGradeManager = true,
            expiredDay = expiredDay
        )
        return Result(true)
    }

    override fun getRoleMember(
        projectId: String,
        roleId: Int,
        page: Int?,
        pageSize: Int?
    ): Result<GroupMemberDTO> {
        return Result(
            permissionRoleMemberService.getRoleMember(
                projectId = projectId,
                roleId = roleId,
                page = page,
                pageSize = pageSize
            ))
    }

    override fun getProjectAllMember(projectId: String, page: Int?, pageSize: Int?): Result<ProjectMembersVO?> {
        return Result(permissionRoleMemberService.getProjectAllMember(projectId, page, pageSize))
    }

    override fun deleteRoleMember(
        userId: String,
        projectId: String,
        roleId: Int,
        managerGroup: Boolean,
        members: String,
        type: ManagerScopesEnum
    ): Result<Boolean> {
        Result(permissionRoleMemberService.deleteRoleMember(
            executeUserId = userId,
            projectId = projectId,
            roleId = roleId,
            deleteUserId = members,
            type = type,
            managerGroup = managerGroup
        ))
        return Result(true)
    }

    override fun getUserAllGroup(
        userId: String,
        projectId: String,
        searchUserId: String
    ): Result<List<UserGroupInfoDTO>?> {
        return Result(permissionRoleMemberService.getUserGroups(projectId, searchUserId))
    }
}
