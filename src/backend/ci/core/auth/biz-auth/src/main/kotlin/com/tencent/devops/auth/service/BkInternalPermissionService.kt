package com.tencent.devops.auth.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.auth.service.iam.PermissionManageFacadeService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupPermissionService
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * 蓝盾内部权限类，非第三方接口。
 * */
@Service
class BkInternalPermissionService(
    private val permissionManageFacadeService: PermissionManageFacadeService,
    private val permissionResourceGroupPermissionService: PermissionResourceGroupPermissionService,
    private val userManageService: UserManageService
) {
    // 初始化缓存（单条权限校验结果）
    private val permissionCache = Caffeine.newBuilder()
        .maximumSize(50000) // 最大Key数量
        .expireAfterWrite(5, TimeUnit.MINUTES) // 写入5分钟后过期
        .build<String, Boolean>() // Key类型: String, Value类型: Boolean

    // 用户在项目下加入的用户组
    private val projectCodeAndMemberId2groupIds = Caffeine.newBuilder()
        .maximumSize(50000) // 最大Key数量
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build<String, List<Int>>()

    fun validateUserResourcePermission(
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String?,
        action: String
    ): Boolean {
        // 1. 构建唯一缓存键（避免resourceCode为null时冲突）
        val cacheKey = buildCacheKey(userId, projectCode, resourceType, resourceCode, action)
        // 2. 尝试从缓存读取
        return permissionCache.get(cacheKey) {
            // 3. 缓存未命中时同步加载
            val groupIds = listMemberGroupIdsInProjectWithCache(projectCode, userId)
            if (resourceCode == null) {
                // 项目级权限校验
                permissionResourceGroupPermissionService.isGroupsHasProjectLevelPermission(
                    projectCode = projectCode,
                    filterIamGroupIds = groupIds,
                    action = action
                )
            } else {
                // 资源级权限校验
                permissionResourceGroupPermissionService.isGroupsHasPermission(
                    projectCode = projectCode,
                    filterIamGroupIds = groupIds,
                    relatedResourceType = resourceType,
                    relatedResourceCode = resourceCode,
                    action = action
                )
            }
        } ?: false // 防止加载逻辑返回null
    }

    private fun buildCacheKey(
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String?, // 可空参数
        action: String
    ): String {
        val key = listOfNotNull(userId, projectCode, resourceType, resourceCode, action)
        return DigestUtils.md5Hex(key.joinToString("|"))
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
        action: String,
        projectCode: String,
        resourceType: String
    ): List<String> {
        val groupIds = listMemberGroupIdsInProjectWithCache(projectCode, userId)
        return permissionResourceGroupPermissionService.listResourcesWithPermission(
            projectCode = projectCode,
            filterIamGroupIds = groupIds,
            relatedResourceType = resourceType,
            action = action
        )
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
        // 获取用户的所属组织
        val memberDeptInfos = userManageService.getUserDepartmentPath(userId)
        return permissionResourceGroupPermissionService.listProjectsWithPermission(
            memberIds = memberDeptInfos.toMutableList().plus(userId),
            action = action
        )
    }

    private fun listMemberGroupIdsInProjectWithCache(
        projectCode: String,
        userId: String
    ): List<Int> {
        val cacheKey = projectCode.plus("_").plus(userId)
        return projectCodeAndMemberId2groupIds.get(cacheKey) {
            permissionManageFacadeService.listMemberGroupIdsInProject(projectCode, userId)
        }
    }
}
