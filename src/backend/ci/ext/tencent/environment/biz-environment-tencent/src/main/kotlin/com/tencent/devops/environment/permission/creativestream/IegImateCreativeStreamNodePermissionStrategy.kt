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

import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.environment.pojo.workspace.WorkspaceShareRoleQueryItem
import com.tencent.devops.environment.service.openworkspace.IegImateShareAccessService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class IegImateCreativeStreamNodePermissionStrategy(
    private val nodeTypeService: CreativeStreamNodeTypeService,
    private val iegImateShareAccessService: IegImateShareAccessService
) : CreativeStreamNodePermissionStrategy {
    override val order = 150

    override fun prefetch(projectId: String, nodeIds: Collection<Long>) {
        nodeTypeService.prefetch(projectId, nodeIds)
    }

    override fun prefetch(userId: String, projectId: String, nodeIds: Collection<Long>) {
        prefetch(projectId, nodeIds)
        val items = nodeIds.mapNotNull { nodeId ->
            buildQueryItem(userId, projectId, nodeId)
        }
        iegImateShareAccessService.prefetchRoles(items)
    }

    override fun supports(projectId: String, nodeId: Long): Boolean {
        return nodeTypeService.getNodeType(projectId, nodeId) == CreativeStreamNodeType.IEG_IMATE
    }

    override fun checkPermission(
        userId: String,
        projectId: String,
        nodeId: Long,
        permission: AuthPermission
    ): Boolean {
        val workspaceName = nodeTypeService.getWorkspaceName(projectId, nodeId) ?: run {
            logger.warn(
                "IEG iMate workspace not found|userId=$userId|projectId=$projectId|nodeId=$nodeId"
            )
            return false
        }
        val role = iegImateShareAccessService.getRole(
            userId = userId,
            workspaceName = workspaceName
        )
        val allowed = permission in permissionsOf(role)
        if (!allowed && permission in WRITE_PERMISSIONS) {
            logger.warn(
                "IEG iMate permission denied|userId=$userId|projectId=$projectId|" +
                    "nodeId=$nodeId|workspaceName=$workspaceName|role=$role|permission=$permission"
            )
        }
        return allowed
    }

    private fun buildQueryItem(
        userId: String,
        projectId: String,
        nodeId: Long
    ): WorkspaceShareRoleQueryItem? {
        if (!supports(projectId, nodeId)) {
            return null
        }
        val workspaceName = nodeTypeService.getWorkspaceName(projectId, nodeId) ?: return null
        return WorkspaceShareRoleQueryItem(userId = userId, workspaceName = workspaceName)
    }

    private fun permissionsOf(role: String?): Set<AuthPermission> {
        return when (role?.lowercase()) {
            IegImateShareAccessService.ADMIN_ROLE -> ADMIN_PERMISSIONS
            in MEMBER_ROLES -> MEMBER_PERMISSIONS
            else -> emptySet()
        }
    }

    companion object {
        private val MEMBER_ROLES = setOf("user", "observer", "collaborator")
        private val MEMBER_PERMISSIONS = setOf(
            AuthPermission.VIEW,
            AuthPermission.USE,
            AuthPermission.LIST
        )
        private val ADMIN_PERMISSIONS = MEMBER_PERMISSIONS + setOf(
            AuthPermission.EDIT,
            AuthPermission.DELETE
        )
        private val WRITE_PERMISSIONS = setOf(AuthPermission.EDIT, AuthPermission.DELETE)
        private val logger = LoggerFactory.getLogger(IegImateCreativeStreamNodePermissionStrategy::class.java)
    }
}
