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
import com.tencent.devops.remotedev.pojo.RemoteDevRepository
import com.tencent.devops.remotedev.pojo.Workspace
import com.tencent.devops.remotedev.pojo.WorkspaceCreate
import com.tencent.devops.remotedev.pojo.WorkspaceDetail
import com.tencent.devops.remotedev.pojo.WorkspaceOpHistory
import com.tencent.devops.remotedev.pojo.WorkspaceResponse
import com.tencent.devops.remotedev.pojo.WorkspaceUserDetail
import com.tencent.devops.remotedev.service.GitTransferService
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.remotedev.service.redis.RedisHeartBeat
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum
import org.springframework.beans.factory.annotation.Autowired

@RestResource
@Suppress("ALL")
class UserWorkspaceResourceImpl @Autowired constructor(
    private val gitTransferService: GitTransferService,
    private val workspaceService: WorkspaceService,
    private val redisHeartBeat: RedisHeartBeat
) : UserWorkspaceResource {

    override fun createWorkspace(userId: String, workspace: WorkspaceCreate): Result<WorkspaceResponse> {
        return Result(workspaceService.createWorkspace(userId, workspace))
    }

    override fun startWorkspace(userId: String, workspaceId: Long): Result<WorkspaceResponse> {
        return Result(workspaceService.startWorkspace(userId, workspaceId))
    }

    override fun stopWorkspace(userId: String, workspaceId: Long): Result<Boolean> {
        return Result(workspaceService.stopWorkspace(userId, workspaceId))
    }

    override fun shareWorkspace(userId: String, workspaceId: Long, sharedUser: String): Result<Boolean> {
        return Result(workspaceService.shareWorkspace(userId, workspaceId, sharedUser))
    }

    override fun deleteWorkspace(userId: String, workspaceId: Long): Result<Boolean> {
        return Result(workspaceService.deleteWorkspace(userId, workspaceId))
    }

    override fun getWorkspaceList(userId: String, page: Int?, pageSize: Int?): Result<Page<Workspace>> {
        return Result(workspaceService.getWorkspaceList(userId, page, pageSize))
    }

    override fun getWorkspaceDetail(userId: String, workspaceId: Long): Result<WorkspaceDetail?> {
        return Result(workspaceService.getWorkspaceDetail(userId, workspaceId))
    }

    override fun getWorkspaceUserDetail(userId: String): Result<WorkspaceUserDetail?> {
        return Result(workspaceService.getWorkspaceUserDetail(userId))
    }

    override fun getAuthorizedGitRepository(
        userId: String,
        search: String?,
        page: Int?,
        pageSize: Int?
    ): Result<List<RemoteDevRepository>> {
        return Result(
            workspaceService.getAuthorizedGitRepository(
                userId = userId,
                search = search,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun getRepositoryBranch(
        userId: String,
        pathWithNamespace: String,
        search: String?,
        page: Int?,
        pageSize: Int?
    ): Result<List<String>> {
        return Result(
            workspaceService.getRepositoryBranch(
                userId = userId,
                pathWithNamespace = pathWithNamespace,
                search = search,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun getWorkspaceTimeline(
        userId: String,
        workspaceId: Long,
        page: Int?,
        pageSize: Int?
    ): Result<Page<WorkspaceOpHistory>> {
        return Result(
            workspaceService.getWorkspaceTimeline(
                userId = userId,
                workspaceId = workspaceId,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun checkDevfile(userId: String, pathWithNamespace: String, branch: String): Result<List<String>> {
        return Result(
            workspaceService.checkDevfile(
                userId = userId,
                pathWithNamespace = pathWithNamespace,
                branch = branch
            )
        )
    }

    override fun isOAuth(
        userId: String,
        redirectUrlType: RedirectUrlTypeEnum?,
        redirectUrl: String?,
        refreshToken: Boolean?
    ): Result<AuthorizeResult> {
        // 权限校验？
        return gitTransferService.isOAuth(
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
}
