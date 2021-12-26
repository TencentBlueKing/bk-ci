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

package com.tencent.devops.project.service.iam

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
import com.tencent.devops.auth.api.ServiceGroupResource
import com.tencent.devops.auth.api.service.ServiceDeptResource
import com.tencent.devops.auth.pojo.dto.GroupDTO
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.auth.utils.IamGroupUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dao.UserDao
import com.tencent.devops.project.dispatch.ProjectDispatcher
import com.tencent.devops.project.listener.TxIamV3CreateEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit

class IamV3Service @Autowired constructor(
    val iamManagerService: ManagerService,
    val iamConfiguration: IamConfiguration,
    val projectDao: ProjectDao,
    val dslContext: DSLContext,
    val projectDispatcher: ProjectDispatcher,
    val client: Client,
    val userDao: UserDao
) {
    /**
     *  V3创建项目流程 (V3创建项目未完全迁移完前，需双写V0,V3)
     *  1. 创建分级管理员，并记录iam分级管理员id
     *  2. 添加创建人到分级管理员
     *  3. 添加默认用户组”CI管理员“
     *  4. 添加创建人到CI管理员
     *  5. 分配”ALL action“权限到CI管理员
     */
    fun createIamV3Project(event: TxIamV3CreateEvent) {
        val watcher = Watcher(
            id = "IAM|CreateProject|${event.projectId}|${event.userId}"
        )
        logger.info("start create iamV3 project: $event")
        try {
            val resourceRegisterInfo = event.resourceRegisterInfo
            val userId = event.userId
            var relationIam = false
            if (event.retryCount == 0) {
                logger.info("start create iam V3 project $event")
                watcher.start("createProject")
                // 创建iamV3 分级管理员
                val iamProjectId = createIamProject(userId, resourceRegisterInfo)
                watcher.start("createManagerRole")
                // 创建分级管理员下管理员分组
                val roleId = createRole(userId, iamProjectId.toInt(), resourceRegisterInfo.resourceCode)
                watcher.start("createManagerPermission")
                // 添加管理员分组默认权限
                createManagerPermission(resourceRegisterInfo.resourceCode, resourceRegisterInfo.resourceName, roleId)
                event.iamProjectId = iamProjectId
                watcher.start("findProject")
                val projectInfo = projectDao.getByEnglishName(dslContext, resourceRegisterInfo.resourceCode)
                if (projectInfo == null) {
                    event.retryCount = event.retryCount + 1
                    event.delayMills = 1000
                    projectDispatcher.dispatch(event)
                    return
                } else {
                    relationIam = true
                }
            } else if (event.retryCount < 10) {
                val projectInfo = projectDao.getByEnglishName(dslContext, resourceRegisterInfo.resourceCode)
                if (projectInfo == null) {
                    event.retryCount = event.retryCount + 1
                    event.delayMills = 1000
                    logger.info("find ${resourceRegisterInfo.resourceCode} ${event.retryCount} times")
                    projectDispatcher.dispatch(event)
                    return
                } else {
                    relationIam = true
                }
            } else {
                logger.warn("create iam projectFail, ${resourceRegisterInfo.resourceCode} not find")
            }

            // 修改V3项目对应的projectId
            if (relationIam && !event.iamProjectId.isNullOrEmpty()) {
                projectDao.updateRelationByCode(
                    dslContext,
                    resourceRegisterInfo.resourceCode,
                    event.iamProjectId.toString()
                )
            }
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher = watcher, warnThreshold = 5000)
        }
    }

    // 分级管理员操作的用户范围,只能添加用户所在bg组织. 此处直接从project本地拿,最真实数据在用户中心
    private fun createIamProject(userId: String, resourceRegisterInfo: ResourceRegisterInfo): String {
        val bgName = userDao.get(dslContext, userId)?.bgName!!
        val deptInfo = client.get(ServiceDeptResource::class).getDeptByName(userId, bgName).data
            ?: throw ErrorCodeException(
                errorCode = ProjectMessageCode.QUERY_USER_INFO_FAIL,
                defaultMessage = MessageCodeUtil.getCodeLanMessage(
                    messageCode = ProjectMessageCode.QUERY_USER_INFO_FAIL,
                    defaultMessage = "获取用户$userId 信息失败",
                    params = arrayOf(userId)
                )
            )
        val bgId = deptInfo.results[0].id
        logger.info("user $userId bg: $bgId bgName: $bgName")
        val subjectScopes = ManagerScopes(
            ManagerScopesEnum.getType(ManagerScopesEnum.DEPARTMENT),
            bgId.toString())
        val authorizationScopes = AuthorizationUtils.buildManagerResources(
            projectId = resourceRegisterInfo.resourceCode,
            projectName = resourceRegisterInfo.resourceName,
            iamConfiguration = iamConfiguration
        )
        val createManagerDTO = CreateManagerDTO.builder().system(iamConfiguration.systemId)
            .name("$SYSTEM_DEFAULT_NAME-${resourceRegisterInfo.resourceName}")
            .description(IamGroupUtils.buildManagerDescription(resourceRegisterInfo.resourceName, userId))
            .members(arrayListOf(userId))
            .authorization_scopes(authorizationScopes)
            .subject_scopes(arrayListOf(subjectScopes)).build()
        return iamManagerService.createManager(createManagerDTO).toString()
    }

    private fun createRole(userId: String, iamProjectId: Int, projectCode: String): Int {
        val defaultGroup = ManagerRoleGroup(
            IamGroupUtils.buildIamGroup(projectCode, DefaultGroupType.MANAGER.displayName),
            IamGroupUtils.buildDefaultDescription(projectCode, DefaultGroupType.MANAGER.displayName, userId),
            true
        )
        val defaultGroups = mutableListOf<ManagerRoleGroup>()
        defaultGroups.add(defaultGroup)
        val managerRoleGroup = ManagerRoleGroupDTO.builder().groups(defaultGroups).build()
        // 添加项目管理员
        val roleId = iamManagerService.batchCreateRoleGroup(iamProjectId, managerRoleGroup)
        val groupMember = ManagerMember(ManagerScopesEnum.getType(ManagerScopesEnum.USER), userId)
        val groupMembers = mutableListOf<ManagerMember>()
        groupMembers.add(groupMember)
        val expired = System.currentTimeMillis() / 1000 + TimeUnit.DAYS.toSeconds(DEFAULT_EXPIRED_AT)
        val managerMemberGroup = ManagerMemberGroupDTO.builder().members(groupMembers).expiredAt(expired).build()
        // 项目创建人添加至管理员分组
        iamManagerService.createRoleGroupMember(roleId, managerMemberGroup)
        // 添加本地用户组信息
        client.get(ServiceGroupResource::class).createGroup(
            userId = userId,
            projectCode = projectCode,
            groupInfo = GroupDTO(
                groupCode = DefaultGroupType.MANAGER.value,
                groupName = DefaultGroupType.MANAGER.displayName,
                displayName = DefaultGroupType.MANAGER.displayName,
                relationId = roleId.toString(),
                groupType = true
            )
        )
        return roleId
    }

    private fun createManagerPermission(projectId: String, projectName: String, roleId: Int) {
        val managerResources = mutableListOf<ManagerResources>()
        val managerPaths = mutableListOf<List<ManagerPath>>()
        val path = ManagerPath(
            iamConfiguration.systemId,
            AuthResourceType.PROJECT.value,
            projectId,
            projectName
        )
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
        iamManagerService.createRolePermission(roleId, permission)
    }

    companion object {
        private const val DEFAULT_EXPIRED_AT = 365L // 用户组默认一年有效期
        private const val SYSTEM_DEFAULT_NAME = "蓝盾"
        val logger = LoggerFactory.getLogger(IamV3Service::class.java)
    }
}
