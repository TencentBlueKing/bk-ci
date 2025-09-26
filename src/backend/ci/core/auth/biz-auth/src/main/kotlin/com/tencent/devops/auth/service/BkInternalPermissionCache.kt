package com.tencent.devops.auth.service

import com.github.benmanes.caffeine.cache.Cache
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.util.CacheHelper

/**
 * 蓝盾内部权限缓存管理类
 * 封装了所有内部权限相关的缓存创建、加载和销毁逻辑
 */
object BkInternalPermissionCache {

    // 1. 单条权限校验结果缓存
    private val permissionCache: Cache<String, Boolean> = CacheHelper.createCache(100000)

    // 2. 用户在项目下加入的用户组缓存
    private val projectUserGroupCache: Cache<String, List<Int>> = CacheHelper.createCache()

    // 3. 用户按操作获取资源的缓存
    private val userResourceCache: Cache<String, List<String>> = CacheHelper.createCache()

    // 4. 用户按操作获取项目的缓存
    private val userProjectsCache: Cache<String, List<String>> = CacheHelper.createCache()

    // region 权限校验缓存 (Permission Cache)
    fun getOrLoadPermission(
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        action: String,
        loader: () -> Boolean
    ): Boolean {
        return CacheHelper.getOrLoad(
            permissionCache, userId, projectCode, resourceType, resourceCode, action,
            loader = loader
        )
    }

    fun invalidatePermission(
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        action: String
    ) {
        val cacheKey = CacheHelper.buildCacheKey(userId, projectCode, resourceType, resourceCode, action)
        permissionCache.invalidate(cacheKey)
    }

    // region 项目用户组缓存 (Project User Group Cache)
    fun getOrLoadProjectUserGroups(
        projectCode: String,
        userId: String,
        enableTemplateInvalidationOnUserExpiry: Boolean?,
        loader: () -> List<Int>
    ): List<Int> {
        return CacheHelper.getOrLoad(
            projectUserGroupCache, projectCode, userId,
            enableTemplateInvalidationOnUserExpiry, loader = loader
        )
    }

    fun invalidateProjectUserGroups(
        projectCode: String,
        userId: String
    ) {
        projectUserGroupCache.invalidate(CacheHelper.buildCacheKey(projectCode, userId, true))
        projectUserGroupCache.invalidate(CacheHelper.buildCacheKey(projectCode, userId, false))
        projectUserGroupCache.invalidate(CacheHelper.buildCacheKey(projectCode, userId, null))
    }

    fun batchInvalidateProjectUserGroups(
        projectCode: String,
        userIds: List<String>
    ) {
        userIds.forEach { userId ->
            projectUserGroupCache.invalidate(CacheHelper.buildCacheKey(projectCode, userId, true))
            projectUserGroupCache.invalidate(CacheHelper.buildCacheKey(projectCode, userId, false))
            projectUserGroupCache.invalidate(CacheHelper.buildCacheKey(projectCode, userId, null))
        }
    }

    // 用户资源缓存 (User Resource Cache)
    fun getOrLoadUserResources(
        userId: String,
        projectCode: String,
        resourceType: String,
        action: String,
        loader: () -> List<String>
    ): List<String> {
        return CacheHelper.getOrLoad(userResourceCache, userId, projectCode, resourceType, action, loader = loader)
    }

    fun invalidateUserResources(
        userId: String,
        projectCode: String,
        resourceType: String,
        action: String
    ) {
        invalidatePermission(
            userId = userId,
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode,
            action = action
        )
        val cacheKey = CacheHelper.buildCacheKey(userId, projectCode, resourceType, action)
        userResourceCache.invalidate(cacheKey)
    }

    // 用户项目缓存 (User Projects Cache)
    fun getOrLoadUserProjects(
        userId: String,
        action: String,
        loader: () -> List<String>
    ): List<String> {
        return CacheHelper.getOrLoad(userProjectsCache, userId, action, loader = loader)
    }

    fun invalidateUserProjects(
        userId: String,
        action: String
    ) {
        val cacheKey = CacheHelper.buildCacheKey(userId, action)
        userProjectsCache.invalidate(cacheKey)
    }
    // endregion

    /**
     * 销毁（清空）所有内部权限相关的缓存
     */
    fun invalidateAll() {
        permissionCache.invalidateAll()
        projectUserGroupCache.invalidateAll()
        userResourceCache.invalidateAll()
        userProjectsCache.invalidateAll()
    }
}
