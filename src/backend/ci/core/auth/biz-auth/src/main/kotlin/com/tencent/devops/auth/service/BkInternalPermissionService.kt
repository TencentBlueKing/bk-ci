package com.tencent.devops.auth.service

import com.tencent.devops.auth.dao.AuthResourceDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.dao.AuthResourceGroupMemberDao
import com.tencent.devops.auth.provider.rbac.service.BkInternalPermissionReconciler
import com.tencent.devops.auth.service.iam.PermissionResourceGroupPermissionService
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.ResourceTypeId
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.function.Supplier

/**
 * 蓝盾内部权限类，非第三方接口。
 * */
@Service
class BkInternalPermissionService(
    private val dslContext: DSLContext,
    private val userManageService: UserManageService,
    private val authResourceGroupMemberDao: AuthResourceGroupMemberDao,
    private val authResourceGroupDao: AuthResourceGroupDao,
    private val permissionResourceGroupPermissionService: PermissionResourceGroupPermissionService,
    private val superManagerService: SuperManagerService,
    private val authResourceService: AuthResourceDao,
    private val meterRegistry: MeterRegistry
) {
    private fun createTimer(methodName: String): Timer {
        return Timer.builder("rbac.internal.service.duration") // 指标名称
            .description("Duration of BkInternalPermissionService methods")
            .tag("class", "BkInternalPermissionService") // 静态标签：类名
            .tag("method", methodName) // 动态标签：方法名
            .register(meterRegistry)
    }

    /**
     *   校验项目级权限，如校验是否有项目管理权限或者整个项目流水线权限，参数如下：
     *   resourceType:project、resourceCode:projectCode或者resourceType:pipeline、resourceCode:* 均可
     *   action:pipeline_execute/project_manage
     *   校验具体资源级权限，如校验是否有某条流水线权限，参数如下：
     *   resourceType:pipeline、resourceCode:p-1、action: pipeline_execute
     * */
    fun validateUserResourcePermission(
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        action: String,
        enableSuperManagerCheck: Boolean = true
    ): Boolean {
        return createTimer(::validateUserResourcePermission.name).record(Supplier {
            val (fixResourceType, fixResourceCode) = buildFixResourceTypeAndCode(
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode
            )
            // 调用BkInternalPermissionCache处理缓存
            BkInternalPermissionCache.getOrLoadPermission(
                userId = userId,
                projectCode = projectCode,
                resourceType = fixResourceType,
                resourceCode = fixResourceCode,
                action = action
            ) {
                val isProjectOrSuperManager = checkProjectOrSuperManager(
                    userId = userId,
                    projectCode = projectCode,
                    action = action,
                    enableSuperManagerCheck = enableSuperManagerCheck
                )
                if (isProjectOrSuperManager) {
                    true
                } else {
                    val groupIds = listMemberGroupIdsInProjectWithCache(projectCode, userId)
                    permissionResourceGroupPermissionService.isGroupsHasPermission(
                        projectCode = projectCode,
                        filterIamGroupIds = groupIds,
                        relatedResourceType = fixResourceType,
                        relatedResourceCode = fixResourceCode,
                        action = action
                    )
                }
            }
        }) ?: false
    }

    fun buildFixResourceTypeAndCode(
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): Pair<String, String> {
        return if (resourceType == ResourceTypeId.PROJECT || resourceCode == "*") {
            Pair(ResourceTypeId.PROJECT, projectCode)
        } else {
            Pair(resourceType, resourceCode)
        }
    }

    private fun checkProjectOrSuperManager(
        userId: String,
        projectCode: String,
        action: String,
        enableSuperManagerCheck: Boolean
    ): Boolean {
        // 首先检查最高权限（超级管理员）
        if (enableSuperManagerCheck) {
            val isSuperManager = superManagerService.projectManagerCheck(
                userId = userId,
                projectCode = projectCode,
                resourceType = action.substringBeforeLast("_"),
                action = action
            )
            if (isSuperManager) {
                return true // 如果是超级管理员，立即返回 true
            }
        }

        // 如果不是超级管理员，再检查是否为项目管理员
        return checkProjectManager(
            projectCode = projectCode,
            userId = userId
        )
    }

    private fun checkProjectManager(
        userId: String,
        projectCode: String
    ): Boolean {
        return authResourceGroupMemberDao.checkResourceManager(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = ResourceTypeId.PROJECT,
            resourceCode = projectCode,
            memberId = userId
        )
    }

    fun batchValidateUserResourcePermission(
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        actions: List<String>
    ): Map<String, Boolean> {
        return actions.associateWith { action ->
            validateUserResourcePermission(
                userId = userId,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCode = resourceCode,
                action = action,
                enableSuperManagerCheck = false
            )
        }
    }

    /**
     * 获取用户在指定项目下对某资源类型的有特定操作权限的资源ID列表。
     */
    fun getUserResourceByAction(
        userId: String,
        projectCode: String,
        resourceType: String,
        action: String
    ): List<String> {
        val resources = createTimer(::getUserResourceByAction.name).record(Supplier {
            val hasProjectLevelPermission = validateUserResourcePermission(
                userId = userId,
                projectCode = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = projectCode,
                action = action
            )

            // 如果有项目级权限，直接返回结果
            if (hasProjectLevelPermission) {
                authResourceService.getResourceCodeByType(
                    dslContext = dslContext,
                    projectCode = projectCode,
                    resourceType = resourceType
                )
            } else {
                // 1. 首先获取基于权限的资源列表（会走缓存）。
                val permissionBasedResources = BkInternalPermissionCache.getOrLoadUserResources(
                    userId = userId,
                    projectCode = projectCode,
                    resourceType = resourceType,
                    action = action
                ) {
                    val groupIds = listMemberGroupIdsInProjectWithCache(projectCode, userId)
                    permissionResourceGroupPermissionService.listResourcesWithPermission(
                        projectCode = projectCode,
                        filterIamGroupIds = groupIds,
                        relatedResourceType = resourceType,
                        action = action
                    )
                }

                // 2. 然后获取用户自己创建的资源列表（实时获取，不缓存）。
                val userCreatedResources = authResourceService.list(
                    dslContext = dslContext,
                    projectCode = projectCode,
                    resourceType = resourceType,
                    createUser = userId
                )
                (permissionBasedResources + userCreatedResources).distinct()
            }
        }) ?: emptyList()
        return resources
    }

    fun getUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String
    ): Map<AuthPermission, List<String>> {
        return actions.associate {
            val actionResourceList = getUserResourceByAction(
                userId = userId,
                action = it,
                projectCode = projectCode,
                resourceType = resourceType
            )
            val authPermission = it.substringAfterLast("_")
            AuthPermission.get(authPermission) to actionResourceList
        }
    }

    fun getUserProjectsByAction(
        userId: String,
        action: String
    ): List<String> {
        return (createTimer(::getUserProjectsByAction.name).record(Supplier {
            BkInternalPermissionCache.getOrLoadUserProjects(userId, action) {
                val memberDeptInfos = userManageService.getUserDepartmentPath(userId)
                permissionResourceGroupPermissionService.listProjectsWithPermission(
                    memberIds = memberDeptInfos.toMutableList().plus(userId),
                    action = action
                )
            }
        }) ?: emptyList()).also {
            if (logger.isDebugEnabled) {
                logger.debug("get user projects by action Internal :$userId|$action|$it")
            }
        }
    }

    fun filterUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String,
        resourceCodes: List<String>
    ): Map<AuthPermission, List<String>> {
        return createTimer(::filterUserResourcesByActions.name).record(Supplier {
            // 过滤掉已删除的资源
            val enabledResourceCodes = authResourceService.listByResourceCodes(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = resourceType,
                resourceCodes = resourceCodes,
            ).map { it.resourceCode }
            val permissionMap = mutableMapOf<AuthPermission, List<String>>()
            actions.forEach { action ->
                val authPermission = AuthPermission.get(action.substringAfterLast("_"))
                val hasProjectLevelPermission = validateUserResourcePermission(
                    userId = userId,
                    projectCode = projectCode,
                    resourceType = ResourceTypeId.PROJECT,
                    resourceCode = projectCode,
                    action = action
                )

                if (hasProjectLevelPermission) {
                    permissionMap[authPermission] = enabledResourceCodes
                    return@forEach
                }
                // 否则获取用户有权限的操作，然后进行过滤
                val userResources = getUserResourceByAction(
                    userId = userId,
                    projectCode = projectCode,
                    resourceType = resourceType,
                    action = action
                )
                permissionMap[authPermission] = resourceCodes.filter { userResources.contains(it) }
            }
            permissionMap
        }) ?: emptyMap()
    }

    fun listMemberGroupIdsInProjectWithCache(
        projectCode: String,
        userId: String,
        enableTemplateInvalidationOnUserExpiry: Boolean? = null
    ): List<Int> {
        // 调用BkInternalPermissionCache处理缓存
        return BkInternalPermissionCache.getOrLoadProjectUserGroups(
            projectCode = projectCode,
            userId = userId,
            enableTemplateInvalidationOnUserExpiry
        ) {
            // 获取用户的所属组织
            val memberDeptInfos = userManageService.getUserDepartmentPath(userId)
            // 查询项目下包含该成员及所属组织的用户组列表
            val projectGroupIds = authResourceGroupMemberDao.listResourceGroupMember(
                dslContext = dslContext,
                projectCode = projectCode,
                resourceType = AuthResourceType.PROJECT.value,
                memberIds = memberDeptInfos + userId,
                minExpiredTime = if (enableTemplateInvalidationOnUserExpiry == true) LocalDateTime.now() else null
            ).map { it.iamGroupId.toString() }
            // 通过项目组ID获取人员模板ID
            val iamTemplateIds = authResourceGroupDao.listByRelationId(
                dslContext = dslContext,
                projectCode = projectCode,
                iamGroupIds = projectGroupIds
            ).filter { it.iamTemplateId != null }
                .map { it.iamTemplateId.toString() }
            authResourceGroupMemberDao.listMemberGroupIdsInProject(
                dslContext = dslContext,
                projectCode = projectCode,
                memberId = userId,
                iamTemplateIds = iamTemplateIds,
                memberDeptInfos = memberDeptInfos
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkInternalPermissionReconciler::class.java)
    }
}
