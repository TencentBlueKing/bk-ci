/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 */

package com.tencent.devops.remotedev.service.permission

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.ResourceTypeId
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import org.springframework.stereotype.Service

@Service
class RbacWorkspaceGroupPermissionService(
    private val client: Client,
    private val tokenService: ClientTokenService
) : WorkspaceGroupPermissionService {

    override fun validatePermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission,
        workspaceGroupId: Long?,
        message: String
    ) {
        if (!hasPermission(userId, projectId, authPermission, workspaceGroupId)) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun filterGroup(userId: String, projectId: String, authPermission: AuthPermission): List<Long> {
        val action = buildAction(authPermission)
        val codes = client.get(ServicePermissionAuthResource::class).getUserResourceByPermission(
            token = tokenService.getSystemToken(),
            userId = userId,
            projectCode = projectId,
            resourceType = ResourceTypeId.CGS_GROUP,
            action = action
        ).data ?: emptyList()
        return codes.mapNotNull { it.toLongOrNull() }
    }

    override fun filterGroups(
        userId: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<Long>> {
        val actions = authPermissions.map { buildAction(it) }
        val permissionResourcesMap = client.get(ServicePermissionAuthResource::class).getUserResourcesByPermissions(
            token = tokenService.getSystemToken(),
            userId = userId,
            projectCode = projectId,
            action = actions,
            resourceType = ResourceTypeId.CGS_GROUP
        ).data ?: emptyMap()
        // 转换字符串id为Long
        return permissionResourcesMap.mapValues { (_, v) -> v.mapNotNull { it.toLongOrNull() } }
    }

    override fun hasPermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission,
        workspaceGroupId: Long?
    ): Boolean {
        val action = buildAction(authPermission)
        val (resourceCode, resourceType) = if (workspaceGroupId == null) {
            Pair(projectId, ResourceTypeId.PROJECT)
        } else {
            Pair(workspaceGroupId.toString(), ResourceTypeId.CGS_GROUP)
        }
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            token = tokenService.getSystemToken(),
            userId = userId,
            projectCode = projectId,
            resourceType = resourceType,
            relationResourceType = null,
            action = action,
            resourceCode = resourceCode
        ).data ?: false
    }

    override fun createResource(userId: String, projectId: String, workspaceGroupId: Long, groupName: String) {
        client.get(ServicePermissionAuthResource::class).resourceCreateRelation(
            userId = userId,
            token = tokenService.getSystemToken(),
            projectCode = projectId,
            resourceType = ResourceTypeId.CGS_GROUP,
            resourceCode = workspaceGroupId.toString(),
            resourceName = groupName
        )
    }

    override fun editResource(projectId: String, workspaceGroupId: Long, groupName: String) {
        client.get(ServicePermissionAuthResource::class).resourceModifyRelation(
            token = tokenService.getSystemToken(),
            projectCode = projectId,
            resourceType = ResourceTypeId.CGS_GROUP,
            resourceCode = workspaceGroupId.toString(),
            resourceName = groupName
        )
    }

    override fun deleteResource(projectId: String, workspaceGroupId: Long) {
        client.get(ServicePermissionAuthResource::class).resourceDeleteRelation(
            token = tokenService.getSystemToken(),
            projectCode = projectId,
            resourceType = ResourceTypeId.CGS_GROUP,
            resourceCode = workspaceGroupId.toString()
        )
    }

    private fun buildAction(permission: AuthPermission): String =
        "${ResourceTypeId.CGS_GROUP.lowercase()}_${permission.value.lowercase()}"
}
