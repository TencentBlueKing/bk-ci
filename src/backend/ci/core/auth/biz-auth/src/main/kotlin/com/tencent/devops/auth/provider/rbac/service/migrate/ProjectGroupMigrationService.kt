package com.tencent.devops.auth.provider.rbac.service.migrate

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.dto.manager.Action
import com.tencent.bk.sdk.iam.dto.manager.AuthorizationScopes
import com.tencent.bk.sdk.iam.dto.manager.ManagerPath
import com.tencent.bk.sdk.iam.dto.manager.ManagerResources
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.dao.AuthResourceGroupMemberDao
import com.tencent.devops.auth.dao.AuthResourceGroupPermissionDao
import com.tencent.devops.auth.pojo.AuthResourceGroup
import com.tencent.devops.auth.pojo.AuthResourceGroupMember
import com.tencent.devops.auth.pojo.AuthResourceInfo
import com.tencent.devops.auth.pojo.dto.GroupAddDTO
import com.tencent.devops.auth.pojo.dto.ProjectGroupIncrementalMigrationDTO
import com.tencent.devops.auth.pojo.dto.ProjectGroupMigrationDTO
import com.tencent.devops.auth.pojo.dto.ProjectGroupMigrationDetailDTO
import com.tencent.devops.auth.pojo.dto.ProjectGroupMigrationGroupResultDTO
import com.tencent.devops.auth.pojo.dto.ProjectGroupMigrationResultDTO
import com.tencent.devops.auth.pojo.dto.ResourceGroupPermissionDTO
import com.tencent.devops.auth.pojo.enum.MemberType
import com.tencent.devops.auth.pojo.enum.ProjectGroupMigrationStatus
import com.tencent.devops.auth.provider.rbac.service.AuthResourceCodeConverter
import com.tencent.devops.auth.provider.rbac.service.AuthResourceService
import com.tencent.devops.auth.provider.rbac.service.RbacCommonService
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupPermissionService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupService
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

