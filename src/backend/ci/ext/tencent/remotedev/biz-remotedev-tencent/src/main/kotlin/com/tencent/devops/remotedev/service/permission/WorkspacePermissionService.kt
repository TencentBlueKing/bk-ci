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

interface WorkspacePermissionService {

    fun validatePermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission,
        workspaceName: String? = null,
        message: String
    )

    fun filterWorkspace(userId: String, projectId: String, authPermission: AuthPermission): List<String>

    fun filterWorkspaces(
        userId: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<String>>

    fun hasPermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission,
        workspaceName: String? = null
    ): Boolean

    fun createResource(userId: String, projectId: String, workspaceName: String, displayName: String)

    fun editResource(projectId: String, workspaceName: String, displayName: String)

    fun deleteResource(projectId: String, workspaceName: String)
}
