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

package com.tencent.devops.auth.service.migrate

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.bk.sdk.iam.dto.manager.Action
import com.tencent.bk.sdk.iam.dto.manager.AuthorizationScopes
import com.tencent.bk.sdk.iam.dto.manager.ManagerMember
import com.tencent.bk.sdk.iam.dto.manager.ManagerPath
import com.tencent.bk.sdk.iam.dto.manager.ManagerResources
import com.tencent.bk.sdk.iam.dto.manager.ManagerRoleGroup
import com.tencent.bk.sdk.iam.dto.manager.RoleGroupMemberInfo
import com.tencent.bk.sdk.iam.dto.manager.dto.ManagerMemberGroupDTO
import com.tencent.bk.sdk.iam.dto.manager.dto.ManagerRoleGroupDTO
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthMigrationDao
import com.tencent.devops.auth.dao.AuthResourceGroupConfigDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.pojo.migrate.MigrateTaskDataResult
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.PermissionGroupPoliciesService
import com.tencent.devops.auth.service.RbacCacheService
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.auth.service.migrate.MigrateIamApiService.Companion.GROUP_API_POLICY
import com.tencent.devops.auth.service.migrate.MigrateIamApiService.Companion.GROUP_WEB_POLICY
import com.tencent.devops.auth.service.migrate.MigrateIamApiService.Companion.USER_CUSTOM_POLICY
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.DefaultGroupType
import com.tencent.devops.common.auth.utils.RbacAuthUtils
import com.tencent.devops.common.web.utils.I18nUtil
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Suppress("LongParameterList", "TooManyFunctions")
abstract class AbMigratePolicyService(
    private val v2ManagerService: V2ManagerService,
    private val iamConfiguration: IamConfiguration,
    private val dslContext: DSLContext,
    private val authResourceGroupDao: AuthResourceGroupDao,
    private val authResourceGroupConfigDao: AuthResourceGroupConfigDao,
    private val migrateIamApiService: MigrateIamApiService,
    private val authMigrationDao: AuthMigrationDao,
    private val permissionService: PermissionService,
    private val rbacCacheService: RbacCacheService,
    private val deptService: DeptService,
    private val permissionGroupPoliciesService: PermissionGroupPoliciesService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(AbMigratePolicyService::class.java)

        // 自定义用户组默认过期时间6个月
        private const val DEFAULT_EXPIRED_DAY = 180L

        // 毫秒转换
        const val MILLISECOND = 1000

        // 用户创建用户组group_code
        private const val CUSTOM_GROUP_CODE = "custom"

        const val RBAC_QC_GROUP_NAME = "质管人员"
    }

    fun migrateGroupPolicy(
        projectCode: String,
        projectName: String,
        version: String,
        gradeManagerId: Int
    ) {
        logger.info("start to migrate group policy")
        val watcher = Watcher("migrateGroupPolicy|$projectCode")
        try {
            val managerGroupId = authResourceGroupDao.get(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode,
                groupCode = DefaultGroupType.MANAGER.value
            )?.relationId?.toInt() ?: throw ErrorCodeException(
                errorCode = AuthMessageCode.ERROR_AUTH_GROUP_NOT_EXIST,
                params = arrayOf(DefaultGroupType.MANAGER.value),
                defaultMessage = "${DefaultGroupType.MANAGER.value} group not exist"
            )
            // 蓝盾创建的默认用户组权限
            watcher.start("group_api_policy")
            val groupApiPolicyCount = loopMigrateGroup(
                projectCode = projectCode,
                version = version,
                migrateType = GROUP_API_POLICY,
                projectName = projectName,
                gradeManagerId = gradeManagerId,
                managerGroupId = managerGroupId
            )
            // 用户在权限中心创建的用户组权限
            watcher.start("group_web_policy")
            val groupWebPolicyCount = loopMigrateGroup(
                projectCode = projectCode,
                version = version,
                migrateType = GROUP_WEB_POLICY,
                projectName = projectName,
                gradeManagerId = gradeManagerId,
                managerGroupId = managerGroupId
            )
            watcher.start("calculateGroupCount")
            val beforeGroupCount = groupApiPolicyCount + groupWebPolicyCount
            calculateGroupCount(projectCode, beforeGroupCount)
        } finally {
            watcher.stop()
            logger.info("migrate group policy|$projectCode|$watcher")
        }
    }

    private fun loopMigrateGroup(
        projectCode: String,
        version: String,
        migrateType: String,
        projectName: String,
        gradeManagerId: Int,
        managerGroupId: Int
    ): Int {
        var page = 1
        var totalCount = 0
        val pageSize = PageUtil.MAX_PAGE_SIZE
        do {
            val taskDataResp = migrateIamApiService.getMigrateData(
                projectCode = projectCode,
                migrateType = migrateType,
                version = version,
                page = page,
                pageSize = pageSize
            )
            migrateGroup(
                projectCode = projectCode,
                projectName = projectName,
                gradeManagerId = gradeManagerId,
                managerGroupId = managerGroupId,
                results = taskDataResp.results
            )
            page++
            totalCount = taskDataResp.count
        } while (taskDataResp.results.size == pageSize)
        return totalCount
    }

    private fun migrateGroup(
        projectCode: String,
        projectName: String,
        gradeManagerId: Int,
        managerGroupId: Int,
        results: List<MigrateTaskDataResult>
    ) {
        results.forEach result@{ result ->
            logger.info("migrate group|${result.projectId}|${result.subject.name}|${result.subject.id}")
            val rbacAuthorizationScopeList = buildRbacAuthorizationScopeList(
                projectCode = projectCode,
                projectName = projectName,
                managerGroupId = managerGroupId,
                result = result
            )
            logger.info(
                "migrate group|${result.projectId}|${result.subject.name}|${
                    JsonUtil.toJson(
                        rbacAuthorizationScopeList,
                        false
                    )
                }"
            )

            if (rbacAuthorizationScopeList.isEmpty()) {
                return@result
            }

            // 创建用户组
            val groupName = getGroupName(projectName = projectName, result = result)
            val groupInfo = authResourceGroupDao.getByGroupName(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode,
                groupName = groupName
            )
            val (defaultGroup, groupId) = groupInfo?.let {
                Pair(it.defaultGroup, it.relationId.toInt())
            } ?: run {
                val rbacGroupId = createRbacGroup(
                    groupName = groupName,
                    gradeManagerId = gradeManagerId,
                    projectCode = projectCode,
                    projectName = projectName
                )
                Pair(false, rbacGroupId)
            }

            // 用户组授权
            rbacAuthorizationScopeList.forEach { authorizationScope ->
                v2ManagerService.grantRoleGroupV2(groupId, authorizationScope)
            }
            // 迁移的用户组默认都添加project_visit权限
            val projectVisitScope = buildProjectVisitAuthorizationScope(
                projectCode = projectCode,
                projectName = projectName
            )
            v2ManagerService.grantRoleGroupV2(groupId, projectVisitScope)
            // 往用户组添加成员
            batchAddGroupMember(groupId = groupId, defaultGroup = defaultGroup, members = result.members)
        }
    }

    abstract fun buildRbacAuthorizationScopeList(
        projectCode: String,
        projectName: String,
        managerGroupId: Int,
        result: MigrateTaskDataResult
    ): List<AuthorizationScopes>

    abstract fun batchAddGroupMember(groupId: Int, defaultGroup: Boolean, members: List<RoleGroupMemberInfo>?)

    abstract fun getGroupName(projectName: String, result: MigrateTaskDataResult): String

    fun migrateUserCustomPolicy(
        projectCode: String,
        projectName: String,
        version: String,
        gradeManagerId: Int
    ) {
        logger.info("start to migrate user custom policy|$projectCode")
        val startEpoch = System.currentTimeMillis()
        try {
            val managerGroupId = authResourceGroupDao.get(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode,
                groupCode = DefaultGroupType.MANAGER.value
            )?.relationId?.toInt() ?: throw ErrorCodeException(
                errorCode = AuthMessageCode.ERROR_AUTH_GROUP_NOT_EXIST,
                params = arrayOf(DefaultGroupType.MANAGER.value),
                defaultMessage = "${DefaultGroupType.MANAGER.value} group not exist"
            )
            loopMigrateUserCustom(
                projectCode = projectCode,
                projectName = projectName,
                gradeManagerId = gradeManagerId,
                managerGroupId = managerGroupId,
                version = version
            )
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to migrate user custom policy $projectCode"
            )
        }
    }

    private fun loopMigrateUserCustom(
        projectCode: String,
        projectName: String,
        gradeManagerId: Int,
        managerGroupId: Int,
        version: String
    ): Int {
        var page = 1
        var totalCount = 0
        val pageSize = PageUtil.MAX_PAGE_SIZE
        do {
            val taskDataResp = migrateIamApiService.getMigrateData(
                projectCode = projectCode,
                migrateType = USER_CUSTOM_POLICY,
                version = version,
                page = page,
                pageSize = pageSize
            )
            migrateUserCustom(
                projectCode = projectCode,
                projectName = projectName,
                gradeManagerId = gradeManagerId,
                managerGroupId = managerGroupId,
                results = taskDataResp.results
            )
            page++
            totalCount = taskDataResp.count
        } while (taskDataResp.results.size == pageSize)
        return totalCount
    }

    private fun migrateUserCustom(
        projectCode: String,
        projectName: String,
        gradeManagerId: Int,
        managerGroupId: Int,
        results: List<MigrateTaskDataResult>
    ) {
        results.forEach { result ->
            logger.info("migrate user custom policy|${result.projectId}|${result.subject.id}")
            val userId = result.subject.id
            // 离职人员,直接忽略
            if (deptService.getUserInfo(userId = "admin", name = userId) == null) {
                logger.warn("user has resigned, skip custom policy migration|${result.projectId}|$userId")
                return@forEach
            }

            result.permissions.forEach permission@{ permission ->
                val groupIds = matchResourceGroup(
                    userId = userId,
                    projectCode = projectCode,
                    projectName = projectName,
                    gradeManagerId = gradeManagerId,
                    managerGroupId = managerGroupId,
                    permission = permission
                )
                groupIds.forEach { groupId ->
                    val managerMember = ManagerMember(ManagerScopesEnum.getType(ManagerScopesEnum.USER), userId)
                    val managerMemberGroupDTO = ManagerMemberGroupDTO.builder()
                        .members(listOf(managerMember))
                        .expiredAt(
                            System.currentTimeMillis() / MILLISECOND + TimeUnit.DAYS.toSeconds(DEFAULT_EXPIRED_DAY)
                        ).build()
                    v2ManagerService.createRoleGroupMemberV2(groupId, managerMemberGroupDTO)
                }
            }
        }
    }

    /**
     * 匹配到的资源组Id列表
     *
     * v3 action只能匹配到一个组
     * v0 iam返回的action列表包含创建动作,创建动作依赖的资源是项目,所以一个action组可能匹配到多个组,
     * 如actions:[pipeline_create,pipeline_view],pipeline_create需查找项目下的组,pipeline_view需查找pipeline下的组
     */
    abstract fun matchResourceGroup(
        userId: String,
        projectCode: String,
        projectName: String,
        gradeManagerId: Int,
        managerGroupId: Int,
        permission: AuthorizationScopes
    ): List<Int>

    /**
     * 根据action匹配资源最小action组
     */
    protected fun matchMinResourceGroup(
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        actions: List<String>
    ): Int? {
        logger.info("match min resource group|$userId|$resourceType|$actions")
        // 判断用户是否已有资源actions权限
        val hasPermission = permissionService.batchValidateUserResourcePermission(
            userId = userId,
            actions = actions,
            projectCode = projectCode,
            resourceCode = resourceCode,
            resourceType = resourceType
        ).all { it.value }

        if (hasPermission) {
            logger.info("user has resource action permission|$userId|$actions$projectCode|$resourceCode")
            return null
        }
        return getMatchResourceGroupId(
            resourceType = resourceType,
            actions = actions,
            projectCode = projectCode,
            resourceCode = resourceCode,
            userId = userId
        ).second ?: run {
            logger.info("user not match resource group|$userId|$actions$projectCode|$resourceCode")
            null
        }
    }

    /**
     * 匹配或创建项目资源用户组
     *
     * 有任意资源权限,直接匹配项目下资源权限组,如果没有匹配到,则创建
     */
    protected fun matchOrCreateProjectResourceGroup(
        userId: String,
        projectCode: String,
        projectName: String,
        resourceType: String,
        actions: List<String>,
        gradeManagerId: Int
    ): Int? {
        logger.info("match or create project resource group|$userId|$resourceType|$actions")
        // 判断用户是否已有项目任意资源actions权限
        val hasPermission = actions.all { action ->
            permissionService.validateUserResourcePermission(
                userId = userId,
                action = action,
                projectCode = projectCode,
                resourceType = resourceType
            )
        }
        if (hasPermission) {
            logger.info("user has project any resource permission|$userId|$actions|$projectCode")
            return null
        }
        val (groupConfigId, groupId) = getMatchResourceGroupId(
            resourceType = AuthResourceType.PROJECT.value,
            actions = actions,
            projectCode = projectCode,
            resourceCode = projectCode,
            userId = userId
        )
        return groupId ?: run {
            groupConfigId?.let {
                createProjectResourceGroup(
                    groupConfigId = groupConfigId,
                    gradeManagerId = gradeManagerId,
                    projectCode = projectCode,
                    projectName = projectName
                )
            }
        }
    }

    private fun getMatchResourceGroupId(
        resourceType: String,
        actions: List<String>,
        projectCode: String,
        resourceCode: String,
        userId: String
    ): Pair<Long? /*groupConfigId*/, Int? /*groupId*/> {
        rbacCacheService.getGroupConfigAction(resourceType).forEach groupConfig@{ groupConfig ->
            if (groupConfig.actions.containsAll(actions)) {
                val groupId = authResourceGroupDao.get(
                    dslContext = dslContext,
                    projectCode = projectCode,
                    resourceType = resourceType,
                    resourceCode = resourceCode,
                    groupCode = groupConfig.groupCode
                )?.relationId?.toInt()
                logger.info(
                    "user match resource group" +
                        "|$userId|$actions|$projectCode|$resourceCode|${groupConfig.groupCode}|$groupId"
                )
                return Pair(groupConfig.id, groupId)
            }
        }
        return Pair(null, null)
    }

    private fun createRbacGroup(
        groupName: String,
        gradeManagerId: Int,
        projectCode: String,
        projectName: String
    ): Int {
        val managerRoleGroup = ManagerRoleGroup().apply {
            name = groupName
            description = I18nUtil.getCodeLanMessage(
                messageCode = AuthMessageCode.MIGRATION_GROUP_DESCRIPTION,
                params = arrayOf(
                    groupName,
                    DateTimeUtil.toDateTime(LocalDateTime.now(), "yyyy-MM-dd'T'HH:mm:ssZ")
                )
            )
        }
        val managerRoleGroupDTO = ManagerRoleGroupDTO.builder()
            .groups(listOf(managerRoleGroup))
            .createAttributes(false)
            .build()
        val groupId = v2ManagerService.batchCreateRoleGroupV2(gradeManagerId, managerRoleGroupDTO)
        val groupConfig = authResourceGroupConfigDao.getByName(
            dslContext = dslContext,
            resourceType = AuthResourceType.PROJECT.value,
            groupName = groupName
        )
        authResourceGroupDao.create(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode,
            resourceName = projectName,
            iamResourceCode = projectCode,
            groupCode = groupConfig?.groupCode ?: CUSTOM_GROUP_CODE,
            groupName = groupName,
            defaultGroup = groupConfig != null,
            relationId = groupId.toString()
        )
        return groupId
    }

    /**
     * 创建项目级资源用户组
     *
     * 针对用户自定义权限,当用户有资源任意权限,如有任意流水线执行权限，项目默认组不能匹配该action,需要创建项目级流水线执行者组,用于管理这类权限
     */
    private fun createProjectResourceGroup(
        groupConfigId: Long,
        gradeManagerId: Int,
        projectCode: String,
        projectName: String
    ): Int? {
        logger.info("create project resource group|$groupConfigId|$gradeManagerId|$projectCode|$projectName")
        val groupConfig = authResourceGroupConfigDao.getById(dslContext = dslContext, id = groupConfigId) ?: run {
            logger.warn("group config not found|id:$groupConfigId")
            return null
        }
        val managerRoleGroup = ManagerRoleGroup().apply {
            name = groupConfig.groupName
            description = groupConfig.description
        }
        val managerRoleGroupDTO = ManagerRoleGroupDTO.builder()
            .groups(listOf(managerRoleGroup))
            .createAttributes(false)
            .build()
        val iamGroupId = v2ManagerService.batchCreateRoleGroupV2(gradeManagerId, managerRoleGroupDTO)

        authResourceGroupDao.create(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode,
            resourceName = projectName,
            iamResourceCode = projectCode,
            groupCode = groupConfig.groupCode,
            groupName = groupConfig.groupName,
            defaultGroup = false,
            relationId = iamGroupId.toString()
        )
        permissionGroupPoliciesService.grantGroupPermission(
            authorizationScopesStr = groupConfig.authorizationScopes,
            projectCode = projectCode,
            projectName = projectName,
            iamResourceCode = projectCode,
            resourceName = projectName,
            iamGroupId = iamGroupId
        )
        return iamGroupId
    }

    private fun calculateGroupCount(projectCode: String, beforeGroupCount: Int) {
        val afterGroupCount = authResourceGroupDao.countByResourceCode(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode
        )
        authMigrationDao.updateGroupCount(
            dslContext = dslContext,
            projectCode = projectCode,
            beforeGroupCount = beforeGroupCount,
            afterGroupCount = afterGroupCount
        )
    }

    /**
     * 迁移的组都需要添加project_visit权限
     */
    private fun buildProjectVisitAuthorizationScope(
        projectCode: String,
        projectName: String
    ): AuthorizationScopes {
        val projectVisit = RbacAuthUtils.buildAction(AuthPermission.VISIT, AuthResourceType.PROJECT)
        val projectPath = ManagerPath().apply {
            system = iamConfiguration.systemId
            id = projectCode
            name = projectName
            type = AuthResourceType.PROJECT.value
        }
        val projectManagerResource = ManagerResources.builder()
            .system(iamConfiguration.systemId)
            .type(AuthResourceType.PROJECT.value)
            .paths(listOf(listOf(projectPath)))
            .build()
        return AuthorizationScopes.builder()
            .system(iamConfiguration.systemId)
            .actions(listOf(Action(projectVisit)))
            .resources(listOf(projectManagerResource))
            .build()
    }
}
