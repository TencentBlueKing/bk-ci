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

package com.tencent.devops.auth.service.permission.iam

import com.google.common.cache.CacheBuilder
import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.bk.sdk.iam.dto.InstanceDTO
import com.tencent.bk.sdk.iam.dto.action.ActionDTO
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.devops.auth.pojo.dto.RoleMemberDTO
import com.tencent.devops.auth.service.AuthGroupService
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.iam.PermissionRoleMemberService
import com.tencent.devops.auth.service.iam.PermissionRoleService
import com.tencent.devops.auth.service.iam.impl.AbsPermissionProjectService
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.auth.utils.AuthUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.lang.RuntimeException
import java.util.concurrent.TimeUnit

class TxPermissionProjectServiceImpl @Autowired constructor(
    override val permissionRoleService: PermissionRoleService,
    override val permissionRoleMemberService: PermissionRoleMemberService,
    override val authHelper: AuthHelper,
    override val policyService: PolicyService,
    override val client: Client,
    override val iamConfiguration: IamConfiguration,
    override val deptService: DeptService,
    override val groupService: AuthGroupService
) : AbsPermissionProjectService(
    permissionRoleService = permissionRoleService,
    permissionRoleMemberService = permissionRoleMemberService,
    authHelper = authHelper,
    policyService = policyService,
    client = client,
    iamConfiguration = iamConfiguration,
    deptService = deptService,
    groupService = groupService
) {
    private val projectIdCache = CacheBuilder.newBuilder()
        .maximumSize(5000)
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build<String, String>()

    override fun getProjectUsers(projectCode: String, group: BkAuthGroup?): List<String> {
        val allGroupAndUser = getProjectGroupAndUserList(projectCode)
        return if (group == null) {
            val allMembers = mutableListOf<String>()
            allGroupAndUser.map { allMembers.addAll(it.userIdList) }
            allMembers
        } else {
            // TODO: 待iam完成group lable后补齐
            // 通过分组类型匹配分组用户
            emptyList()
        }
    }

    override fun getProjectGroupAndUserList(
        projectCode: String
    ): List<BkAuthGroupAndUserList> {
        // 1. 转换projectCode为iam侧分级管理员Id
        val iamProjectId = getExtProjectId(projectCode)
        // 2. 获取项目下的所有用户组
        val roleInfos = permissionRoleService.getPermissionRole(iamProjectId)
        logger.info("[IAM] $projectCode $iamProjectId roleInfos: $roleInfos")
        val result = mutableListOf<BkAuthGroupAndUserList>()
        // 3. 获取用户组下的所有用户
        roleInfos.forEach {
            val groupMemberInfos = permissionRoleMemberService.getRoleMember(
                projectId = iamProjectId,
                roleId = it.id,
                page = 0,
                pageSize = 1000
            ).results
            logger.info("[IAM] $projectCode $iamProjectId ,role ${it.id}| users $groupMemberInfos")
            val members = mutableListOf<String>()
            groupMemberInfos.forEach { memberInfo ->
                // 如果为组织需要获取组织对应的用户
                if (memberInfo.type == ManagerScopesEnum.getType(ManagerScopesEnum.DEPARTMENT)) {
                    logger.info("[IAM] $projectCode $iamProjectId ,role ${it.id}| dept ${memberInfo.id}")
                    val deptUsers = deptService.getDeptUser(memberInfo.id.toInt(), null) ?: null
                    if (deptUsers != null) {
                        members.addAll(deptUsers)
                    }
                } else {
                    members.add(memberInfo.id)
                }
            }
            val groupAndUser = BkAuthGroupAndUserList(
                displayName = it.name,
                roleId = it.id,
                roleName = it.name,
                userIdList = members,
                // TODO: 待iam完成group lable后补齐
                type = ""
            )
            result.add(groupAndUser)
        }
        return result
    }

    override fun getUserProjects(userId: String): List<String> {
        val viewAction = "project_view"
        val managerAction = "all_action"
        val actionDTOs = mutableListOf<ActionDTO>()
        val viewActionDto = ActionDTO()
        viewActionDto.id = viewAction
        val managerActionDto = ActionDTO()
        managerActionDto.id = managerAction
        actionDTOs.add(viewActionDto)
        actionDTOs.add(managerActionDto)
        val actionPolicyDTOs = policyService.batchGetPolicyByActionList(userId, actionDTOs, null)
            ?: return emptyList()
        logger.info("[IAM] getUserProjects actionPolicyDTOs $actionPolicyDTOs")
        val projects = mutableSetOf<String>()
        actionPolicyDTOs.forEach {
            projects.addAll(AuthUtils.getProjects(it.condition))
        }
        return projects.toList()
    }

    /**
     * 判断是否为项目下用户, 若提供角色，则需要判断是否为该角色下用户
     * 优先判断all_action(项目管理员)
     * 若未提供需判断是否有project_veiws权限
     */
    override fun isProjectUser(userId: String, projectCode: String, group: BkAuthGroup?): Boolean {

        val managerActionType = "all_action"
        val instance = buildInstance(projectCode, AuthResourceType.PROJECT.value)
        logger.info("[IAM] v3 isProjectUser actionType[$managerActionType] instance[$instance]")
        val managerPermission = authHelper.isAllowed(userId, managerActionType, instance)
        // 若为校验管理员权限,直接返回是否有all_action接口
        if (group != null && group == BkAuthGroup.MANAGER) {
            return managerPermission
        }
        // 有管理员权限直接返回
        if (managerPermission) {
            return managerPermission
        }
        // 无管理员权限判断是否有项目查看权限来判断是否为项目人员
        val viewActionType = "project_view"
        return authHelper.isAllowed(userId, viewActionType, instance)
    }

    override fun createProjectUser(userId: String, projectCode: String, role: String): Boolean {
        val projectId = getExtProjectId(projectCode)
        val projectRoles = permissionRoleService.getPermissionRole(projectId)
        var roleId = 0
        projectRoles.forEach {
            // TODO: 通过roleType匹配对应的roleId
        }
        val managerRole = role == BkAuthGroup.MANAGER.value
        val members = mutableListOf<RoleMemberDTO>()
        members.add(RoleMemberDTO(type = ManagerScopesEnum.USER, id = userId))
        permissionRoleMemberService.createRoleMember(userId, projectId, roleId, members, managerRole)
        return true
    }

    override fun getProjectRoles(projectCode: String, projectId: String): List<BKAuthProjectRolesResources> {
        val roleInfos = permissionRoleService.getPermissionRole(projectId.toInt())
        logger.info("[IAM] getProjectRole $roleInfos")
        val roleList = mutableListOf<BKAuthProjectRolesResources>()
        roleInfos.forEach {
            val role = BKAuthProjectRolesResources(
                displayName = it.name,
                roleName = it.name,
                roleId = it.id,
                type = ""
            )
            roleList.add(role)
        }
        return roleList
    }

    override fun getUserByExt(group: BkAuthGroup, projectCode: String): List<String> {
        val groupInfo = groupService.getGroupByName(projectCode, group.value) ?: return emptyList()
        val extProjectId = getExtProjectId(projectCode)
        val relationId = groupInfo!!.relationId
        if (relationId.isNullOrEmpty()) {
            logger.warn("$projectCode not bind iam userGroup")
            return emptyList()
        }
        val groupMemberInfos = permissionRoleMemberService.getRoleMember(
            projectId = extProjectId,
            roleId = relationId.toInt(),
            page = 0,
            pageSize = 1000
        ).results
        val users = mutableListOf<String>()
        groupMemberInfos.forEach {
            if (it.type == ManagerScopesEnum.getType(ManagerScopesEnum.USER)) {
                users.add(it.id)
            }
        }
        return users
    }

    private fun getExtProjectId(projectCode: String): Int {
        val iamProjectId = if (projectIdCache.getIfPresent(projectCode) != null) {
            projectIdCache.getIfPresent(projectCode)!!
        } else {
            val projectInfo = client.get(ServiceProjectResource::class).get(projectCode).data

            if (projectInfo != null && !projectInfo.relationId.isNullOrEmpty()) {
                projectIdCache.put(projectCode, projectInfo!!.relationId!!)
            }
            projectInfo?.relationId
        }
        if (iamProjectId.isNullOrEmpty()) {
            logger.warn("[IAM] $projectCode iamProject is empty")
            throw RuntimeException()
        }
        return iamProjectId.toInt()
    }

    private fun buildInstance(id: String, type: String): InstanceDTO {
        val instance = InstanceDTO()
        instance.id = id
        instance.system = iamConfiguration.systemId
        instance.type = type
        return instance
    }

    companion object {
        val logger = LoggerFactory.getLogger(TxPermissionProjectServiceImpl::class.java)
    }
}
