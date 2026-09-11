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
 */

package com.tencent.devops.environment.service.openworkspace

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.tencent.devops.environment.pojo.workspace.WorkspaceShareRoleQueryItem
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Optional
import java.util.concurrent.TimeUnit

/**
 * IEG iMate 团队共享分身角色：一律走 CDS ShareAccessRole，结果缓存 5 秒。
 */
@Service
class IegImateShareAccessService(
    private val openWorkspaceShareRoleClient: OpenWorkspaceShareRoleClient
) {
    private val roleCache: Cache<RoleCacheKey, Optional<String>> = CacheBuilder.newBuilder()
        .maximumSize(ROLE_CACHE_MAX_SIZE)
        .expireAfterWrite(ROLE_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS)
        .build()

    fun isAdmin(userId: String, workspaceName: String): Boolean {
        return getRole(
            userId = userId,
            workspaceName = workspaceName
        ).equals(ADMIN_ROLE, ignoreCase = true)
    }

    fun getRole(userId: String, workspaceName: String): String? {
        val cacheKey = RoleCacheKey(userId = userId, workspaceName = workspaceName)
        roleCache.getIfPresent(cacheKey)?.let { return it.orElse(null) }
        prefetchRoles(
            listOf(WorkspaceShareRoleQueryItem(userId = userId, workspaceName = workspaceName))
        )
        return roleCache.getIfPresent(cacheKey)?.orElse(null)
    }

    fun prefetchRoles(items: List<WorkspaceShareRoleQueryItem>) {
        val missing = items.distinct().filter { item ->
            roleCache.getIfPresent(
                RoleCacheKey(userId = item.userId, workspaceName = item.workspaceName)
            ) == null
        }
        if (missing.isEmpty()) {
            return
        }
        logger.info("Prefetch ieg imate share roles|size={}", missing.size)
        val results = openWorkspaceShareRoleClient.queryShareAccessRoles(missing)
        if (results == null) {
            logger.warn("Prefetch ieg imate share roles failed|size={}", missing.size)
            return
        }
        val resultByKey = results.associateBy {
            RoleCacheKey(userId = it.userId, workspaceName = it.workspaceName)
        }
        missing.forEach { item ->
            val cacheKey = RoleCacheKey(userId = item.userId, workspaceName = item.workspaceName)
            val role = resultByKey[cacheKey]?.role?.takeIf { it.isNotBlank() }
            roleCache.put(cacheKey, Optional.ofNullable(role))
            logger.info(
                "Got ieg imate share role|userId={}|workspaceName={}|role={}",
                item.userId,
                item.workspaceName,
                role
            )
        }
    }

    companion object {
        const val ADMIN_ROLE = "admin"
        private const val ROLE_CACHE_MAX_SIZE = 10_000L
        private const val ROLE_CACHE_EXPIRE_SECONDS = 5L
        private val logger = LoggerFactory.getLogger(IegImateShareAccessService::class.java)
    }

    private data class RoleCacheKey(
        val userId: String,
        val workspaceName: String
    )
}
