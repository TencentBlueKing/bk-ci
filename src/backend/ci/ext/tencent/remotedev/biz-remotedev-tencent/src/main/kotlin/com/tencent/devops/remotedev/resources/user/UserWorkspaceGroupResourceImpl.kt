/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 */

package com.tencent.devops.remotedev.resources.user

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.user.UserWorkspaceGroupResource
import com.tencent.devops.remotedev.pojo.WorkspaceGroup
import com.tencent.devops.remotedev.pojo.WorkspaceGroupAssignment
import com.tencent.devops.remotedev.pojo.WorkspaceGroupCreate
import com.tencent.devops.remotedev.pojo.WorkspaceGroupItem
import com.tencent.devops.remotedev.pojo.WorkspaceGroupUpdate
import com.tencent.devops.remotedev.service.WorkspaceGroupService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
@Suppress("ALL")
class UserWorkspaceGroupResourceImpl @Autowired constructor(
    private val workspaceGroupService: WorkspaceGroupService
) : UserWorkspaceGroupResource {

    override fun getGroups(userId: String, projectId: String): Result<List<WorkspaceGroup>> {
        return Result(workspaceGroupService.listGroups(userId, projectId))
    }

    override fun addGroup(userId: String, group: WorkspaceGroupCreate): Result<Boolean> {
        return Result(workspaceGroupService.createGroup(userId, group))
    }

    override fun updateGroup(userId: String, group: WorkspaceGroupUpdate): Result<Boolean> {
        return Result(workspaceGroupService.updateGroup(userId, group))
    }

    override fun deleteGroup(userId: String, projectId: String, groupId: Long): Result<Boolean> {
        return Result(workspaceGroupService.deleteGroup(userId, projectId, groupId))
    }

    override fun addWorkspaces(userId: String, groupId: Long, assignment: WorkspaceGroupAssignment): Result<Boolean> {
        return Result(
            workspaceGroupService.addWorkspaces(
                userId = userId,
                projectId = assignment.projectId,
                groupId = groupId,
                names = assignment.workspaceNames
            )
        )
    }

    override fun removeWorkspace(
        userId: String,
        projectId: String,
        groupId: Long,
        workspaceName: String
    ): Result<Boolean> {
        return Result(workspaceGroupService.removeWorkspace(userId, projectId, groupId, workspaceName))
    }

    override fun listWorkspaces(
        userId: String,
        projectId: String,
        groupId: Long,
        page: Int,
        pageSize: Int
    ): Result<Page<WorkspaceGroupItem>> {
        return Result(workspaceGroupService.listWorkspaces(userId, projectId, groupId, page, pageSize))
    }
}
