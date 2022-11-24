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

package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.kubernetes.api.service.ServiceRemoteDevResource
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.WorkspaceReq
import com.tencent.devops.process.pojo.github.GithubAppUrl
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.pojo.Workspace
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class WorkspaceService constructor(
    private val workspaceDao: WorkspaceDao,
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val client: Client
) {
    fun getAuthorizedGitRepository(userId: String): GithubAppUrl {
        TODO("Not yet implemented")
    }

    fun createWorkspace(userId: String, workspace: Workspace): String {
        logger.info("$userId create workspace ${JsonUtil.toJson(workspace)}")

        val workspaceId = workspaceDao.createWorkspace(
            userId = userId,
            workspace = workspace,
            workspaceStatus = WorkspaceStatus.PREPARING,
            dslContext = dslContext
        )

        val workspaceName = client.get(ServiceRemoteDevResource::class).createWorkspace(userId, WorkspaceReq(
            workspaceId = workspaceId,
            name = workspace.name,
            repositoryUrl = workspace.repositoryUrl,
            branch = workspace.branch,
            devFilePath = workspace.devFilePath
        )).data

        workspaceName?.let {
            // 创建成功后，更新name
            workspaceDao.updateWorkspaceName(workspaceId, it, WorkspaceStatus.RUNNING, dslContext)
            // 获取远程登录url
            val workspaceUrl = client.get(ServiceRemoteDevResource::class).getWorkspaceUrl(userId, workspaceName).data

            return workspaceUrl!!
        }

        return ""
    }

    fun startWorkspace(userId: String, workspaceId: Long): Boolean {
        TODO("Not yet implemented")
    }

    fun shareWorkspace(userId: String, workspaceId: Long, sharedUser: String): Boolean {
        TODO("Not yet implemented")
    }

    fun deleteWorkspace(userId: String, workspaceId: Long): Boolean {
        TODO("Not yet implemented")
    }

    fun getWorkspaceList(userId: String): Workspace {
        TODO("Not yet implemented")
    }

    fun getWorkspaceDetail(userId: String, workspaceId: Long): Workspace {
        TODO("Not yet implemented")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceService::class.java)
    }
}
