package com.tencent.devops.auth.service

import com.tencent.devops.auth.dao.AuthResourceDao
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.dao.AuthResourceGroupMemberDao
import com.tencent.devops.auth.provider.rbac.service.BkInternalPermissionComparator
import com.tencent.devops.auth.service.iam.PermissionResourceGroupPermissionService
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.util.CacheHelper
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
    // 1. 单条权限校验结果
    private val permissionCache = CacheHelper.createCache<String, Boolean>()

    // 2. 用户在项目下加入的用户组
    private val projectUserGroupCache = CacheHelper.createCache<String, List<Int>>()

    // 3. 用户按操作获取资源的缓存
    private val userResourceCache = CacheHelper.createCache<String, List<String>>()

    // 4. 用户按操作获取项目的缓存
    private val userProjectsCache = CacheHelper.createCache<String, List<String>>()

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
        action: String
    ): Boolean {
        return createTimer(::validateUserResourcePermission.name).record(Supplier {
            val (fixResourceType, fixResourceCode) =
                if (resourceType == ResourceTypeId.PROJECT || resourceCode == "*") {
                    Pair(ResourceTypeId.PROJECT, projectCode)
                } else {
                    Pair(resourceType, resourceCode)
                }
            // 缓存键直接使用传入的资源信息
            CacheHelper.getOrLoad(permissionCache, userId, projectCode, fixResourceType, fixResourceCode, action) {
                val isManager = checkManager(
                    userId = userId,
                    projectCode = projectCode,
                    action = action
                )
                if (isManager) {
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

    private fun checkManager(
        userId: String,
        projectCode: String,
        action: String
    ): Boolean {
        return superManagerService.projectManagerCheck(
            userId = userId,
            projectCode = projectCode,
            resourceType = action.substringBeforeLast("_"),
            action = action
        ) || hasManagerPermission(
            userId = userId,
            projectCode = projectCode
        )
    }

    private fun hasManagerPermission(
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
                action = action
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
            val isManager = checkManager(
                userId = userId,
                projectCode = projectCode,
                action = action
            )
            val hasProjectLevelPermission by lazy {
                validateUserResourcePermission(
                    userId = userId,
                    projectCode = projectCode,
                    resourceType = AuthResourceType.PROJECT.value,
                    resourceCode = projectCode,
                    action = action
                )
            }
            // 如果是项目管理员或者有项目级权限，直接返回结果
            if (isManager || hasProjectLevelPermission) {
                authResourceService.getResourceCodeByType(
                    dslContext = dslContext,
                    projectCode = projectCode,
                    resourceType = resourceType
                )
            } else {
                // 1. 首先获取基于权限的资源列表（会走缓存）。
                val permissionBasedResources = CacheHelper.getOrLoad(
                    userResourceCache,
                    userId, projectCode,
                    resourceType, action
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
    ): Map<String, List<String>> {
        return actions.associateWith {
            getUserResourceByAction(
                userId = userId,
                action = it,
                projectCode = projectCode,
                resourceType = resourceType
            )
        }
    }

    fun getUserProjectsByAction(
        userId: String,
        action: String
    ): List<String> {
        return (createTimer(::getUserProjectsByAction.name).record(Supplier {
            CacheHelper.getOrLoad(userProjectsCache, userId, action) {
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
            if (hasManagerPermission(userId = userId, projectCode = projectCode)) {
                return@Supplier actions.associate {
                    val authPermission = it.substringAfterLast("_")
                    AuthPermission.get(authPermission) to resourceCodes
                }
            }
            val groupIds = listMemberGroupIdsInProjectWithCache(
                projectCode = projectCode,
                userId = userId
            )
            val permissionMap = mutableMapOf<AuthPermission, List<String>>()
            actions.forEach { action ->
                val authPermission = AuthPermission.get(action.substringAfterLast("_"))
                // 若有超级管理员权限或者项目级别权限，直接返回结果。
                val superManagerPermission = superManagerService.projectManagerCheck(
                    userId = userId,
                    projectCode = projectCode,
                    resourceType = resourceType,
                    action = action
                )
                val hasProjectLevelPermission by lazy {
                    permissionResourceGroupPermissionService.isGroupsHasProjectLevelPermission(
                        projectCode = projectCode,
                        filterIamGroupIds = groupIds,
                        action = action
                    )
                }
                if (superManagerPermission || hasProjectLevelPermission) {
                    permissionMap[authPermission] = resourceCodes
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
        userId: String
    ): List<Int> {
        return CacheHelper.getOrLoad(projectUserGroupCache, projectCode, userId) {
            listMemberGroupIdsInProject(projectCode, userId)
        }
    }

    fun listMemberGroupIdsInProject(
        projectCode: String,
        memberId: String
    ): List<Int> {
        return createTimer(::listMemberGroupIdsInProject.name).record(Supplier {
            // 获取用户加入的项目级用户组模板ID
            val iamTemplateIds = listProjectMemberGroupTemplateIds(
                projectCode = projectCode,
                memberId = memberId
            )
            // 获取用户的所属组织
            val memberDeptInfos = userManageService.getUserDepartmentPath(memberId)
            authResourceGroupMemberDao.listMemberGroupIdsInProject(
                dslContext = dslContext,
                projectCode = projectCode,
                memberId = memberId,
                iamTemplateIds = iamTemplateIds,
                memberDeptInfos = memberDeptInfos
            )
        }) ?: emptyList()
    }

    private fun listProjectMemberGroupTemplateIds(
        projectCode: String,
        memberId: String
    ): List<String> {
        // 查询项目下包含该成员的组列表
        val projectGroupIds = authResourceGroupMemberDao.listResourceGroupMember(
            dslContext = dslContext,
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            memberId = memberId,
            minExpiredTime = LocalDateTime.now()
        ).map { it.iamGroupId.toString() }
        // 通过项目组ID获取人员模板ID
        return authResourceGroupDao.listByRelationId(
            dslContext = dslContext,
            projectCode = projectCode,
            iamGroupIds = projectGroupIds
        ).filter { it.iamTemplateId != null }
            .map { it.iamTemplateId.toString() }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkInternalPermissionComparator::class.java)
    }
}
