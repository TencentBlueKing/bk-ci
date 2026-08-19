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
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceUserResource
import com.tencent.devops.remotedev.api.service.ServiceRemoteDevResource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class CloudDesktopCreativeStreamNodePermissionStrategy(
    private val nodeTypeService: CreativeStreamNodeTypeService,
    private val client: Client
) : CreativeStreamNodePermissionStrategy {
    override val order = 200
    private val managerCache: Cache<String, Set<String>> = CacheBuilder.newBuilder()
        .maximumSize(MANAGER_CACHE_MAX_SIZE)
        .expireAfterWrite(MANAGER_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
        .build()

    override fun prefetch(projectId: String, nodeIds: Collection<Long>) {
        nodeTypeService.prefetch(projectId, nodeIds)
    }

    override fun supports(projectId: String, nodeId: Long): Boolean {
        return nodeTypeService.getNodeType(projectId, nodeId) == CreativeStreamNodeType.CLOUD_DESKTOP
    }

    override fun checkPermission(
        userId: String,
        projectId: String,
        nodeId: Long,
        permission: AuthPermission
    ): Boolean {
        if (isCloudDesktopManager(userId, projectId)) {
            return true
        }
        val workspaceName = nodeTypeService.getWorkspaceName(projectId, nodeId) ?: run {
            logger.warn(
                "Cloud desktop workspace not found|userId=$userId|projectId=$projectId|nodeId=$nodeId"
            )
            return false
        }
        // TODO: getProjectWorkspaceMembers 无批量接口且此处未缓存，列表判权时每个云桌面节点都会打一次 RPC。
        //  当前创作流节点量小可接受；节点上量或下游出现超时/限流时，按 (projectId, workspaceName) 缓存 members。
        val members = runCatching {
            client.get(ServiceRemoteDevResource::class).getProjectWorkspaceMembers(
                projectId = projectId,
                workspaceName = workspaceName
            ).data
        }.onFailure {
            logger.warn(
                "Get cloud desktop members failed|userId=$userId|projectId=$projectId|" +
                    "nodeId=$nodeId|workspaceName=$workspaceName",
                it
            )
        }.getOrNull() ?: return false

        return when {
            members.owner == userId -> true
            userId in members.viewers -> permission in VIEWER_PERMISSIONS
            else -> false
        }
    }

    private fun isCloudDesktopManager(userId: String, projectId: String): Boolean {
        val managers = runCatching {
            managerCache.get(projectId) {
                client.get(ServiceUserResource::class)
                    .getProjectUserRoles(projectId, BkAuthGroup.CGS_MANAGER)
                    .data
                    ?.toSet()
                    .orEmpty()
            }
        }.onFailure {
            logger.warn("Check cloud desktop manager failed|userId=$userId|projectId=$projectId", it)
        }.getOrDefault(emptySet())
        return userId in managers
    }

    companion object {
        private const val MANAGER_CACHE_MAX_SIZE = 1000L
        private const val MANAGER_CACHE_EXPIRE_MINUTES = 1L
        private val VIEWER_PERMISSIONS = setOf(
            AuthPermission.VIEW,
            AuthPermission.USE,
            AuthPermission.LIST
        )
        private val logger = LoggerFactory.getLogger(CloudDesktopCreativeStreamNodePermissionStrategy::class.java)
    }
}
