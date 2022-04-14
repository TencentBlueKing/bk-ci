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

package com.tencent.devops.auth.service.simple

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.entity.GroupMemberInfo
import com.tencent.devops.auth.pojo.enum.GroupType
import com.tencent.devops.auth.service.AuthGroupMemberService
import com.tencent.devops.auth.service.AuthGroupService
import com.tencent.devops.auth.service.ci.PermissionProjectService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import com.tencent.devops.common.service.utils.MessageCodeUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class SimplePermissionProjectServiceImpl @Autowired constructor(
    private val groupMemberService: AuthGroupMemberService,
    private val groupService: AuthGroupService
): PermissionProjectService {

    override fun getProjectUsers(projectCode: String, group: BkAuthGroup?): List<String> {
        if (group == null) {
            // 未指定用户组，直接返回整个项目下的用户
            return groupMemberService.getProjectMember(projectCode) ?: emptyList()
        } else {
            // 指定用户组，按组获取用户列表
            val groupId = groupService.getGroupByCode(projectCode, group.value)?.id
                ?: throw ErrorCodeException(
                    errorCode = AuthMessageCode.GROUP_NOT_EXIST,
                    defaultMessage = MessageCodeUtil.getCodeLanMessage(AuthMessageCode.GROUP_NOT_EXIST)
                )
            val members = mutableListOf<String>()
            val groupMemberInfos = groupMemberService.getRoleMember(groupId, projectCode) ?: return emptyList()
            groupMemberInfos.result.forEach {
                members.add(it.id)
            }
            return members
        }
    }

    override fun getProjectGroupAndUserList(projectCode: String): List<BkAuthGroupAndUserList> {
        // 获取项目下的用户组
        val groupInfos = groupService.getGroupByProject(projectCode) ?: return emptyList()
        val groupAndUsers = mutableListOf<BkAuthGroupAndUserList>()
        // 获取项目下的用户map。 map<用户组Id, 用户列表>
        val groupMemberMap = groupMemberService.getProjectMemberMap(projectCode)
        groupInfos.forEach {
            groupAndUsers.add(
                BkAuthGroupAndUserList(
                    displayName = it.displayName,
                    roleId = it.id,
                    roleName = it.groupName,
                    userIdList = groupMemberMap[it.id.toString()] ?: emptyList(),
                    type = ""
                )
            )
        }
        return groupAndUsers
    }

    override fun getUserProjects(userId: String): List<String> {
        val groupIds = groupMemberService.getUserJoinGroup(userId)
        if (groupIds == null) {
            logger.info("$userId not join any project")
            return emptyList()
        }
        val groupInfos = groupService.getGroupByCodes(groupIds) ?: emptyList()
        val projectCodes = mutableListOf<String>()
        groupInfos.forEach {
            projectCodes.add(it.projectCode)
        }
        return projectCodes
    }

    override fun isProjectUser(userId: String, projectCode: String, group: BkAuthGroup?): Boolean {
        // 获取用户加入项目下的用户组
        val groupInfos = groupMemberService.getUserGroupByProject(userId, projectCode)
        if (groupInfos == null) {
            logger.warn("$userId not join $projectCode group")
            return false
        }
        if (group == null) {
            return true
        } else {
            // 获取默认用户组对应的组Id
            val groupId = groupService.getGroupByCode(projectCode, group.value)?.id
            if (groupId == null) {
                logger.warn("$projectCode not exist $group")
                return false
            }
            groupInfos.map {
                if (it.groupId.toInt() == groupId) {
                    return true
                }
            }
        }
        return false
    }

    override fun checkProjectManager(userId: String, projectCode: String): Boolean {
        val groupInfos = groupMemberService.getUserGroupByProject(userId, projectCode)
        if (groupInfos.isNullOrEmpty()) {
            logger.warn("$userId not join $projectCode group")
            return false
        }
        val managerGroupId = groupService.getGroupByCode(projectCode, DefaultGroupType.MANAGER.value)
        if (managerGroupId == null) {
            logger.warn("$projectCode not exist manager group")
            return false
        }

        groupInfos.map {
            if (it.groupId.toInt() == managerGroupId.id) {
                return true
            }
        }
        return false
    }

    override fun createProjectUser(userId: String, projectCode: String, roleCode: String): Boolean {
        logger.info("createProjectUser $projectCode $userId $roleCode")
        val groupInfo = groupService.getGroupByCode(projectCode, roleCode)
        if (groupInfo == null) {
            logger.warn("$projectCode not exist $roleCode")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.GROUP_NOT_EXIST,
                defaultMessage = MessageCodeUtil.getCodeLanMessage(AuthMessageCode.GROUP_NOT_EXIST)
            )
        }
        groupMemberService.createGroupMember(
            GroupMemberInfo(
                userId = userId,
                projectCode = projectCode,
                groupId = groupInfo.id,
                userType = true,
                groupType = groupInfo.groupType,
                expiredDay = 365
            )
        )
        logger.info("createProjectUser success $projectCode $userId $roleCode")
        return true
    }

    override fun getProjectRoles(projectCode: String, projectId: String): List<BKAuthProjectRolesResources> {
        val groupInfos = groupService.getGroupByProject(projectCode)
        if (groupInfos == null) {
            logger.warn("$projectCode not exist group")
            return emptyList()
        }
        val groups = mutableListOf<BKAuthProjectRolesResources>()
        groupInfos.forEach {
            groups.add(
                BKAuthProjectRolesResources(
                    displayName = it.displayName,
                    roleName = it.groupName,
                    roleId = it.id,
                    type = (if (it.groupType) {
                        GroupType.DEFAULT
                    } else {
                        GroupType.USER_BUILD
                    }).toString()
                )
            )
        }
        return groups
    }

    companion object {
        val logger = LoggerFactory.getLogger(SimplePermissionProjectServiceImpl::class.java)
    }
}