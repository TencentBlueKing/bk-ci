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

package com.tencent.devops.remotedev.resources.user

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.user.UserProjectWorkspaceResource
import com.tencent.devops.remotedev.pojo.ProjectWorkspace
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceAssign
import com.tencent.devops.remotedev.pojo.ProjectWorkspaceCreate
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.workspace.CreateControl
import com.tencent.devops.remotedev.service.workspace.DeleteControl
import com.tencent.devops.remotedev.service.workspace.DeliverControl
import org.springframework.beans.factory.annotation.Autowired

@RestResource
@Suppress("ALL")
class UserProjectWorkspaceResourceImpl @Autowired constructor(
    private val workspaceService: WorkspaceService,
    private val permissionService: PermissionService,
    private val createControl: CreateControl,
    private val deliverControl: DeliverControl,
    private val deleteControl: DeleteControl
) : UserProjectWorkspaceResource {
    override fun createWorkspace(
        userId: String,
        bkTicket: String,
        projectId: String,
        workspace: ProjectWorkspaceCreate
    ): Result<Boolean> {
        permissionService.checkUserManager(userId, projectId)
        createControl.asyncCreateWorkspace(userId, bkTicket, projectId, workspace)
        return Result(true)
    }

    override fun deleteWorkspace(userId: String, projectId: String, workspaceName: String): Result<Boolean> {
        permissionService.checkUserManager(userId, projectId)
        return Result(
            deleteControl.deleteWorkspace(
                userId = userId,
                workspaceName = workspaceName,
                needPermission = false,
                checkDeleteImmediately = true
            )
        )
    }

    override fun getWorkspaceList(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<ProjectWorkspace>> {
        permissionService.checkUserManager(userId, projectId)
        return Result(workspaceService.getProjectWorkspaceList(userId, projectId, page, pageSize))
    }

    override fun assignUser(
        userId: String,
        projectId: String,
        workspaceName: String,
        assigns: List<ProjectWorkspaceAssign>
    ): Result<Boolean> {
        permissionService.checkUserManager(userId, projectId)
        deliverControl.assignUser2Workspace(userId, projectId, workspaceName, assigns)
        return Result(true)
    }

    override fun checkManager(userId: String, projectId: String): Result<Boolean> {
        kotlin.runCatching { permissionService.checkUserManager(userId, projectId) }.fold(
            {
                return Result(true)
            },
            {
                return Result(false)
            }
        )
    }
}