@Suppress("LongParameterList")
class ProjectGroupMigrationService(
    private val dslContext: DSLContext,
    private val iamConfiguration: IamConfiguration,
    private val authResourceService: AuthResourceService,
    private val rbacCommonService: RbacCommonService,
    private val authResourceCodeConverter: AuthResourceCodeConverter,
    private val authResourceGroupDao: AuthResourceGroupDao,
    private val authResourceGroupMemberDao: AuthResourceGroupMemberDao,
    private val authResourceGroupPermissionDao: AuthResourceGroupPermissionDao,
    private val permissionResourceGroupService: PermissionResourceGroupService,
    private val permissionResourceGroupPermissionService: PermissionResourceGroupPermissionService,
    private val permissionResourceMemberService: PermissionResourceMemberService,
    private val deptService: DeptService
) {

    /**
     * 迁移源项目的项目级用户组到目标项目。
     *
     * 这条链路的核心目标不是“套用目标项目当前模板”，而是：
     * - 以源项目数据库中的组/权限/成员记录为准做快照
     * - 在目标项目重建空组
     * - 再通过 IAM 逐步回放权限和成员
     *
     * 这样做有两个原因：
     * 1. 像 `qc` 这类非 custom 组，如果先按目标模板建组，再叠加源权限，最终权限会变宽。
     * 2. 权限真实生效点仍然在 IAM，不能只靠本地权限表“复制数据”了事。
     */
    fun migrate(migrationDTO: ProjectGroupMigrationDTO): ProjectGroupMigrationResultDTO {
        if (migrationDTO.sourceProjectCode == migrationDTO.targetProjectCode) {
            return buildFailedResult(
                migrationDTO = migrationDTO,
                errors = listOf("sourceProjectCode and targetProjectCode cannot be the same")
            )
        }
        runCatching {
            authResourceService.get(
                projectCode = migrationDTO.sourceProjectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = migrationDTO.sourceProjectCode
            )
        }.getOrElse {
            logger.warn("get source project auth resource failed", it)
            return buildFailedResult(
                migrationDTO = migrationDTO,
                errors = listOf("source project auth resource not found: ${migrationDTO.sourceProjectCode}")
            )
        }
        val targetProject = runCatching {
            authResourceService.get(
                projectCode = migrationDTO.targetProjectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = migrationDTO.targetProjectCode
            )
        }.getOrElse {
            logger.warn("get target project auth resource failed", it)
            return buildFailedResult(
                migrationDTO = migrationDTO,
                errors = listOf("target project auth resource not found: ${migrationDTO.targetProjectCode}")
            )
        }
        val sourceGroups = listProjectGroups(migrationDTO.sourceProjectCode)
        val targetGroups = listProjectGroups(migrationDTO.targetProjectCode)
        // 目标项目管理员组是唯一受保护对象：只保留、不删除、不覆盖。
        val protectedTargetGroups = targetGroups.filter(::isProtectedManagerGroup).map { it.groupName }
        // deleteGroup() 会拒绝删除 defaultGroup=true 的组，这里先把非管理员但不可删除的组拦下来，
        // 避免迁移执行到一半才因为目标项目清理失败而留下部分变更。
        val blockedTargetGroups = targetGroups.filter { !isProtectedManagerGroup(it) && it.defaultGroup }
            .map { it.groupName }
        if (blockedTargetGroups.isNotEmpty()) {
            return ProjectGroupMigrationResultDTO(
                sourceProjectCode = migrationDTO.sourceProjectCode,
                targetProjectCode = migrationDTO.targetProjectCode,
                dryRun = migrationDTO.dryRun,
                status = ProjectGroupMigrationStatus.FAILED,
                protectedTargetGroups = protectedTargetGroups,
                blockedTargetGroups = blockedTargetGroups,
                errors = listOf("target project contains undeletable non-manager groups")
            )
        }
        val groupsToDelete = targetGroups.filterNot(::isProtectedManagerGroup)
        // 源项目管理员组也不参与迁移，避免把 A 项目的管理员模型覆盖到 B。
        val sourceSnapshots = sourceGroups.filterNot(::isProtectedManagerGroup).map { group ->
            snapshotGroup(group = group, includeMembers = migrationDTO.includeMembers)
        }

        val sourceResourceMap = buildSourceResourceMap(
            projectCode = migrationDTO.sourceProjectCode,
            snapshots = sourceSnapshots
        )
        val targetResourceIndex = buildTargetResourceIndex(
            projectCode = migrationDTO.targetProjectCode,
            snapshots = sourceSnapshots
        )
        if (migrationDTO.dryRun) {
            // dry-run 只预演“会删什么、会建什么、哪些权限能映射成功”，
            // 不对目标项目做任何写操作，方便在正式执行前先看清风险点。
            return buildDryRunResult(
                migrationDTO = migrationDTO,
                protectedTargetGroups = protectedTargetGroups,
                groupsToDelete = groupsToDelete,
                sourceSnapshots = sourceSnapshots,
                sourceResourceMap = sourceResourceMap,
                targetResourceIndex = targetResourceIndex
            )
        }
        val deletedTargetGroups = mutableListOf<String>()
        groupsToDelete.forEach { targetGroup ->
            runCatching {
                // 正式迁移前先清空目标项目的非管理员项目组，避免源/目标同名组并存，
                // 也避免旧权限残留和本次重建结果叠加。
                permissionResourceGroupService.deleteGroup(
                    userId = null,
                    projectId = migrationDTO.targetProjectCode,
                    resourceType = AuthResourceType.PROJECT.value,
                    groupId = targetGroup.relationId
                )
                deletedTargetGroups.add(targetGroup.groupName)
            }.getOrElse {
                logger.warn("delete target group failed", it)
                return ProjectGroupMigrationResultDTO(
                    sourceProjectCode = migrationDTO.sourceProjectCode,
                    targetProjectCode = migrationDTO.targetProjectCode,
                    dryRun = false,
                    status = ProjectGroupMigrationStatus.FAILED,
                    protectedTargetGroups = protectedTargetGroups,
                    deletedTargetGroups = deletedTargetGroups,
                    errors = listOf("delete target group failed: ${targetGroup.groupName}")
                )
            }
        }
        val groupResults = sourceSnapshots.map { snapshot ->
            // 每个组单独编排，失败信息按组聚合，方便后续补偿或人工核对。
            executeGroupMigration(
                snapshot = snapshot,
                targetProject = targetProject,
                sourceResourceMap = sourceResourceMap,
                targetResourceIndex = targetResourceIndex
            )
        }
        val status = calculateOverallStatus(
            groupResults = groupResults,
            topLevelErrors = emptyList()
        )
        return ProjectGroupMigrationResultDTO(
            sourceProjectCode = migrationDTO.sourceProjectCode,
            targetProjectCode = migrationDTO.targetProjectCode,
            dryRun = false,
            status = status,
            protectedTargetGroups = protectedTargetGroups,
            deletedTargetGroups = deletedTargetGroups,
            groupResults = groupResults
        )
    }

    /**
     * 增量补齐模式不删除目标项目现有组，也不处理成员。
     * 它只负责在目标项目中补齐缺失的项目级/单资源权限，
     * 以便在资源分批迁移完成后可以重复执行，逐步收敛权限。
     */
    fun migrateIncremental(migrationDTO: ProjectGroupIncrementalMigrationDTO): ProjectGroupMigrationResultDTO {
        if (migrationDTO.sourceProjectCode == migrationDTO.targetProjectCode) {
            return buildFailedResult(
                sourceProjectCode = migrationDTO.sourceProjectCode,
                targetProjectCode = migrationDTO.targetProjectCode,
                dryRun = migrationDTO.dryRun,
                errors = listOf("sourceProjectCode and targetProjectCode cannot be the same")
            )
        }
        runCatching {
            authResourceService.get(
                projectCode = migrationDTO.sourceProjectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = migrationDTO.sourceProjectCode
            )
        }.getOrElse {
            logger.warn("get source project auth resource failed", it)
            return buildFailedResult(
                sourceProjectCode = migrationDTO.sourceProjectCode,
                targetProjectCode = migrationDTO.targetProjectCode,
                dryRun = migrationDTO.dryRun,
                errors = listOf("source project auth resource not found: ${migrationDTO.sourceProjectCode}")
            )
        }
        val targetProject = runCatching {
            authResourceService.get(
                projectCode = migrationDTO.targetProjectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = migrationDTO.targetProjectCode
            )
        }.getOrElse {
            logger.warn("get target project auth resource failed", it)
            return buildFailedResult(
                sourceProjectCode = migrationDTO.sourceProjectCode,
                targetProjectCode = migrationDTO.targetProjectCode,
                dryRun = migrationDTO.dryRun,
                errors = listOf("target project auth resource not found: ${migrationDTO.targetProjectCode}")
            )
        }
        val sourceSnapshots = listProjectGroups(migrationDTO.sourceProjectCode)
            .filterNot(::isProtectedManagerGroup)
            // 增量接口不处理成员，因此这里显式关闭成员快照，避免多余查询和误导性的结果统计。
            .map { group -> snapshotGroup(group = group, includeMembers = false) }
        val targetGroups = listProjectGroups(migrationDTO.targetProjectCode)
        val protectedTargetGroups = targetGroups.filter(::isProtectedManagerGroup).map { it.groupName }
        val sourceResourceMap = buildSourceResourceMap(
            projectCode = migrationDTO.sourceProjectCode,
            snapshots = sourceSnapshots
        )
        val targetResourceIndex = buildTargetResourceIndex(
            projectCode = migrationDTO.targetProjectCode,
            snapshots = sourceSnapshots
        )
        if (migrationDTO.dryRun) {
            // 增量 dry-run 的重点是告诉调用方：
            // 1. 目标组是否已存在
            // 2. 哪些项目级 action 仍然缺失
            // 3. 哪些单资源权限当前可补、哪些仍会 skipped
            return buildIncrementalDryRunResult(
                migrationDTO = migrationDTO,
                protectedTargetGroups = protectedTargetGroups,
                targetGroups = targetGroups,
                sourceSnapshots = sourceSnapshots,
                sourceResourceMap = sourceResourceMap,
                targetResourceIndex = targetResourceIndex
            )
        }
        val groupResults = sourceSnapshots.map { snapshot ->
            executeIncrementalGroupMigration(
                snapshot = snapshot,
                targetProject = targetProject,
                existingTargetGroup = findExistingTargetGroup(
                    snapshot = snapshot,
                    targetGroups = targetGroups
                ),
                sourceResourceMap = sourceResourceMap,
                targetResourceIndex = targetResourceIndex
            )
        }
        return ProjectGroupMigrationResultDTO(
            sourceProjectCode = migrationDTO.sourceProjectCode,
            targetProjectCode = migrationDTO.targetProjectCode,
            dryRun = false,
            status = calculateOverallStatus(groupResults = groupResults, topLevelErrors = emptyList()),
            protectedTargetGroups = protectedTargetGroups,
            groupResults = groupResults
        )
    }

    private fun buildDryRunResult(
        migrationDTO: ProjectGroupMigrationDTO,
        protectedTargetGroups: List<String>,
        groupsToDelete: List<AuthResourceGroup>,
        sourceSnapshots: List<GroupSnapshot>,
        sourceResourceMap: Map<ResourceKey, AuthResourceInfo>,
        targetResourceIndex: Map<String, Map<String, List<AuthResourceInfo>>>
    ): ProjectGroupMigrationResultDTO {
        // dry-run 的重点是把真实执行时的映射结果原样暴露出来：
        // 哪些组会被删、哪些组会被重建、哪些单资源权限会 success / skipped。
        val groupResults = sourceSnapshots.map { snapshot ->
            val mapping = buildSingleResourceScopePlan(
                snapshot = snapshot,
                targetProjectCode = migrationDTO.targetProjectCode,
                sourceResourceMap = sourceResourceMap,
                targetResourceIndex = targetResourceIndex
            )
            // dry-run 阶段即使存在 skipped 也维持 DRY_RUN，
            // 具体风险通过 details / errors 让调用方自己判断是否继续执行。
            val status = if (mapping.errors.isEmpty()) {
                ProjectGroupMigrationStatus.DRY_RUN
            } else {
                ProjectGroupMigrationStatus.FAILED
            }
            ProjectGroupMigrationGroupResultDTO(
                sourceGroupId = snapshot.group.relationId,
                sourceGroupName = snapshot.group.groupName,
                sourceGroupCode = snapshot.group.groupCode,
                targetGroupName = snapshot.group.groupName,
                status = status,
                projectActions = snapshot.projectActions,
                singleResourcePermissionCount = snapshot.singleResourcePermissions.size,
                migratedMemberCount = snapshot.members.size,
                details = mapping.details,
                errors = mapping.errors
            )
        }
        val errors = groupResults.flatMap { it.errors }
        return ProjectGroupMigrationResultDTO(
            sourceProjectCode = migrationDTO.sourceProjectCode,
            targetProjectCode = migrationDTO.targetProjectCode,
            dryRun = true,
            status = ProjectGroupMigrationStatus.DRY_RUN,
            protectedTargetGroups = protectedTargetGroups,
            deletedTargetGroups = groupsToDelete.map { it.groupName },
            groupResults = groupResults,
            errors = errors
        )
    }

    private fun buildIncrementalDryRunResult(
        migrationDTO: ProjectGroupIncrementalMigrationDTO,
        protectedTargetGroups: List<String>,
        targetGroups: List<AuthResourceGroup>,
        sourceSnapshots: List<GroupSnapshot>,
        sourceResourceMap: Map<ResourceKey, AuthResourceInfo>,
        targetResourceIndex: Map<String, Map<String, List<AuthResourceInfo>>>
    ): ProjectGroupMigrationResultDTO {
        // 这里不再像全量接口那样关心“会删除哪些目标组”，
        // 而是聚焦“当前目标组已经补到了什么，还缺什么”。
        val groupResults = sourceSnapshots.map { snapshot ->
            val existingTargetGroup = findExistingTargetGroup(
                snapshot = snapshot,
                targetGroups = targetGroups
            )
            val targetPermissionSnapshot = existingTargetGroup?.let {
                buildTargetPermissionSnapshot(
                    projectCode = migrationDTO.targetProjectCode,
                    iamGroupId = it.relationId
                )
            } ?: TargetGroupPermissionSnapshot()
            val missingProjectActions = snapshot.projectActions.filterNot {
                targetPermissionSnapshot.projectActions.contains(it)
            }
            val plan = buildIncrementalSingleResourceScopePlan(
                snapshot = snapshot,
                targetProjectCode = migrationDTO.targetProjectCode,
                sourceResourceMap = sourceResourceMap,
                targetResourceIndex = targetResourceIndex,
                existingPermissionKeys = targetPermissionSnapshot.singleResourcePermissions
            )
            ProjectGroupMigrationGroupResultDTO(
                sourceGroupId = snapshot.group.relationId,
                sourceGroupName = snapshot.group.groupName,
                sourceGroupCode = snapshot.group.groupCode,
                targetGroupId = existingTargetGroup?.relationId,
                targetGroupName = existingTargetGroup?.groupName ?: snapshot.group.groupName,
                status = ProjectGroupMigrationStatus.DRY_RUN,
                projectActions = missingProjectActions,
                singleResourcePermissionCount = plan.grantablePermissionCount,
                details = plan.details,
                errors = plan.errors
            )
        }
        return ProjectGroupMigrationResultDTO(
            sourceProjectCode = migrationDTO.sourceProjectCode,
            targetProjectCode = migrationDTO.targetProjectCode,
            dryRun = true,
            status = ProjectGroupMigrationStatus.DRY_RUN,
            protectedTargetGroups = protectedTargetGroups,
            groupResults = groupResults,
            errors = groupResults.flatMap { it.errors }
        )
    }

    private fun executeGroupMigration(
        snapshot: GroupSnapshot,
        targetProject: AuthResourceInfo,
        sourceResourceMap: Map<ResourceKey, AuthResourceInfo>,
        targetResourceIndex: Map<String, Map<String, List<AuthResourceInfo>>>
    ): ProjectGroupMigrationGroupResultDTO {
        // 一个组内的执行顺序固定为：建组 -> 项目级权限 -> 单资源权限 -> 成员。
        // 顺序不能倒，因为后面的 IAM 授权和加成员都依赖新建出来的目标组 ID。
        val details = mutableListOf<ProjectGroupMigrationDetailDTO>()
        val errors = mutableListOf<String>()
        val createdGroup = runCatching {
            createTargetGroup(
                targetProjectCode = targetProject.projectCode,
                sourceGroup = snapshot.group
            )
        }.getOrElse {
            logger.warn("create target group failed", it)
            errors.add("create target group failed: ${snapshot.group.groupName}")
            return ProjectGroupMigrationGroupResultDTO(
                sourceGroupId = snapshot.group.relationId,
                sourceGroupName = snapshot.group.groupName,
                sourceGroupCode = snapshot.group.groupCode,
                status = ProjectGroupMigrationStatus.FAILED,
                projectActions = snapshot.projectActions,
                singleResourcePermissionCount = snapshot.singleResourcePermissions.size,
                errors = errors
            )
        }
        if (snapshot.projectActions.isNotEmpty()) {
            runCatching {
                // 项目级动作直接按源库快照回放到目标项目，不依赖目标项目的默认组模板。
                val authorizationScopes = permissionResourceGroupPermissionService.buildProjectPermissions(
                    projectCode = targetProject.projectCode,
                    projectName = targetProject.resourceName,
                    actions = snapshot.projectActions
                )
                permissionResourceGroupPermissionService.grantGroupPermission(
                    authorizationScopesStr = authorizationScopes,
                    projectCode = targetProject.projectCode,
                    projectName = targetProject.resourceName,
                    resourceType = AuthResourceType.PROJECT.value,
                    groupCode = createdGroup.groupCode,
                    iamResourceCode = targetProject.resourceCode,
                    resourceName = targetProject.resourceName,
                    iamGroupId = createdGroup.relationId,
                    registerMonitorPermission = false
                )
            }.getOrElse {
                logger.warn("grant project scope failed", it)
                errors.add("grant project scopes failed: ${snapshot.group.groupName}")
            }
        }
        val singleResourcePlan = buildSingleResourceScopePlan(
            snapshot = snapshot,
            targetProjectCode = targetProject.projectCode,
            sourceResourceMap = sourceResourceMap,
            targetResourceIndex = targetResourceIndex
        )
        // 单资源权限允许局部 skipped，但真正能重建的 scope 仍然要继续授权，
        // 这样可以最大限度保留源项目权限结构，同时把无法迁移的明细暴露给调用方。
        details.addAll(singleResourcePlan.details)
        errors.addAll(singleResourcePlan.errors)
        if (singleResourcePlan.authorizationScopes.isNotEmpty()) {
            runCatching {
                permissionResourceGroupPermissionService.grantGroupPermission(
                    authorizationScopesStr = JsonUtil.toJson(singleResourcePlan.authorizationScopes),
                    projectCode = targetProject.projectCode,
                    projectName = targetProject.resourceName,
                    resourceType = AuthResourceType.PROJECT.value,
                    groupCode = createdGroup.groupCode,
                    iamResourceCode = targetProject.resourceCode,
                    resourceName = targetProject.resourceName,
                    iamGroupId = createdGroup.relationId,
                    registerMonitorPermission = false
                )
            }.getOrElse {
                logger.warn("grant single resource scopes failed", it)
                errors.add("grant single resource scopes failed: ${snapshot.group.groupName}")
            }
        }
        val memberMigration = if (snapshot.members.isEmpty()) {
            MemberMigrationResult()
        } else {
            migrateMembers(
                targetProjectCode = targetProject.projectCode,
                targetGroupId = createdGroup.relationId,
                members = snapshot.members
            )
        }
        details.addAll(memberMigration.details)
        errors.addAll(memberMigration.errors)
        return ProjectGroupMigrationGroupResultDTO(
            sourceGroupId = snapshot.group.relationId,
            sourceGroupName = snapshot.group.groupName,
            sourceGroupCode = snapshot.group.groupCode,
            targetGroupId = createdGroup.relationId,
            targetGroupName = createdGroup.groupName,
            status = calculateGroupStatus(errors = errors, details = details),
            projectActions = snapshot.projectActions,
            singleResourcePermissionCount = snapshot.singleResourcePermissions.size,
            migratedMemberCount = memberMigration.migratedCount,
            skippedMemberCount = memberMigration.skippedCount,
            details = details,
            errors = errors
        )
    }

    private fun executeIncrementalGroupMigration(
        snapshot: GroupSnapshot,
        targetProject: AuthResourceInfo,
        existingTargetGroup: AuthResourceGroup?,
        sourceResourceMap: Map<ResourceKey, AuthResourceInfo>,
        targetResourceIndex: Map<String, Map<String, List<AuthResourceInfo>>>
    ): ProjectGroupMigrationGroupResultDTO {
        // 增量模式遵循“组先存在、权限再补齐”的思路：
        // - 目标已有同 groupCode 的组则直接复用
        // - 没有则补建空组，但仍不灌默认模板权限
        val details = mutableListOf<ProjectGroupMigrationDetailDTO>()
        val errors = mutableListOf<String>()
        val targetGroup = runCatching {
            existingTargetGroup ?: createTargetGroup(
                targetProjectCode = targetProject.projectCode,
                sourceGroup = snapshot.group
            )
        }.getOrElse {
            logger.warn("create incremental target group failed", it)
            errors.add("create target group failed: ${snapshot.group.groupName}")
            return ProjectGroupMigrationGroupResultDTO(
                sourceGroupId = snapshot.group.relationId,
                sourceGroupName = snapshot.group.groupName,
                sourceGroupCode = snapshot.group.groupCode,
                status = ProjectGroupMigrationStatus.FAILED,
                projectActions = snapshot.projectActions,
                singleResourcePermissionCount = snapshot.singleResourcePermissions.size,
                errors = errors
            )
        }
        // 先从目标项目当前库表权限快照中算出差量，再决定这次要不要发起 IAM 授权，
        // 避免每次增量执行都重复 grant 已经存在的项目级/单资源权限。
        val targetPermissionSnapshot = buildTargetPermissionSnapshot(
            projectCode = targetProject.projectCode,
            iamGroupId = targetGroup.relationId
        )
        val missingProjectActions = snapshot.projectActions.filterNot {
            targetPermissionSnapshot.projectActions.contains(it)
        }
        if (missingProjectActions.isNotEmpty()) {
            runCatching {
                val authorizationScopes = permissionResourceGroupPermissionService.buildProjectPermissions(
                    projectCode = targetProject.projectCode,
                    projectName = targetProject.resourceName,
                    actions = missingProjectActions
                )
                permissionResourceGroupPermissionService.grantGroupPermission(
                    authorizationScopesStr = authorizationScopes,
                    projectCode = targetProject.projectCode,
                    projectName = targetProject.resourceName,
                    resourceType = AuthResourceType.PROJECT.value,
                    groupCode = targetGroup.groupCode,
                    iamResourceCode = targetProject.resourceCode,
                    resourceName = targetProject.resourceName,
                    iamGroupId = targetGroup.relationId,
                    registerMonitorPermission = false
                )
            }.getOrElse {
                logger.warn("grant incremental project scope failed", it)
                errors.add("grant project scopes failed: ${snapshot.group.groupName}")
            }
        }
        val singleResourcePlan = buildIncrementalSingleResourceScopePlan(
            snapshot = snapshot,
            targetProjectCode = targetProject.projectCode,
            sourceResourceMap = sourceResourceMap,
            targetResourceIndex = targetResourceIndex,
            existingPermissionKeys = targetPermissionSnapshot.singleResourcePermissions
        )
        details.addAll(singleResourcePlan.details)
        errors.addAll(singleResourcePlan.errors)
        if (singleResourcePlan.authorizationScopes.isNotEmpty()) {
            runCatching {
                permissionResourceGroupPermissionService.grantGroupPermission(
                    authorizationScopesStr = JsonUtil.toJson(singleResourcePlan.authorizationScopes),
                    projectCode = targetProject.projectCode,
                    projectName = targetProject.resourceName,
                    resourceType = AuthResourceType.PROJECT.value,
                    groupCode = targetGroup.groupCode,
                    iamResourceCode = targetProject.resourceCode,
                    resourceName = targetProject.resourceName,
                    iamGroupId = targetGroup.relationId,
                    registerMonitorPermission = false
                )
            }.getOrElse {
                logger.warn("grant incremental single resource scopes failed", it)
                errors.add("grant single resource scopes failed: ${snapshot.group.groupName}")
            }
        }
        return ProjectGroupMigrationGroupResultDTO(
            sourceGroupId = snapshot.group.relationId,
            sourceGroupName = snapshot.group.groupName,
            sourceGroupCode = snapshot.group.groupCode,
            targetGroupId = targetGroup.relationId,
            targetGroupName = targetGroup.groupName,
            status = calculateGroupStatus(errors = errors, details = details),
            projectActions = missingProjectActions,
            singleResourcePermissionCount = singleResourcePlan.grantablePermissionCount,
            details = details,
            errors = errors
        )
    }

    private fun buildSingleResourceScopePlan(
        snapshot: GroupSnapshot,
        targetProjectCode: String,
        sourceResourceMap: Map<ResourceKey, AuthResourceInfo>,
        targetResourceIndex: Map<String, Map<String, List<AuthResourceInfo>>>
    ): ScopePlanResult {
        if (snapshot.singleResourcePermissions.isEmpty()) {
            return ScopePlanResult()
        }
        val details = mutableListOf<ProjectGroupMigrationDetailDTO>()
        val errors = mutableListOf<String>()
        val successfulMappings = mutableListOf<ResolvedPermission>()
        snapshot.singleResourcePermissions.forEach { permission ->
            // T_AUTH_RESOURCE_GROUP_PERMISSION 里保留的是源项目侧 relatedResourceCode，
            // 由于 A/B 项目资源迁移后 ID 可能变化，这里必须先查出源资源名称，再去 B 项目按名称重定位。
            val sourceKey = ResourceKey(
                resourceType = permission.relatedResourceType,
                resourceCode = permission.relatedResourceCode
            )
            val sourceResource = sourceResourceMap[sourceKey]
            if (sourceResource == null) {
                // 以库表数据为准，但如果源资源元数据已缺失，只能保留痕迹并跳过该条权限。
                details.add(
                    ProjectGroupMigrationDetailDTO(
                        status = ProjectGroupMigrationStatus.SKIPPED,
                        action = permission.action,
                        resourceType = permission.relatedResourceType,
                        sourceResourceCode = permission.relatedResourceCode,
                        reason = "source resource not found, skipped"
                    )
                )
                return@forEach
            }
            val candidates = targetResourceIndex[permission.relatedResourceType]
                ?.get(sourceResource.resourceName)
                ?: emptyList()
            if (candidates.size != 1) {
                // 目标项目可能没迁过来，或者同类型同名资源异常重复；两种情况都不能安全授权。
                details.add(
                    ProjectGroupMigrationDetailDTO(
                        status = ProjectGroupMigrationStatus.SKIPPED,
                        action = permission.action,
                        resourceType = permission.relatedResourceType,
                        sourceResourceCode = permission.relatedResourceCode,
                        sourceResourceName = sourceResource.resourceName,
                        reason = when {
                            candidates.isEmpty() -> "target resource not found by name, skipped"
                            else -> "target resource matched multiple candidates by name, skipped"
                        }
                    )
                )
                return@forEach
            }
            val targetResource = candidates.single()
            // 源项目和目标项目的资源 ID 可能不同，所以这里只复用“资源类型 + 资源名称”定位到的目标资源，
            // 后续构造 IAM scope 时全部使用目标项目自己的 resourceCode / iamResourceCode。
            successfulMappings.add(
                ResolvedPermission(
                    action = permission.action,
                    actionRelatedResourceType = resolveActionRelatedResourceType(permission),
                    relatedResourceType = permission.relatedResourceType,
                    targetResource = targetResource
                )
            )
            details.add(
                ProjectGroupMigrationDetailDTO(
                    status = ProjectGroupMigrationStatus.SUCCESS,
                    action = permission.action,
                    resourceType = permission.relatedResourceType,
                    sourceResourceCode = permission.relatedResourceCode,
                    sourceResourceName = sourceResource.resourceName,
                    targetResourceCode = targetResource.resourceCode,
                    targetResourceName = targetResource.resourceName
                )
            )
        }
        val authorizationScopes = successfulMappings
            // 同一目标资源上的多个 action 合并成一条 IAM scope，减少重复授权请求。
            .groupBy {
                ScopeKey(
                    actionRelatedResourceType = it.actionRelatedResourceType,
                    relatedResourceType = it.relatedResourceType,
                    targetResourceCode = it.targetResource.resourceCode
                )
            }.map { (key, values) ->
                buildSingleResourceScope(
                    targetProjectCode = targetProjectCode,
                    targetResource = values.first().targetResource,
                    actionRelatedResourceType = key.actionRelatedResourceType,
                    relatedResourceType = key.relatedResourceType,
                    actions = values.map { it.action }.distinct()
                )
            }
        return ScopePlanResult(
            authorizationScopes = authorizationScopes,
            details = details,
            errors = errors
        )
    }

    private fun buildIncrementalSingleResourceScopePlan(
        snapshot: GroupSnapshot,
        targetProjectCode: String,
        sourceResourceMap: Map<ResourceKey, AuthResourceInfo>,
        targetResourceIndex: Map<String, Map<String, List<AuthResourceInfo>>>,
        existingPermissionKeys: Set<TargetPermissionKey>
    ): ScopePlanResult {
        if (snapshot.singleResourcePermissions.isEmpty()) {
            return ScopePlanResult()
        }
        val details = mutableListOf<ProjectGroupMigrationDetailDTO>()
        val errors = mutableListOf<String>()
        val successfulMappings = mutableListOf<ResolvedPermission>()
        snapshot.singleResourcePermissions.forEach { permission ->
            // 与全量模式一样，仍然按“资源类型 + 资源名称”跨项目重定位资源；
            // 额外增加一步：如果目标组当前已经有这条权限，则只记明细，不再重复授权。
            val sourceKey = ResourceKey(
                resourceType = permission.relatedResourceType,
                resourceCode = permission.relatedResourceCode
            )
            val sourceResource = sourceResourceMap[sourceKey]
            if (sourceResource == null) {
                details.add(
                    ProjectGroupMigrationDetailDTO(
                        status = ProjectGroupMigrationStatus.SKIPPED,
                        action = permission.action,
                        resourceType = permission.relatedResourceType,
                        sourceResourceCode = permission.relatedResourceCode,
                        reason = "source resource not found, skipped"
                    )
                )
                return@forEach
            }
            val candidates = targetResourceIndex[permission.relatedResourceType]
                ?.get(sourceResource.resourceName)
                ?: emptyList()
            if (candidates.size != 1) {
                details.add(
                    ProjectGroupMigrationDetailDTO(
                        status = ProjectGroupMigrationStatus.SKIPPED,
                        action = permission.action,
                        resourceType = permission.relatedResourceType,
                        sourceResourceCode = permission.relatedResourceCode,
                        sourceResourceName = sourceResource.resourceName,
                        reason = when {
                            candidates.isEmpty() -> "target resource not found by name, skipped"
                            else -> "target resource matched multiple candidates by name, skipped"
                        }
                    )
                )
                return@forEach
            }
            val targetResource = candidates.single()
            val permissionKey = TargetPermissionKey(
                action = permission.action,
                actionRelatedResourceType = permission.actionRelatedResourceType,
                relatedResourceType = permission.relatedResourceType,
                relatedResourceCode = targetResource.resourceCode
            )
            // 增量接口是 additive，只补缺的，不做 revoke，也不重复发同一条 grant。
            val alreadyExists = existingPermissionKeys.contains(permissionKey)
            if (!alreadyExists) {
                successfulMappings.add(
                    ResolvedPermission(
                        action = permission.action,
                        actionRelatedResourceType = resolveActionRelatedResourceType(permission),
                        relatedResourceType = permission.relatedResourceType,
                        targetResource = targetResource
                    )
                )
            }
            details.add(
                ProjectGroupMigrationDetailDTO(
                    status = ProjectGroupMigrationStatus.SUCCESS,
                    action = permission.action,
                    resourceType = permission.relatedResourceType,
                    sourceResourceCode = permission.relatedResourceCode,
                    sourceResourceName = sourceResource.resourceName,
                    targetResourceCode = targetResource.resourceCode,
                    targetResourceName = targetResource.resourceName,
                    reason = if (alreadyExists) "permission already exists" else null
                )
            )
        }
        val authorizationScopes = successfulMappings
            .groupBy {
                ScopeKey(
                    actionRelatedResourceType = it.actionRelatedResourceType,
                    relatedResourceType = it.relatedResourceType,
                    targetResourceCode = it.targetResource.resourceCode
                )
            }.map { (key, values) ->
                buildSingleResourceScope(
                    targetProjectCode = targetProjectCode,
                    targetResource = values.first().targetResource,
                    actionRelatedResourceType = key.actionRelatedResourceType,
                    relatedResourceType = key.relatedResourceType,
                    actions = values.map { it.action }.distinct()
                )
            }
        return ScopePlanResult(
            authorizationScopes = authorizationScopes,
            details = details,
            errors = errors,
            grantablePermissionCount = successfulMappings.size
        )
    }

    private fun buildSingleResourceScope(
        targetProjectCode: String,
        targetResource: AuthResourceInfo,
        actionRelatedResourceType: String,
        relatedResourceType: String,
        actions: List<String>
    ): AuthorizationScopes {
        val projectPath = ManagerPath().apply {
            system = iamConfiguration.systemId
            id = targetProjectCode
            name = targetProjectCode
            type = AuthResourceType.PROJECT.value
        }
        val targetPath = ManagerPath().apply {
            system = iamConfiguration.systemId
            // IAM path 末级资源 ID 必须使用目标项目侧的编码，
            // 不能把源项目 relatedResourceCode 原样带过来，否则会把权限授到错误对象上。
            id = authResourceCodeConverter.code2IamCode(
                projectCode = targetProjectCode,
                resourceType = relatedResourceType,
                resourceCode = targetResource.resourceCode
            ) ?: targetResource.iamResourceCode
            name = targetResource.resourceName
            type = relatedResourceType
        }
        val resources = ManagerResources.builder()
            .system(iamConfiguration.systemId)
            .type(actionRelatedResourceType)
            .paths(listOf(listOf(projectPath, targetPath)))
            .build()
        return AuthorizationScopes().also {
            it.system = iamConfiguration.systemId
            it.actions = actions.map { action -> Action(action) }
            it.resources = listOf(resources)
        }
    }

    private fun migrateMembers(
        targetProjectCode: String,
        targetGroupId: Int,
        members: List<AuthResourceGroupMember>
    ): MemberMigrationResult {
        val details = mutableListOf<ProjectGroupMigrationDetailDTO>()
        val errors = mutableListOf<String>()
        var migratedCount = 0
        var skippedCount = 0
        val users = members.filter { it.memberType == MemberType.USER.type }
        val activeUsers = mutableListOf<AuthResourceGroupMember>()
        users.forEach { member ->
            if (deptService.isUserDeparted(member.memberId)) {
                skippedCount += 1
                details.add(
                    ProjectGroupMigrationDetailDTO(
                        status = ProjectGroupMigrationStatus.FAILED,
                        memberId = member.memberId,
                        memberType = member.memberType,
                        reason = "user departed"
                    )
                )
            } else {
                activeUsers.add(member)
            }
        }
        // user / department 两类成员支持批量加组，按 expiredTime 分桶后可以保留原过期时间，
        // 同时减少 IAM 调用次数；template 不支持批量接口，下面单独逐条处理。
        activeUsers.groupBy { it.expiredTime.timestamp() }.forEach { (expiredAt, groupedMembers) ->
            runCatching {
                permissionResourceMemberService.batchAddResourceGroupMembers(
                    projectCode = targetProjectCode,
                    iamGroupId = targetGroupId,
                    expiredTime = expiredAt,
                    members = groupedMembers.map { it.memberId },
                    departments = emptyList()
                )
                migratedCount += groupedMembers.size
            }.getOrElse {
                logger.warn("batch add users failed", it)
                errors.add("batch add users failed")
            }
        }
        val departments = members.filter { it.memberType == MemberType.DEPARTMENT.type }
        departments.groupBy { it.expiredTime.timestamp() }.forEach { (expiredAt, groupedMembers) ->
            runCatching {
                permissionResourceMemberService.batchAddResourceGroupMembers(
                    projectCode = targetProjectCode,
                    iamGroupId = targetGroupId,
                    expiredTime = expiredAt,
                    members = emptyList(),
                    departments = groupedMembers.map { it.memberId }
                )
                migratedCount += groupedMembers.size
            }.getOrElse {
                logger.warn("batch add departments failed", it)
                errors.add("batch add departments failed")
            }
        }
        val templates = members.filter { it.memberType == MemberType.TEMPLATE.type }
        templates.forEach { member ->
            runCatching {
                permissionResourceMemberService.addGroupMember(
                    projectCode = targetProjectCode,
                    memberId = member.memberId,
                    memberType = member.memberType,
                    expiredAt = member.expiredTime.timestamp(),
                    iamGroupId = targetGroupId
                )
                migratedCount += 1
            }.getOrElse {
                logger.warn("add template member failed", it)
                errors.add("add template member failed: ${member.memberId}")
                details.add(
                    ProjectGroupMigrationDetailDTO(
                        status = ProjectGroupMigrationStatus.FAILED,
                        memberId = member.memberId,
                        memberType = member.memberType,
                        reason = "add template member failed"
                    )
                )
            }
        }
        return MemberMigrationResult(
            migratedCount = migratedCount,
            skippedCount = skippedCount,
            details = details,
            errors = errors
        )
    }

    private fun createTargetGroup(
        targetProjectCode: String,
        sourceGroup: AuthResourceGroup
    ): AuthResourceGroup {
        // 迁移后的目标组需要同时满足两个条件：
        // 1. 保留源组 groupCode（例如 qc 迁到目标后仍然是 qc，而不是 custom）
        // 2. 不走按 groupCode 自动灌默认权限的模板建组链路
        // 所以这里单独创建一个“空 IAM 组 + 指定 groupCode 的本地组记录”，
        // 后续权限全部再按源项目快照回放。
        //
        // custom 组允许多个实例共享同一 groupCode，不能走 createEmptyProjectGroupWithCode 的
        // groupCode 去重逻辑，否则会把多个自定义组错误合并到同一个目标组。
        val groupAddDTO = GroupAddDTO(
            groupName = sourceGroup.groupName,
            groupDesc = sourceGroup.description ?: sourceGroup.groupName
        )
        val groupId = if (isCustomGroup(sourceGroup)) {
            permissionResourceGroupService.createGroup(
                projectId = targetProjectCode,
                groupAddDTO = groupAddDTO
            )
        } else {
            permissionResourceGroupService.createEmptyProjectGroupWithCode(
                projectId = targetProjectCode,
                groupCode = sourceGroup.groupCode,
                groupAddDTO = groupAddDTO
            )
        }
        val record = authResourceGroupDao.getByRelationId(
            dslContext = dslContext,
            projectCode = targetProjectCode,
            iamGroupId = groupId.toString()
        ) ?: error("target group record not found")
        return authResourceGroupDao.convert(record) ?: error("convert target group failed")
    }

    // 源项目（资源类型+资源id）->资源信息
    private fun buildSourceResourceMap(
        projectCode: String,
        snapshots: List<GroupSnapshot>
    ): Map<ResourceKey, AuthResourceInfo> {
        // 这里只补一层源资源元数据缓存，用来把库表里的 relatedResourceCode 解析成 resourceName，
        // 后面单资源映射统一按“资源类型 + 资源名称”去目标项目查找。
        val resourceCodesByType = snapshots.flatMap { it.singleResourcePermissions }
            .groupBy { it.relatedResourceType }
            .mapValues { (_, permissions) -> permissions.map { it.relatedResourceCode }.distinct() }
        return resourceCodesByType.flatMap { (resourceType, resourceCodes) ->
            authResourceService.listByResourceCodes(
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCodes = resourceCodes
            ).map {
                ResourceKey(resourceType = it.resourceType, resourceCode = it.resourceCode) to it
            }
        }.toMap()
    }

    // 目标项目 <资源类型 -> <资源名称 -> 目标项目里同名资源列表>>
    private fun buildTargetResourceIndex(
        projectCode: String,
        snapshots: List<GroupSnapshot>
    ): Map<String, Map<String, List<AuthResourceInfo>>> {
        // 目标项目按资源类型分页拉全量后，再按名称建索引。
        // 这样一方面能覆盖“先迁资源、后迁权限”的场景，另一方面也能保留重名检测能力。
        val resourceTypes = snapshots.flatMap { it.singleResourcePermissions }
            .map { it.relatedResourceType }
            .distinct()
        return resourceTypes.associateWith { resourceType ->
            val count = authResourceService.count(
                projectCode = projectCode,
                resourceType = resourceType,
                resourceName = null
            )
            val resources = mutableListOf<AuthResourceInfo>()
            var offset = 0
            val limit = 200
            while (offset < count) {
                resources.addAll(
                    authResourceService.list(
                        projectCode = projectCode,
                        resourceType = resourceType,
                        resourceName = null,
                        limit = limit,
                        offset = offset
                    )
                )
                offset += limit
            }
            resources.groupBy { it.resourceName }
        }
    }

    private fun snapshotGroup(
        group: AuthResourceGroup,
        includeMembers: Boolean
    ): GroupSnapshot {
        // 迁移快照完全基于当前数据库中的组、权限、成员记录，
        // 不去反推默认模板，也不从 IAM 侧重新推导，保证“以库表数据为准”。
        val permissions = authResourceGroupPermissionDao.listByGroupId(
            dslContext = dslContext,
            projectCode = group.projectCode,
            iamGroupId = group.relationId
        )
        val projectActions = extractProjectActions(
            permissions = permissions,
            projectCode = group.projectCode
        )
        val singleResourcePermissions = extractSingleResourcePermissions(
            permissions = permissions,
            projectCode = group.projectCode
        )
        val members = if (includeMembers) {
            authResourceGroupMemberDao.listResourceGroupMember(
                dslContext = dslContext,
                projectCode = group.projectCode,
                iamGroupId = group.relationId,
                minExpiredTime = LocalDateTime.now()
            )
        } else {
            emptyList()
        }
        return GroupSnapshot(
            group = group,
            projectActions = projectActions,
            singleResourcePermissions = singleResourcePermissions,
            members = members
        )
    }

    private fun listProjectGroups(projectCode: String): List<AuthResourceGroup> {
        // 明确限定迁移范围为挂在 project resource 下的项目级用户组。
        return authResourceGroupDao.getByResourceCode(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode
        ).sortedBy { it.groupName }
    }

    private fun calculateOverallStatus(
        groupResults: List<ProjectGroupMigrationGroupResultDTO>,
        topLevelErrors: List<String>
    ): ProjectGroupMigrationStatus {
        // 顶层状态只做批次级摘要：
        // - SUCCESS: 所有组都完整成功
        // - PARTIAL_SUCCESS: 至少有一组只完成了部分迁移
        // - FAILED: 整批没有任何可视为成功的结果，或有顶层硬失败
        if (topLevelErrors.isNotEmpty()) {
            return ProjectGroupMigrationStatus.FAILED
        }
        if (groupResults.isEmpty()) {
            return ProjectGroupMigrationStatus.SUCCESS
        }
        return when {
            groupResults.all { it.status == ProjectGroupMigrationStatus.SUCCESS } ->
                ProjectGroupMigrationStatus.SUCCESS
            // 只要有任何一个组不是完整成功，就把整体结果压成 PARTIAL_SUCCESS，
            // 让调用方一眼能看出这次迁移不是“全量无损复制”。
            groupResults.any { it.status == ProjectGroupMigrationStatus.SUCCESS } ->
                ProjectGroupMigrationStatus.PARTIAL_SUCCESS

            else -> ProjectGroupMigrationStatus.FAILED
        }
    }

    private fun calculateGroupStatus(
        errors: List<String>,
        details: List<ProjectGroupMigrationDetailDTO>
    ): ProjectGroupMigrationStatus {
        // 没有硬失败，但存在 skipped，说明该组已经部分迁移成功，
        // 同时也确实有权限或成员没能完整复制，所以返回 PARTIAL_SUCCESS。
        if (errors.isEmpty() && details.any { it.status == ProjectGroupMigrationStatus.SKIPPED }) {
            return ProjectGroupMigrationStatus.PARTIAL_SUCCESS
        }
        if (errors.isEmpty() && details.none { it.status == ProjectGroupMigrationStatus.FAILED }) {
            return ProjectGroupMigrationStatus.SUCCESS
        }
        if (details.any {
                it.status == ProjectGroupMigrationStatus.SUCCESS || it.status == ProjectGroupMigrationStatus.SKIPPED
            }
        ) {
            return ProjectGroupMigrationStatus.PARTIAL_SUCCESS
        }
        return ProjectGroupMigrationStatus.FAILED
    }

    private fun buildTargetPermissionSnapshot(
        projectCode: String,
        iamGroupId: Int
    ): TargetGroupPermissionSnapshot {
        // 目标侧权限仍然以本地 T_AUTH_RESOURCE_GROUP_PERMISSION 为准做差量判断。
        // 因为 grantGroupPermission() 结束后会触发 syncGroupPermissions()，这里拿到的是可重复使用的落库事实。
        val permissions = authResourceGroupPermissionDao.listByGroupId(
            dslContext = dslContext,
            projectCode = projectCode,
            iamGroupId = iamGroupId
        )
        return TargetGroupPermissionSnapshot(
            projectActions = extractProjectActions(permissions = permissions, projectCode = projectCode).toSet(),
            singleResourcePermissions = extractSingleResourcePermissions(
                permissions = permissions,
                projectCode = projectCode
            ).map {
                TargetPermissionKey(
                    action = it.action,
                    actionRelatedResourceType = resolveActionRelatedResourceType(it),
                    relatedResourceType = it.relatedResourceType,
                    relatedResourceCode = it.relatedResourceCode
                )
            }.toSet()
        )
    }

    private fun resolveActionRelatedResourceType(permission: ResourceGroupPermissionDTO): String {
        return runCatching {
            rbacCommonService.getActionInfo(permission.action).relatedResourceType
        }.getOrElse {
            logger.warn(
                "resolve action related resource type failed, fallback to snapshot value|{}|{}",
                permission.action,
                permission.actionRelatedResourceType,
                it
            )
            permission.actionRelatedResourceType
        }
    }

    private fun extractProjectActions(
        permissions: List<ResourceGroupPermissionDTO>,
        projectCode: String
    ): List<String> {
        return permissions.filter {
            it.relatedResourceType == AuthResourceType.PROJECT.value &&
                    it.relatedResourceCode == projectCode
        }.map { it.action }.distinct().sorted()
    }

    private fun extractSingleResourcePermissions(
        permissions: List<ResourceGroupPermissionDTO>,
        projectCode: String
    ): List<ResourceGroupPermissionDTO> {
        return permissions.filterNot {
            it.relatedResourceType == AuthResourceType.PROJECT.value &&
                    it.relatedResourceCode == projectCode
        }
    }

    private fun buildFailedResult(
        migrationDTO: ProjectGroupMigrationDTO,
        errors: List<String>
    ): ProjectGroupMigrationResultDTO {
        return buildFailedResult(
            sourceProjectCode = migrationDTO.sourceProjectCode,
            targetProjectCode = migrationDTO.targetProjectCode,
            dryRun = migrationDTO.dryRun,
            errors = errors
        )
    }

    private fun buildFailedResult(
        sourceProjectCode: String,
        targetProjectCode: String,
        dryRun: Boolean,
        errors: List<String>
    ): ProjectGroupMigrationResultDTO {
        return ProjectGroupMigrationResultDTO(
            sourceProjectCode = sourceProjectCode,
            targetProjectCode = targetProjectCode,
            dryRun = dryRun,
            status = ProjectGroupMigrationStatus.FAILED,
            errors = errors
        )
    }

    private fun isProtectedManagerGroup(group: AuthResourceGroup): Boolean {
        return group.groupCode == BkAuthGroup.MANAGER.value
    }

    private fun isCustomGroup(group: AuthResourceGroup): Boolean {
        return group.groupCode == CUSTOM_GROUP_CODE
    }

    private fun findExistingTargetGroup(
        snapshot: GroupSnapshot,
        targetGroups: List<AuthResourceGroup>
    ): AuthResourceGroup? {
        return if (isCustomGroup(snapshot.group)) {
            targetGroups.find { isCustomGroup(it) && it.groupName == snapshot.group.groupName }
        } else {
            targetGroups.find { it.groupCode == snapshot.group.groupCode }
        }
    }

    private data class GroupSnapshot(
        // 源组本身的信息，供目标项目重建组壳使用。
        val group: AuthResourceGroup,
        // 直接作用在整个项目上的 action 集合。
        val projectActions: List<String>,
        // 作用在具体资源实例上的权限快照，例如某条流水线、某个凭据。
        val singleResourcePermissions: List<ResourceGroupPermissionDTO>,
        // 需要迁移的成员快照，可按请求开关决定是否读取。
        val members: List<AuthResourceGroupMember>
    )

    private data class ResourceKey(
        // 用源项目 resourceType + resourceCode 精确反查源资源元数据。
        val resourceType: String,
        val resourceCode: String
    )

    private data class ResolvedPermission(
        // 具体权限动作，例如 pipeline_view / pipeline_edit。
        val action: String,
        // IAM resource type，表示 action 归属在哪类资源上。
        val actionRelatedResourceType: String,
        // 业务资源类型，例如 pipeline / credential。
        val relatedResourceType: String,
        // 已经成功映射到目标项目的资源对象。
        val targetResource: AuthResourceInfo
    )

    private data class ScopeKey(
        // 用于把同一个目标资源上的多个 action 合并成一条 IAM 授权请求。
        val actionRelatedResourceType: String,
        val relatedResourceType: String,
        val targetResourceCode: String
    )

    private data class ScopePlanResult(
        // 可以真正提交给 IAM 的单资源授权 scope。
        val authorizationScopes: List<AuthorizationScopes> = emptyList(),
        // 映射过程的逐条明细，包含 success / skipped。
        val details: List<ProjectGroupMigrationDetailDTO> = emptyList(),
        // 预留给未来的硬失败场景；当前单资源缺失主要记录在 details 里。
        val errors: List<String> = emptyList(),
        // 本次实际需要补授的单资源权限数量。
        val grantablePermissionCount: Int = 0
    )

    private data class MemberMigrationResult(
        // 实际成功迁移到目标组的成员数。
        val migratedCount: Int = 0,
        // 因离职等原因被跳过的成员数。
        val skippedCount: Int = 0,
        // 成员维度的迁移明细。
        val details: List<ProjectGroupMigrationDetailDTO> = emptyList(),
        // 成员迁移过程中的接口失败信息。
        val errors: List<String> = emptyList()
    )

    private data class TargetGroupPermissionSnapshot(
        // 目标组当前已有的项目级 action 集合。
        val projectActions: Set<String> = emptySet(),
        // 目标组当前已有的单资源权限键集合，用于增量判断“这条权限是否已经补过”。
        val singleResourcePermissions: Set<TargetPermissionKey> = emptySet()
    )

    private data class TargetPermissionKey(
        // 权限动作本身，例如 pipeline_view / pipeline_edit。
        val action: String,
        // IAM 视角下 action 挂载在哪一类资源上。
        val actionRelatedResourceType: String,
        // 业务资源类型，例如 pipeline / credential。
        val relatedResourceType: String,
        // 已经映射到目标项目后的资源 code，用它判断目标侧权限是否已存在。
        val relatedResourceCode: String
    )

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectGroupMigrationService::class.java)
        private const val CUSTOM_GROUP_CODE = "custom"
    }
}
