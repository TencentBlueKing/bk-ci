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
import com.tencent.devops.auth.api.service.ServiceGroupStrategyResource
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.pojo.StrategyEntity
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.auth.utils.IamGroupUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dao.UserDao
import com.tencent.devops.project.dispatch.ProjectDispatcher
import com.tencent.devops.project.listener.TxIamV3CreateEvent
import com.tencent.devops.project.pojo.SubjectScope
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
    fun createIamV3Project(event: TxIamV3CreateEvent) {
        val watcher = Watcher(id = "IAM|CreateProject|${event.projectId}|${event.userId}")
        logger.info("start create iamV3 project: $event")
        try {
            val resourceRegisterInfo = event.resourceRegisterInfo
            val userId = event.userId
            val projectCode = resourceRegisterInfo.resourceCode
            val projectName = resourceRegisterInfo.resourceName
            var relationIam = false
            if (event.retryCount == 0) {
                logger.info("start create iam V3 project $event")
                watcher.start("createProject")
                // 创建iamV3分级管理员
                val gradeManagerId = createGradeManager(
                    userId = userId,
                    resourceRegisterInfo = resourceRegisterInfo,
                    subjectScopes = event.subjectScopes
                )
                logger.info("iamV3 project gradeManagerId: $gradeManagerId")
                watcher.start("batchCreateDefaultGroups")
                // 批量创建默认用户组
                batchCreateDefaultGroups(
                    userId = userId,
                    gradeManagerId = gradeManagerId.toInt(),
                    projectCode = projectCode,
                    projectName = projectName
                )
                event.iamProjectId = gradeManagerId
                watcher.start("findProject")
                val projectInfo = projectDao.getByEnglishName(dslContext, projectCode)
                if (projectInfo == null) {
                    event.retryCount = event.retryCount + 1
                    event.delayMills = 1000
                    projectDispatcher.dispatch(event)
                    return
                } else {
                    relationIam = true
                }
            } else if (event.retryCount < 10) {
                val projectInfo = projectDao.getByEnglishName(dslContext, projectCode)
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
                    projectCode,
                    event.iamProjectId.toString()
                )
            }
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher = watcher, warnThreshold = 5000)
        }
    }

    private fun batchCreateDefaultGroups(
        userId: String,
        gradeManagerId: Int,
        projectCode: String,
        projectName: String,
    ) {
        // 创建管理员组，赋予权限，并把项目创建人加入到管理员组
        createManagerGroup(
            userId = userId,
            gradeManagerId = gradeManagerId,
            projectCode = projectCode,
            projectName = projectName
        )
        // 创建默认组（开发组，测试组等），并赋予权限
        createDefaultGroup(
            userId = userId,
            gradeManagerId = gradeManagerId,
            projectCode = projectCode,
            defaultGroupType = DefaultGroupType.DEVELOPER
        )
        createDefaultGroup(
            userId = userId,
            gradeManagerId = gradeManagerId,
            projectCode = projectCode,
            defaultGroupType = DefaultGroupType.MAINTAINER
        )
        createDefaultGroup(
            userId = userId,
            gradeManagerId = gradeManagerId,
            projectCode = projectCode,
            defaultGroupType = DefaultGroupType.TESTER
        )
        createDefaultGroup(
            userId = userId,
            gradeManagerId = gradeManagerId,
            projectCode = projectCode,
            defaultGroupType = DefaultGroupType.QC
        )
        createDefaultGroup(
            userId = userId,
            gradeManagerId = gradeManagerId,
            projectCode = projectCode,
            defaultGroupType = DefaultGroupType.PM
        )
    }

    private fun createGradeManager(
        userId: String,
        resourceRegisterInfo: ResourceRegisterInfo,
        subjectScopes: List<SubjectScope>?
    ): String {
        val iamSubjectScopes: ArrayList<ManagerScopes> = ArrayList()
        // 若授权人员范围为空，则设置为全部人员
        if (subjectScopes == null) {
            iamSubjectScopes.add(ManagerScopes(ManagerScopesEnum.getType(ManagerScopesEnum.ALL), "*"))
        } else {
            subjectScopes.forEach {
                if (it.type == DEPARTMENT) {
                    iamSubjectScopes.add(ManagerScopes(ManagerScopesEnum.getType(ManagerScopesEnum.DEPARTMENT), it.id))
                } else {
                    iamSubjectScopes.add(ManagerScopes(ManagerScopesEnum.getType(ManagerScopesEnum.USER), it.id))
                }
            }
        }
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
            .subject_scopes(iamSubjectScopes).build()
        return iamManagerService.createManager(createManagerDTO).toString()
    }

    private fun createManagerGroup(userId: String, gradeManagerId: Int, projectCode: String, projectName: String) {
        val defaultGroup = ManagerRoleGroup(
            IamGroupUtils.buildIamGroup(projectCode, DefaultGroupType.MANAGER.displayName),
            IamGroupUtils.buildDefaultDescription(projectCode, DefaultGroupType.MANAGER.displayName, userId),
            false
        )
        val defaultGroups = mutableListOf<ManagerRoleGroup>()
        defaultGroups.add(defaultGroup)
        val managerRoleGroup = ManagerRoleGroupDTO.builder().groups(defaultGroups).build()
        // 创建组
        val roleId = iamManagerService.batchCreateRoleGroup(gradeManagerId, managerRoleGroup)
        val groupMember = ManagerMember(ManagerScopesEnum.getType(ManagerScopesEnum.USER), userId)
        val groupMembers = mutableListOf<ManagerMember>()
        groupMembers.add(groupMember)
        val expired = System.currentTimeMillis() / 1000 + TimeUnit.DAYS.toSeconds(DEFAULT_EXPIRED_AT)
        val managerMemberGroup = ManagerMemberGroupDTO.builder().members(groupMembers).expiredAt(expired).build()
        // 项目创建人添加至管理员分组
        iamManagerService.createRoleGroupMember(roleId, managerMemberGroup)
        createManagerPermission(projectCode, projectName, roleId)
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

    private fun createDefaultGroup(
        userId: String,
        gradeManagerId: Int,
        projectCode: String,
        defaultGroupType: DefaultGroupType
    ) {
        val defaultGroup = ManagerRoleGroup(
            IamGroupUtils.buildIamGroup(projectCode, defaultGroupType.displayName),
            IamGroupUtils.buildDefaultDescription(projectCode, defaultGroupType.displayName, userId),
            false
        )
        val defaultGroups = mutableListOf<ManagerRoleGroup>()
        defaultGroups.add(defaultGroup)
        val managerRoleGroup = ManagerRoleGroupDTO.builder().groups(defaultGroups).build()
        // 创建默认组
        val roleId = iamManagerService.batchCreateRoleGroup(gradeManagerId, managerRoleGroup)
        // 赋予权限
        try {
            when (defaultGroupType) {
                DefaultGroupType.DEVELOPER -> addIamGroupAction(roleId, projectCode, DefaultGroupType.DEVELOPER)
                DefaultGroupType.MAINTAINER -> addIamGroupAction(roleId, projectCode, DefaultGroupType.MAINTAINER)
                DefaultGroupType.TESTER -> addIamGroupAction(roleId, projectCode, DefaultGroupType.TESTER)
                DefaultGroupType.QC -> addIamGroupAction(roleId, projectCode, DefaultGroupType.QC)
                DefaultGroupType.PM -> addIamGroupAction(roleId, projectCode, DefaultGroupType.PM)
            }
        } catch (e: Exception) {
            iamManagerService.deleteRoleGroup(roleId)
            logger.warn(
                "create iam group permission fail : projectCode = $projectCode |" +
                    " iamRoleId = $roleId | groupInfo = ${defaultGroupType.value}", e
            )
            throw e
        }
    }

    private fun addIamGroupAction(
        roleId: Int,
        projectCode: String,
        group: DefaultGroupType
    ) {
        logger.info("iamV3 createDefaultGroup : ${group.value}")
        val actions = getGroupStrategy(group)
        if (actions.first.isNotEmpty()) {
            val authorizationScopes = buildCreateAuthorizationScopes(actions.first, projectCode)
            iamManagerService.createRolePermission(roleId, authorizationScopes)
        }
        if (actions.second.isNotEmpty()) {
            actions.second.forEach { (resource, actions) ->
                val groupAuthorizationScopes = buildOtherAuthorizationScopes(actions, projectCode, resource)
                iamManagerService.createRolePermission(roleId, groupAuthorizationScopes)
            }
        }
    }

    private fun getGroupStrategy(defaultGroup: DefaultGroupType): Pair<List<String>, Map<String, List<String>>> {
        val strategyList = client.get(ServiceGroupStrategyResource::class).getGroupStrategy()
        var strategyInfo: StrategyEntity? = null
        strategyList.forEach { strategyEntity ->
            if (strategyEntity.name == defaultGroup.displayName) {
                strategyInfo = strategyEntity
                return@forEach
            }
        }
        if (strategyInfo == null) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.STRATEGT_NAME_NOT_EXIST,
                defaultMessage = MessageCodeUtil.getCodeMessage(
                    messageCode = AuthMessageCode.STRATEGT_NAME_NOT_EXIST,
                    params = arrayOf(defaultGroup.value)
                )
            )
        }
        logger.info("getGroupStrategy ${strategyInfo!!.strategy}")
        val projectStrategyList = mutableListOf<String>()
        val resourceStrategyMap = mutableMapOf<String, List<String>>()
        strategyInfo!!.strategy.forEach { resource, list ->
            val actionData = buildAction(resource, list)
            projectStrategyList.addAll(actionData.first)
            resourceStrategyMap.putAll(actionData.second)
        }
        return Pair(projectStrategyList, resourceStrategyMap)
    }

    private fun buildCreateAuthorizationScopes(actions: List<String>, projectCode: String): AuthorizationScopes {
        val projectInfo = client.get(ServiceProjectResource::class).get(projectCode).data
        val managerResources = mutableListOf<ManagerResources>()
        val managerPath = mutableListOf<ManagerPath>()
        val projectPath = ManagerPath(
            iamConfiguration.systemId,
            AuthResourceType.PROJECT.value,
            projectCode,
            projectInfo?.projectName ?: ""
        )
        managerPath.add(projectPath)
        val paths = mutableListOf<List<ManagerPath>>()
        paths.add(managerPath)
        managerResources.add(
            ManagerResources.builder()
                .system(iamConfiguration.systemId)
                .type(AuthResourceType.PROJECT.value)
                .paths(paths).build()
        )
        val action = mutableListOf<Action>()
        actions.forEach {
            action.add(Action(it))
        }
        return AuthorizationScopes.builder()
            .system(iamConfiguration.systemId)
            .actions(action)
            .resources(managerResources)
            .build()
    }

    private fun buildOtherAuthorizationScopes(
        actions: List<String>,
        projectCode: String,
        defaultType: String? = null
    ): AuthorizationScopes? {
        val projectInfo = client.get(ServiceProjectResource::class).get(projectCode).data

        val resourceTypes = mutableSetOf<String>()
        var type = ""
        actions.forEach {
            resourceTypes.add(it.substringBeforeLast("_"))
            type = it.substringBeforeLast("_")
        }

        if (resourceTypes.size > 1) {
            logger.warn("buildOtherAuthorizationScopes not same resourceType : resourceTypes = $resourceTypes")
            return null
        }
        val managerResources = mutableListOf<ManagerResources>()
        val managerPath = mutableListOf<ManagerPath>()
        val projectPath = ManagerPath(
            iamConfiguration.systemId,
            AuthResourceType.PROJECT.value,
            projectCode,
            projectInfo?.projectName ?: ""
        )
        val iamType = if (defaultType.isNullOrEmpty()) {
            AuthResourceType.get(type).value
        } else {
            defaultType
        }

        val resourcePath = ManagerPath(
            iamConfiguration.systemId,
            iamType,
            "*",
            ""
        )
        managerPath.add(projectPath)
        managerPath.add(resourcePath)
        val paths = mutableListOf<List<ManagerPath>>()
        paths.add(managerPath)
        managerResources.add(
            ManagerResources.builder()
                .system(iamConfiguration.systemId)
                .type(iamType)
                .paths(paths).build()
        )
        val action = mutableListOf<Action>()
        actions.forEach {
            action.add(Action(it))
        }
        return AuthorizationScopes.builder()
            .system(iamConfiguration.systemId)
            .actions(action)
            .resources(managerResources)
            .build()
    }

    private fun buildAction(resource: String, actionList: List<String>): Pair<List<String>, Map<String, List<String>>> {
        val projectStrategyList = mutableListOf<String>()
        val resourceStrategyMap = mutableMapOf<String, List<String>>()
        val resourceStrategyList = mutableListOf<String>()
        // 如果是project相关的资源, 直接拼接action
        if (resource == AuthResourceType.PROJECT.value) {
            actionList.forEach { projectAction ->
                projectStrategyList.add(resource + "_" + projectAction)
            }
        } else {
            actionList.forEach {
                // 如果是非project资源。 若action是create,需挂在project下,因create相关的资源都是绑定在项目下。
                if (it == AuthPermission.CREATE.value) {
                    projectStrategyList.add(resource + "_" + it)
                } else {
                    resourceStrategyList.add(resource + "_" + it)
                }
            }
            resourceStrategyMap[resource] = resourceStrategyList
            logger.info("$resource $resourceStrategyList")
        }
        return Pair(projectStrategyList, resourceStrategyMap)
    }

    companion object {
        private const val DEFAULT_EXPIRED_AT = 365L // 用户组默认一年有效期
        private const val SYSTEM_DEFAULT_NAME = "蓝盾"
        private const val DEPARTMENT = "department"
        val logger = LoggerFactory.getLogger(IamV3Service::class.java)
    }
}
