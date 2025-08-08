/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.auth.provider.rbac.service.migrate

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.dto.V2PageInfoDTO
import com.tencent.bk.sdk.iam.dto.manager.Action
import com.tencent.bk.sdk.iam.dto.manager.AuthorizationScopes
import com.tencent.bk.sdk.iam.dto.manager.ManagerPath
import com.tencent.bk.sdk.iam.dto.manager.ManagerResources
import com.tencent.bk.sdk.iam.dto.manager.RoleGroupMemberInfo
import com.tencent.bk.sdk.iam.dto.manager.dto.SearchTemplatesDTO
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.dao.AuthMigrationDao
import com.tencent.devops.auth.dao.AuthResourceGroupConfigDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.pojo.enum.MemberType
import com.tencent.devops.auth.provider.rbac.pojo.migrate.MigrateTaskDataResult
import com.tencent.devops.auth.provider.rbac.service.AuthResourceCodeConverter
import com.tencent.devops.auth.provider.rbac.service.RbacCommonService
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupPermissionService
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
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
    private val rbacCommonService: RbacCommonService,
    private val authMigrationDao: AuthMigrationDao,
    private val deptService: DeptService,
    private val permissionResourceGroupPermissionService: PermissionResourceGroupPermissionService,
    private val permissionResourceMemberService: PermissionResourceMemberService
) : AbMigratePolicyService(
    v2ManagerService = v2ManagerService,
    iamConfiguration = iamConfiguration,
    dslContext = dslContext,
    authResourceGroupDao = authResourceGroupDao,
    authResourceGroupConfigDao = authResourceGroupConfigDao,
    migrateIamApiService = migrateIamApiService,
    authMigrationDao = authMigrationDao,
    permissionService = permissionService,
    rbacCommonService = rbacCommonService,
    deptService = deptService,
    permissionResourceGroupPermissionService = permissionResourceGroupPermissionService,
    permissionResourceMemberService = permissionResourceMemberService
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

        // v0有的资源，但是在rbac没有,需要跳过
        private val skipResourceTypes = listOf(
            "dev_image", "prod_image", "custom_dir", "gs-apk_task",
            "cluster_test", "cluster_prod", "namespace", "templates",
            "metric", "job_template", "script", "scan_task", "wetest_task",
            "email_group", "xinghai_all", "android", "ios", "macos"
        )

        private val oldResourceTypeMappingNewResourceType = mapOf(
            "quality_gate_group" to "quality_group"
        )
        private val certActions = listOf("cert_edit")
        private val envNodeActions = listOf("env_node_edit", "env_node_use", "env_node_delete")
        private const val V0_QC_GROUP_NAME = "质量管理员"
        private const val MAX_GROUP_MEMBER = 1000
        private const val CERT_VIEW = "cert_view"
        private const val ENV_NODE_VIEW = "env_node_view"
        private val PIPELINE_VIEWER_ACTION_GROUP = setOf(
            "pipeline_download", "pipeline_view",
            "pipeline_share", "pipeline_list"
        )
        private val PIPELINE_EXECUTOR_ACTION_GROUP = setOf(
            "pipeline_download", "pipeline_view",
            "pipeline_share", "pipeline_list", "pipeline_execute"
        )
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
    ): Pair<List<AuthorizationScopes>, List<String>> {
        val rbacAuthorizationScopes = mutableListOf<AuthorizationScopes>()
        val projectActions = mutableListOf<Action>()
        val groupIdListOfPipelineActionGroup = mutableListOf<String>()
        result.permissions.forEach permission@{ permission ->
            // 如果发现资源类型是跳过的,则跳过
            if (skipResourceTypes.contains(permission.resources[0].type)) {
                return@permission
            }
            val (resourceCreateActions, resourceActions) = buildRbacActions(
                actions = permission.actions.map { it.id }
            )
            if (resourceCreateActions.isNotEmpty()) {
                projectActions.addAll(resourceCreateActions)
            }
            if (resourceActions.isEmpty()) {
                return@permission
            }
            val resources = permission.resources

            if (resources.isNotEmpty() && resources[0].type == AuthResourceType.PIPELINE_DEFAULT.value &&
                resources[0].paths.isNotEmpty()) {
                val groupIdOfPipelineActionGroup = getGroupIdOfPipelineActionGroup(
                    projectCode = projectCode,
                    resourceCode = resources[0].paths[0][0].id,
                    resourceActions = resourceActions.map { it.id }.toSet()
                )
                if (groupIdOfPipelineActionGroup != null) {
                    groupIdListOfPipelineActionGroup.add(groupIdOfPipelineActionGroup)
                    return@permission
                }
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
        return Pair(rbacAuthorizationScopes, groupIdListOfPipelineActionGroup)
    }

    private fun getGroupIdOfPipelineActionGroup(
        projectCode: String,
        resourceCode: String,
        resourceActions: Set<String>
    ): String? {
        val pipelineGroupCode = when (resourceActions) {
            PIPELINE_EXECUTOR_ACTION_GROUP -> BkAuthGroup.EXECUTOR.value
            PIPELINE_VIEWER_ACTION_GROUP -> BkAuthGroup.VIEWER.value
            else -> null
        } ?: return null
        return authResourceGroupDao.get(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
            resourceCode = resourceCode,
            groupCode = pipelineGroupCode
        )?.relationId
    }

    /**
     *   在构造用户组授权范围的动作组时，由于rbac和v0版本版本动作不一致，需要做一些转换
     *   同时由于rbac增加了一些v0版本不做限制的动作，为了保持旧版的用户组权限不变，需要增加一些额外的动作
     * */
    private fun buildRbacActions(actions: List<String>): Pair<List<Action>, List<Action>> {
        val resourceCreateActions = mutableListOf<Action>()
        val resourceActions = mutableListOf<Action>()
        fillNewActions(actions).forEach { action ->
            // 创建的action,需要关联在项目下
            if (action.contains(AuthPermission.CREATE.value)) {
                resourceCreateActions.add(Action(action))
            } else {
                resourceActions.add(Action(action))
            }
        }
        return Pair(resourceCreateActions, resourceActions)
    }

    /**
     * action替换或移除
     */
    private fun fillNewActions(actions: List<String>): List<String> {
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
                certActions.contains(action) && !rbacActions.contains(CERT_VIEW) -> {
                    rbacActions.add(CERT_VIEW)
                }
                envNodeActions.contains(action) && !rbacActions.contains(ENV_NODE_VIEW) -> {
                    rbacActions.add(ENV_NODE_VIEW)
                }
            }
        }
        return rbacActions
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
        // 如果发现资源类型是跳过的,则跳过
        if (skipResourceTypes.contains(resourceType)) {
            return emptyList()
        }
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
            actions = fillNewActions(userActions)
        )
    }

    override fun batchAddGroupMember(
        projectCode: String,
        groupId: Int,
        defaultGroup: Boolean,
        members: List<RoleGroupMemberInfo>?,
        gradeManagerId: Int?,
        groupName: String?,
        groupIdOfPipelineActionGroupList: List<String>
    ) {
        // 1.往流水级别用户组添加人员模板
        if (groupIdOfPipelineActionGroupList.isNotEmpty()) {
            // 获取人员模板ID
            val subjectTemplateId = v2ManagerService.getGradeManagerRoleTemplate(
                gradeManagerId.toString(),
                SearchTemplatesDTO.builder().name(groupName).build(),
                V2PageInfoDTO().apply {
                    page = PageUtil.DEFAULT_PAGE
                    pageSize = PageUtil.DEFAULT_PAGE_SIZE
                }
            ).results.firstOrNull { it.name == groupName }?.id.toString()
            groupIdOfPipelineActionGroupList.forEach {
                logger.info("add subject template to group of pipeline:$it|$subjectTemplateId")
                addGroupMember(
                    projectCode = projectCode,
                    groupId = it.toInt(),
                    defaultGroup = true,
                    member = RoleGroupMemberInfo().apply {
                        type = MemberType.TEMPLATE.type
                        id = subjectTemplateId
                        name = subjectTemplateId
                        expiredAt = 0
                    }
                )
            }
        }

        // 2.往用户组添加用户
        if (members.isNullOrEmpty()) {
            return
        }

        if (members.size > MAX_GROUP_MEMBER) {
            logger.warn("group member size is too large, max size is $MAX_GROUP_MEMBER")
            return
        }
        members.forEach member@{ member ->
            addGroupMember(
                projectCode = projectCode,
                defaultGroup = defaultGroup,
                member = member,
                groupId = groupId
            )
        }
    }

    private fun addGroupMember(
        projectCode: String,
        defaultGroup: Boolean,
        member: RoleGroupMemberInfo,
        groupId: Int
    ) {
        val expiredDay = if (defaultGroup) {
            // 默认用户组,2年或3年随机过期
            V0_GROUP_EXPIRED_DAY[RandomUtils.nextInt(2, 4)]
        } else {
            // 自定义用户组,半年或者一年过期
            V0_GROUP_EXPIRED_DAY[RandomUtils.nextInt(0, 2)]
        }
        permissionResourceMemberService.addGroupMember(
            projectCode = projectCode,
            memberId = member.id,
            memberType = member.type,
            expiredAt = System.currentTimeMillis() / MILLISECOND + TimeUnit.DAYS.toSeconds(expiredDay) +
                TimeUnit.DAYS.toSeconds(RandomUtils.nextLong(0, 180)),
            iamGroupId = groupId
        )
    }

    override fun getGroupName(projectName: String, result: MigrateTaskDataResult): String {
        return if (result.subject.name == V0_QC_GROUP_NAME) {
            RBAC_QC_GROUP_NAME
        } else {
            result.subject.name!!
        }
    }
}
