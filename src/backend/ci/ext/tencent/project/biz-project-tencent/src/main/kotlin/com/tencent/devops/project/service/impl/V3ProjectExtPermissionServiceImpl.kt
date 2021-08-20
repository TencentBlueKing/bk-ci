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

package com.tencent.devops.project.service.impl

import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.devops.auth.api.ServiceRoleMemberResource
import com.tencent.devops.auth.api.ServiceRoleResource
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.auth.pojo.dto.RoleMemberDTO
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.service.ProjectExtPermissionService
import com.tencent.devops.project.service.ProjectService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class V3ProjectExtPermissionServiceImpl @Autowired constructor(
    val client: Client,
    val projectService: ProjectService,
    val tokenService: ClientTokenService
): ProjectExtPermissionService {
    override fun verifyUserProjectPermission(
        accessToken: String,
        projectCode: String,
        userId: String
    ): Boolean {
        return client.get(ServiceProjectAuthResource::class).isProjectUser(
            token = tokenService.getSystemToken(null)!!,
            userId = userId,
            projectCode = projectCode
        ).data ?: false
    }

    override fun createUser2Project(
        createUser: String,
        userIds: List<String>,
        projectCode: String,
        roleId: Int?,
        roleName: String?,
        checkManager: Boolean
    ): Boolean {
        val projectInfo = projectService.getByEnglishName(projectCode) ?:
            throw ErrorCodeException(
                errorCode = ProjectMessageCode.PROJECT_NOT_EXIST,
                defaultMessage = MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PROJECT_NOT_EXIST)
            )
        val projectRelationId = projectInfo.relationId
        if (projectRelationId.isNullOrEmpty()) {
            logger.warn("create V3 project user, not binding $projectCode iamV3")
            throw ErrorCodeException(
                errorCode = ProjectMessageCode.PROJECT_NOT_EXIST,
                defaultMessage = "关联系统未绑定"
            )
        }
        // 获取目标用户组在iam内用户组id
        val groupInfos = client.get(ServiceRoleResource::class).getProjectRoles(
            userId = createUser,
            projectId = projectRelationId.toInt()).data
        if (groupInfos == null) {
            logger.warn("$projectCode $projectRelationId group is empty")
            return false
        }

        var managerFlag = false
        val groupMap = mutableMapOf<String, Int>()
        groupInfos.map {
            groupMap[it.code] = it.id
            if (it.code == DefaultGroupType.MANAGER.value) {
                groupMap["ci_manager"] = it.id
                managerFlag = true
            }
        }
        var relationGroupId : Int? = null
        if (!roleName.isNullOrEmpty()) {
            relationGroupId = groupMap[roleName!!]
        }
        val memberList = mutableListOf<RoleMemberDTO>()
        userIds.map {
            memberList.add(
                RoleMemberDTO(
                    id = it,
                    type = ManagerScopesEnum.USER
                )
            )
        }

        // 添加用户到用户组
        client.get(ServiceRoleMemberResource::class).createRoleMember(
            createUser,
            projectRelationId!!.toInt(),
            relationGroupId!!,
            managerFlag,
            memberList,
            checkManager
        )
        return true
    }

    companion object {
        val logger = LoggerFactory.getLogger(V3ProjectExtPermissionServiceImpl::class.java)
    }
}
