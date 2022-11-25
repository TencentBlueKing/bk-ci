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

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.kubernetes.api.service.ServiceRemoteDevResource
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.WorkspaceReq
import com.tencent.devops.process.pojo.github.GithubAppUrl
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.pojo.Workspace
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceDetail
import com.tencent.devops.remotedev.pojo.WorkspaceOpHistory
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

@Service
class WorkspaceService constructor(
    private val workspaceDao: WorkspaceDao,
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val workspaceHistoryDao: WorkspaceHistoryDao,
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao,
    private val gitTransferService: GitTransferService,
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceService::class.java)
    }

    fun getAuthorizedGitRepository(userId: String, search: String?, page: Int?, pageSize: Int?): List<GithubAppUrl> {
        logger.info("$userId get user git repository")
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        return gitTransferService.getProjectList(
            userId = userId,
            page = pageNotNull,
            pageSize = pageSizeNotNull,
            search = search,
            owned = false,
            minAccessLevel = GitAccessLevelEnum.DEVELOPER
        )
    }

    fun createWorkspace(userId: String, workspace: Workspace): String {
        logger.info("$userId create workspace ${JsonUtil.toJson(workspace)}")

        val workspaceId = workspaceDao.createWorkspace(
            userId = userId, workspace = workspace, workspaceStatus = WorkspaceStatus.PREPARING, dslContext = dslContext
        )

        val workspaceName = client.get(ServiceRemoteDevResource::class).createWorkspace(
            userId,
            WorkspaceReq(
                workspaceId = workspaceId,
                name = workspace.name,
                repositoryUrl = workspace.repositoryUrl,
                branch = workspace.branch,
                devFilePath = workspace.devFilePath
            )
        ).data

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

    fun getWorkspaceList(userId: String, page: Int?, pageSize: Int?): Page<Workspace> {
        logger.info("$userId get user workspace list")
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        val count = workspaceDao.countWorkspace(dslContext, userId)
        val result = workspaceDao.limitFetchWorkspace(
            dslContext = dslContext,
            userId = userId,
            limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        ) ?: emptyList()

        return Page(
            page = pageNotNull, pageSize = pageSizeNotNull, count = count,
            records = result.map {
                val status = WorkspaceStatus.values()[it.status]
                Workspace(
                    workspaceId = it.id,
                    name = it.name,
                    repositoryUrl = it.url,
                    branch = it.branch,
                    devFilePath = it.yamlPath,
                    wsTemplateId = it.templateId,
                    status = status,
                    lastStatusUpdateTime = it.lastStatusUpdateTime.timestamp()
                )
            }
        )
    }

    fun getWorkspaceDetail(userId: String, workspaceId: Long): WorkspaceDetail? {
        logger.info("$userId get workspace from id $workspaceId")
        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceId = workspaceId) ?: return null

        val workspaceStatus = WorkspaceStatus.values()[workspace.status]

        val history = workspaceHistoryDao.fetchHistory(dslContext, workspaceId)

        val last = history.firstOrNull() ?: return null

        // TODO: 2022/11/24 优惠时间需后续配置
        val discountTime = 0

        val usageTime = history.sumOf { Duration.between(it.startTime, it.endTime).seconds }.run {
            this + if (workspaceStatus.isRunning()) {
                // 如果正在运行，需要加上该次启动距离当前的时间
                Duration.between(last.startTime, LocalDateTime.now()).seconds
            } else 0
        }

        val sleepingTime = history.sumOf { it.lastSleepTimeCost }.run {
            this + if (workspaceStatus.isSleeping()) {
                // 如果正在休眠，需要加上距离上次结束距离的时间
                Duration.between(last.endTime, LocalDateTime.now()).seconds
            } else 0
        }
        val chargeableTime = usageTime - discountTime

        return with(workspace) {
            WorkspaceDetail(
                workspaceId = id,
                name = name,
                status = workspaceStatus,
                lastUpdateTime = updateTime.timestamp(),
                chargeableTime = chargeableTime,
                usageTime = usageTime,
                sleepingTime = sleepingTime,
                cpu = cpu,
                memory = memory,
                disk = disk,
                yaml = yaml
            )
        }
    }

    fun getWorkspaceTimeline(
        userId: String,
        workspaceId: Long,
        page: Int?,
        pageSize: Int?
    ): Page<WorkspaceOpHistory> {
        logger.info("$userId get workspace time line from id $workspaceId")
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        val count = workspaceOpHistoryDao.countOpHistory(dslContext, workspaceId)
        val result = workspaceOpHistoryDao.limitFetchOpHistory(
            dslContext = dslContext,
            workspaceId = workspaceId,
            limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        )

        return Page(
            page = pageNotNull, pageSize = pageSizeNotNull, count = count,
            records = result.map {
                WorkspaceOpHistory(
                    createdTime = it.createdTime.timestamp(),
                    operator = it.operator,
                    action = WorkspaceAction.values()[it.action],
                    actionMessage = it.actionMsg
                )
            }
        )
    }
}
