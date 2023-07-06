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
import com.tencent.devops.auth.common.Constants
import com.tencent.devops.auth.dao.AuthMigrationDao
import com.tencent.devops.auth.dao.AuthResourceGroupConfigDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.pojo.migrate.MigrateTaskDataResult
import com.tencent.devops.auth.service.AuthResourceCodeConverter
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.PermissionGroupPoliciesService
import com.tencent.devops.auth.service.RbacCacheService
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.auth.api.AuthResourceType
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * v3权限策略迁移到rbac
 *
 * 权限策略包含：
 * 1. 蓝盾创建的默认用户组权限
 *     1.1. 包含all_action，直接加入管理员
 *     1.2. project_views_manager忽略权限
 * 2. 用户在权限中心创建的用户组权限
 * 3. 用户自定义权限
 */
@Suppress("LongParameterList", "NestedBlockDepth", "TooManyFunctions")
class MigrateV3PolicyService constructor(
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
        // 项目视图管理
        private const val PROJECT_VIEWS_MANAGER = "project_views_manager"
        // 项目查看权限
        private const val PROJECT_VIEW = "project_view"
        // 项目访问权限
        private const val PROJECT_VISIT = "project_visit"
        // v3项目禁用启用
        private const val PROJECT_DELETE = "project_delete"
        // rbac项目禁用启用
        private const val PROJECT_ENABLE = "project_enable"
        // v3质量红线启用,rbac没有
        private const val QUALITY_GROUP_ENABLE = "quality_group_enable"
        // 流水线查看权限,v3没有pipeline_list权限,迁移至rbac需要添加
        private const val PIPELINE_VIEW = "pipeline_view"
        // 项目访问权限
        private const val PIPELINE_LIST = "pipeline_list"

        // 过期用户增加5分钟
        private const val EXPIRED_MEMBER_ADD_TIME = 5L
        private val logger = LoggerFactory.getLogger(MigrateV3PolicyService::class.java)
    }

    /**
     * 启动迁移任务
     *
     * @param v3GradeManagerIds v3分级管理员ID
     */
    fun startMigrateTask(v3GradeManagerIds: List<String>) {
        migrateIamApiService.startV3MigrateTask(v3GradeManagerIds)
    }

    /**
     * 轮询获取迁移任务状态,如果状态没有成功,等待30s再重试
     */
    fun loopTaskStatus(projectCode: String) {
        logger.info("loop $projectCode migrate v3 task status")
        val migrateTaskStatus = migrateIamApiService.getV3MigrateTaskStatus()
        if (migrateTaskStatus != MigrateIamApiService.SUCCESSFUL_IAM_MIGRATE_TASK_SUCCESS) {
            logger.info(
                "$projectCode migrate task status $migrateTaskStatus not success, " +
                    "sleep ${MigrateIamApiService.SLEEP_LOOP_IAM_GET_MIGRATE_TASK}(s)"
            )
            Thread.sleep(MigrateIamApiService.SLEEP_LOOP_IAM_GET_MIGRATE_TASK)
            loopTaskStatus(projectCode)
        }
    }

    @Suppress("ReturnCount")
    override fun buildRbacAuthorizationScopeList(
        projectCode: String,
        projectName: String,
        managerGroupId: Int,
        result: MigrateTaskDataResult
    ): List<AuthorizationScopes> {
        val rbacAuthorizationScopes = mutableListOf<AuthorizationScopes>()
        result.permissions.forEach permission@{ permission ->
            val (isManager, rbacActions) = buildRbacActions(
                managerGroupId = managerGroupId,
                permission = permission,
                members = result.members
            )
            // 已经添加到管理员组,不需要再做处理
            if (isManager) {
                return emptyList()
            }
            // 操作为空,可能action在rbac已经不存在,直接跳过
            if (rbacActions.isEmpty()) {
                return@permission
            }
            val rbacResources = buildRbacManagerResources(projectCode = projectCode, permission = permission)
            rbacAuthorizationScopes.add(
                AuthorizationScopes.builder()
                    .system(iamConfiguration.systemId)
                    .actions(rbacActions)
                    .resources(rbacResources)
                    .build()
            )
        }
        return rbacAuthorizationScopes
    }

    private fun buildRbacActions(
        managerGroupId: Int,
        permission: AuthorizationScopes,
        members: List<RoleGroupMemberInfo>?
    ): Pair<Boolean /*是否已添加到管理员组*/, List<Action>> {
        val rbacActions = mutableListOf<Action>()
        permission.actions.forEach action@{ action ->
            when (action.id) {
                // 如果包含all_action,则直接添加到管理员组
                Constants.ALL_ACTION -> {
                    logger.info("match all_action,member add to manager group $managerGroupId")
                    batchAddGroupMember(groupId = managerGroupId, defaultGroup = true, members = members)
                    return Pair(true, emptyList())
                }
                PROJECT_VIEWS_MANAGER, PROJECT_DELETE, QUALITY_GROUP_ENABLE -> {
                    logger.info("skip ${action.id} action")
                }
                else -> {
                    rbacActions.add(action)
                }
            }
        }
        return Pair(false, rbacActions)
    }

    @Suppress("NestedBlockDepth", "ReturnCount")
    private fun buildRbacManagerResources(
        projectCode: String,
        permission: AuthorizationScopes
    ): List<ManagerResources> {
        val rbacManagerResources = mutableListOf<ManagerResources>()
        permission.resources.forEach resource@{ resource ->
            val rbacPaths = mutableListOf<List<ManagerPath>>()
            resource.paths.forEach paths@{ managerPaths ->
                val rbacManagerPaths = mutableListOf<ManagerPath>()
                managerPaths.forEach managerPath@{ managerPath ->
                    val v3ResourceCode = managerPath.id
                    if (v3ResourceCode == "*") {
                        return@managerPath
                    }
                    // 先将v3资源code转换成rbac资源code,可能存在为空的情况,ci已经删除但是iam没有删除,直接用iam数据填充
                    val rbacResourceCode = migrateResourceCodeConverter.getRbacResourceCode(
                        projectCode = projectCode,
                        resourceType = managerPath.type,
                        migrateResourceCode = v3ResourceCode
                    ) ?: v3ResourceCode
                    // 获取rbac资源code对应的iam资源code
                    val iamResourceCode = authResourceCodeConverter.code2IamCode(
                        projectCode = projectCode,
                        resourceType = managerPath.type,
                        resourceCode = rbacResourceCode
                    ) ?: rbacResourceCode
                    val rbacManagerPath = ManagerPath().apply {
                        system = iamConfiguration.systemId
                        id = iamResourceCode
                        name = managerPath.name
                        type = managerPath.type
                    }
                    rbacManagerPaths.add(rbacManagerPath)
                }
                rbacPaths.add(rbacManagerPaths)
            }
            rbacManagerResources.add(
                ManagerResources.builder()
                    .system(iamConfiguration.systemId)
                    .type(resource.type)
                    .paths(rbacPaths)
                    .build()
            )
        }
        return rbacManagerResources
    }

    override fun matchResourceGroup(
        userId: String,
        projectCode: String,
        projectName: String,
        gradeManagerId: Int,
        managerGroupId: Int,
        permission: AuthorizationScopes
    ): List<Int> {
        // v3资源都只有一层
        val resource = permission.resources[0]
        val resourceType = resource.type
        val userActions = permission.actions.map { it.id }
        logger.info("match resource group|$projectCode|$resourceType|$userActions")
        // 如果path为空,则直接跳过
        if (resource.paths.isEmpty()) {
            return emptyList()
        }
        val groupIds = mutableListOf<Int>()
        val matchGroupId = when {
            // 如果有all_action,直接加入管理员组
            userActions.contains(Constants.ALL_ACTION) -> managerGroupId
            // 项目类型
            resource.type == AuthResourceType.PROJECT.value ->
                v3MatchMinResourceGroup(
                    userId = userId,
                    projectCode = projectCode,
                    resourceType = AuthResourceType.PROJECT.value,
                    v3ResourceCode = projectCode,
                    userActions = permission.actions.map { it.id }
                )
            // 项目任意资源
            isAnyResource(resource) -> {
                val finalUserActions = replaceOrRemoveAction(userActions)
                matchOrCreateProjectResourceGroup(
                    userId = userId,
                    projectCode = projectCode,
                    projectName = projectName,
                    resourceType = resourceType,
                    actions = finalUserActions,
                    gradeManagerId = gradeManagerId
                )
            }
            // 具体资源权限
            resource.paths[0].size >= 2 -> {
                v3MatchMinResourceGroup(
                    userId = userId,
                    projectCode = projectCode,
                    resourceType = resourceType,
                    v3ResourceCode = resource.paths[0][1].id,
                    userActions = permission.actions.map { it.id }
                )
            }
            else -> null
        }
        matchGroupId?.let { groupIds.add(it) }
        return groupIds
    }

    /**
     * 有项目下任意资源权限
     */
    private fun isAnyResource(
        resource: ManagerResources
    ): Boolean {
        return resource.paths[0].size >= 2 && resource.paths[0][1].id == "*" ||
            (resource.paths[0].size == 1 && resource.paths[0][0].type == AuthResourceType.PROJECT.value)
    }

    private fun v3MatchMinResourceGroup(
        userId: String,
        projectCode: String,
        resourceType: String,
        v3ResourceCode: String,
        userActions: List<String>
    ): Int? {
        logger.info("match min resource group|$userId|$projectCode|$resourceType|$v3ResourceCode|$userActions")
        // 先将v3资源code转换成rbac资源code
        val rbacResourceCode = migrateResourceCodeConverter.getRbacResourceCode(
            projectCode = projectCode,
            resourceType = resourceType,
            migrateResourceCode = v3ResourceCode
        ) ?: return null
        val finalUserActions = replaceOrRemoveAction(userActions)

        return matchMinResourceGroup(
            userId = userId,
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = rbacResourceCode,
            actions = finalUserActions
        )
    }

    /**
     * action替换或移除
     *
     */
    private fun replaceOrRemoveAction(actions: List<String>): List<String> {
        val finalUserActions = actions.toMutableList()
        // project_view需要替换成project_visit
        if (finalUserActions.contains(PROJECT_VIEW)) {
            finalUserActions.remove(PROJECT_VIEW)
            finalUserActions.add(PROJECT_VISIT)
        }
        if (finalUserActions.contains(PROJECT_VIEWS_MANAGER)) {
            finalUserActions.remove(PROJECT_VIEWS_MANAGER)
        }
        // project_delete需替换成project_enable
        if (finalUserActions.contains(PROJECT_DELETE)) {
            finalUserActions.remove(PROJECT_DELETE)
            finalUserActions.add(PROJECT_ENABLE)
        }
        if (finalUserActions.contains(QUALITY_GROUP_ENABLE)) {
            finalUserActions.remove(QUALITY_GROUP_ENABLE)
        }
        // v3没有pipeline_list权限,但是rbac有这个权限,迁移时需要补充
        if (finalUserActions.contains(PIPELINE_VIEW)) {
            finalUserActions.add(PIPELINE_LIST)
        }
        return finalUserActions
    }

    override fun batchAddGroupMember(groupId: Int, defaultGroup: Boolean, members: List<RoleGroupMemberInfo>?) {
        members?.forEach member@{ member ->
            // 已过期用户,迁移时无法添加到用户组成员,增加5分钟添加到iam就过期，方便用户续期
            val expiredAt = if (member.expiredAt * MILLISECOND < System.currentTimeMillis()) {
                System.currentTimeMillis() / MILLISECOND + TimeUnit.MINUTES.toSeconds(EXPIRED_MEMBER_ADD_TIME)
            } else {
                member.expiredAt
            }
            val managerMember = ManagerMember(member.type, member.id)
            val managerMemberGroupDTO = ManagerMemberGroupDTO.builder()
                .members(listOf(managerMember))
                .expiredAt(expiredAt)
                .build()
            v2ManagerService.createRoleGroupMemberV2(groupId, managerMemberGroupDTO)
        }
    }

    override fun getGroupName(projectName: String, result: MigrateTaskDataResult): String {
        return result.subject.name!!.substringAfter("$projectName-")
    }
}
