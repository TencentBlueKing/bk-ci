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

import com.tencent.devops.common.auth.api.AuthPermission

interface WorkspaceGroupPermissionService {

    fun validatePermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission,
        workspaceGroupId: Long? = null,
        message: String
    )

    fun filterGroup(userId: String, projectId: String, authPermission: AuthPermission): List<Long>

    fun filterGroups(
        userId: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<Long>>

    fun hasPermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission,
        workspaceGroupId: Long? = null
    ): Boolean

    fun createResource(userId: String, projectId: String, workspaceGroupId: Long, groupName: String)

    fun editResource(projectId: String, workspaceGroupId: Long, groupName: String)

    fun deleteResource(projectId: String, workspaceGroupId: Long)
}
