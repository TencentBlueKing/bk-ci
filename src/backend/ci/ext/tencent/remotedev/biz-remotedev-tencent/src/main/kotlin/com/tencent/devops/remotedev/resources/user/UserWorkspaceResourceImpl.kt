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
import com.tencent.devops.remotedev.api.user.UserWorkspaceResource
import com.tencent.devops.remotedev.pojo.BkTicketInfo
import com.tencent.devops.remotedev.pojo.RemoteDevGitType
import com.tencent.devops.remotedev.pojo.RemoteDevRepository
import com.tencent.devops.remotedev.pojo.Workspace
import com.tencent.devops.remotedev.pojo.WorkspaceCreate
import com.tencent.devops.remotedev.pojo.WorkspaceDetail
import com.tencent.devops.remotedev.pojo.WorkspaceOpHistory
import com.tencent.devops.remotedev.pojo.WorkspaceResponse
import com.tencent.devops.remotedev.pojo.WorkspaceUserDetail
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.redis.RedisHeartBeat
import com.tencent.devops.remotedev.service.transfer.RemoteDevGitTransfer
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import org.springframework.beans.factory.annotation.Autowired

@RestResource
@Suppress("ALL")
class UserWorkspaceResourceImpl @Autowired constructor(
    private val gitTransfer: RemoteDevGitTransfer,
    private val workspaceService: WorkspaceService,
    private val redisHeartBeat: RedisHeartBeat,
    private val permissionService: PermissionService
) : UserWorkspaceResource {

    override fun createWorkspace(
        userId: String,
        bkTicket: String,
        workspace: WorkspaceCreate
    ): Result<WorkspaceResponse> {
        return Result(workspaceService.createWorkspace(userId, bkTicket, workspace))
    }

    override fun startWorkspace(
        userId: String,
        bkTicket: String,
        workspaceName: String
    ): Result<WorkspaceResponse> {
        return Result(workspaceService.startWorkspace(userId, bkTicket, workspaceName))
    }

    override fun stopWorkspace(userId: String, workspaceName: String): Result<Boolean> {
        return Result(workspaceService.stopWorkspace(userId, workspaceName))
    }

    override fun shareWorkspace(userId: String, workspaceName: String, sharedUser: String): Result<Boolean> {
        return Result(workspaceService.shareWorkspace(userId, workspaceName, sharedUser))
    }

    override fun deleteWorkspace(userId: String, workspaceName: String): Result<Boolean> {
        return Result(workspaceService.deleteWorkspace(userId, workspaceName))
    }

    override fun getWorkspaceList(userId: String, page: Int?, pageSize: Int?): Result<Page<Workspace>> {
        return Result(workspaceService.getWorkspaceList(userId, page, pageSize))
    }

    override fun getWorkspaceDetail(userId: String, workspaceName: String): Result<WorkspaceDetail?> {
        return Result(workspaceService.getWorkspaceDetail(userId, workspaceName))
    }

    override fun getWorkspaceUserDetail(userId: String): Result<WorkspaceUserDetail?> {
        return Result(workspaceService.getWorkspaceUserDetail(userId))
    }

    override fun getAuthorizedGitRepository(
        userId: String,
        search: String?,
        page: Int?,
        pageSize: Int?,
        gitType: RemoteDevGitType
    ): Result<List<RemoteDevRepository>> {
        return Result(
            workspaceService.getAuthorizedGitRepository(
                userId = userId,
                search = search,
                page = page,
                pageSize = pageSize,
                gitType = gitType
            )
        )
    }

    override fun getRepositoryBranch(
        userId: String,
        pathWithNamespace: String,
        gitType: RemoteDevGitType
    ): Result<List<String>> {
        return Result(
            workspaceService.getRepositoryBranch(
                userId = userId,
                pathWithNamespace = pathWithNamespace,
                gitType = gitType
            )
        )
    }

    override fun getWorkspaceTimeline(
        userId: String,
        workspaceName: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<WorkspaceOpHistory>> {
        return Result(
            workspaceService.getWorkspaceTimeline(
                userId = userId,
                workspaceName = workspaceName,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun checkDevfile(
        userId: String,
        pathWithNamespace: String,
        branch: String,
        gitType: RemoteDevGitType
    ): Result<List<String>> {
        return Result(
            workspaceService.checkDevfile(
                userId = userId,
                pathWithNamespace = pathWithNamespace,
                branch = branch,
                gitType = gitType
            )
        )
    }

    override fun isOAuth(
        userId: String,
        redirectUrlType: RedirectUrlTypeEnum?,
        redirectUrl: String?,
        refreshToken: Boolean?,
        gitType: RemoteDevGitType
    ): Result<AuthorizeResult> {
        // 权限校验？
        return gitTransfer.load(gitType).isOAuth(
            userId = userId,
            redirectUrlType = redirectUrlType,
            redirectUrl = redirectUrl,
            refreshToken = refreshToken
        )
    }

    override fun workspaceHeartbeat(userId: String, workspaceName: String): Result<Boolean> {
        redisHeartBeat.refreshHeartbeat(workspaceName)
        return Result(true)
    }

    override fun checkUserPermission(userId: String, workspaceName: String): Result<Boolean> {
        return Result(permissionService.checkUserPermission(userId, workspaceName))
    }

    override fun checkUserCreate(userId: String): Result<Boolean> {
        return Result(workspaceService.checkUserCreate(userId))
    }

    override fun updateBkTicket(userId: String, bkTicketInfo: BkTicketInfo): Result<Boolean> {
        workspaceService.updateBkTicket(userId, bkTicketInfo.bkTicket, bkTicketInfo.hostName)
        return Result(true)
    }

    override fun checkUpdate(userId: String): Result<String> {
        return Result(workspaceService.checkUpdate(userId))
    }
}
