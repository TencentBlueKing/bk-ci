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
import com.tencent.bk.sdk.iam.dto.manager.Action
import com.tencent.bk.sdk.iam.dto.manager.AuthorizationScopes
import com.tencent.bk.sdk.iam.dto.manager.ManagerMember
import com.tencent.bk.sdk.iam.dto.manager.ManagerPath
import com.tencent.bk.sdk.iam.dto.manager.ManagerResources
import com.tencent.bk.sdk.iam.dto.manager.RoleGroupMemberInfo
import com.tencent.bk.sdk.iam.dto.manager.dto.ManagerMemberGroupDTO
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.dao.AuthMigrationDao
import com.tencent.devops.auth.dao.AuthResourceGroupConfigDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.pojo.migrate.MigrateTaskDataResult
import com.tencent.devops.auth.service.AuthResourceCodeConverter
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.PermissionGroupPoliciesService
import com.tencent.devops.auth.service.RbacCacheService
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import org.apache.commons.lang3.RandomUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

@Suppress("LongParameterList", "NestedBlockDepth", "TooManyFunctions")
class MigrateV0PolicyService constructor(
    private val v2ManagerService: V2ManagerService,
    private val iamConfiguration: IamConfiguration,
    private val dslContext: DSLContext,
    private val authResourceGroupDao: AuthResourceGroupDao,
    private val authResourceGroupConfigDao: AuthResourceGroupConfigDao,
    private val migrateResourceCodeConverter: MigrateResourceCodeConverter,
    private val migrateIamApiService: MigrateIamApiService,
    private val authResourceCodeConverter: AuthResourceCodeConverter,
    private val permissionService: PermissionService,
    private val rbacCacheService: RbacCacheService,
    private val authMigrationDao: AuthMigrationDao,
    private val deptService: DeptService,
    private val permissionGroupPoliciesService: PermissionGroupPoliciesService
) : AbMigratePolicyService(
    v2ManagerService = v2ManagerService,
    iamConfiguration = iamConfiguration,
    dslContext = dslContext,
    authResourceGroupDao = authResourceGroupDao,
    authResourceGroupConfigDao = authResourceGroupConfigDao,
    migrateIamApiService = migrateIamApiService,
    authMigrationDao = authMigrationDao,
    permissionService = permissionService,
    rbacCacheService = rbacCacheService,
    deptService = deptService,
    permissionGroupPoliciesService = permissionGroupPoliciesService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(MigrateV0PolicyService::class.java)

        // v0默认用户组过期时间,2年或者3年
        private val V0_GROUP_EXPIRED_DAY = listOf(180L, 360L, 720L, 1080L)

        // v0的资源类型group(版本体验、质量红线组),task(版本体验、codecc任务)存在重复,iam迁移时添加了serviceCode,所以需要转换
        private val oldActionMappingNewAction = mapOf(
            "codecc_task_defect_view" to "codecc_task_view-defect",
            "codecc_task_task_manage" to "codecc_task_manage",
            "codecc_task_defect_manage" to "codecc_task_manage-defect",
            "codecc_task_report_view" to "codecc_task_view-report",

            "quality_gate_group_delete" to "quality_group_delete",
            "quality_gate_group_edit" to "quality_group_edit",
            "quality_gate_group_create" to "quality_group_create"
        )

        // v0有的action，但是在rbac没有,需要跳过
        private val skipActions = listOf("quality_gate_group_enable")

        private val oldResourceTypeMappingNewResourceType = mapOf(
            "quality_gate_group" to "quality_group"
        )
        private const val V0_QC_GROUP_NAME = "质量管理员"
    }

    fun startMigrateTask(projectCodes: List<String>): Int {
        logger.info("start v0 migrate task $projectCodes")
        return migrateIamApiService.startV0MigrateTask(projectCodes = projectCodes)
    }

    /**
     * 轮询获取迁移任务状态,如果状态没有成功,等待30s再重试
     */
    fun loopTaskStatus(migrateTaskId: Int) {
        logger.info("loop $migrateTaskId migrate v0 task status")
        val migrateTaskStatus = migrateIamApiService.getV0MigrateTaskStatus(migrateTaskId)
        if (migrateTaskStatus != MigrateIamApiService.SUCCESSFUL_IAM_MIGRATE_TASK_SUCCESS) {
            logger.info(
                "$migrateTaskId migrate task status $migrateTaskStatus not success, " +
                    "sleep ${MigrateIamApiService.SLEEP_LOOP_IAM_GET_MIGRATE_TASK}(s)"
            )
            Thread.sleep(MigrateIamApiService.SLEEP_LOOP_IAM_GET_MIGRATE_TASK)
            loopTaskStatus(migrateTaskId)
        }
    }

    override fun buildRbacAuthorizationScopeList(
        projectCode: String,
        projectName: String,
        managerGroupId: Int,
        result: MigrateTaskDataResult
    ): List<AuthorizationScopes> {
        val rbacAuthorizationScopes = mutableListOf<AuthorizationScopes>()
        val projectActions = mutableListOf<Action>()
        result.permissions.forEach permission@{ permission ->
            val (resourceCreateActions, resourceActions) = buildRbacActions(
                actions = permission.actions.map { it.id }
            )
            if (resourceCreateActions.isNotEmpty()) {
                projectActions.addAll(resourceCreateActions)
            }
            if (resourceActions.isEmpty()) {
                return@permission
            }
            val rbacResources = buildRbacManagerResources(
                projectCode = projectCode,
                projectName = projectName,
                permission = permission
            )
            rbacAuthorizationScopes.add(
                AuthorizationScopes.builder()
                    .system(iamConfiguration.systemId)
                    .actions(resourceActions)
                    .resources(rbacResources)
                    .build()
            )
        }
        // 添加项目resource
        if (projectActions.isNotEmpty()) {
            val projectResource = buildProjectManagerResources(
                projectCode = projectCode,
                projectName = projectName
            )
            rbacAuthorizationScopes.add(
                AuthorizationScopes.builder()
                    .system(iamConfiguration.systemId)
                    .actions(projectActions)
                    .resources(listOf(projectResource))
                    .build()
            )
        }
        return rbacAuthorizationScopes
    }

    private fun buildRbacActions(actions: List<String>): Pair<List<Action>, List<Action>> {
        val resourceCreateActions = mutableListOf<Action>()
        val resourceActions = mutableListOf<Action>()
        replaceOrRemoveAction(actions).forEach { action ->
            // 创建的action,需要关联在项目下
            if (action.contains(AuthPermission.CREATE.value)) {
                resourceCreateActions.add(Action(action))
            } else {
                resourceActions.add(Action(action))
            }
        }
        return Pair(resourceCreateActions, resourceActions)
    }

    @Suppress("NestedBlockDepth", "ReturnCount", "LongMethod")
    private fun buildRbacManagerResources(
        projectCode: String,
        projectName: String,
        permission: AuthorizationScopes
    ): List<ManagerResources> {
        val rbacManagerResources = mutableListOf<ManagerResources>()
        permission.resources.forEach resource@{ resource ->
            val rbacPaths = mutableListOf<List<ManagerPath>>()
            // v0迁移,paths中iam没有返回项目信息,需要补充
            if (resource.paths.isEmpty()) {
                val rbacManagerPaths = mutableListOf<ManagerPath>()
                val rbacProjectManagerPath = ManagerPath().apply {
                    system = iamConfiguration.systemId
                    id = projectCode
                    name = projectName
                    type = AuthResourceType.PROJECT.value
                }
                rbacManagerPaths.add(rbacProjectManagerPath)
                rbacPaths.add(rbacManagerPaths)
            } else {
                resource.paths.forEach paths@{ managerPaths ->
                    val rbacManagerPaths = mutableListOf<ManagerPath>()
                    // 补充项目path
                    val rbacProjectManagerPath = ManagerPath().apply {
                        system = iamConfiguration.systemId
                        id = projectCode
                        name = projectName
                        type = AuthResourceType.PROJECT.value
                    }
                    rbacManagerPaths.add(rbacProjectManagerPath)
                    managerPaths.forEach managerPath@{ managerPath ->
                        val pathResourceType =
                            oldResourceTypeMappingNewResourceType[managerPath.type] ?: managerPath.type
                        val v0ResourceCode = managerPath.id
                        // 先将v3资源code转换成rbac资源code,可能存在为空的情况,ci已经删除但是iam没有删除,直接用iam数据填充
                        val rbacResourceCode = migrateResourceCodeConverter.getRbacResourceCode(
                            projectCode = projectCode,
                            resourceType = pathResourceType,
                            migrateResourceCode = v0ResourceCode
                        ) ?: v0ResourceCode
                        // 获取rbac资源code对应的iam资源code
                        val iamResourceCode = authResourceCodeConverter.code2IamCode(
                            projectCode = projectCode,
                            resourceType = pathResourceType,
                            resourceCode = rbacResourceCode
                        ) ?: rbacResourceCode
                        val rbacManagerPath = ManagerPath().apply {
                            system = iamConfiguration.systemId
                            id = iamResourceCode
                            name = managerPath.name
                            type = pathResourceType
                        }
                        rbacManagerPaths.add(rbacManagerPath)
                    }
                    rbacPaths.add(rbacManagerPaths)
                }
            }
            val resourceType = oldResourceTypeMappingNewResourceType[resource.type] ?: resource.type
            rbacManagerResources.add(
                ManagerResources.builder()
                    .system(iamConfiguration.systemId)
                    .type(resourceType)
                    .paths(rbacPaths)
                    .build()
            )
        }
        return rbacManagerResources
    }

    private fun buildProjectManagerResources(
        projectCode: String,
        projectName: String
    ): ManagerResources {
        val rbacPaths = mutableListOf<List<ManagerPath>>()
        val rbacManagerPaths = mutableListOf<ManagerPath>()
        val rbacProjectManagerPath = ManagerPath().apply {
            system = iamConfiguration.systemId
            id = projectCode
            name = projectName
            type = AuthResourceType.PROJECT.value
        }
        rbacManagerPaths.add(rbacProjectManagerPath)
        rbacPaths.add(rbacManagerPaths)
        return ManagerResources.builder()
            .system(iamConfiguration.systemId)
            .type(AuthResourceType.PROJECT.value)
            .paths(rbacPaths)
            .build()
    }

    override fun matchResourceGroup(
        userId: String,
        projectCode: String,
        projectName: String,
        gradeManagerId: Int,
        managerGroupId: Int,
        permission: AuthorizationScopes
    ): List<Int> {
        val resource = permission.resources[0]
        val resourceType = oldResourceTypeMappingNewResourceType[resource.type] ?: resource.type
        val userActions = permission.actions.map { it.id }
        logger.info("find match resource group|$userId|$projectCode|$resourceType|$userActions")
        val (resourceCreateActions, resourceActions) = buildRbacActions(
            actions = permission.actions.map { it.id }
        )
        val matchGroupIds = mutableListOf<Int>()
        // 创建action匹配到的组
        if (resourceCreateActions.isNotEmpty()) {
            v0MatchMinResourceGroup(
                userId = userId,
                projectCode = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                v0ResourceCode = projectCode,
                userActions = resourceCreateActions.map { it.id }
            )?.let { matchGroupIds.add(it) }
        }
        // 资源action匹配到的组
        if (resourceActions.isNotEmpty()) {
            val matchResourceGroupId = if (resource.paths.isEmpty() || resource.paths[0].isEmpty()) {
                matchOrCreateProjectResourceGroup(
                    userId = userId,
                    projectCode = projectCode,
                    projectName = projectName,
                    resourceType = resourceType,
                    actions = resourceActions.map { it.id },
                    gradeManagerId = gradeManagerId
                )
            } else {
                val v0ResourceCode = resource.paths[0][0].id
                v0MatchMinResourceGroup(
                    userId = userId,
                    projectCode = projectCode,
                    resourceType = resourceType,
                    v0ResourceCode = v0ResourceCode,
                    userActions = resourceActions.map { it.id }
                )
            }
            matchResourceGroupId?.let { matchGroupIds.add(matchResourceGroupId) }
        }
        // 项目下任意资源
        return matchGroupIds
    }

    private fun v0MatchMinResourceGroup(
        userId: String,
        projectCode: String,
        resourceType: String,
        v0ResourceCode: String,
        userActions: List<String>
    ): Int? {
        logger.info("match min resource group|$userId|$projectCode|$resourceType|$v0ResourceCode|$userActions")
        // 先将v0资源code转换成rbac资源code
        val rbacResourceCode = migrateResourceCodeConverter.getRbacResourceCode(
            projectCode = projectCode,
            resourceType = resourceType,
            migrateResourceCode = v0ResourceCode
        ) ?: return null

        return matchMinResourceGroup(
            userId = userId,
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = rbacResourceCode,
            actions = replaceOrRemoveAction(userActions)
        )
    }

    /**
     * action替换或移除
     *
     */
    private fun replaceOrRemoveAction(actions: List<String>): List<String> {
        val rbacActions = actions.toMutableList()
        actions.forEach action@{ action ->
            when {
                oldActionMappingNewAction.containsKey(action) -> {
                    rbacActions.remove(action)
                    rbacActions.add(oldActionMappingNewAction[action]!!)
                }
                skipActions.contains(action) -> {
                    logger.info("skip $action action")
                    rbacActions.remove(action)
                }
            }
        }
        return rbacActions
    }

    override fun batchAddGroupMember(groupId: Int, defaultGroup: Boolean, members: List<RoleGroupMemberInfo>?) {
        members?.forEach member@{ member ->
            val expiredDay = if (defaultGroup) {
                // 默认用户组,2年或3年随机过期
                V0_GROUP_EXPIRED_DAY[RandomUtils.nextInt(2, 4)]
            } else {
                // 自定义用户组,半年或者一年过期
                V0_GROUP_EXPIRED_DAY[RandomUtils.nextInt(0, 2)]
            }
            val expiredAt = System.currentTimeMillis() / MILLISECOND + TimeUnit.DAYS.toSeconds(expiredDay)
            val managerMember = ManagerMember(member.type, member.id)
            val managerMemberGroupDTO = ManagerMemberGroupDTO.builder()
                .members(listOf(managerMember))
                .expiredAt(expiredAt)
                .build()
            v2ManagerService.createRoleGroupMemberV2(groupId, managerMemberGroupDTO)
        }
    }

    override fun getGroupName(projectName: String, result: MigrateTaskDataResult): String {
        return if (result.subject.name == V0_QC_GROUP_NAME) {
            RBAC_QC_GROUP_NAME
        } else {
            result.subject.name!!
        }
    }
}
