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

package com.tencent.devops.environment.permission.creativestream

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.support.api.service.ServiceIMateResource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class IMateCreativeStreamNodePermissionStrategy(
    private val nodeTypeService: CreativeStreamNodeTypeService,
    private val client: Client
) : CreativeStreamNodePermissionStrategy {
    override val order = 100
    private val authorizationCache: Cache<AuthorizationCacheKey, Boolean> = CacheBuilder.newBuilder()
        .maximumSize(AUTHORIZATION_CACHE_MAX_SIZE)
        .expireAfterWrite(AUTHORIZATION_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
        .build()

    override fun prefetch(projectId: String, nodeIds: Collection<Long>) {
        nodeTypeService.prefetch(projectId, nodeIds)
    }

    override fun supports(projectId: String, nodeId: Long): Boolean {
        return nodeTypeService.getNodeType(projectId, nodeId) == CreativeStreamNodeType.IMATE
    }

    override fun checkPermission(
        userId: String,
        projectId: String,
        nodeId: Long,
        permission: AuthPermission
    ): Boolean {
        if (nodeTypeService.getCreatedUser(projectId, nodeId) == userId) {
            return true
        }
        val workspaceName = nodeTypeService.getWorkspaceName(projectId, nodeId) ?: run {
            logger.warn("iMate workspace not found|userId=$userId|projectId=$projectId|nodeId=$nodeId")
            return false
        }
        val cacheKey = AuthorizationCacheKey(userId = userId, clientUuid = workspaceName)
        // TODO: 仅缓存 authorized=true，未授权用户列表判权时每个 iMate 节点仍会逐个打 checkAuthorization RPC。
        //  当前创作流节点量小可接受；节点上量时给负结果也加上（更短 TTL）缓存，避免逐节点远程调用。
        if (authorizationCache.getIfPresent(cacheKey) == true) {
            return permission in AUTHORIZED_PERMISSIONS
        }
        val authorized = runCatching {
            client.get(ServiceIMateResource::class).checkAuthorization(
                username = userId,
                clientUuid = workspaceName
            )
        }.onFailure {
            logger.warn(
                "Check iMate authorization failed|userId=$userId|projectId=$projectId|" +
                    "nodeId=$nodeId|workspaceName=$workspaceName",
                it
            )
        }.getOrNull()?.data?.authorized ?: false
        if (authorized) {
            authorizationCache.put(cacheKey, true)
        }
        return authorized && permission in AUTHORIZED_PERMISSIONS
    }

    companion object {
        private const val AUTHORIZATION_CACHE_MAX_SIZE = 10_000L
        private const val AUTHORIZATION_CACHE_EXPIRE_MINUTES = 1L
        private val AUTHORIZED_PERMISSIONS = setOf(
            AuthPermission.VIEW,
            AuthPermission.USE,
            AuthPermission.LIST
        )
        private val logger = LoggerFactory.getLogger(IMateCreativeStreamNodePermissionStrategy::class.java)
    }

    private data class AuthorizationCacheKey(
        val userId: String,
        val clientUuid: String
    )
}
