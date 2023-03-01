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

package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.bk.sdk.iam.dto.InstanceDTO
import com.tencent.bk.sdk.iam.dto.PageInfoDTO
import com.tencent.bk.sdk.iam.dto.V2PageInfoDTO
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.common.Constants
import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.auth.utils.RbacAuthUtils
import org.slf4j.LoggerFactory

class RbacPermissionProjectService(
    private val authHelper: AuthHelper,
    private val authResourceService: AuthResourceService,
    private val iamV2ManagerService: V2ManagerService,
    private val iamConfiguration: IamConfiguration,
    private val deptService: DeptService,
    private val authGroupService: AuthGroupService
) : PermissionProjectService {

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionProjectService::class.java)
    }

    override fun getProjectUsers(projectCode: String, group: BkAuthGroup?): List<String> {
        // 新的rbac版本中，没有ci管理员组，不可以调用此接口来获取ci管理员组的成员!
        val allGroupAndUser = getProjectGroupAndUserList(projectCode)
        return if (group == null) {
            val allMembers = mutableSetOf<String>()
            allGroupAndUser.map { allMembers.addAll(it.userIdList) }
            allMembers.toList()
        } else {
            val dbGroupInfo = authGroupService.getGroupByCode(
                projectCode = projectCode,
                groupCode = group.value
            ) ?: return emptyList()
            val groupInfo = allGroupAndUser.filter { it.roleId == dbGroupInfo.id }
            return if (groupInfo.isEmpty())
                emptyList()
            else
                groupInfo[0].userIdList
        }
    }

    override fun getProjectGroupAndUserList(projectCode: String): List<BkAuthGroupAndUserList> {
        // 1、获取分级管理员id
        val gradeManagerId = authResourceService.get(
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode
        ).relationId
        // 2、获取分级管理员下所有的用户组
        // todo 最多获取1000个用户组是否合理
        val pageInfoDTO = V2PageInfoDTO()
        pageInfoDTO.page = 1
        pageInfoDTO.pageSize = 1000
        val groupInfoList = iamV2ManagerService.getGradeManagerRoleGroupV2(gradeManagerId, null, pageInfoDTO).results
        logger.info(
            "[RBAC-IAM] getProjectGroupAndUserList: projectCode = $projectCode |" +
                " gradeManagerId = $gradeManagerId | groupInfoList: $groupInfoList"
        )
        val result = mutableListOf<BkAuthGroupAndUserList>()
        groupInfoList.forEach {
            // 3、获取组成员
            // todo 最多获取1000个用户或组是否合理
            val pageInfoDTO = PageInfoDTO()
            pageInfoDTO.limit = 0
            pageInfoDTO.offset = 1000
            val groupMemberInfoList = iamV2ManagerService.getRoleGroupMemberV2(it.id, pageInfoDTO).results
            logger.info(
                "[RBAC-IAM] getProjectGroupAndUserList ,groupId: ${it.id} " +
                    "| groupMemberInfoList: $groupMemberInfoList"
            )
            val members = mutableListOf<String>()
            groupMemberInfoList.forEach { memberInfo ->
                if (memberInfo.type == ManagerScopesEnum.getType(ManagerScopesEnum.DEPARTMENT)) {
                    logger.info("[RBAC-IAM] department:$memberInfo")
                    val deptUsers = deptService.getDeptUser(memberInfo.id.toInt(), null)
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
                type = ""
            )
            result.add(groupAndUser)
        }
        return result
    }

    override fun getUserProjects(userId: String): List<String> {
        val projectList = authHelper.getInstanceList(
            userId,
            RbacAuthUtils.buildAction(AuthPermission.VISIT, authResourceType = AuthResourceType.PROJECT),
            RbacAuthUtils.extResourceType(AuthResourceType.PROJECT)
        )
        logger.info("get user projects:$projectList")
        return projectList
    }

    override fun isProjectUser(userId: String, projectCode: String, group: BkAuthGroup?): Boolean {
        val managerPermission = checkProjectManager(userId, projectCode)
        // 有管理员权限或者若为校验管理员权限,直接返回是否时管理员成员
        if (managerPermission || (group != null && group == BkAuthGroup.MANAGER)) {
            return managerPermission
        }
        val instanceDTO = InstanceDTO()
        instanceDTO.system = iamConfiguration.systemId
        instanceDTO.id = projectCode
        instanceDTO.type = AuthResourceType.PROJECT.value
        return authHelper.isAllowed(
            userId,
            RbacAuthUtils.buildAction(AuthPermission.VISIT, authResourceType = AuthResourceType.PROJECT),
            instanceDTO
        )
    }

    override fun checkProjectManager(userId: String, projectCode: String): Boolean {
        val instanceDTO = InstanceDTO()
        instanceDTO.system = iamConfiguration.systemId
        instanceDTO.id = projectCode
        instanceDTO.type = AuthResourceType.PROJECT.value
        return authHelper.isAllowed(
            userId,
            RbacAuthUtils.buildAction(AuthPermission.MANAGE, authResourceType = AuthResourceType.PROJECT),
            instanceDTO
        )
    }

    override fun createProjectUser(userId: String, projectCode: String, roleCode: String): Boolean {
        return true
    }

    override fun getProjectRoles(projectCode: String, projectId: String): List<BKAuthProjectRolesResources> {
        return emptyList()
    }
}
