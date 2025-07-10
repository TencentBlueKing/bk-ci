package com.tencent.devops.auth.service

import com.tencent.devops.auth.provider.rbac.service.AuthResourceService
import com.tencent.devops.auth.provider.rbac.service.RbacCommonService
import com.tencent.devops.auth.provider.rbac.service.RbacPermissionPostProcessor
import com.tencent.devops.auth.service.iam.PermissionManageFacadeService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupPermissionService
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.util.CacheHelper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 蓝盾内部权限类，非第三方接口。
 * */
@Service
class BkInternalPermissionService(
    private val permissionManageFacadeService: PermissionManageFacadeService,
    private val permissionResourceGroupPermissionService: PermissionResourceGroupPermissionService,
    private val userManageService: UserManageService,
    private val rbacCommonService: RbacCommonService,
    private val superManagerService: SuperManagerService,
    private val authResourceService: AuthResourceService
) {
    // 1. 单条权限校验结果
    private val permissionCache = CacheHelper.createCache<String, Boolean>()

    // 2. 用户在项目下加入的用户组
    private val projectUserGroupCache = CacheHelper.createCache<String, List<Int>>()

    // 3. 用户按操作获取资源的缓存
    private val userResourceCache = CacheHelper.createCache<String, List<String>>()

    // 4. 用户按操作获取项目的缓存
    private val userProjectsCache = CacheHelper.createCache<String, List<String>>()

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
        val startEpoch = System.currentTimeMillis()
        try {
            val (fixResourceType, fixResourceCode) =
                if (resourceType == ResourceTypeId.PROJECT || resourceCode == "*") {
                    Pair(ResourceTypeId.PROJECT, projectCode)
                } else {
                    Pair(resourceType, resourceCode)
                }
            // 缓存键直接使用传入的资源信息
            return CacheHelper.getOrLoad(permissionCache, userId, projectCode, fixResourceType, fixResourceCode, action) {
                val isManager = checkManager(
                    userId = userId,
                    projectCode = projectCode,
                    resourceType = resourceType,
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
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to validate user resource permission local" +
                    "$userId|$action|$projectCode|$resourceType|$resourceCode"
            )
        }
    }

    private fun checkManager(
        userId: String,
        projectCode: String,
        resourceType: String,
        action: String
    ): Boolean {
        return superManagerService.projectManagerCheck(
            userId = userId,
            projectCode = projectCode,
            resourceType = resourceType,
            action = action
        ) || rbacCommonService.checkProjectManager(
            userId = userId,
            projectCode = projectCode
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

    fun getUserResourceByAction(
        userId: String,
        projectCode: String,
        resourceType: String,
        action: String
    ): List<String> {
        val startEpoch = System.currentTimeMillis()
        return try {
            val groupIds = listMemberGroupIdsInProjectWithCache(projectCode, userId)
            CacheHelper.getOrLoad(userResourceCache, userId, projectCode, resourceType, action) {
                val isManager = checkManager(
                    userId = userId,
                    projectCode = projectCode,
                    resourceType = resourceType,
                    action = action
                )
                if (isManager) {
                    authResourceService.listByProjectAndType(
                        projectCode = projectCode,
                        resourceType = resourceType
                    )
                } else {
                    permissionResourceGroupPermissionService.listResourcesWithPermission(
                        projectCode = projectCode,
                        filterIamGroupIds = groupIds,
                        relatedResourceType = resourceType,
                        action = action
                    )
                }
            }
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to get user resources local|" +
                    "$userId|$action|$projectCode|$resourceType"
            )
        }
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
        val startEpoch = System.currentTimeMillis()
        return try {
            CacheHelper.getOrLoad(userProjectsCache, userId, action) {
                val memberDeptInfos = userManageService.getUserDepartmentPath(userId)
                permissionResourceGroupPermissionService.listProjectsWithPermission(
                    memberIds = memberDeptInfos.toMutableList().plus(userId),
                    action = action
                )
            }
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to get user projects local $userId|$action"
            )
        }
    }

    fun filterUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String,
        resourceCodes: List<String>
    ): Map<AuthPermission, List<String>> {
        val startEpoch = System.currentTimeMillis()
        try {
            if (rbacCommonService.checkProjectManager(userId = userId, projectCode = projectCode)) {
                return actions.associate {
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
            return permissionMap
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to filter user resources local" +
                    "$userId|$projectCode|$resourceType|$resourceCodes|$actions"
            )
        }
    }

    private fun listMemberGroupIdsInProjectWithCache(
        projectCode: String,
        userId: String
    ): List<Int> {
        return CacheHelper.getOrLoad(projectUserGroupCache, projectCode, userId) {
            permissionManageFacadeService.listMemberGroupIdsInProject(projectCode, userId)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionPostProcessor::class.java)
    }
}
