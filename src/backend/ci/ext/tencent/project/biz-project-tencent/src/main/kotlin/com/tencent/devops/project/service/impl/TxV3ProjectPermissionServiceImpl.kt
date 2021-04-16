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

package com.tencent.devops.project.service.impl

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.bk.sdk.iam.dto.manager.Action
import com.tencent.bk.sdk.iam.dto.manager.AuthorizationScopes
import com.tencent.bk.sdk.iam.dto.manager.ManagerMember
import com.tencent.bk.sdk.iam.dto.manager.ManagerPath
import com.tencent.bk.sdk.iam.dto.manager.ManagerResources
import com.tencent.bk.sdk.iam.dto.manager.ManagerRoleGroup
import com.tencent.bk.sdk.iam.dto.manager.ManagerScopes
import com.tencent.bk.sdk.iam.dto.manager.dto.CreateManagerDTO
import com.tencent.bk.sdk.iam.dto.manager.dto.ManagerMemberGroupDTO
import com.tencent.bk.sdk.iam.dto.manager.dto.ManagerRoleGroupDTO
import com.tencent.bk.sdk.iam.service.ManagerService
import com.tencent.bk.sdk.iam.util.JsonUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.auth.utils.IamUtils
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.ProjectPermissionService
import com.tencent.devops.project.service.iam.AuthorizationUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit

class TxV3ProjectPermissionServiceImpl @Autowired constructor(
    val iamManagerService: ManagerService,
    val iamConfiguration: IamConfiguration
) : ProjectPermissionService {

    override fun verifyUserProjectPermission(accessToken: String?, projectCode: String, userId: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun createResources(
        userId: String,
        accessToken: String?,
        resourceRegisterInfo: ResourceRegisterInfo,
        userDeptDetail: UserDeptDetail?
    ): String {
        // TODO: (V3创建项目未完全迁移完前，需双写V0,V3)
        /**
         *  V3创建项目流程 (V3创建项目未完全迁移完前，需双写V0,V3)
         *  1. 创建分级管理员，并记录iam分级管理员id
         *  2. 添加默认用户组”CI管理员“
         *  3. 分配”ALL action“权限到CI管理员
         *  4. 添加创建人到分级管理员
         */
        val iamProjectId = createIamProject(userId, resourceRegisterInfo)
        createRole(userId, iamProjectId.toInt(), resourceRegisterInfo.resourceCode)
        createManagerPermission(resourceRegisterInfo.resourceCode.toInt(), resourceRegisterInfo.resourceName)
        return iamProjectId
    }

    override fun deleteResource(projectCode: String) {
        TODO("Not yet implemented")
    }

    override fun modifyResource(projectCode: String, projectName: String) {
        TODO("Not yet implemented")
    }

    override fun getUserProjects(userId: String): List<String> {
        TODO("Not yet implemented")
    }

    override fun getUserProjectsAvailable(userId: String): Map<String, String> {
        TODO("Not yet implemented")
    }

    override fun verifyUserProjectPermission(
        accessToken: String?,
        projectCode: String,
        userId: String,
        permission: AuthPermission
    ): Boolean {
        TODO("Not yet implemented")
    }

    private fun createIamProject(userId: String, resourceRegisterInfo: ResourceRegisterInfo): String {
        val subjectScopes = ManagerScopes("user", "*")
        val authorizationScopes = AuthorizationUtils.buildManagerResources(
            projectId = resourceRegisterInfo.resourceCode,
            projectName = resourceRegisterInfo.resourceName,
            iamConfiguration = iamConfiguration
        )
        val createManagerDTO = CreateManagerDTO.builder().system(iamConfiguration.systemId)
            .name(resourceRegisterInfo.resourceName)
            .description(resourceRegisterInfo.resourceName)
            .members(arrayListOf(userId))
            .authorization_scopes(authorizationScopes)
            .subject_scopes(arrayListOf(subjectScopes)).build()
        logger.info("createIamProject: ${JsonUtil.toJson(createManagerDTO)}")
        logger.info("system ${iamConfiguration.systemId} | ${iamConfiguration.appCode} | ${iamConfiguration.apigwBaseUrl}")
        return iamManagerService.createManager(createManagerDTO).toString()
    }

    private fun createRole(userId: String, iamProjectId: Int, projectCode: String) {
        val defaultGroup = ManagerRoleGroup(IamUtils.buildIamGroup(projectCode, MANAGER_ROLE), MANAGER_ROLE)
        val defaultGroups = mutableListOf<ManagerRoleGroup>()
        defaultGroups.add(defaultGroup)
        val managerRoleGroup = ManagerRoleGroupDTO.builder().groups(defaultGroups).build()
        val groupId = iamManagerService.createManagerRoleGroup(iamProjectId, managerRoleGroup)
        val groupMember = ManagerMember(ManagerScopesEnum.USER, userId)
        val groupMembers = mutableListOf<ManagerMember>()
        groupMembers.add(groupMember)
        val expired = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(DEFAULT_EXPIRED_AT)
        val managerMemberGroup = ManagerMemberGroupDTO.builder().members(groupMembers).expiredAt(expired).build()
        iamManagerService.createRoleGroupMember(groupId, managerMemberGroup)
    }

    private fun createManagerPermission(projectId: Int, projectName: String) {
        val managerResources = mutableListOf<ManagerResources>()
        val managerPaths = mutableListOf<List<ManagerPath>>()
        val path = ManagerPath(iamConfiguration.systemId, AuthResourceType.PROJECT.value, projectId.toString(), projectName)
        val paths = mutableListOf<ManagerPath>()
        paths.add(path)
        managerPaths.add(paths)
        val resources = ManagerResources.builder()
            .system(iamConfiguration.systemId)
            .type(AuthResourceType.PROJECT.value)
            .paths(managerPaths)
            .build()
        managerResources.add(resources)

        val permission = AuthorizationScopes.builder()
            .actions(arrayListOf(Action("all_action")))
            .system(iamConfiguration.systemId)
            .resources(managerResources)
            .build()
        iamManagerService.createRolePermission(projectId, permission)
    }

    companion object {
        private const val DEFAULT_EXPIRED_AT = 365L // 用户组默认一年有效期
        private const val MANAGER_ROLE = "管理员"
        val logger = LoggerFactory.getLogger(TxV3ProjectPermissionServiceImpl::class.java)
    }
}
