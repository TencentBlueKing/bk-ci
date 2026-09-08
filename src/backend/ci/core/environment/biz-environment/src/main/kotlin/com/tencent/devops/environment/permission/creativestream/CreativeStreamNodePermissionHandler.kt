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
 */

package com.tencent.devops.environment.permission.creativestream

import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.pojo.enums.NodeType
import org.jooq.DSLContext
import org.springframework.stereotype.Component

@Component
class CreativeStreamNodePermissionHandler(
    private val dslContext: DSLContext,
    private val nodeDao: NodeDao,
    strategies: List<CreativeStreamNodePermissionStrategy>
) {
    private val strategies = strategies.sortedBy { it.order }

    fun listNodePermissions(
        userId: String,
        projectId: String,
        nodeIds: Collection<Long>? = null,
        permissions: Set<AuthPermission>
    ): Map<AuthPermission, Set<Long>> {
        val permissionNodeIds = permissions.associateWith { mutableSetOf<Long>() }
        val candidateNodeIds = nodeIds ?: nodeDao.listNodes(
            dslContext = dslContext,
            projectId = projectId,
            nodeType = NodeType.CREATE
        ).map { it.nodeId }
        if (candidateNodeIds.isEmpty()) {
            return permissionNodeIds
        }
        // 先批量预取一次节点数据，避免后续逐节点重复查询。
        strategies.forEach { it.prefetch(projectId, candidateNodeIds) }
        candidateNodeIds.forEach { nodeId ->
            // supports 与权限无关，每个节点只解析一次策略，再对多个权限复用。
            val strategy = strategies.firstOrNull { it.supports(projectId, nodeId) } ?: return@forEach
            permissions.forEach { permission ->
                if (strategy.checkPermission(userId, projectId, nodeId, permission)) {
                    permissionNodeIds.getValue(permission).add(nodeId)
                }
            }
        }
        return permissionNodeIds
    }

    fun checkPermission(
        userId: String,
        projectId: String,
        nodeId: Long,
        permission: AuthPermission
    ): Boolean {
        val strategy = strategies.firstOrNull { it.supports(projectId, nodeId) } ?: return false
        return strategy.checkPermission(
            userId = userId,
            projectId = projectId,
            nodeId = nodeId,
            permission = permission
        )
    }
}
