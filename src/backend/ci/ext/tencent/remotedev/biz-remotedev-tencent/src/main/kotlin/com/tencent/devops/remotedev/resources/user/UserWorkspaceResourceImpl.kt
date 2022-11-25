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
import com.tencent.devops.process.pojo.github.GithubAppUrl
import com.tencent.devops.remotedev.api.user.UserWorkspaceResource
import com.tencent.devops.remotedev.pojo.Workspace
import com.tencent.devops.remotedev.pojo.WorkspaceDetail
import com.tencent.devops.remotedev.pojo.WorkspaceOpHistory
import com.tencent.devops.remotedev.service.GitTransferService
import com.tencent.devops.remotedev.service.WorkspaceService
import com.tencent.devops.repository.pojo.AuthorizeResult
import com.tencent.devops.repository.pojo.enums.RedirectUrlTypeEnum

@RestResource
@Suppress("ALL")
class UserWorkspaceResourceImpl constructor(
    val gitTransferService: GitTransferService,
    val workspaceService: WorkspaceService
) : UserWorkspaceResource {

    override fun getAuthorizedGitRepository(userId: String): Result<GithubAppUrl> {
        TODO("Not yet implemented")
    }

    override fun createWorkspace(userId: String, workspace: Workspace): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override fun startWorkspace(userId: String, workspaceId: Long): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override fun shareWorkspace(userId: String, workspaceId: Long, sharedUser: String): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override fun deleteWorkspace(userId: String, workspaceId: Long): Result<Boolean> {
        TODO("Not yet implemented")
    }

    override fun getWorkspaceList(userId: String, page: Int?, pageSize: Int?): Result<Page<Workspace>> {
        TODO("Not yet implemented")
    }

    override fun getWorkspaceDetail(userId: String, workspaceId: Long): Result<WorkspaceDetail?> {
        return Result(workspaceService.getWorkspaceDetail(userId, workspaceId))
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

    override fun isOAuth(
        userId: String,
        redirectUrlType: RedirectUrlTypeEnum?,
        redirectUrl: String?,
        gitProjectId: Long,
        refreshToken: Boolean?
    ): Result<AuthorizeResult> {
        // 权限校验？
        return gitTransferService.isOAuth(
            userId = userId,
            redirectUrlType = redirectUrlType,
            redirectUrl = redirectUrl,
            gitProjectId = gitProjectId,
            refreshToken = refreshToken
        )
    }
}
