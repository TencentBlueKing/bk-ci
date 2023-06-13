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
import com.tencent.bk.sdk.iam.dto.manager.dto.SearchGroupDTO
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BKAuthProjectRolesResources
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.api.pojo.BkAuthGroupAndUserList
import com.tencent.devops.common.auth.utils.RbacAuthUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

@Suppress("LongParameterList")
class RbacPermissionProjectService(
    private val authHelper: AuthHelper,
    private val authResourceService: AuthResourceService,
    private val iamV2ManagerService: V2ManagerService,
    private val iamConfiguration: IamConfiguration,
    private val authResourceGroupDao: AuthResourceGroupDao,
    private val dslContext: DSLContext,
    private val rbacCacheService: RbacCacheService
) : PermissionProjectService {

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionProjectService::class.java)
    }

    override fun getProjectUsers(projectCode: String, group: BkAuthGroup?): List<String> {
        return when (group) {
            // 新的rbac版本中，没有ci管理员组，不可以调用此接口来获取ci管理员组的成员
            BkAuthGroup.CIADMIN, BkAuthGroup.CI_MANAGER -> emptyList()
            // 获取项目下组全部成员
            null -> {
                getProjectGroupAndUserList(projectCode).flatMap { it.userIdList }.distinct()
            }
            else -> {
                val dbGroupInfo = authResourceGroupDao.get(
                    dslContext = dslContext,
                    projectCode = projectCode,
                    resourceType = AuthResourceType.PROJECT.value,
                    resourceCode = projectCode,
                    groupCode = group.value
                ) ?: return emptyList()
                val groupInfo = getProjectGroupAndUserList(projectCode)
                    .find { it.roleId == dbGroupInfo.relationId.toInt() }
                groupInfo?.userIdList ?: emptyList()
            }
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
        val v2PageInfoDTO = V2PageInfoDTO().apply {
            page = 1
            pageSize = 1000
        }
        val searchGroupDTO = SearchGroupDTO.builder().inherit(false).build()
        val groupInfoList = iamV2ManagerService.getGradeManagerRoleGroupV2(
            gradeManagerId,
            searchGroupDTO,
            v2PageInfoDTO
        ).results
        logger.info(
            "[RBAC-IAM] getProjectGroupAndUserList: projectCode = $projectCode |" +
                " gradeManagerId = $gradeManagerId | groupInfoList: $groupInfoList"
        )
        val result = mutableListOf<BkAuthGroupAndUserList>()
        groupInfoList.forEach {
            // 3、获取组成员
            val pageInfoDTO = PageInfoDTO().apply {
                limit = 1000
                offset = 0
            }
            val groupMemberInfoList = iamV2ManagerService.getRoleGroupMemberV2(it.id, pageInfoDTO).results
            logger.info(
                "[RBAC-IAM] getProjectGroupAndUserList ,groupId: ${it.id} " +
                    "| groupMemberInfoList: $groupMemberInfoList"
            )
            val members = mutableListOf<String>()
            groupMemberInfoList.forEach { memberInfo ->
                // todo 暂时不返回部门的用户
                if (memberInfo.type == ManagerScopesEnum.getType(ManagerScopesEnum.USER)) {
                    members.add(memberInfo.id)
                }
            }
            val groupAndUser = BkAuthGroupAndUserList(
                displayName = it.name,
                roleId = it.id,
                roleName = it.name,
                userIdList = members.toSet().toList(),
                type = ""
            )
            result.add(groupAndUser)
        }
        return result
    }

    override fun getUserProjects(userId: String): List<String> {
        logger.info("[rbac] get user projects|userId = $userId")
        return getUserProjectsByPermission(
            userId = userId,
            action = AuthPermission.VISIT.value
        )
    }

    override fun getUserProjectsByPermission(
        userId: String,
        action: String
    ): List<String> {
        logger.info("[rbac] get user projects by permission|$userId|$action")
        val startEpoch = System.currentTimeMillis()
        try {
            val useAction = RbacAuthUtils.buildAction(AuthPermission.get(action), AuthResourceType.PROJECT)
            val instanceMap = authHelper.groupRbacInstanceByType(userId, useAction)
            return if (instanceMap.contains("*")) {
                logger.info("super manager has all project|$userId")
                authResourceService.getAllResourceCode(
                    resourceType = AuthResourceType.PROJECT.value
                )
            } else {
                val projectList = instanceMap[AuthResourceType.PROJECT.value] ?: emptyList()
                logger.info("get user projects:$projectList")
                projectList
            }
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to get user projects by permission"
            )
        }
    }

    override fun isProjectUser(userId: String, projectCode: String, group: BkAuthGroup?): Boolean {
        logger.info("[rbac] check project user|userId = $userId")
        val startEpoch = System.currentTimeMillis()
        try {
            val managerPermission = checkProjectManager(userId, projectCode)
            val checkCiManager = group != null && (group == BkAuthGroup.MANAGER || group == BkAuthGroup.CI_MANAGER)
            // 有管理员权限或者若为校验管理员权限,直接返回是否时管理员成员
            if (managerPermission || checkCiManager) {
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
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to check project user"
            )
        }
    }

    override fun checkProjectManager(userId: String, projectCode: String): Boolean {
        return rbacCacheService.checkProjectManager(userId, projectCode)
    }

    override fun createProjectUser(userId: String, projectCode: String, roleCode: String): Boolean {
        return true
    }

    override fun getProjectRoles(projectCode: String, projectId: String): List<BKAuthProjectRolesResources> {
        return emptyList()
    }
}
